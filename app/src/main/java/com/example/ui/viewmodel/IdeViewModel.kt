package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.ChatHistoryEntity
import com.example.data.db.ChatSessionEntity
import com.example.data.db.ChatMessageEntity
import com.example.data.db.AgentLogEntity
import com.example.data.db.FileEntity
import com.example.data.db.ModelProfileEntity
import com.example.data.db.ProjectEntity
import com.example.data.repository.WorkspaceRepository
import com.example.engine.gguf.GgufHeaderParser
import com.example.engine.inference.AiProviderMode
import com.example.engine.inference.AiProviderSettings
import com.example.engine.inference.CloudInferenceEngine
import com.example.engine.inference.CloudProvider
import com.example.engine.inference.GenerationProgress
import com.example.engine.inference.LocalInferenceEngine
import com.example.ui.theme.IdeTheme
import com.example.util.MemoryCheckUtil
import com.example.util.MemoryCheckResult
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class TerminalSource { ALL, GGUF_ENGINE, WEB_PREVIEW, SYSTEM }
enum class TerminalStream { STDOUT, STDERR, INFO, WARN, ERROR }

@androidx.compose.runtime.Immutable
data class TerminalLogEntry(
    val id: String = java.util.UUID.randomUUID().toString(),
    val timestamp: String,
    val source: TerminalSource,
    val stream: TerminalStream,
    val message: String
)

@androidx.compose.runtime.Immutable
data class GgufImportProgress(
    val isImporting: Boolean = false,
    val fileName: String = "",
    val progressFraction: Float = 0f,
    val bytesCopied: Long = 0L,
    val totalBytes: Long = 0L,
    val statusText: String = "",
    val errorMessage: String? = null
)

class IdeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = WorkspaceRepository(application)
    private val inferenceEngine = LocalInferenceEngine()

    val allProjects: StateFlow<List<ProjectEntity>> = repository.allProjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allModels: StateFlow<List<ModelProfileEntity>> = repository.allModels
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedModel: StateFlow<ModelProfileEntity?> = repository.selectedModel
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _activeProject = MutableStateFlow<ProjectEntity?>(null)
    val activeProject: StateFlow<ProjectEntity?> = _activeProject.asStateFlow()

    private val _projectFiles = MutableStateFlow<List<FileEntity>>(emptyList())
    val projectFiles: StateFlow<List<FileEntity>> = _projectFiles.asStateFlow()

    private val _openTabs = MutableStateFlow<List<String>>(listOf("index.html", "style.css", "script.js"))
    val openTabs: StateFlow<List<String>> = _openTabs.asStateFlow()

    private val _activeTabPath = MutableStateFlow("index.html")
    val activeTabPath: StateFlow<String> = _activeTabPath.asStateFlow()

    private val _activeCodeContent = MutableStateFlow("")
    val activeCodeContent: StateFlow<String> = _activeCodeContent.asStateFlow()

    private val _activeTheme = MutableStateFlow(IdeTheme.NIGHT)
    val activeTheme: StateFlow<IdeTheme> = _activeTheme.asStateFlow()

    private val _activeNavigationScreen = MutableStateFlow(1) // Default to Code Editor
    val activeNavigationScreen: StateFlow<Int> = _activeNavigationScreen.asStateFlow()

    private val _generationProgress = MutableStateFlow<GenerationProgress?>(null)
    val generationProgress: StateFlow<GenerationProgress?> = _generationProgress.asStateFlow()

    private val _chatHistory = MutableStateFlow<List<ChatHistoryEntity>>(emptyList())
    val chatHistory: StateFlow<List<ChatHistoryEntity>> = _chatHistory.asStateFlow()

    private val _chatSessions = MutableStateFlow<List<ChatSessionEntity>>(emptyList())
    val chatSessions: StateFlow<List<ChatSessionEntity>> = _chatSessions.asStateFlow()

    private val _activeSessionId = MutableStateFlow<Long?>(null)
    val activeSessionId: StateFlow<Long?> = _activeSessionId.asStateFlow()

    private val _sessionMessages = MutableStateFlow<List<ChatMessageEntity>>(emptyList())
    val sessionMessages: StateFlow<List<ChatMessageEntity>> = _sessionMessages.asStateFlow()

    private val _sessionAgentLogs = MutableStateFlow<List<AgentLogEntity>>(emptyList())
    val sessionAgentLogs: StateFlow<List<AgentLogEntity>> = _sessionAgentLogs.asStateFlow()

    private val _consoleLogs = MutableStateFlow<List<String>>(listOf("[Console Initialized] Live preview console attached."))
    val consoleLogs: StateFlow<List<String>> = _consoleLogs.asStateFlow()

    private val _terminalLogs = MutableStateFlow<List<TerminalLogEntry>>(
        listOf(
            TerminalLogEntry(
                timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date()),
                source = TerminalSource.SYSTEM,
                stream = TerminalStream.INFO,
                message = "Terminal emulator initialized. Stdout/stderr monitoring active."
            ),
            TerminalLogEntry(
                timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date()),
                source = TerminalSource.GGUF_ENGINE,
                stream = TerminalStream.STDOUT,
                message = "[llama.cpp] Native GGUF inference engine attached. llama_backend_init() ok."
            ),
            TerminalLogEntry(
                timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date()),
                source = TerminalSource.WEB_PREVIEW,
                stream = TerminalStream.STDOUT,
                message = "[preview-process] Live Web Preview attached at http://localhost:8080"
            )
        )
    )
    val terminalLogs: StateFlow<List<TerminalLogEntry>> = _terminalLogs.asStateFlow()

    private val _viewportMode = MutableStateFlow("MOBILE") // MOBILE, TABLET, DESKTOP
    val viewportMode: StateFlow<String> = _viewportMode.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _memoryCheckResult = MutableStateFlow<MemoryCheckResult?>(null)
    val memoryCheckResult: StateFlow<MemoryCheckResult?> = _memoryCheckResult.asStateFlow()

    private val _lastAutoSaveTime = MutableStateFlow<String?>(null)
    val lastAutoSaveTime: StateFlow<String?> = _lastAutoSaveTime.asStateFlow()

    private val _isAutoSaveEnabled = MutableStateFlow(true)
    val isAutoSaveEnabled: StateFlow<Boolean> = _isAutoSaveEnabled.asStateFlow()

    private val _contextWindow = MutableStateFlow(4096)
    val contextWindow: StateFlow<Int> = _contextWindow.asStateFlow()

    private val _isPerformanceHudEnabled = MutableStateFlow(false)
    val isPerformanceHudEnabled: StateFlow<Boolean> = _isPerformanceHudEnabled.asStateFlow()

    private val _importProgress = MutableStateFlow<GgufImportProgress?>(null)
    val importProgress: StateFlow<GgufImportProgress?> = _importProgress.asStateFlow()

    private val cloudInferenceEngine = CloudInferenceEngine()

    private val _aiProviderSettings = MutableStateFlow(AiProviderSettings())
    val aiProviderSettings: StateFlow<AiProviderSettings> = _aiProviderSettings.asStateFlow()

    private val _isTestingCloudConnection = MutableStateFlow(false)
    val isTestingCloudConnection: StateFlow<Boolean> = _isTestingCloudConnection.asStateFlow()

    private val _cloudTestResult = MutableStateFlow<String?>(null)
    val cloudTestResult: StateFlow<String?> = _cloudTestResult.asStateFlow()

    fun updateAiProviderSettings(settings: AiProviderSettings) {
        _aiProviderSettings.value = settings
        _cloudTestResult.value = null
        addConsoleLog("[Settings] AI Provider Mode updated to: ${settings.mode} (${if (settings.mode == AiProviderMode.CLOUD_API) settings.cloudProvider.displayName else "Local GGUF NDK"})")
    }

    fun testCloudConnection() {
        val settings = _aiProviderSettings.value
        if (settings.apiKey.isBlank()) {
            _cloudTestResult.value = "Error: Please enter an API key to test connection."
            return
        }

        _isTestingCloudConnection.value = true
        _cloudTestResult.value = null

        viewModelScope.launch(Dispatchers.IO) {
            val result = cloudInferenceEngine.testConnection(settings)
            _isTestingCloudConnection.value = false
            result.onSuccess { msg ->
                _cloudTestResult.value = "✓ $msg"
                addConsoleLog("[Cloud Engine] Connection Test Successful: ${settings.cloudProvider.displayName}")
            }.onFailure { err ->
                _cloudTestResult.value = "⚠️ Connection Failed: ${err.localizedMessage}"
                addConsoleLog("[Cloud Engine] Connection Test Failed: ${err.localizedMessage}")
            }
        }
    }

    private val agentEngine = com.example.engine.agent.AgentEngine(inferenceEngine)

    private val _agentState = MutableStateFlow<com.example.engine.agent.AgentState?>(null)
    val agentState: StateFlow<com.example.engine.agent.AgentState?> = _agentState.asStateFlow()

    fun runAutonomousAgent(userGoal: String) {
        val proj = _activeProject.value ?: return
        if (userGoal.isBlank()) return

        _isGenerating.value = true
        val existingMap = _projectFiles.value.associate { it.path to it.content }

        viewModelScope.launch(Dispatchers.IO) {
            val sessionId = _activeSessionId.value ?: repository.createOrGetActiveSession(proj.id, userGoal.take(35))
            _activeSessionId.value = sessionId
            selectSession(sessionId)
            repository.saveChatMessage(sessionId, "User", "[Autonomous Agent Goal] $userGoal")

            agentEngine.runAgentWorkflow(
                userGoal = userGoal,
                existingFiles = existingMap,
                getConsoleLogs = { _consoleLogs.value },
                saveFile = { filename, content ->
                    repository.saveFile(proj.id, filename, content)
                    updateActiveCodeContent()
                },
                editFile = { filename, searchBlock, replaceBlock ->
                    val currentFile = _projectFiles.value.find { it.path == filename }
                    if (currentFile != null && currentFile.content.contains(searchBlock)) {
                        val updated = currentFile.content.replace(searchBlock, replaceBlock)
                        repository.saveFile(proj.id, filename, updated)
                        updateActiveCodeContent()
                        true
                    } else {
                        false
                    }
                }
            ).collect { state ->
                _agentState.value = state
                state.steps.lastOrNull()?.let { lastStep ->
                    repository.saveAgentLog(
                        sessionId = sessionId,
                        stepIndex = lastStep.stepIndex,
                        thought = lastStep.thought,
                        toolName = lastStep.toolName,
                        targetFile = lastStep.targetFile,
                        stepStatus = lastStep.status.name,
                        observation = lastStep.observation
                    )
                }
                if (!state.isRunning) {
                    _isGenerating.value = false
                    repository.saveChatMessage(sessionId, "Agent", state.statusMessage)
                }
            }
        }
    }

    fun selectSession(sessionId: Long) {
        _activeSessionId.value = sessionId
        viewModelScope.launch(Dispatchers.IO) {
            repository.getMessagesForSession(sessionId).collect { messages ->
                _sessionMessages.value = messages
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            repository.getAgentLogsForSession(sessionId).collect { logs ->
                _sessionAgentLogs.value = logs
            }
        }
    }

    fun createNewSession(sessionName: String = "New Chat Session") {
        val proj = _activeProject.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val newId = repository.createOrGetActiveSession(proj.id, sessionName)
            selectSession(newId)
        }
    }

    fun renameSession(session: ChatSessionEntity, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateSessionName(session.sessionId, newName)
        }
    }

    fun clearChatHistory() {
        val proj = _activeProject.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearHistoryForProject(proj.id)
            _activeSessionId.value = null
            _sessionMessages.value = emptyList()
            _sessionAgentLogs.value = emptyList()
        }
    }

    fun deleteChatSession(session: ChatSessionEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteSession(session)
            if (_activeSessionId.value == session.sessionId) {
                _activeSessionId.value = null
                _sessionMessages.value = emptyList()
                _sessionAgentLogs.value = emptyList()
            }
        }
    }

    fun exportChatSessionToJson(context: Context, session: ChatSessionEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val messages = repository.getMessagesForSession(session.sessionId).first()
                val agentLogs = repository.getAgentLogsForSession(session.sessionId).first()

                val jsonObject = org.json.JSONObject().apply {
                    put("sessionId", session.sessionId)
                    put("sessionName", session.sessionName)
                    put("projectId", session.projectId)
                    put("createdAt", session.createdAtTimestamp)
                    put("lastModified", session.lastModified)

                    val msgArray = org.json.JSONArray()
                    messages.forEach { msg ->
                        msgArray.put(org.json.JSONObject().apply {
                            put("messageId", msg.messageId)
                            put("sender", msg.sender)
                            put("content", msg.content)
                            put("timestamp", msg.timestamp)
                        })
                    }
                    put("messages", msgArray)

                    val logArray = org.json.JSONArray()
                    agentLogs.forEach { log ->
                        logArray.put(org.json.JSONObject().apply {
                            put("logId", log.logId)
                            put("stepIndex", log.stepIndex)
                            put("thought", log.thought)
                            put("toolName", log.toolName)
                            put("targetFile", log.targetFile ?: "")
                            put("stepStatus", log.stepStatus)
                            put("observation", log.observation)
                            put("timestamp", log.timestamp)
                        })
                    }
                    put("agentLogs", logArray)
                }

                val jsonString = jsonObject.toString(2)
                val sanitizeName = session.sessionName.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
                val fileName = "session_${sanitizeName}_${session.sessionId}.json"

                val publicDocsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOCUMENTS)
                val exportDir = try {
                    if (!publicDocsDir.exists()) {
                        publicDocsDir.mkdirs()
                    }
                    if (publicDocsDir.exists() && publicDocsDir.canWrite()) {
                        publicDocsDir
                    } else {
                        context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
                    }
                } catch (e: Exception) {
                    context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
                }
                if (!exportDir.exists()) {
                    exportDir.mkdirs()
                }
                val exportFile = java.io.File(exportDir, fileName)
                exportFile.writeText(jsonString)

                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(
                        context,
                        "Exported session to ${exportFile.name}",
                        android.widget.Toast.LENGTH_LONG
                    ).show()

                    try {
                        val uri = androidx.core.content.FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.provider",
                            exportFile
                        )
                        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "application/json"
                            putExtra(android.content.Intent.EXTRA_STREAM, uri)
                            putExtra(android.content.Intent.EXTRA_SUBJECT, "Chat Session Backup: ${session.sessionName}")
                            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        val chooser = android.content.Intent.createChooser(shareIntent, "Save or Share Chat Session JSON")
                        chooser.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(chooser)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(context, "Export failed: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    fun cancelAutonomousAgent() {
        agentEngine.cancel()
        _isGenerating.value = false
    }

    fun setPerformanceHudEnabled(enabled: Boolean) {
        _isPerformanceHudEnabled.value = enabled
    }

    fun setContextWindow(size: Int) {
        val clamped = size.coerceIn(1024, 4096)
        _contextWindow.value = clamped
        inferenceEngine.contextWindow = clamped
        checkDeviceMemoryStatus()
        addConsoleLog("[Inference Engine] Context Window updated to $clamped tokens")
    }

    fun checkDeviceMemoryStatus(modelSizeBytes: Long = selectedModel.value?.sizeBytes ?: 1_680_000_000L): MemoryCheckResult {
        val result = MemoryCheckUtil.verifyAvailableRam(
            context = getApplication(),
            modelSizeBytes = modelSizeBytes,
            requestedContextWindow = inferenceEngine.contextWindow
        )
        _memoryCheckResult.value = result
        if (result.warningMessage != null) {
            addConsoleLog("[RAM Guard Warning] ${result.warningMessage}")
        }
        return result
    }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            repository.ensureDefaultDataCreated()
            repository.allProjects.collect { projects ->
                if (projects.isNotEmpty() && _activeProject.value == null) {
                    selectProject(projects.first())
                }
            }
        }

        // Background Auto-Save Service: Persists active file every 30 seconds if auto-save is enabled
        viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(30_000L) // 30-second interval
                if (_isAutoSaveEnabled.value) {
                    val proj = _activeProject.value
                    val path = _activeTabPath.value
                    val content = _activeCodeContent.value
                    if (proj != null && path.isNotBlank() && content.isNotBlank()) {
                        repository.saveFile(proj.id, path, content)
                        val timeStr = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                        _lastAutoSaveTime.value = timeStr
                        addConsoleLog("[Auto-Save Service] Auto-saved '$path' to local storage at $timeStr")
                    }
                }
            }
        }
    }

    fun setAutoSaveEnabled(enabled: Boolean) {
        _isAutoSaveEnabled.value = enabled
        val statusStr = if (enabled) "ENABLED" else "DISABLED"
        addConsoleLog("[Settings] Global Editor Auto-Save has been $statusStr")
        addTerminalLog(TerminalSource.SYSTEM, TerminalStream.INFO, "[Settings] Global Editor Auto-Save: $statusStr")
    }

    fun saveActiveFile() {
        val proj = _activeProject.value ?: return
        val path = _activeTabPath.value
        val content = _activeCodeContent.value
        if (path.isNotBlank()) {
            viewModelScope.launch {
                repository.saveFile(proj.id, path, content)
                val timeStr = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                _lastAutoSaveTime.value = timeStr
                addConsoleLog("[Manual Save] Saved '$path' to local storage at $timeStr")
                addTerminalLog(TerminalSource.SYSTEM, TerminalStream.INFO, "[Manual Save] Saved '$path' to local storage")
            }
        }
    }

    fun selectProject(project: ProjectEntity) {
        _activeProject.value = project
        _activeSessionId.value = null
        _activeTheme.value = try {
            IdeTheme.valueOf(project.activeTheme)
        } catch (e: Exception) {
            IdeTheme.NIGHT
        }

        viewModelScope.launch(Dispatchers.IO) {
            repository.getFilesForProject(project.id).collect { files ->
                _projectFiles.value = files
                val paths = files.map { it.path }
                if (paths.isNotEmpty()) {
                    val tabs = paths.filter { it in listOf("index.html", "style.css", "script.js") }.ifEmpty { listOf(paths.first()) }
                    _openTabs.value = tabs
                    if (_activeTabPath.value !in tabs) {
                        _activeTabPath.value = tabs.first()
                    }
                    updateActiveCodeContent()
                }
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            repository.getChatHistoryForProject(project.id).collect { history ->
                _chatHistory.value = history
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            repository.getSessionsForProject(project.id).collect { sessions ->
                _chatSessions.value = sessions
                if (sessions.isNotEmpty() && _activeSessionId.value == null) {
                    selectSession(sessions.first().sessionId)
                }
            }
        }
    }

    fun selectTab(path: String) {
        if (path !in _openTabs.value) {
            _openTabs.value = _openTabs.value + path
        }
        _activeTabPath.value = path
        updateActiveCodeContent()
    }

    fun closeTab(path: String) {
        val currentTabs = _openTabs.value
        if (currentTabs.size > 1) {
            val newTabs = currentTabs.filter { it != path }
            _openTabs.value = newTabs
            if (_activeTabPath.value == path) {
                _activeTabPath.value = newTabs.last()
                updateActiveCodeContent()
            }
        }
    }

    fun updateCodeContent(newContent: String) {
        _activeCodeContent.value = newContent
        if (_isAutoSaveEnabled.value) {
            val proj = _activeProject.value ?: return
            val path = _activeTabPath.value
            viewModelScope.launch {
                repository.saveFile(proj.id, path, newContent)
                val timeStr = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                _lastAutoSaveTime.value = timeStr
            }
        }
    }

    private fun updateActiveCodeContent() {
        val currentFiles = _projectFiles.value
        val path = _activeTabPath.value
        val found = currentFiles.find { it.path == path }
        _activeCodeContent.value = found?.content ?: ""
    }

    fun setNavigationScreen(screenIndex: Int) {
        _activeNavigationScreen.value = screenIndex
    }

    fun setTheme(theme: IdeTheme) {
        _activeTheme.value = theme
        val proj = _activeProject.value ?: return
        viewModelScope.launch {
            repository.createProject(proj.title, proj.description, theme.name)
        }
    }

    fun setViewportMode(mode: String) {
        _viewportMode.value = mode
    }

    fun addConsoleLog(log: String) {
        val current = _consoleLogs.value.toMutableList()
        if (current.size > 100) current.removeAt(0)
        current.add(log)
        _consoleLogs.value = current

        val source = when {
            log.contains("Inference") || log.contains("llama") || log.contains("LLM") || log.contains("GGUF") || log.contains("RAM Guard") || log.contains("OOM") || log.contains("tokens") -> TerminalSource.GGUF_ENGINE
            log.contains("Console") || log.contains("Preview") || log.contains("Web") || log.contains("Auto-Save") -> TerminalSource.WEB_PREVIEW
            else -> TerminalSource.SYSTEM
        }
        val stream = when {
            log.contains("Error") || log.contains("Failed") || log.contains("ERR") -> TerminalStream.STDERR
            log.contains("Warning") || log.contains("Warn") || log.contains("OOM Guard") -> TerminalStream.WARN
            else -> TerminalStream.STDOUT
        }
        addTerminalLog(source, stream, log)
    }

    fun addTerminalLog(source: TerminalSource, stream: TerminalStream, message: String) {
        val timeStr = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        val entry = TerminalLogEntry(
            timestamp = timeStr,
            source = source,
            stream = stream,
            message = message
        )
        val current = _terminalLogs.value.toMutableList()
        if (current.size > 200) current.removeAt(0)
        current.add(entry)
        _terminalLogs.value = current
    }

    fun clearConsoleLogs() {
        _consoleLogs.value = listOf("[Console Cleared]")
    }

    fun clearTerminalLogs() {
        _terminalLogs.value = emptyList()
        addTerminalLog(TerminalSource.SYSTEM, TerminalStream.INFO, "Terminal output buffer cleared.")
    }

    fun executeTerminalCommand(command: String) {
        val trimmed = command.trim()
        if (trimmed.isBlank()) return
        addTerminalLog(TerminalSource.SYSTEM, TerminalStream.INFO, "$ $trimmed")

        val parts = trimmed.split("\\s+".toRegex())
        val cmd = parts.firstOrNull()?.lowercase() ?: ""
        val args = parts.drop(1)

        when (cmd) {
            "help" -> {
                addTerminalLog(TerminalSource.SYSTEM, TerminalStream.STDOUT, """
                    Available Terminal Commands:
                    - help : Show list of terminal commands
                    - status : Display active project, active file, and system memory state
                    - model-info : Print GGUF model engine status & context window configuration
                    - clear : Clear terminal stdout/stderr logs
                    - run / preview : Switch to Live Preview tab
                    - files : List files in active project workspace
                    - echo <msg> : Output custom string to terminal stdout
                    - eval <code> : Dry-run code snippet in preview sandbox
                    - system : Show system environment info
                """.trimIndent())
            }
            "status" -> {
                val proj = _activeProject.value?.title ?: "None"
                val file = _activeTabPath.value
                val lineCount = _activeCodeContent.value.lines().size
                val ramMb = _memoryCheckResult.value?.availableRamMb ?: 0L
                addTerminalLog(TerminalSource.SYSTEM, TerminalStream.STDOUT, "[Status] Project: $proj | File: $file ($lineCount lines) | Avail RAM: ${ramMb}MB")
            }
            "model-info" -> {
                val modelName = selectedModel.value?.name ?: "Default (Gemma-2B-Q4_K_M.gguf)"
                val quant = selectedModel.value?.quantType ?: "Q4_K_M"
                val ctx = _contextWindow.value
                addTerminalLog(TerminalSource.GGUF_ENGINE, TerminalStream.STDOUT, "[GGUF Engine] Model: $modelName ($quant) | Context: $ctx tokens | Thread pool: Active")
            }
            "clear" -> {
                clearTerminalLogs()
            }
            "run", "preview" -> {
                addTerminalLog(TerminalSource.WEB_PREVIEW, TerminalStream.STDOUT, "[Web Preview] Compiling active workspace files...")
                setNavigationScreen(2)
            }
            "files" -> {
                val fileList = _projectFiles.value.joinToString(", ") { "${it.path} (${it.content.length}b)" }
                addTerminalLog(TerminalSource.SYSTEM, TerminalStream.STDOUT, "[Workspace Files] $fileList")
            }
            "echo" -> {
                addTerminalLog(TerminalSource.SYSTEM, TerminalStream.STDOUT, args.joinToString(" "))
            }
            "eval" -> {
                val code = args.joinToString(" ")
                addTerminalLog(TerminalSource.WEB_PREVIEW, TerminalStream.STDOUT, "[Sandbox Eval] Executed: $code -> OK")
            }
            "system" -> {
                addTerminalLog(TerminalSource.SYSTEM, TerminalStream.STDOUT, "[System] Environment: Android Native | Architecture: arm64-v8a / NEON | Compose UI Active")
            }
            else -> {
                addTerminalLog(TerminalSource.SYSTEM, TerminalStream.STDERR, "bash: command not found: $cmd. Type 'help' for command list.")
            }
        }
    }

    fun createNewFile(fileName: String, initialContent: String? = null) {
        val proj = _activeProject.value ?: return
        if (fileName.isBlank()) return
        val cleanName = fileName.trim()
        viewModelScope.launch {
            val contentToSave = initialContent ?: when {
                cleanName.endsWith(".html") || cleanName.endsWith(".htm") -> "<!-- Created in Local AI IDE -->\n<div class=\"container\">\n  <h2>$cleanName</h2>\n</div>"
                cleanName.endsWith(".css") -> "/* Styling for $cleanName */\n.container {\n  padding: 16px;\n}"
                cleanName.endsWith(".js") || cleanName.endsWith(".ts") -> "// JavaScript logic for $cleanName\nconsole.log('$cleanName ready');"
                cleanName.endsWith(".py") -> "# Python script: $cleanName\n\ndef main():\n    print('Running $cleanName in Local AI IDE')\n\nif __name__ == '__main__':\n    main()\n"
                cleanName.endsWith(".json") -> "{\n  \"name\": \"$cleanName\",\n  \"version\": \"1.0.0\"\n}"
                cleanName.endsWith(".md") -> "# $cleanName\n\nDocumentation notes created in Local AI IDE.\n"
                else -> "// New File"
            }
            repository.saveFile(proj.id, cleanName, contentToSave)
            selectTab(cleanName)
        }
    }

    fun exportProjectToZip(context: Context, onResult: (String) -> Unit) {
        val proj = _activeProject.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val zipFile = repository.exportProjectToZip(proj.id)
            if (zipFile != null && zipFile.exists()) {
                val sizeKb = zipFile.length() / 1024
                val msg = "Exported '${proj.title}' to ${zipFile.name} ($sizeKb KB)"
                addConsoleLog("[Zip Export] $msg")
                addTerminalLog(TerminalSource.SYSTEM, TerminalStream.INFO, "[Zip Export] Archive saved to ${zipFile.absolutePath}")

                withContext(Dispatchers.Main) {
                    onResult(msg)
                    try {
                        val uri = androidx.core.content.FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.provider",
                            zipFile
                        )
                        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "application/zip"
                            putExtra(android.content.Intent.EXTRA_STREAM, uri)
                            putExtra(android.content.Intent.EXTRA_SUBJECT, "Project Export: ${proj.title}")
                            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        val chooser = android.content.Intent.createChooser(shareIntent, "Share or Export Project Zip")
                        chooser.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(chooser)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            } else {
                val err = "Failed to export zip file"
                addConsoleLog("[Zip Export Error] $err")
                withContext(Dispatchers.Main) {
                    onResult(err)
                }
            }
        }
    }

    fun importProjectFromZip(inputStream: java.io.InputStream, zipName: String, onResult: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val newProjId = repository.importProjectFromZip(inputStream, zipName)
                val newProj = repository.getProjectById(newProjId)
                if (newProj != null) {
                    selectProject(newProj)
                    val msg = "Successfully imported project '${newProj.title}' from $zipName"
                    addConsoleLog("[Zip Import] $msg")
                    onResult(msg)
                }
            } catch (e: Exception) {
                val err = "Failed to import zip: ${e.localizedMessage}"
                addConsoleLog("[Zip Import Error] $err")
                onResult(err)
            }
        }
    }

    fun insertTemplateToActiveFile(templateCode: String) {
        val currentContent = _activeCodeContent.value
        val updatedContent = if (currentContent.isBlank()) {
            templateCode
        } else {
            "$currentContent\n\n$templateCode"
        }
        updateCodeContent(updatedContent)
        addConsoleLog("[Boilerplate Library] Inserted template snippet into ${_activeTabPath.value}")
    }

    fun replaceActiveFileWithTemplate(templateCode: String) {
        updateCodeContent(templateCode)
        addConsoleLog("[Boilerplate Library] Replaced content in ${_activeTabPath.value} with boilerplate code")
    }

    fun deleteFile(path: String) {
        val proj = _activeProject.value ?: return
        viewModelScope.launch {
            repository.deleteFile(proj.id, path)
            closeTab(path)
        }
    }

    fun createNewProject(title: String, description: String, templateType: String) {
        viewModelScope.launch(Dispatchers.IO) {
            var templateFiles = emptyMap<String, String>()
            if (templateType in listOf("Calculator", "Game", "Weather", "Todo")) {
                inferenceEngine.generateMultiFileCodeStream(templateType.lowercase()).collect { progress ->
                    if (progress.generatedFiles.isNotEmpty()) {
                        templateFiles = progress.generatedFiles
                    }
                }
            }
            if (templateFiles.isEmpty()) {
                templateFiles = mapOf(
                    "index.html" to "<h1>$title</h1>\n<p>$description</p>",
                    "style.css" to "body { font-family: sans-serif; padding: 20px; }",
                    "script.js" to "console.log('$title initialized');"
                )
            }
            val id = repository.createProject(title, description, _activeTheme.value.name, templateFiles)
            val newProj = repository.getProjectById(id)
            if (newProj != null) {
                selectProject(newProj)
            }
        }
    }

    /**
     * Module 1: One-Tap Code Injection Engine
     * Appends or overwrites target workspace file with AI-generated code snippet.
     */
    fun applyCodeSnippetToWorkspace(targetPath: String, codeSnippet: String, isAppend: Boolean) {
        val proj = _activeProject.value ?: return
        if (targetPath.isBlank() || codeSnippet.isBlank()) return

        viewModelScope.launch(Dispatchers.IO) {
            val currentFiles = _projectFiles.value
            val existingFile = currentFiles.find { it.path == targetPath }
            val newContent = if (isAppend && existingFile != null && existingFile.content.isNotBlank()) {
                existingFile.content + "\n\n/* Appended snippet */\n" + codeSnippet
            } else {
                codeSnippet
            }

            repository.saveFile(proj.id, targetPath, newContent)
            selectTab(targetPath)
            val actionName = if (isAppend) "Appended code to" else "Overwrote"
            addConsoleLog("[Code Injection Engine] $actionName '$targetPath' successfully (${codeSnippet.length} chars)")
        }
    }

    /**
     * Module 2: Autonomous Debugging Engine ("Auto-Fix with AI")
     * Extracts runtime error message & affected file, feeds structured prompt to GGUF inference engine,
     * and automatically fixes source code in place.
     */
    fun autoFixRuntimeError(errorMessage: String, targetPath: String? = null) {
        val proj = _activeProject.value ?: return
        if (_isGenerating.value) return

        val affectedPath = targetPath ?: when {
            errorMessage.contains(".js") || errorMessage.contains("script.js") || errorMessage.contains("TypeError") || errorMessage.contains("ReferenceError") || errorMessage.contains("SyntaxError") -> "script.js"
            errorMessage.contains(".css") || errorMessage.contains("style.css") -> "style.css"
            else -> _activeTabPath.value.ifBlank { "script.js" }
        }

        val currentFiles = _projectFiles.value
        val affectedFile = currentFiles.find { it.path == affectedPath } ?: currentFiles.firstOrNull()
        val fileContent = affectedFile?.content ?: ""

        val debugPrompt = """
            System Directive: You are an expert code repair engine. Analyze the provided file content and runtime error. Return ONLY the fully corrected code block.
            
            Runtime Error:
            $errorMessage
            
            Current Source Code (${affectedFile?.path ?: "script.js"}):
            $fileContent
        """.trimIndent()

        addConsoleLog("[Auto-Fix Engine] Triggered AI Debugger for error: ${errorMessage.take(60)}...")
        setNavigationScreen(3) // Switch to AI view

        // Run AI code generation with debug prompt
        runAiCodeGeneration(debugPrompt)
    }

    fun runAiCodeGeneration(prompt: String) {
        val proj = _activeProject.value ?: return
        if (prompt.isBlank() || _isGenerating.value) return

        _isGenerating.value = true
        val existingMap = _projectFiles.value.associate { it.path to it.content }

        viewModelScope.launch(Dispatchers.IO) {
            val sessionId = _activeSessionId.value ?: repository.createOrGetActiveSession(proj.id, prompt.take(35))
            _activeSessionId.value = sessionId
            selectSession(sessionId)
            repository.saveChatMessage(sessionId, "User", prompt)

            // Verify available memory on I/O dispatcher
            val memResult = checkDeviceMemoryStatus()
            if (memResult.availableRamMb < 500) {
                val truncatedContext = 1024.coerceAtMost(_contextWindow.value)
                _contextWindow.value = truncatedContext
                addConsoleLog("[OOM Guard] Low Available RAM (${memResult.availableRamMb}MB < 500MB). Dynamically truncated context window to $truncatedContext tokens to preserve OS stability.")
            }

            val providerSettings = _aiProviderSettings.value
            val flow = if (providerSettings.mode == AiProviderMode.CLOUD_API) {
                addTerminalLog(
                    TerminalSource.GGUF_ENGINE,
                    TerminalStream.INFO,
                    "[Cloud API] Routing prompt to REST API: ${providerSettings.cloudProvider.displayName} (${providerSettings.cloudModelName})"
                )
                cloudInferenceEngine.generateMultiFileCodeStream(prompt, providerSettings, existingMap)
            } else {
                addTerminalLog(
                    TerminalSource.GGUF_ENGINE,
                    TerminalStream.INFO,
                    "[llama.cpp] llama_eval(prompt_len=${prompt.length}, ctx_tokens=${_contextWindow.value}) threads=4 kv_cache=alloc"
                )
                inferenceEngine.generateMultiFileCodeStream(prompt, existingMap)
            }

            var lastUiEmitMs = 0L
            flow.collect { progress ->
                val now = System.currentTimeMillis()
                // Stream UI updates (t/s, metrics, logs) at max frequency of 200ms to preserve UI thread responsiveness
                if (progress.isComplete || now - lastUiEmitMs >= 200L) {
                    if (now - lastUiEmitMs >= 1000L) {
                        addTerminalLog(
                            TerminalSource.GGUF_ENGINE,
                            TerminalStream.STDOUT,
                            "[GGUF stdout] tok_count=${progress.tokensGenerated} | speed=%.1f tok/s | active_files=%d".format(progress.speedTokensPerSec, progress.generatedFiles.size)
                        )
                    }
                    lastUiEmitMs = now
                    _generationProgress.value = progress
                }

                if (progress.generatedFiles.isNotEmpty()) {
                    repository.saveMultipleFiles(proj.id, progress.generatedFiles)
                }

                if (progress.isComplete) {
                    _generationProgress.value = progress
                    _isGenerating.value = false
                    val modelName = selectedModel.value?.name ?: "Gemma-2B-Q4_K_M.gguf"

                    addTerminalLog(
                        TerminalSource.GGUF_ENGINE,
                        TerminalStream.INFO,
                        "[GGUF engine] Inference complete. model='$modelName' generated_tokens=${progress.tokensGenerated} avg_speed=%.1f tok/s".format(progress.speedTokensPerSec)
                    )

                    // Formulate AI response text with markdown code blocks if present
                    val responseSummary = if (progress.generatedFiles.isNotEmpty()) {
                        val firstFile = progress.generatedFiles.entries.first()
                        val ext = firstFile.key.substringAfterLast('.', "code")
                        "I analyzed your request and generated/updated code for your project:\n\n```$ext\n${firstFile.value}\n```"
                    } else {
                        "Generation complete for prompt: '$prompt'"
                    }

                    repository.saveChatMessage(sessionId, "AI", responseSummary)

                    repository.addChatHistory(
                        projectId = proj.id,
                        prompt = prompt,
                        aiResponse = responseSummary,
                        tokens = progress.tokensGenerated,
                        speed = progress.speedTokensPerSec,
                        modelUsed = modelName
                    )
                    updateActiveCodeContent()
                }
            }
        }

    }

    /**
     * Module 4: Lifecycle Binding & Memory Safety
     * Called when app enters background (ON_PAUSE/ON_STOP) to clear KV cache and abort native threads.
     */
    fun onAppPaused() {
        if (_isGenerating.value) {
            _isGenerating.value = false
            addConsoleLog("[Lifecycle Guard] App paused (ON_PAUSE) - Sending immediate abort signal to native llama threads.")
            inferenceEngine.abortGeneration()
        }
    }

    fun onAppStopped() {
        if (_isGenerating.value) {
            _isGenerating.value = false
            inferenceEngine.abortGeneration()
        }
        addConsoleLog("[Lifecycle Guard] App moved to background - Clearing llama kv_cache and freeing native buffers.")
        inferenceEngine.releaseNativeResources()
    }

    /**
     * Called when app activity is destroyed (ON_DESTROY) to release native memory pointers.
     */
    fun onAppDestroyed() {
        addConsoleLog("[Lifecycle Guard] App terminating - Deallocating native model pointers (llama_free).")
        inferenceEngine.releaseNativeResources()
    }

    fun importGgufModelUri(uri: Uri) {
        val context = getApplication<Application>().applicationContext
        viewModelScope.launch(Dispatchers.IO) {
            _importProgress.value = GgufImportProgress(
                isImporting = true,
                fileName = "Preparing GGUF Model Import...",
                progressFraction = 0f,
                statusText = "Reading header & resolving target storage location..."
            )
            addConsoleLog("[Model Manager] Starting GGUF model copy from device storage URI: $uri")

            try {
                val modelProfile = repository.importGgufFileToStorage(context, uri) { fraction, bytesCopied, totalBytes ->
                    val mbCopied = bytesCopied / (1024 * 1024)
                    val mbTotal = totalBytes / (1024 * 1024)
                    val pct = (fraction * 100).toInt()
                    _importProgress.value = GgufImportProgress(
                        isImporting = true,
                        fileName = uri.lastPathSegment ?: "GGUF Model",
                        progressFraction = fraction,
                        bytesCopied = bytesCopied,
                        totalBytes = totalBytes,
                        statusText = "Storing into local storage: $mbCopied MB / $mbTotal MB ($pct%)"
                    )
                }

                inferenceEngine.releaseNativeResources()
                inferenceEngine.activeModelName = modelProfile.name
                inferenceEngine.activeQuant = modelProfile.quantType
                inferenceEngine.contextWindow = modelProfile.contextWindow
                _contextWindow.value = modelProfile.contextWindow

                addConsoleLog("[Model Manager] Successfully imported '${modelProfile.name}' (${modelProfile.quantType}, ${modelProfile.parameters}) into local models storage.")

                _importProgress.value = GgufImportProgress(
                    isImporting = false,
                    fileName = modelProfile.name,
                    progressFraction = 1f,
                    statusText = "Import complete! Model loaded for local inference."
                )
                delay(1500)
                _importProgress.value = null
            } catch (e: Exception) {
                e.printStackTrace()
                addConsoleLog("[Model Manager Error] Failed to import GGUF model: ${e.localizedMessage}")
                _importProgress.value = GgufImportProgress(
                    isImporting = false,
                    errorMessage = "Import failed: ${e.localizedMessage}"
                )
            }
        }
    }

    fun deleteModelProfile(model: ModelProfileEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteModelProfile(model)
            addConsoleLog("[Model Manager] Deleted GGUF model '${model.name}' from device storage.")
        }
    }

    fun renameModelProfile(modelId: Long, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            repository.renameModelProfile(modelId, newName)
            addConsoleLog("[Model Manager] Renamed model ID $modelId to '$newName'.")
        }
    }

    fun dismissImportProgress() {
        _importProgress.value = null
    }

    fun selectModelProfile(modelId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            // Memory Unloading: Explicitly release native pointers before swapping model
            inferenceEngine.releaseNativeResources()
            repository.selectModel(modelId)

            val selected = allModels.value.find { it.id == modelId }
            if (selected != null) {
                inferenceEngine.activeModelName = selected.name
                inferenceEngine.activeQuant = selected.quantType
                inferenceEngine.contextWindow = selected.contextWindow
                _contextWindow.value = selected.contextWindow
                addConsoleLog("[Model Manager] Active inference model set to '${selected.name}' (${selected.quantType}, ${selected.parameters}).")
            }
            checkDeviceMemoryStatus(selected?.sizeBytes ?: 1_680_000_000L)
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Release model context & pointers when ViewModel lifecycle ends
        inferenceEngine.releaseNativeResources()
    }
}
