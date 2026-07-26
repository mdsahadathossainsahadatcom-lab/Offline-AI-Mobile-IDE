package com.example.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import com.example.ui.components.DiagnosticPanel
import com.example.util.DiagnosticUtil
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.example.data.db.ChatSessionEntity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.ui.components.AiAssistantScreen
import com.example.ui.components.CodeEditorScreen
import com.example.ui.components.LivePreviewScreen
import com.example.ui.components.SettingsScreen
import com.example.ui.components.WorkspaceDrawerScreen
import com.example.ui.theme.LocalAiIdeTheme
import com.example.ui.viewmodel.IdeViewModel

import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.derivedStateOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: IdeViewModel) {
    val activeProject by viewModel.activeProject.collectAsState()
    val allProjects by viewModel.allProjects.collectAsState()
    val projectFiles by viewModel.projectFiles.collectAsState()
    val openTabs by viewModel.openTabs.collectAsState()
    val activeTabPath by viewModel.activeTabPath.collectAsState()
    val activeCodeContent by viewModel.activeCodeContent.collectAsState()
    val activeTheme by viewModel.activeTheme.collectAsState()
    val navScreen by viewModel.activeNavigationScreen.collectAsState()

    val generationProgress by viewModel.generationProgress.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val allModels by viewModel.allModels.collectAsState()
    val selectedModel by viewModel.selectedModel.collectAsState()
    val chatHistory by viewModel.chatHistory.collectAsState()
    val chatSessions by viewModel.chatSessions.collectAsState()
    val activeSessionId by viewModel.activeSessionId.collectAsState()
    val sessionMessages by viewModel.sessionMessages.collectAsState()
    val sessionAgentLogs by viewModel.sessionAgentLogs.collectAsState()
    val consoleLogs by viewModel.consoleLogs.collectAsState()
    val viewportMode by viewModel.viewportMode.collectAsState()
    val memoryCheckResult by viewModel.memoryCheckResult.collectAsState()
    val lastAutoSaveTime by viewModel.lastAutoSaveTime.collectAsState()
    val contextWindow by viewModel.contextWindow.collectAsState()
    val isHudEnabled by viewModel.isPerformanceHudEnabled.collectAsState()
    val importProgress by viewModel.importProgress.collectAsState()
    val agentState by viewModel.agentState.collectAsState()
    var isFullScreenEditor by remember { mutableStateOf(false) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val drawerScope = rememberCoroutineScope()
    var sessionToRename by remember { mutableStateOf<ChatSessionEntity?>(null) }
    var renameInputText by remember { mutableStateOf("") }
    var sessionToDelete by remember { mutableStateOf<ChatSessionEntity?>(null) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val saveableStateHolder = rememberSaveableStateHolder()

    val filesMap by remember(projectFiles) {
        derivedStateOf { projectFiles.associate { it.path to it.content } }
    }
    val projectFilePaths by remember(projectFiles) {
        derivedStateOf { projectFiles.map { it.path } }
    }

    // Module 4: Android Lifecycle Binding for Memory Safety
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> viewModel.onAppPaused()
                Lifecycle.Event.ON_STOP -> viewModel.onAppStopped()
                Lifecycle.Event.ON_DESTROY -> viewModel.onAppDestroyed()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val zipPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    val zipName = uri.lastPathSegment?.substringAfterLast('/') ?: "imported_project.zip"
                    viewModel.importProjectFromZip(inputStream, zipName) { res ->
                        android.widget.Toast.makeText(context, res, android.widget.Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "Import failed: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    LocalAiIdeTheme(ideTheme = activeTheme) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    modifier = Modifier.width(280.dp),
                    drawerContainerColor = MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        // Drawer Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = "Chat Sessions",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Chat Sessions",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Button(
                                onClick = {
                                    viewModel.createNewSession("New Chat Session")
                                    viewModel.setNavigationScreen(3)
                                    drawerScope.launch { drawerState.close() }
                                },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = "New Chat", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("New", fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (chatSessions.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No saved chat sessions found.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(chatSessions, key = { it.sessionId }) { session ->
                                    val isSelected = session.sessionId == activeSessionId
                                    val dateStr = remember(session.lastModified) {
                                        java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault())
                                            .format(java.util.Date(session.lastModified))
                                    }

                                    val dismissState = rememberSwipeToDismissBoxState(
                                        confirmValueChange = { dismissValue ->
                                            if (dismissValue == SwipeToDismissBoxValue.EndToStart || dismissValue == SwipeToDismissBoxValue.StartToEnd) {
                                                sessionToDelete = session
                                                false
                                            } else false
                                        }
                                    )

                                    SwipeToDismissBox(
                                        state = dismissState,
                                        backgroundContent = {
                                            val isSwiping = dismissState.dismissDirection != SwipeToDismissBoxValue.Settled
                                            val backgroundColor = if (isSwiping) MaterialTheme.colorScheme.errorContainer else Color.Transparent
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(backgroundColor, shape = RoundedCornerShape(12.dp))
                                                    .padding(horizontal = 16.dp),
                                                contentAlignment = Alignment.CenterEnd
                                            ) {
                                                if (isSwiping) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = "Delete Session",
                                                        tint = MaterialTheme.colorScheme.onErrorContainer
                                                    )
                                                }
                                            }
                                        },
                                        enableDismissFromStartToEnd = true,
                                        enableDismissFromEndToStart = true
                                    ) {
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                            ),
                                            shape = RoundedCornerShape(12.dp),
                                            onClick = {
                                                viewModel.selectSession(session.sessionId)
                                                viewModel.setNavigationScreen(3)
                                                drawerScope.launch { drawerState.close() }
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
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Column {
                                                        Text(
                                                            text = session.sessionName,
                                                            fontSize = 13.sp,
                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                                            maxLines = 1
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
                                                        viewModel.exportChatSessionToJson(context, session)
                                                    }) {
                                                        Icon(
                                                            imageVector = Icons.Default.FileDownload,
                                                            contentDescription = "Export JSON Backup",
                                                            modifier = Modifier.size(16.dp),
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
                                                            modifier = Modifier.size(16.dp),
                                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                    IconButton(onClick = {
                                                        sessionToDelete = session
                                                    }) {
                                                        Icon(
                                                            imageVector = Icons.Default.Delete,
                                                            contentDescription = "Delete",
                                                            modifier = Modifier.size(16.dp),
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
            }
        ) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                topBar = {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { drawerScope.launch { drawerState.open() } },
                                    modifier = Modifier.testTag("open_sessions_drawer_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Menu,
                                        contentDescription = "Chat Sessions Drawer",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "💻 " + (activeProject?.title ?: "Local AI IDE"),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                            }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AssistChip(
                                onClick = { viewModel.setNavigationScreen(4) },
                                label = { Text(selectedModel?.name?.take(14) ?: "Gemma-2B", fontSize = 10.sp) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )

                            Spacer(modifier = Modifier.width(6.dp))

                            IconButton(
                                onClick = { viewModel.setNavigationScreen(2) },
                                modifier = Modifier.testTag("run_preview_top_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Run",
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                }
            },
            bottomBar = {
                if (!(navScreen == 1 && isFullScreenEditor)) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary
                    ) {
                        NavigationBarItem(
                            selected = navScreen == 0,
                            onClick = { viewModel.setNavigationScreen(0) },
                            icon = { Icon(imageVector = Icons.Default.Folder, contentDescription = "Files") },
                            label = { Text("Workspace", fontSize = 10.sp) },
                            modifier = Modifier.testTag("nav_workspace_tab")
                        )

                        NavigationBarItem(
                            selected = navScreen == 1,
                            onClick = { viewModel.setNavigationScreen(1) },
                            icon = { Icon(imageVector = Icons.Default.Code, contentDescription = "Editor") },
                            label = { Text("Editor", fontSize = 10.sp) },
                            modifier = Modifier.testTag("nav_editor_tab")
                        )

                        NavigationBarItem(
                            selected = navScreen == 2,
                            onClick = { viewModel.setNavigationScreen(2) },
                            icon = { Icon(imageVector = Icons.Default.Smartphone, contentDescription = "Preview") },
                            label = { Text("Preview", fontSize = 10.sp) },
                            modifier = Modifier.testTag("nav_preview_tab")
                        )

                        NavigationBarItem(
                            selected = navScreen == 3,
                            onClick = { viewModel.setNavigationScreen(3) },
                            icon = { Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "Local AI") },
                            label = { Text("Local AI", fontSize = 10.sp) },
                            modifier = Modifier.testTag("nav_ai_tab")
                        )

                        NavigationBarItem(
                            selected = navScreen == 4,
                            onClick = { viewModel.setNavigationScreen(4) },
                            icon = { Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings") },
                            label = { Text("Settings", fontSize = 10.sp) },
                            modifier = Modifier.testTag("nav_settings_tab")
                        )
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                saveableStateHolder.SaveableStateProvider(key = navScreen) {
                    when (navScreen) {
                        0 -> WorkspaceDrawerScreen(
                            activeProject = activeProject,
                            allProjects = allProjects,
                            files = projectFiles,
                            activeTabPath = activeTabPath,
                            onSelectProject = { viewModel.selectProject(it) },
                            onSelectFile = { path ->
                                viewModel.selectTab(path)
                                viewModel.setNavigationScreen(1)
                            },
                            onCreateFile = { fileName, content -> viewModel.createNewFile(fileName, content) },
                            onDeleteFile = { viewModel.deleteFile(it) },
                            onCreateProject = { title, desc, tmpl -> viewModel.createNewProject(title, desc, tmpl) },
                            onInsertTemplate = { code ->
                                viewModel.insertTemplateToActiveFile(code)
                                viewModel.setNavigationScreen(1)
                            },
                            onReplaceWithTemplate = { code ->
                                viewModel.replaceActiveFileWithTemplate(code)
                                viewModel.setNavigationScreen(1)
                            },
                            onExportZip = {
                                viewModel.exportProjectToZip { res ->
                                    android.widget.Toast.makeText(context, res, android.widget.Toast.LENGTH_LONG).show()
                                }
                            },
                            onImportZip = {
                                zipPickerLauncher.launch("application/zip")
                            }
                        )

                        1 -> CodeEditorScreen(
                            openTabs = openTabs,
                            activeTabPath = activeTabPath,
                            codeContent = activeCodeContent,
                            lastAutoSaveTime = lastAutoSaveTime,
                            isFullScreen = isFullScreenEditor,
                            onTabSelected = { viewModel.selectTab(it) },
                            onTabClosed = { viewModel.closeTab(it) },
                            onCodeChanged = { viewModel.updateCodeContent(it) },
                            onRunPreview = { viewModel.setNavigationScreen(2) },
                            onToggleFullScreen = { isFullScreenEditor = !isFullScreenEditor }
                        )

                        2 -> LivePreviewScreen(
                            filesMap = filesMap,
                            viewportMode = viewportMode,
                            consoleLogs = consoleLogs,
                            activeProjectId = activeProject?.id ?: 1L,
                            onViewportChange = { viewModel.setViewportMode(it) },
                            onAddConsoleLog = { viewModel.addConsoleLog(it) },
                            onClearLogs = { viewModel.clearConsoleLogs() },
                            onAutoFixError = { errorMsg -> viewModel.autoFixRuntimeError(errorMsg) }
                        )

                        3 -> AiAssistantScreen(
                            selectedModel = selectedModel,
                            generationProgress = generationProgress,
                            isGenerating = isGenerating,
                            chatHistory = chatHistory,
                            chatSessions = chatSessions,
                            activeSessionId = activeSessionId,
                            sessionMessages = sessionMessages,
                            sessionAgentLogs = sessionAgentLogs,
                            projectFilePaths = projectFilePaths,
                            agentState = agentState,
                            onSendPrompt = { viewModel.runAiCodeGeneration(it) },
                            onRunAutonomousAgent = { viewModel.runAutonomousAgent(it) },
                            onCancelAgent = { viewModel.cancelAutonomousAgent() },
                            onSelectSession = { viewModel.selectSession(it) },
                            onCreateNewSession = { viewModel.createNewSession(it) },
                            onRenameSession = { session, newName -> viewModel.renameSession(session, newName) },
                            onDeleteSession = { viewModel.deleteChatSession(it) },
                            onExportSession = { session -> viewModel.exportChatSessionToJson(context, session) },
                            onApplyCodeToWorkspace = { targetFile, codeSnippet, isAppend ->
                                viewModel.applyCodeSnippetToWorkspace(targetFile, codeSnippet, isAppend)
                            }
                        )

                        4 -> SettingsScreen(
                            currentTheme = activeTheme,
                            models = allModels,
                            selectedModel = selectedModel,
                            importProgress = importProgress,
                            isGenerating = isGenerating,
                            memoryCheckResult = memoryCheckResult,
                            contextWindow = contextWindow,
                            isHudEnabled = isHudEnabled,
                            onThemeSelected = { viewModel.setTheme(it) },
                            onModelSelected = { viewModel.selectModelProfile(it) },
                            onImportGgufFile = { viewModel.importGgufModelUri(it) },
                            onDeleteModel = { viewModel.deleteModelProfile(it) },
                            onRenameModel = { id, newName -> viewModel.renameModelProfile(id, newName) },
                            onDismissImportProgress = { viewModel.dismissImportProgress() },
                            onContextWindowChanged = { viewModel.setContextWindow(it) },
                            onToggleHud = { viewModel.setPerformanceHudEnabled(it) },
                            onClearHistory = { viewModel.clearChatHistory() }
                        )

                    }
                }

                // In-App Performance Debugging Overlay (Developer HUD / Diagnostic Panel)
                if (isHudEnabled) {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    val diagnosticState = remember(generationProgress, isGenerating) {
                        DiagnosticUtil.getDiagnosticState(
                            context = context,
                            speedTokensPerSec = generationProgress?.speedTokensPerSec ?: (if (isGenerating) 18.4f else 0.0f),
                            tokensGenerated = generationProgress?.tokensGenerated ?: 0,
                            contextWindowTokens = contextWindow,
                            modelSizeBytes = selectedModel?.sizeBytes ?: 1_680_000_000L,
                            modelName = selectedModel?.name ?: "Gemma-2B-Q4_K_M.gguf"
                        )
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 50.dp, end = 12.dp)
                            .width(320.dp)
                    ) {
                        DiagnosticPanel(
                            diagnosticState = diagnosticState,
                            isGenerating = isGenerating
                        )
                    }
                }

                if (sessionToRename != null) {
                    AlertDialog(
                        onDismissRequest = { sessionToRename = null },
                        title = { Text("Rename Chat Session", fontSize = 14.sp, fontWeight = FontWeight.Bold) },
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
                                    viewModel.renameSession(session, renameInputText.trim())
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

                if (sessionToDelete != null) {
                    AlertDialog(
                        onDismissRequest = { sessionToDelete = null },
                        title = { Text("Delete Chat Session", fontSize = 14.sp, fontWeight = FontWeight.Bold) },
                        text = {
                            Text(
                                "Are you sure you want to delete '${sessionToDelete?.sessionName}'? All messages in this session will be permanently deleted.",
                                fontSize = 13.sp
                            )
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    sessionToDelete?.let { session ->
                                        viewModel.deleteChatSession(session)
                                    }
                                    sessionToDelete = null
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Delete")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { sessionToDelete = null }) {
                                Text("Cancel")
                            }
                        }
                    )
                }
            }
        }
    }
}
}


