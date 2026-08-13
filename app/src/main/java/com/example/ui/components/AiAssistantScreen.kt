package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material.icons.filled.Stop
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.ime
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Description
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.DisposableEffect
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.FileOpen
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
import com.example.engine.agent.AgentState
import com.example.engine.inference.GenerationProgress

import com.example.engine.inference.AiProviderMode
import com.example.engine.inference.AiProviderSettings

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
    allModels: List<ModelProfileEntity> = emptyList(),
    chatHistory: List<ChatHistoryEntity> = emptyList(),
    chatSessions: List<ChatSessionEntity> = emptyList(),
    activeSessionId: Long? = null,
    sessionMessages: List<ChatMessageEntity> = emptyList(),
    sessionAgentLogs: List<AgentLogEntity> = emptyList(),
    projectFilePaths: List<String> = listOf("index.html", "style.css", "script.js"),
    agentState: AgentState? = null,
    aiProviderSettings: AiProviderSettings = AiProviderSettings(),
    onSendPrompt: (String) -> Unit,
    onRunAutonomousAgent: (String) -> Unit = {},
    onCancelAgent: () -> Unit = {},
    onSelectSession: (Long) -> Unit = {},
    onCreateNewSession: (String) -> Unit = {},
    onRenameSession: (ChatSessionEntity, String) -> Unit = { _, _ -> },
    onDeleteSession: (ChatSessionEntity) -> Unit = {},
    onExportSession: (ChatSessionEntity) -> Unit = {},
    onApplyCodeToWorkspace: (String, String, Boolean) -> Unit = { _, _, _ -> },
    onSelectModel: (Long) -> Unit = {},
    onImportGgufFile: (android.net.Uri) -> Unit = {}
) {
    var promptInput by remember { mutableStateOf("") }
    var isAgentMode by remember { mutableStateOf(true) }
    var showHistorySheet by remember { mutableStateOf(false) }
    var sessionToRename by remember { mutableStateOf<ChatSessionEntity?>(null) }
    var renameInputText by remember { mutableStateOf("") }
    var showDiagnosticPanel by remember { mutableStateOf(false) }
    var isThinkModeEnabled by remember { mutableStateOf(true) }
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    var selectedImageUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var ttsEngine by remember { mutableStateOf<TextToSpeech?>(null) }
    var isSpeaking by remember { mutableStateOf(false) }

    DisposableEffect(context) {
        var tts: TextToSpeech? = null
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = java.util.Locale.US
                ttsEngine = tts
            }
        }
        onDispose {
            tts?.stop()
            tts?.shutdown()
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            Toast.makeText(context, "Image attached", Toast.LENGTH_SHORT).show()
        }
    }

    val latestAiResponseText = remember(sessionMessages, chatHistory) {
        sessionMessages.lastOrNull { it.sender != "User" }?.content
            ?: chatHistory.lastOrNull()?.aiResponse
            ?: ""
    }

    val toggleTtsSpeak = {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        if (isSpeaking) {
            ttsEngine?.stop()
            isSpeaking = false
            Toast.makeText(context, "Stopped reading aloud", Toast.LENGTH_SHORT).show()
        } else {
            val parsed = parseThoughtAndContent(latestAiResponseText)
            val textToRead = parsed.mainContent.ifBlank { "No message to read aloud" }
            if (ttsEngine != null && latestAiResponseText.isNotBlank()) {
                ttsEngine?.speak(textToRead, TextToSpeech.QUEUE_FLUSH, null, "TTS_MSG_ID")
                isSpeaking = true
                Toast.makeText(context, "Reading message aloud...", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "No AI response available to speak", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val localGgufPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            onImportGgufFile(uri)
        }
    }

    val sheetState = rememberModalBottomSheetState()

    val activeSessionName = remember(chatSessions, activeSessionId) {
        chatSessions.find { it.sessionId == activeSessionId }?.sessionName ?: "Chat Session"
    }

    val listState = rememberLazyListState()
    val isImeVisible = WindowInsets.ime.asPaddingValues().calculateBottomPadding() > 0.dp

    LaunchedEffect(sessionMessages.size, isImeVisible) {
        if (sessionMessages.isNotEmpty()) {
            listState.animateScrollToItem(sessionMessages.size - 1)
        }
    }

    val diagnosticState = remember(generationProgress, isGenerating, showDiagnosticPanel) {
        DiagnosticUtil.getDiagnosticState(
            context = context,
            speedTokensPerSec = generationProgress?.speedTokensPerSec ?: (if (isGenerating) 18.5f else 0.0f),
            tokensGenerated = generationProgress?.tokensGenerated ?: 0,
            modelSizeBytes = selectedModel?.sizeBytes ?: 0L,
            modelName = selectedModel?.name ?: "No Local Model"
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .padding(12.dp)
    ) {
        // Glass Top App Bar Header Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.65f)),
            border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showHistorySheet = true }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = activeSessionName.ifBlank { "Chat" },
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Session History",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = selectedModel?.name ?: "gemma-4-E2B-it-Q4_K_M.gguf",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        maxLines = 1
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { localGgufPickerLauncher.launch(arrayOf("*/*")) }) {
                        Icon(imageVector = Icons.Default.AttachFile, contentDescription = "Import GGUF", tint = Color.White.copy(alpha = 0.8f))
                    }
                    IconButton(onClick = { showDiagnosticPanel = !showDiagnosticPanel }) {
                        Icon(imageVector = Icons.Default.Analytics, contentDescription = "Diagnostics", tint = if (showDiagnosticPanel) Color(0xFF818CF8) else Color.White.copy(alpha = 0.8f))
                    }
                    IconButton(onClick = { onCreateNewSession("New Chat Session") }) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "New Chat", tint = Color(0xFF6366F1))
                    }
                }
            }
        }

        if (showDiagnosticPanel || isGenerating) {
            Spacer(modifier = Modifier.height(8.dp))
            DiagnosticPanel(
                diagnosticState = diagnosticState,
                isGenerating = isGenerating,
                selectedModel = selectedModel,
                generationProgress = generationProgress
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
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.7f)),
                border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(16.dp)
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

        // 4. Conversation & Code Output History
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (sessionMessages.isNotEmpty()) {
                items(sessionMessages) { msg ->
                    val isUser = msg.sender.equals("User", ignoreCase = true)
                    ChatMessageItem(
                        sender = msg.sender,
                        content = msg.content,
                        isUser = isUser,
                        projectFilePaths = projectFilePaths,
                        onApplyCodeToWorkspace = onApplyCodeToWorkspace
                    )
                }
            } else {
                items(chatHistory) { chat ->
                    ChatMessageItem(
                        sender = "User",
                        content = chat.prompt,
                        isUser = true,
                        projectFilePaths = projectFilePaths,
                        onApplyCodeToWorkspace = onApplyCodeToWorkspace
                    )
                    ChatMessageItem(
                        sender = "AI",
                        content = chat.aiResponse,
                        isUser = false,
                        projectFilePaths = projectFilePaths,
                        onApplyCodeToWorkspace = onApplyCodeToWorkspace
                    )
                }
            }

            if (isGenerating) {
                item {
                    val rawLog = generationProgress?.rawLogText?.ifBlank { "Analyzing prompt and generating reasoning steps..." } ?: "Thinking through response..."
                    val parsedStream = com.example.util.ReasoningParser.parse(rawLog)
                    ThinkingProcessCard(
                        thinkingText = if (parsedStream.thinkingText.isNotEmpty()) parsedStream.thinkingText else rawLog,
                        isCurrentlyThinking = true,
                        isThinkingFinished = false
                    )
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
                                    com.example.util.DateUtils.formatSessionTimestamp(session.lastModified)
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
                                                imageVector = Icons.AutoMirrored.Filled.Chat,
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

        // Quick Action Suggestion Chips above bottom input bar
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val chipsList = listOf(
                "✨ Scientific Calculator",
                "🌙 Add Dark Mode Toggle",
                "🎮 Canvas Brick Game",
                "🌤️ Weather Dashboard",
                "📝 Kanban Task Manager",
                "🎨 SVG Paint Canvas",
                "📱 Make Layout Responsive"
            )
            items(chipsList) { pill ->
                AssistChip(
                    onClick = {
                        promptInput = pill.replace("✨ ", "")
                            .replace("🌙 ", "")
                            .replace("🎮 ", "")
                            .replace("🌤️ ", "")
                            .replace("📝 ", "")
                            .replace("🎨 ", "")
                            .replace("📱 ", "")
                    },
                    label = { Text(pill, fontSize = 11.sp, color = Color.White, maxLines = 1) },
                    shape = CircleShape,
                    border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f)),
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = Color(0xFF1E293B).copy(alpha = 0.6f),
                        labelColor = Color.White
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 5. Modern Floating Bottom Input Capsule
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding(),
            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.45f)),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.15f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                if (selectedImageUri != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF6366F1).copy(alpha = 0.3f),
                        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.2f)),
                        modifier = Modifier.padding(bottom = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = "Attached Image",
                                tint = Color(0xFF818CF8),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Image attached",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            )
                            IconButton(
                                onClick = { selectedImageUri = null },
                                modifier = Modifier.size(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove Image",
                                    tint = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = promptInput,
                    onValueChange = { promptInput = it },
                    placeholder = {
                        Text(
                            text = "Type your message here...",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    },
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 14.sp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                    maxLines = 4,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = { imagePickerLauncher.launch("image/*") },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Attachment",
                                tint = if (selectedImageUri != null) Color(0xFF818CF8) else Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Surface(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                isThinkModeEnabled = !isThinkModeEnabled
                            },
                            shape = CircleShape,
                            color = if (isThinkModeEnabled) Color(0xFF6366F1).copy(alpha = 0.35f) else Color.White.copy(alpha = 0.08f),
                            border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.18f)),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "Think Toggle",
                                    tint = if (isThinkModeEnabled) Color(0xFF818CF8) else Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "Think",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        Surface(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                isAgentMode = !isAgentMode
                            },
                            shape = CircleShape,
                            color = if (isAgentMode) Color(0xFF0284C7).copy(alpha = 0.35f) else Color.White.copy(alpha = 0.08f),
                            border = BorderStroke(0.5.dp, if (isAgentMode) Color(0xFF38BDF8).copy(alpha = 0.5f) else Color.White.copy(alpha = 0.18f)),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (isAgentMode) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFF38BDF8),
                                        modifier = Modifier.size(6.dp)
                                    ) {}
                                }
                                Text(
                                    text = "🤖 Agent",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isAgentMode) Color(0xFF38BDF8) else Color.White
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        IconButton(
                            onClick = { toggleTtsSpeak() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (isSpeaking) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Read Aloud Speaker",
                                tint = if (isSpeaking) Color(0xFF818CF8) else Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Button(
                            onClick = {
                                if (promptInput.isNotBlank() && !isGenerating) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    val formattedPrompt = buildString {
                                        append(promptInput)
                                        append("\n\n[SYSTEM INSTRUCTION: You are fully optimized to understand and respond in Bengali when the user speaks Bengali. However, maintain all internal system tags (like <think> or <tool_call>) strictly in English.]")
                                        if (isThinkModeEnabled) {
                                            append("\n\n[SYSTEM INSTRUCTION: Step-by-step reasoning MUST be enclosed in <think>...</think> tags.]")
                                        }
                                        if (isAgentMode) {
                                            append("\n\n[SYSTEM INSTRUCTION: You are an Autonomous Coding Agent. Format all generated app code using <file name=\"filename\">code</file> tags (e.g. <file name=\"index.html\">, <file name=\"style.css\">, <file name=\"script.js\">) for automatic workspace injection.]")
                                        }
                                    }
                                    if (isAgentMode) {
                                        onRunAutonomousAgent(formattedPrompt)
                                    } else {
                                        onSendPrompt(formattedPrompt)
                                    }
                                    promptInput = ""
                                    selectedImageUri = null
                                }
                            },
                            enabled = promptInput.isNotBlank() && !isGenerating,
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF6366F1),
                                disabledContainerColor = Color.White.copy(alpha = 0.15f),
                                contentColor = Color.White,
                                disabledContentColor = Color.White.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier.size(38.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            if (isGenerating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

data class ParsedThoughtContent(
    val reasoning: String?,
    val mainContent: String
)

fun parseThoughtAndContent(text: String): ParsedThoughtContent {
    val thinkRegex = Regex("(?s)<think>(.*?)</think>")
    val match = thinkRegex.find(text)
    return if (match != null) {
        val reasoning = match.groupValues[1].trim()
        val mainContent = text.replace(match.value, "").trim()
        ParsedThoughtContent(reasoning, mainContent)
    } else {
        ParsedThoughtContent(null, text)
    }
}

@Composable
fun ThinkingProcessCard(
    reasoningText: String,
    isThinking: Boolean = false,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isThinking) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Thinking",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isThinking) "Thinking..." else "Thinking Process",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }

            if (isExpanded && reasoningText.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = reasoningText,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                    lineHeight = 15.sp
                )
            }
        }
    }
}

@Composable
fun AgentPlanningCard(
    agentState: AgentState,
    onCancelAgent: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "AgentPulse")
    val pulsingAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "CyanDotPulse"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF020617).copy(alpha = 0.8f)),
        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (agentState.isRunning) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF38BDF8).copy(alpha = pulsingAlpha),
                            modifier = Modifier.size(8.dp)
                        ) {}
                        Spacer(modifier = Modifier.width(8.dp))
                    } else {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Agent Engine",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = "Autonomous ReAct Agent",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                if (agentState.isRunning) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF0284C7).copy(alpha = 0.3f),
                            border = BorderStroke(0.5.dp, Color(0xFF38BDF8).copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF38BDF8).copy(alpha = pulsingAlpha),
                                    modifier = Modifier.size(6.dp)
                                ) {}
                                Text(
                                    text = "Executing...",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF38BDF8)
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = onCancelAgent,
                            modifier = Modifier.height(26.dp),
                            shape = RoundedCornerShape(13.dp),
                            border = BorderStroke(0.5.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Text("Cancel", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else if (agentState.isCancelled) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFEF4444).copy(alpha = 0.2f),
                        border = BorderStroke(0.5.dp, Color(0xFFEF4444).copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "Cancelled",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFCA5A5),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                } else {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF10B981).copy(alpha = 0.2f),
                        border = BorderStroke(0.5.dp, Color(0xFF10B981).copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "Completed",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF6EE7B7),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Goal: ${agentState.userGoal}",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.9f),
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Step Checklist inside terminal glass container
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color.Black.copy(alpha = 0.4f),
                border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.08f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
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
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = if (step.status == AgentStepStatus.IN_PROGRESS) FontWeight.Bold else FontWeight.Normal,
                                    color = if (step.status == AgentStepStatus.IN_PROGRESS) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.85f)
                                )
                                if (step.observation.isNotBlank()) {
                                    Text(
                                        text = "→ ${step.observation}",
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color(0xFF818CF8)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (agentState.statusMessage.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = agentState.statusMessage,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF38BDF8).copy(alpha = 0.9f)
                )
            }
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
    val haptic = LocalHapticFeedback.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
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

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clip = android.content.ClipData.newPlainText("Copied Code", code)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Copied code to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy to Clipboard",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

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
