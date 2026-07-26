package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.data.db.ChatHistoryEntity
import com.example.data.db.ChatSessionEntity
import com.example.data.db.ChatMessageEntity
import com.example.data.db.AgentLogEntity
import com.example.data.db.ModelProfileEntity
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import com.example.util.DiagnosticUtil
import com.example.ui.components.DiagnosticPanel
import androidx.compose.material.icons.filled.Analytics
import com.example.engine.agent.AgentStepStatus
import com.example.engine.inference.GenerationProgress

data class CodeSegment(
    val isCodeBlock: Boolean,
    val text: String,
    val language: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAssistantScreen(
    selectedModel: ModelProfileEntity?,
    generationProgress: GenerationProgress?,
    isGenerating: Boolean,
    chatHistory: List<ChatHistoryEntity> = emptyList(),
    chatSessions: List<ChatSessionEntity> = emptyList(),
    activeSessionId: Long? = null,
    sessionMessages: List<ChatMessageEntity> = emptyList(),
    sessionAgentLogs: List<AgentLogEntity> = emptyList(),
    projectFilePaths: List<String> = listOf("index.html", "style.css", "script.js"),
    agentState: AgentState? = null,
    onSendPrompt: (String) -> Unit,
    onRunAutonomousAgent: (String) -> Unit = {},
    onCancelAgent: () -> Unit = {},
    onSelectSession: (Long) -> Unit = {},
    onCreateNewSession: (String) -> Unit = {},
    onRenameSession: (ChatSessionEntity, String) -> Unit = { _, _ -> },
    onDeleteSession: (ChatSessionEntity) -> Unit = {},
    onExportSession: (ChatSessionEntity) -> Unit = {},
    onApplyCodeToWorkspace: (String, String, Boolean) -> Unit = { _, _, _ -> }
) {
    var promptInput by remember { mutableStateOf("") }
    var isAgentMode by remember { mutableStateOf(true) }
    var showHistorySheet by remember { mutableStateOf(false) }
    var sessionToRename by remember { mutableStateOf<ChatSessionEntity?>(null) }
    var renameInputText by remember { mutableStateOf("") }
    var showDiagnosticPanel by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val activeSessionName = remember(chatSessions, activeSessionId) {
        chatSessions.find { it.sessionId == activeSessionId }?.sessionName ?: "Chat Session"
    }

    val diagnosticState = remember(generationProgress, isGenerating, showDiagnosticPanel) {
        DiagnosticUtil.getDiagnosticState(
            context = context,
            speedTokensPerSec = generationProgress?.speedTokensPerSec ?: (if (isGenerating) 18.5f else 0.0f),
            tokensGenerated = generationProgress?.tokensGenerated ?: 0,
            modelSizeBytes = selectedModel?.sizeBytes ?: 1_680_000_000L,
            modelName = selectedModel?.name ?: "Gemma-2B-Q4_K_M.gguf"
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(12.dp)
    ) {
        // Session History Header Bar
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(onClick = { showHistorySheet = true }) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "History Sessions",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = activeSessionName,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = {
                            val currentSession = chatSessions.find { it.sessionId == activeSessionId }
                            if (currentSession != null) {
                                onExportSession(currentSession)
                            } else {
                                Toast.makeText(context, "No active session to export", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(imageVector = Icons.Default.FileDownload, contentDescription = "Export Session JSON", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Export", fontSize = 11.sp)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    OutlinedButton(
                        onClick = { onCreateNewSession("New Chat Session") },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "New Chat", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("New Chat", fontSize = 11.sp)
                    }
                }
            }
        }

        // Mode Switcher: Auto Agent Workflow vs Standard Code Prompt
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            SegmentedButton(
                selected = isAgentMode,
                onClick = { isAgentMode = true },
                shape = SegmentedButtonDefaults.itemShape(0, 2)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "Agent", modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Auto Agent Workflow", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            SegmentedButton(
                selected = !isAgentMode,
                onClick = { isAgentMode = false },
                shape = SegmentedButtonDefaults.itemShape(1, 2)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Code, contentDescription = "Prompt", modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Standard Prompt", fontSize = 11.sp)
                }
            }
        }

        // 1. Active Model Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Memory, contentDescription = "Model", tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = selectedModel?.name ?: "Gemma-2B-it-Q4_K_M.gguf",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "OFFLINE GGUF • ${selectedModel?.quantType ?: "Q4_K_M"} • ${selectedModel?.parameters ?: "2.5B"}",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    AssistChip(
                        onClick = { showDiagnosticPanel = !showDiagnosticPanel },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Analytics,
                                contentDescription = "Diagnostics",
                                tint = if (showDiagnosticPanel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        label = { Text(if (showDiagnosticPanel) "Hide Panel" else "Diagnostics", fontSize = 10.sp) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (showDiagnosticPanel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                            labelColor = if (showDiagnosticPanel) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.secondary
                        )
                    )
                }
            }
        }

        if (showDiagnosticPanel || isGenerating) {
            Spacer(modifier = Modifier.height(8.dp))
            DiagnosticPanel(
                diagnosticState = diagnosticState,
                isGenerating = isGenerating
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 2. Autonomous Agent Planning Card (ReAct Engine)
        if (agentState != null && (agentState.isRunning || agentState.steps.isNotEmpty())) {
            AgentPlanningCard(
                agentState = agentState,
                onCancelAgent = onCancelAgent
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // 3. Active Generation Progress Box
        if (generationProgress != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isGenerating) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Complete", tint = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = generationProgress.statusText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Speed, contentDescription = "Speed", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.width(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${"%.1f".format(generationProgress.speedTokensPerSec)} t/s",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    if (isGenerating) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }

                    if (generationProgress.rawLogText.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(90.dp)
                                .background(Color(0xFF090D16), shape = RoundedCornerShape(8.dp))
                                .padding(8.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = generationProgress.rawLogText,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        // 3. Quick Prompt Pills
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                "✨ Scientific Calculator",
                "🌙 Add Dark Mode Toggle",
                "🎮 Canvas Brick Game",
                "🌤️ Weather Dashboard",
                "📝 Kanban Task Manager",
                "🎨 SVG Paint Canvas",
                "📱 Make Layout Responsive"
            ).forEach { pill ->
                AssistChip(
                    onClick = { promptInput = pill.replace("✨ ", "").replace("🌙 ", "").replace("🎮 ", "").replace("🌤️ ", "").replace("📝 ", "").replace("🎨 ", "").replace("📱 ", "") },
                    label = { Text(pill, fontSize = 11.sp) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 4. Conversation & Code Output History (with One-Tap Code Injection Parser)
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (sessionMessages.isNotEmpty()) {
                items(sessionMessages) { msg ->
                    if (msg.sender == "User") {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "User Prompt",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = msg.content,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                val segments = parseResponseSegments(msg.content)
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
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                items(chatHistory) { chat ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "Prompt", tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = chat.prompt,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Parse response text for markdown code blocks
                            val segments = parseResponseSegments(chat.aiResponse)
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
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "⚡ Generated ${chat.tokensGenerated} tokens @ ${chat.speedTokensPerSec} t/s via ${chat.modelUsed}",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }

        // History Navigation Drawer Sheet
        if (showHistorySheet) {
            ModalBottomSheet(
                onDismissRequest = { showHistorySheet = false },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "History",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Saved Chat Sessions",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Button(
                            onClick = {
                                onCreateNewSession("New Chat Session")
                                showHistorySheet = false
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "New", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New Session", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (chatSessions.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No saved chat history sessions yet.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(chatSessions) { session ->
                                val isSelected = session.sessionId == activeSessionId
                                val dateStr = remember(session.lastModified) {
                                    java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault())
                                        .format(java.util.Date(session.lastModified))
                                }

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    onClick = {
                                        onSelectSession(session.sessionId)
                                        showHistorySheet = false
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Chat,
                                                contentDescription = "Session",
                                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = session.sessionName,
                                                    fontSize = 13.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = dateStr,
                                                    fontSize = 10.sp,
                                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                                )
                                            }
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(onClick = {
                                                onExportSession(session)
                                            }) {
                                                Icon(
                                                    imageVector = Icons.Default.FileDownload,
                                                    contentDescription = "Export Session JSON",
                                                    modifier = Modifier.size(18.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            IconButton(onClick = {
                                                sessionToRename = session
                                                renameInputText = session.sessionName
                                            }) {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = "Rename",
                                                    modifier = Modifier.size(18.dp),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            IconButton(onClick = {
                                                onDeleteSession(session)
                                            }) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete",
                                                    modifier = Modifier.size(18.dp),
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Rename Session Dialog
        if (sessionToRename != null) {
            AlertDialog(
                onDismissRequest = { sessionToRename = null },
                title = { Text("Rename Session", fontSize = 14.sp, fontWeight = FontWeight.Bold) },
                text = {
                    OutlinedTextField(
                        value = renameInputText,
                        onValueChange = { renameInputText = it },
                        label = { Text("Session Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        val session = sessionToRename
                        if (session != null && renameInputText.isNotBlank()) {
                            onRenameSession(session, renameInputText.trim())
                        }
                        sessionToRename = null
                    }) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { sessionToRename = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 5. Prompt Bar Input
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = promptInput,
                onValueChange = { promptInput = it },
                placeholder = { Text("Prompt offline GGUF model...", fontSize = 12.sp) },
                modifier = Modifier.weight(1f),
                singleLine = false,
                maxLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            Button(
                onClick = {
                    if (promptInput.isNotBlank() && !isGenerating) {
                        if (isAgentMode) {
                            onRunAutonomousAgent(promptInput)
                        } else {
                            onSendPrompt(promptInput)
                        }
                        promptInput = ""
                    }
                },
                enabled = promptInput.isNotBlank() && !isGenerating,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(52.dp)
            ) {
                Icon(imageVector = Icons.Default.Send, contentDescription = "Generate")
            }
        }
    }
}

@Composable
fun AgentPlanningCard(
    agentState: AgentState,
    onCancelAgent: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Agent Engine",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Autonomous ReAct Agent",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (agentState.isRunning) {
                    OutlinedButton(
                        onClick = onCancelAgent,
                        modifier = Modifier.height(28.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Cancel Agent", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                } else if (agentState.isCancelled) {
                    AssistChip(
                        onClick = {},
                        label = { Text("Cancelled", fontSize = 10.sp) },
                        colors = AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    )
                } else {
                    AssistChip(
                        onClick = {},
                        label = { Text("Completed", fontSize = 10.sp) },
                        colors = AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Goal: ${agentState.userGoal}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Step Checklist
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                agentState.steps.forEach { step ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val (icon, color) = when (step.status) {
                            AgentStepStatus.COMPLETED -> "🟢" to Color(0xFF10B981)
                            AgentStepStatus.IN_PROGRESS -> "🟡" to Color(0xFFF59E0B)
                            AgentStepStatus.FAILED -> "🔴" to Color(0xFFEF4444)
                            AgentStepStatus.PENDING -> "⚪" to Color(0xFF94A3B8)
                        }

                        Text(text = icon, fontSize = 11.sp)
                        Spacer(modifier = Modifier.width(6.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Step ${step.stepIndex}: ${step.thought}",
                                fontSize = 11.sp,
                                fontWeight = if (step.status == AgentStepStatus.IN_PROGRESS) FontWeight.Bold else FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (step.observation.isNotBlank()) {
                                Text(
                                    text = "→ ${step.observation}",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = agentState.statusMessage,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Composable
fun CodeInjectionBlockView(
    code: String,
    language: String,
    projectFilePaths: List<String>,
    onApply: (String, String, Boolean) -> Unit
) {
    val defaultTarget = resolveTargetFileName(language, projectFilePaths)
    var selectedTargetFile by remember { mutableStateOf(defaultTarget) }
    var isAppendMode by remember { mutableStateOf(false) }
    var isDropdownExpanded by remember { mutableStateOf(false) }

    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = "Code Block",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = language.uppercase().ifEmpty { "CODE" },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Target File Resolver Picker
                Box {
                    OutlinedButton(
                        onClick = { isDropdownExpanded = true },
                        modifier = Modifier.height(28.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Description, contentDescription = "File", modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(selectedTargetFile, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }

                    DropdownMenu(
                        expanded = isDropdownExpanded,
                        onDismissRequest = { isDropdownExpanded = false }
                    ) {
                        val allOptions = (projectFilePaths + listOf("index.html", "style.css", "script.js", "main.py")).distinct()
                        allOptions.forEach { path ->
                            DropdownMenuItem(
                                text = { Text(path, fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                                onClick = {
                                    selectedTargetFile = path
                                    isDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Code Content Preview Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .background(Color(0xFF030712), shape = RoundedCornerShape(6.dp))
                    .padding(8.dp)
                    .verticalScroll(rememberScrollState())
                    .horizontalScroll(rememberScrollState())
            ) {
                Text(
                    text = code,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF38BDF8)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Option Selector & Floating Action Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mode Toggle: Overwrite vs Append
                SingleChoiceSegmentedButtonRow(modifier = Modifier.height(30.dp)) {
                    SegmentedButton(
                        selected = !isAppendMode,
                        onClick = { isAppendMode = false },
                        shape = SegmentedButtonDefaults.itemShape(0, 2)
                    ) {
                        Text("Overwrite", fontSize = 9.sp)
                    }
                    SegmentedButton(
                        selected = isAppendMode,
                        onClick = { isAppendMode = true },
                        shape = SegmentedButtonDefaults.itemShape(1, 2)
                    ) {
                        Text("Append", fontSize = 9.sp)
                    }
                }

                Button(
                    onClick = { onApply(selectedTargetFile, code, isAppendMode) },
                    modifier = Modifier.height(32.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Apply", modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Apply to Workspace", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

fun parseResponseSegments(rawText: String): List<CodeSegment> {
    val list = mutableListOf<CodeSegment>()
    val codeBlockRegex = Regex("```([a-zA-Z0-9_-]*)\\s*\\n([\\s\\S]*?)```")
    var lastIdx = 0

    for (match in codeBlockRegex.findAll(rawText)) {
        if (match.range.first > lastIdx) {
            val precedingText = rawText.substring(lastIdx, match.range.first)
            if (precedingText.isNotBlank()) {
                list.add(CodeSegment(false, precedingText))
            }
        }
        val lang = match.groupValues[1].lowercase().trim()
        val code = match.groupValues[2].trimEnd()
        list.add(CodeSegment(true, code, lang))
        lastIdx = match.range.last + 1
    }

    if (lastIdx < rawText.length) {
        val remaining = rawText.substring(lastIdx)
        if (remaining.isNotBlank()) {
            list.add(CodeSegment(false, remaining))
        }
    }

    if (list.isEmpty() && rawText.isNotBlank()) {
        list.add(CodeSegment(false, rawText))
    }

    return list
}

fun resolveTargetFileName(language: String, projectFilePaths: List<String>): String {
    val langLower = language.lowercase()
    val candidate = when {
        langLower.contains("html") || langLower.contains("htm") -> "index.html"
        langLower.contains("css") -> "style.css"
        langLower.contains("js") || langLower.contains("javascript") || langLower.contains("ts") -> "script.js"
        langLower.contains("python") || langLower.contains("py") -> "main.py"
        langLower.contains("json") -> "data.json"
        langLower.contains("md") || langLower.contains("markdown") -> "README.md"
        else -> "script.js"
    }

    return projectFilePaths.find { it.equals(candidate, ignoreCase = true) }
        ?: projectFilePaths.find { it.endsWith(candidate.substringAfter('.'), ignoreCase = true) }
        ?: candidate
}
