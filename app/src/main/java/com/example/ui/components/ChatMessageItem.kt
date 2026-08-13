package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.ReasoningParser

/**
 * Chat message item representing either a user prompt or an AI response bubble.
 * Integrates ThinkingProcessCard for reasoning traces (<think>...</think>).
 *
 * @param sender Sender indicator (e.g., "User", "AI", "Agent").
 * @param content Raw content text.
 * @param isUser Message direction boolean.
 * @param isStreaming True if AI message is currently streaming.
 * @param projectFilePaths List of active project files for workspace code injection.
 * @param onApplyCodeToWorkspace Callback to apply code block snippets to project files.
 * @param modifier Custom layout modifier.
 */
@Composable
fun ChatMessageItem(
    sender: String,
    content: String,
    isUser: Boolean,
    isStreaming: Boolean = false,
    projectFilePaths: List<String> = emptyList(),
    onApplyCodeToWorkspace: (String, String, Boolean) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    if (isUser) {
        // User Message Bubble
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.End
        ) {
            Surface(
                color = androidx.compose.ui.graphics.Color(0xFF6366F1).copy(alpha = 0.85f),
                shape = RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp),
                border = BorderStroke(0.5.dp, androidx.compose.ui.graphics.Color.White.copy(alpha = 0.2f)),
                modifier = Modifier.widthIn(max = 300.dp)
            ) {
                Text(
                    text = content,
                    fontSize = 13.sp,
                    color = androidx.compose.ui.graphics.Color.White,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    } else {
        // AI Response Message Bubble with Thinking/Reasoning support
        val parsed = ReasoningParser.parse(content)

        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            // 1. Thinking / Reasoning Process Card (Only shown if thinkingText is present or streaming)
            if (parsed.thinkingText.isNotEmpty() || parsed.isCurrentlyThinking) {
                ThinkingProcessCard(
                    thinkingText = parsed.thinkingText,
                    isCurrentlyThinking = parsed.isCurrentlyThinking,
                    isThinkingFinished = parsed.isThinkingFinished
                )
                Spacer(modifier = Modifier.height(6.dp))
            }

            // 2. Main AI Response Content (Display finalAnswerText below thinking card)
            if (parsed.finalAnswerText.isNotEmpty() || !parsed.isCurrentlyThinking) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color(0xFF1E293B).copy(alpha = 0.7f)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(0.5.dp, androidx.compose.ui.graphics.Color.White.copy(alpha = 0.12f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        val segments = parseResponseSegments(parsed.finalAnswerText)
                        segments.forEach { segment ->
                            if (segment.isCodeBlock) {
                                CodeInjectionBlockView(
                                    code = segment.text,
                                    language = segment.language,
                                    projectFilePaths = projectFilePaths,
                                    onApply = { targetFile, codeSnippet, isAppend ->
                                        onApplyCodeToWorkspace(targetFile, codeSnippet, isAppend)
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        Toast.makeText(context, "Successfully updated $targetFile", Toast.LENGTH_SHORT).show()
                                    }
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                            } else {
                                Text(
                                    text = segment.text,
                                    fontSize = 13.sp,
                                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.95f),
                                    lineHeight = 18.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
