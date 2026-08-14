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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.agent.AgentParser
import com.example.engine.agent.AgentStepStatus
import com.example.util.ReasoningParser

/**
 * Chat message item representing either a user prompt or an AI response bubble.
 * Integrates ThinkingProcessCard for reasoning traces (<think>...</think>)
 * and AgentToolExecutionCard for Agent Tool Calls (<tool_call>...</tool_call>).
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
        // User Message Bubble - Sleek Glassmorphic Surface
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.End
        ) {
            Surface(
                color = Color(0xFF6366F1).copy(alpha = 0.85f),
                shape = RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp),
                border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.2f)),
                modifier = Modifier.widthIn(max = 300.dp)
            ) {
                Text(
                    text = content,
                    fontSize = 13.sp,
                    color = Color.White,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    } else {
        // AI / Agent Response Message Bubble with Thinking and Tool Calls support
        val parsedReasoning = ReasoningParser.parse(content)
        val parsedAgent = AgentParser.parse(parsedReasoning.finalAnswerText)

        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            // Sender Badge for Agent or Special Model
            if (sender.equals("Agent", ignoreCase = true)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
                ) {
                    AgentActiveStatusPill(
                        isRunning = isStreaming,
                        label = if (isStreaming) "Agent Executing" else "Autonomous Agent"
                    )
                }
            }

            // 1. Thinking / Reasoning Process Card (Only shown if thinkingText is present or streaming)
            if (parsedReasoning.thinkingText.isNotEmpty() || parsedReasoning.isCurrentlyThinking) {
                ThinkingProcessCard(
                    thinkingText = parsedReasoning.thinkingText,
                    isCurrentlyThinking = parsedReasoning.isCurrentlyThinking,
                    isThinkingFinished = parsedReasoning.isThinkingFinished
                )
                Spacer(modifier = Modifier.height(6.dp))
            }

            // 2. Agent Tool Calls (Rendered as Glassmorphic Tool Cards)
            if (parsedAgent.toolCalls.isNotEmpty()) {
                parsedAgent.toolCalls.forEach { toolCall ->
                    AgentToolExecutionCard(
                        toolCall = toolCall,
                        status = if (isStreaming) AgentStepStatus.IN_PROGRESS else AgentStepStatus.COMPLETED,
                        observation = "Executed action for ${toolCall.filename ?: toolCall.workspaceName ?: "workspace"}"
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }

            // 3. Main AI Response Content
            val displayText = parsedAgent.cleanText.ifBlank {
                if (parsedAgent.toolCalls.isEmpty() && parsedReasoning.thinkingText.isEmpty()) {
                    parsedReasoning.finalAnswerText
                } else {
                    ""
                }
            }

            if (displayText.isNotEmpty() || (!parsedReasoning.isCurrentlyThinking && parsedAgent.toolCalls.isEmpty())) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.7f)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.12f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        val segments = parseResponseSegments(displayText)
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
                                    color = Color.White.copy(alpha = 0.95f),
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
