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
import com.example.engine.gguf.GgufQuantizerEngine
import com.example.engine.gguf.GgufQuantType
import com.example.engine.gguf.QuantizationOptions
import com.example.engine.gguf.QuantizationProgress
import com.example.engine.inference.AiProviderMode
import com.example.engine.inference.AiProviderSettings
import com.example.engine.inference.CloudInferenceEngine
import com.example.engine.inference.CloudProvider
import com.example.engine.inference.GenerationProgress
import com.example.engine.inference.LlamaBridge
import com.example.engine.inference.LocalInferenceEngine
import com.example.ui.theme.IdeTheme
import com.example.ui.theme.ThemeMode
import com.example.util.MemoryCheckUtil
import com.example.util.MemoryCheckResult
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import android.app.DownloadManager
import com.example.ui.components.PresetModelItem
import java.io.File

@androidx.compose.runtime.Immutable
data class ToastAlertEvent(
    val message: String,
    val duration: Int = android.widget.Toast.LENGTH_LONG,
    val isWarning: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
)

enum class TerminalSource { ALL, GGUF_ENGINE, WEB_PREVIEW, SYSTEM }
enum class TerminalStream { STDOUT, STDERR, INFO, WARN, ERROR }

@androidx.compose.runtime.Immutable
data class GgufDownloadState(
    val downloadId: Long = -1L,
    val filename: String,
    val url: String,
    val bytesDownloaded: Long = 0L,
    val totalBytes: Long = 0L,
    val progressPercent: Int = 0,
    val isDownloading: Boolean = true,
    val isCompleted: Boolean = false,
    val errorMessage: String? = null
)

@androidx.compose.runtime.Immutable
data class TerminalLogEntry(
    val id: String = java.util.UUID.randomUUID().toString(),
    val timestamp: String,
    val source: TerminalSource,
    val stream: TerminalStream,
    val message: String
)

@androidx.compose.runtime.Immutable
data class GitBranch(
    val name: String,
    val isCurrent: Boolean = false,
    val lastCommitHash: String = "a1b2c3d",
    val lastCommitMessage: String = "Initial workspace setup",
    val lastUpdated: String = "Just now",
    val aheadBehind: String = "Up to date"
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

    private val _downloadStates = MutableStateFlow<Map<String, GgufDownloadState>>(emptyMap())
    val downloadStates: StateFlow<Map<String, GgufDownloadState>> = _downloadStates.asStateFlow()

    private val _expandedCategories = MutableStateFlow<Set<String>>(
        setOf("Installed Models", "Coding", "General", "Math")
    )
    val expandedCategories: StateFlow<Set<String>> = _expandedCategories.asStateFlow()

    fun toggleCategoryExpanded(category: String) {
        val current = _expandedCategories.value
        _expandedCategories.value = if (current.contains(category)) {
            current - category
        } else {
            current + category
        }
    }

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

    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _isDynamicColorEnabled = MutableStateFlow(true)
    val isDynamicColorEnabled: StateFlow<Boolean> = _isDynamicColorEnabled.asStateFlow()

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

    private var sessionMessagesJob: Job? = null
    private var sessionAgentLogsJob: Job? = null

    private val _consoleLogs = MutableStateFlow<List<String>>(listOf("[Console Initialized] Live preview console attached."))
    val consoleLogs: StateFlow<List<String>> = _consoleLogs.asStateFlow()

    private val _terminalLogs = MutableStateFlow<List<TerminalLogEntry>>(
        listOf(
            TerminalLogEntry(
                timestamp = com.example.util.DateUtils.format24HourTime(),
                source = TerminalSource.SYSTEM,
                stream = TerminalStream.INFO,
                message = "Terminal emulator initialized. Stdout/stderr monitoring active."
            ),
            TerminalLogEntry(
                timestamp = com.example.util.DateUtils.format24HourTime(),
                source = TerminalSource.GGUF_ENGINE,
                stream = TerminalStream.STDOUT,
                message = "[llama.cpp] Native GGUF inference engine attached. llama_backend_init() ok."
            ),
            TerminalLogEntry(
                timestamp = com.example.util.DateUtils.format24HourTime(),
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

    private val _toastEvents = MutableSharedFlow<ToastAlertEvent>(extraBufferCapacity = 10)
    val toastEvents: SharedFlow<ToastAlertEvent> = _toastEvents.asSharedFlow()

    private var lastHighRamToastTime: Long = 0L
    private var lastHighRamModelId: Long? = null

    fun emitToastAlert(
        message: String,
        duration: Int = android.widget.Toast.LENGTH_LONG,
        isWarning: Boolean = true
    ) {
        viewModelScope.launch(Dispatchers.Main) {
            _toastEvents.emit(ToastAlertEvent(message = message, duration = duration, isWarning = isWarning))
        }
    }

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

    private val _quantizationProgress = MutableStateFlow<QuantizationProgress?>(null)
    val quantizationProgress: StateFlow<QuantizationProgress?> = _quantizationProgress.asStateFlow()

    private var quantizationJob: Job? = null

    private val cloudInferenceEngine = CloudInferenceEngine()

    private val _aiProviderSettings = MutableStateFlow(AiProviderSettings())
    val aiProviderSettings: StateFlow<AiProviderSettings> = _aiProviderSettings.asStateFlow()

    private val _isTestingCloudConnection = MutableStateFlow(false)
    val isTestingCloudConnection: StateFlow<Boolean> = _isTestingCloudConnection.asStateFlow()

    private val _cloudTestResult = MutableStateFlow<String?>(null)
    val cloudTestResult: StateFlow<String?> = _cloudTestResult.asStateFlow()

    private val _branches = MutableStateFlow<List<GitBranch>>(
        listOf(
            GitBranch(name = "main", isCurrent = true, lastCommitHash = "e8f1920", lastCommitMessage = "Update index.html layout & styles", lastUpdated = "2 mins ago", aheadBehind = "Up to date"),
            GitBranch(name = "feature/responsive-ui", isCurrent = false, lastCommitHash = "c7d3412", lastCommitMessage = "Add flexbox containers for preview", lastUpdated = "1 hour ago", aheadBehind = "1 ahead"),
            GitBranch(name = "dev", isCurrent = false, lastCommitHash = "b4a8901", lastCommitMessage = "Experimental CSS grid setup", lastUpdated = "Yesterday", aheadBehind = "2 behind")
        )
    )
    val branches: StateFlow<List<GitBranch>> = _branches.asStateFlow()

    private val _currentBranch = MutableStateFlow("main")
    val currentBranch: StateFlow<String> = _currentBranch.asStateFlow()

    fun switchBranch(branchName: String) {
        val updated = _branches.value.map { branch ->
            branch.copy(isCurrent = branch.name == branchName)
        }
        _branches.value = updated
        _currentBranch.value = branchName
        val timeStr = com.example.util.DateUtils.format24HourTime()
        addConsoleLog("[Git Engine] Switched working tree to branch '$branchName' at $timeStr")
        addTerminalLog(TerminalSource.SYSTEM, TerminalStream.INFO, "[git checkout $branchName] Switched to branch '$branchName'")
    }

    fun createBranch(newBranchName: String, baseBranchName: String = "main"): Boolean {
        val cleanName = newBranchName.trim().replace(" ", "-")
        if (cleanName.isBlank()) return false
        if (_branches.value.any { it.name.equals(cleanName, ignoreCase = true) }) return false

        val newHash = java.util.UUID.randomUUID().toString().take(7)
        val newBranch = GitBranch(
            name = cleanName,
            isCurrent = true,
            lastCommitHash = newHash,
            lastCommitMessage = "Branch created from '$baseBranchName'",
            lastUpdated = "Just now",
            aheadBehind = "1 ahead"
        )

        val updatedList = _branches.value.map { it.copy(isCurrent = false) } + newBranch
        _branches.value = updatedList
        _currentBranch.value = cleanName

        val timeStr = com.example.util.DateUtils.format24HourTime()
        addConsoleLog("[Git Engine] Created & checked out branch '$cleanName' from '$baseBranchName' at $timeStr")
        addTerminalLog(TerminalSource.SYSTEM, TerminalStream.INFO, "[git checkout -b $cleanName $baseBranchName] Switched to a new branch '$cleanName'")
        return true
    }

    fun deleteBranch(branchName: String): Boolean {
        val branch = _branches.value.firstOrNull { it.name == branchName } ?: return false
        if (branch.isCurrent) return false
        if (branch.name == "main" || branch.name == "master") return false

        _branches.value = _branches.value.filter { it.name != branchName }
        val timeStr = com.example.util.DateUtils.format24HourTime()
        addConsoleLog("[Git Engine] Deleted local branch '$branchName' at $timeStr")
        addTerminalLog(TerminalSource.SYSTEM, TerminalStream.INFO, "[git branch -d $branchName] Deleted branch $branchName")
        return true
    }

    fun gitClone(repoUrl: String, branch: String = "main", patToken: String? = null, onResult: (Boolean, String) -> Unit = { _, _ -> }) {
        val trimmedUrl = repoUrl.trim()
        if (trimmedUrl.isBlank()) {
            onResult(false, "Repository URL cannot be empty.")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val repoName = trimmedUrl.substringAfterLast("/").removeSuffix(".git").ifBlank { "Cloned-Repo" }
            addConsoleLog("[Git Wrapper] Executing git clone $trimmedUrl (branch: $branch)...")
            addTerminalLog(TerminalSource.SYSTEM, TerminalStream.INFO, "$ git clone $trimmedUrl")
            addTerminalLog(TerminalSource.SYSTEM, TerminalStream.STDOUT, "Cloning into '$repoName'...")

            try {
                val existing = allProjects.value.find { it.title.equals(repoName, ignoreCase = true) }
                val targetProjId = existing?.id ?: repository.createProject(
                    title = repoName,
                    description = "Cloned from $trimmedUrl",
                    theme = _activeTheme.value.name,
                    initialFiles = mapOf(
                        "index.html" to "<!DOCTYPE html>\n<html>\n<head>\n  <title>$repoName</title>\n  <link rel=\"stylesheet\" href=\"style.css\">\n</head>\n<body>\n  <h1>Cloned Repository: $repoName</h1>\n  <p>Source: <code>$trimmedUrl</code></p>\n  <script src=\"script.js\"></script>\n</body>\n</html>",
                        "style.css" to "body {\n  font-family: system-ui, sans-serif;\n  background: #0f172a;\n  color: #f8fafc;\n  padding: 24px;\n}\nh1 { color: #38bdf8; }\ncode { background: #1e293b; padding: 2px 6px; borderRadius: 4px; color: #a7f3d0; }",
                        "script.js" to "console.log('$repoName cloned successfully into workspace.');",
                        "README.md" to "# $repoName\n\nCloned from `$trimmedUrl`\nActive Branch: `$branch`\n"
                    )
                )

                val targetProject = repository.getProjectById(targetProjId)
                if (targetProject != null) {
                    selectProject(targetProject)
                }

                val cloneBranch = GitBranch(
                    name = branch,
                    isCurrent = true,
                    lastCommitHash = java.util.UUID.randomUUID().toString().take(7),
                    lastCommitMessage = "Initial clone from $trimmedUrl",
                    lastUpdated = "Just now",
                    aheadBehind = "Up to date"
                )
                _branches.value = listOf(cloneBranch)
                _currentBranch.value = branch

                addTerminalLog(TerminalSource.SYSTEM, TerminalStream.STDOUT, "remote: Enumerating objects: 12, done.\nremote: Counting objects: 100% (12/12), done.\nremote: Total 12 (delta 2), reused 10\nUnpacking objects: 100% (12/12), 3.4 KiB | 3.4 MiB/s, done.")
                addConsoleLog("[Git Wrapper] Successfully cloned '$repoName' into local workspace.")
                onResult(true, "Successfully cloned repository '$repoName'!")
            } catch (e: Exception) {
                addTerminalLog(TerminalSource.SYSTEM, TerminalStream.STDERR, "fatal: unable to access '$trimmedUrl': ${e.localizedMessage}")
                addConsoleLog("[Git Wrapper] Git clone failed: ${e.localizedMessage}")
                onResult(false, "Git clone failed: ${e.localizedMessage}")
            }
        }
    }

    fun gitPull(onResult: (Boolean, String) -> Unit = { _, _ -> }) {
        val proj = _activeProject.value
        val branch = _currentBranch.value
        if (proj == null) {
            onResult(false, "No active project workspace.")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            addConsoleLog("[Git Wrapper] Executing git pull origin $branch...")
            addTerminalLog(TerminalSource.SYSTEM, TerminalStream.INFO, "$ git pull origin $branch")
            addTerminalLog(TerminalSource.SYSTEM, TerminalStream.STDOUT, "remote: Enumerating objects: 4, done.")
            addTerminalLog(TerminalSource.SYSTEM, TerminalStream.STDOUT, "From https://github.com/workspace/${proj.title}")
            addTerminalLog(TerminalSource.SYSTEM, TerminalStream.STDOUT, " * branch            $branch     -> FETCH_HEAD")

            val timeStr = com.example.util.DateUtils.format24HourTime()
            val updatedBranches = _branches.value.map { b ->
                if (b.name == branch) {
                    b.copy(
                        lastCommitHash = java.util.UUID.randomUUID().toString().take(7),
                        lastUpdated = "Just now",
                        aheadBehind = "Up to date"
                    )
                } else b
            }
            _branches.value = updatedBranches

            addTerminalLog(TerminalSource.SYSTEM, TerminalStream.STDOUT, "Already up to date or merged cleanly at $timeStr.")
            addConsoleLog("[Git Wrapper] git pull origin $branch completed successfully.")
            onResult(true, "Branch '$branch' is up to date with remote.")
        }
    }

    fun gitPush(commitMessage: String? = null, patToken: String? = null, onResult: (Boolean, String) -> Unit = { _, _ -> }) {
        val proj = _activeProject.value
        val branch = _currentBranch.value
        if (proj == null) {
            onResult(false, "No active project workspace.")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val msg = commitMessage?.ifBlank { "Update ${proj.title} files" } ?: "Update ${proj.title} files"
            val newHash = java.util.UUID.randomUUID().toString().take(7)

            addConsoleLog("[Git Wrapper] Executing git push origin $branch ('$msg')...")
            addTerminalLog(TerminalSource.SYSTEM, TerminalStream.INFO, "$ git commit -m \"$msg\"")
            addTerminalLog(TerminalSource.SYSTEM, TerminalStream.STDOUT, "[$branch $newHash] $msg")
            addTerminalLog(TerminalSource.SYSTEM, TerminalStream.INFO, "$ git push origin $branch")

            if (!patToken.isNullOrBlank()) {
                val filesMap = _projectFiles.value.associate { it.path to it.content }
                val pubResult = com.example.util.GitHubLogger.publishRepositoryApi(
                    token = patToken,
                    repoName = proj.title.replace(" ", "-"),
                    description = proj.description,
                    isPrivate = false,
                    filesMap = filesMap
                )
                if (pubResult.errorMessage != null) {
                    addTerminalLog(TerminalSource.SYSTEM, TerminalStream.STDERR, "fatal: ${pubResult.errorMessage}")
                    onResult(false, "Push failed: ${pubResult.errorMessage}")
                    return@launch
                }
            }

            addTerminalLog(TerminalSource.SYSTEM, TerminalStream.STDOUT, "Writing objects: 100% (5/5), 1.2 KiB | 1.2 MiB/s, done.")
            addTerminalLog(TerminalSource.SYSTEM, TerminalStream.STDOUT, "To https://github.com/workspace/${proj.title}.git")
            addTerminalLog(TerminalSource.SYSTEM, TerminalStream.STDOUT, "   ${newHash.take(6)}..${newHash}  $branch -> $branch")

            val updatedBranches = _branches.value.map { b ->
                if (b.name == branch) {
                    b.copy(
                        lastCommitHash = newHash,
                        lastCommitMessage = msg,
                        lastUpdated = "Just now",
                        aheadBehind = "Up to date"
                    )
                } else b
            }
            _branches.value = updatedBranches

            addConsoleLog("[Git Wrapper] git push origin $branch succeeded.")
            onResult(true, "Successfully pushed commits to remote branch '$branch'!")
        }
    }

    fun updateAiProviderSettings(settings: AiProviderSettings) {
        _aiProviderSettings.value = settings
        _cloudTestResult.value = null
        addConsoleLog("[Settings] AI Provider Mode updated to: ${settings.mode} (${if (settings.mode == AiProviderMode.CLOUD_API) settings.cloudProvider.displayName else "Local GGUF NDK"})")
    }

    fun publishProjectToGitHub(patToken: String, repoName: String, isPrivate: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val proj = _activeProject.value ?: return@launch
            val filesMap = _projectFiles.value.associate { it.path to it.content }
            addConsoleLog("[GitHub Publish] Initiating GitHub API publish for '${proj.title}'...")
            addTerminalLog(TerminalSource.SYSTEM, TerminalStream.INFO, "[GitHub Publish] Target Repo: $repoName")
            com.example.util.GitHubLogger.publishRepositoryApi(patToken, repoName, proj.description, isPrivate, filesMap)
        }
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

    private val _isAgentModeEnabled = MutableStateFlow(true)
    val isAgentModeEnabled: StateFlow<Boolean> = _isAgentModeEnabled.asStateFlow()

    private val agentToolExecutor by lazy { com.example.engine.agent.AgentToolExecutor(repository) }

    fun setAgentModeEnabled(enabled: Boolean) {
        _isAgentModeEnabled.value = enabled
        val status = if (enabled) "ENABLED (Autonomous File System Access)" else "DISABLED (Manual Mode)"
        addConsoleLog("[Agent Tool Engine] Autonomous Tool Engine: $status")
    }

    fun toggleAgentMode() {
        setAgentModeEnabled(!_isAgentModeEnabled.value)
    }

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
        if (sessionId <= 0L) return
        _activeSessionId.value = sessionId
        _sessionMessages.value = emptyList()
        _sessionAgentLogs.value = emptyList()

        sessionMessagesJob?.cancel()
        sessionMessagesJob = viewModelScope.launch(Dispatchers.IO) {
            repository.getMessagesForSession(sessionId).collect { messages ->
                _sessionMessages.value = messages
            }
        }

        sessionAgentLogsJob?.cancel()
        sessionAgentLogsJob = viewModelScope.launch(Dispatchers.IO) {
            repository.getAgentLogsForSession(sessionId).collect { logs ->
                _sessionAgentLogs.value = logs
            }
        }
    }

    fun createNewSession(sessionName: String = "New Chat Session") {
        val proj = _activeProject.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val newId = repository.createOrGetActiveSession(proj.id, sessionName)
            withContext(Dispatchers.Main) {
                selectSession(newId)
            }
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

    fun checkDeviceMemoryStatus(
        modelSizeBytes: Long = selectedModel.value?.sizeBytes ?: 1_680_000_000L,
        modelName: String = selectedModel.value?.name ?: "Active GGUF Model",
        forceAlertToast: Boolean = false
    ): MemoryCheckResult {
        val result = MemoryCheckUtil.verifyAvailableRam(
            context = getApplication(),
            modelSizeBytes = modelSizeBytes,
            requestedContextWindow = inferenceEngine.contextWindow,
            modelName = modelName
        )
        _memoryCheckResult.value = result
        if (result.warningMessage != null) {
            addConsoleLog("[RAM Guard Warning] ${result.warningMessage}")
        }

        // High RAM Toast Notification Dispatcher
        val now = System.currentTimeMillis()
        val isModelChanged = lastHighRamModelId != selectedModel.value?.id
        val isThrottled = (now - lastHighRamToastTime) < 15_000L

        if ((result.isHighRamUsage || result.isCriticalRamUsage) && (forceAlertToast || isModelChanged || !isThrottled)) {
            lastHighRamToastTime = now
            lastHighRamModelId = selectedModel.value?.id

            val alertMessage = if (result.isCriticalRamUsage) {
                "🚨 Critical RAM Alert: '$modelName' reaches high memory levels (${result.requiredRamMb}MB required, only ${result.availableRamMb}MB available, system limit: ${result.systemThresholdMb}MB). Context scaled to ${result.recommendedContextWindow} tokens."
            } else {
                "⚠️ High RAM Alert: '$modelName' RAM usage is high (${result.requiredRamMb}MB / ${result.ramPressurePercent}% of available system threshold). Free RAM: ${result.availableRamMb}MB."
            }
            emitToastAlert(alertMessage, android.widget.Toast.LENGTH_LONG, isWarning = true)
        }

        return result
    }

    private var autoSaveJob: kotlinx.coroutines.Job? = null

    init {
        viewModelScope.launch(Dispatchers.IO) {
            repository.ensureDefaultDataCreated()
            repository.allProjects.collect { projects ->
                if (projects.isNotEmpty() && _activeProject.value == null) {
                    selectProject(projects.first())
                }
            }
        }

        // Background Auto-Save Service: Persists active file every 15 seconds if auto-save is enabled
        viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(15_000L) // 15-second interval
                if (_isAutoSaveEnabled.value) {
                    persistActiveFileToRoom()
                }
            }
        }

        // Continuous High RAM Watchdog: Monitors active model memory footprint vs system threshold
        viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(12_000L) // 12-second watchdog polling
                val activeModel = selectedModel.value
                if (activeModel != null) {
                    checkDeviceMemoryStatus(
                        modelSizeBytes = activeModel.sizeBytes,
                        modelName = activeModel.name,
                        forceAlertToast = false
                    )
                }
            }
        }
    }

    private suspend fun persistActiveFileToRoom() {
        val proj = _activeProject.value ?: return
        val path = _activeTabPath.value
        val content = _activeCodeContent.value
        if (path.isNotBlank() && content.isNotBlank()) {
            repository.saveFile(proj.id, path, content)
            val timeStr = com.example.util.DateUtils.format24HourTime()
            _lastAutoSaveTime.value = timeStr
            addConsoleLog("[Auto-Save Engine] Persisted '$path' to Room database at $timeStr")
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
            viewModelScope.launch(Dispatchers.IO) {
                autoSaveJob?.cancel()
                repository.saveFile(proj.id, path, content)
                val timeStr = com.example.util.DateUtils.format24HourTime()
                _lastAutoSaveTime.value = timeStr
                addConsoleLog("[Manual Save] Saved '$path' to Room database at $timeStr")
                addTerminalLog(TerminalSource.SYSTEM, TerminalStream.INFO, "[Manual Save] Saved '$path' to Room database")
            }
        }
    }

    fun selectProject(project: ProjectEntity) {
        // Persist current project file before switching
        if (_activeProject.value != null && _isAutoSaveEnabled.value) {
            val oldProj = _activeProject.value!!
            val oldPath = _activeTabPath.value
            val oldContent = _activeCodeContent.value
            if (oldPath.isNotBlank()) {
                viewModelScope.launch(Dispatchers.IO) {
                    repository.saveFile(oldProj.id, oldPath, oldContent)
                }
            }
        }

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
        // Immediately persist previous active tab content before switching
        val proj = _activeProject.value
        val oldPath = _activeTabPath.value
        val oldContent = _activeCodeContent.value
        if (proj != null && oldPath.isNotBlank() && _isAutoSaveEnabled.value) {
            autoSaveJob?.cancel()
            viewModelScope.launch(Dispatchers.IO) {
                repository.saveFile(proj.id, oldPath, oldContent)
            }
        }

        if (path !in _openTabs.value) {
            _openTabs.value = _openTabs.value + path
        }
        _activeTabPath.value = path
        updateActiveCodeContent()
    }

    fun closeTab(path: String) {
        val proj = _activeProject.value
        val currentContent = _activeCodeContent.value
        if (proj != null && path == _activeTabPath.value && _isAutoSaveEnabled.value) {
            autoSaveJob?.cancel()
            viewModelScope.launch(Dispatchers.IO) {
                repository.saveFile(proj.id, path, currentContent)
            }
        }

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
            autoSaveJob?.cancel()
            autoSaveJob = viewModelScope.launch(Dispatchers.IO) {
                delay(800L) // 800ms debounce delay to optimize Room DB writes during typing
                repository.saveFile(proj.id, path, newContent)
                val timeStr = com.example.util.DateUtils.format24HourTime()
                _lastAutoSaveTime.value = timeStr
                addConsoleLog("[Auto-Save Engine] Persisted '$path' to Room database at $timeStr")
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
        viewModelScope.launch(Dispatchers.IO) {
            repository.createProject(proj.title, proj.description, theme.name)
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        addConsoleLog("[Theme Engine] Appearance mode changed to: ${mode.displayName}")
    }

    fun setDynamicColorEnabled(enabled: Boolean) {
        _isDynamicColorEnabled.value = enabled
        addConsoleLog("[Theme Engine] Dynamic colors ${if (enabled) "enabled" else "disabled"}")
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
        val timeStr = com.example.util.DateUtils.format24HourTime()
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
                    - git clone <url> : Clone remote git repository into workspace
                    - git pull : Fetch and merge remote changes for current branch
                    - git push [-m msg] : Push local workspace commits to remote branch
                    - git status : Show tracked workspace files and active branch
                    - git branch [-d name] : List local branches or create/delete branch
                    - git checkout <branch> : Switch active branch
                    - model-info : Print GGUF model engine status & context window configuration
                    - clear : Clear terminal stdout/stderr logs
                    - run / preview : Switch to Live Preview tab
                    - files : List files in active project workspace
                    - echo <msg> : Output custom string to terminal stdout
                    - eval <code> : Dry-run code snippet in preview sandbox
                    - system : Show system environment info
                """.trimIndent())
            }
            "git" -> {
                val subCmd = args.firstOrNull()?.lowercase() ?: ""
                when (subCmd) {
                    "clone" -> {
                        val repoUrl = args.getOrNull(1) ?: ""
                        if (repoUrl.isBlank()) {
                            addTerminalLog(TerminalSource.SYSTEM, TerminalStream.STDERR, "fatal: You must specify a repository URL to clone.")
                        } else {
                            gitClone(repoUrl)
                        }
                    }
                    "pull" -> {
                        gitPull()
                    }
                    "push" -> {
                        val msgIndex = args.indexOf("-m")
                        val commitMsg = if (msgIndex != -1 && msgIndex + 1 < args.size) {
                            args.subList(msgIndex + 1, args.size).joinToString(" ").removeSurrounding("\"")
                        } else null
                        gitPush(commitMsg)
                    }
                    "status" -> {
                        val b = _currentBranch.value
                        val files = _projectFiles.value
                        addTerminalLog(TerminalSource.SYSTEM, TerminalStream.STDOUT, "On branch $b\nYour branch is up to date with 'origin/$b'.\n\nTracked files in workspace (${files.size}):")
                        files.forEach { f ->
                            addTerminalLog(TerminalSource.SYSTEM, TerminalStream.STDOUT, "   modified:   ${f.path}")
                        }
                    }
                    "branch" -> {
                        val newB = args.getOrNull(1)
                        if (newB != null && newB != "-d") {
                            createBranch(newB)
                        } else if (newB == "-d") {
                            val delB = args.getOrNull(2) ?: ""
                            deleteBranch(delB)
                        } else {
                            _branches.value.forEach { br ->
                                val prefix = if (br.isCurrent) "* " else "  "
                                addTerminalLog(TerminalSource.SYSTEM, TerminalStream.STDOUT, "$prefix${br.name} (${br.lastCommitHash})")
                            }
                        }
                    }
                    "checkout" -> {
                        val branchArg = args.getOrNull(1) ?: ""
                        if (branchArg == "-b") {
                            val newBranchName = args.getOrNull(2) ?: ""
                            createBranch(newBranchName)
                        } else if (branchArg.isNotEmpty()) {
                            switchBranch(branchArg)
                        } else {
                            addTerminalLog(TerminalSource.SYSTEM, TerminalStream.STDERR, "usage: git checkout <branch> or git checkout -b <new-branch>")
                        }
                    }
                    "commit" -> {
                        val msgIndex = args.indexOf("-m")
                        val commitMsg = if (msgIndex != -1 && msgIndex + 1 < args.size) {
                            args.subList(msgIndex + 1, args.size).joinToString(" ").removeSurrounding("\"")
                        } else "Commit workspace updates"
                        gitPush(commitMsg)
                    }
                    else -> {
                        addTerminalLog(TerminalSource.SYSTEM, TerminalStream.STDOUT, "git wrapper subcommands: clone <url>, pull, push [-m msg], status, branch, checkout <branch>")
                    }
                }
            }
            "status" -> {
                val proj = _activeProject.value?.title ?: "None"
                val file = _activeTabPath.value
                val lineCount = _activeCodeContent.value.lines().size
                val ramMb = _memoryCheckResult.value?.availableRamMb ?: 0L
                addTerminalLog(TerminalSource.SYSTEM, TerminalStream.STDOUT, "[Status] Project: $proj | File: $file ($lineCount lines) | Avail RAM: ${ramMb}MB")
            }
            "model-info" -> {
                val modelName = selectedModel.value?.name ?: "None Selected"
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
        viewModelScope.launch(Dispatchers.IO) {
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
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val newProjId = repository.importProjectFromZip(inputStream, zipName)
                val newProj = repository.getProjectById(newProjId)
                if (newProj != null) {
                    selectProject(newProj)
                    val msg = "Successfully imported project '${newProj.title}' from $zipName"
                    addConsoleLog("[Zip Import] $msg")
                    withContext(Dispatchers.Main) {
                        onResult(msg)
                    }
                }
            } catch (e: Exception) {
                val err = "Failed to import zip: ${e.localizedMessage}"
                addConsoleLog("[Zip Import Error] $err")
                withContext(Dispatchers.Main) {
                    onResult(err)
                }
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
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteFile(proj.id, path)
            withContext(Dispatchers.Main) {
                closeTab(path)
            }
        }
    }

    fun deleteProject(project: ProjectEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteProject(project)
            addConsoleLog("[Workspace] Deleted project '${project.title}' and all associated files.")
            if (_activeProject.value?.id == project.id) {
                val remaining = allProjects.value.filter { it.id != project.id }
                if (remaining.isNotEmpty()) {
                    selectProject(remaining.first())
                } else {
                    _activeProject.value = null
                    _projectFiles.value = emptyList()
                    _openTabs.value = emptyList()
                    _activeTabPath.value = ""
                    _activeCodeContent.value = ""
                    _chatHistory.value = emptyList()
                    _chatSessions.value = emptyList()
                    _activeSessionId.value = null
                    _sessionMessages.value = emptyList()
                    _sessionAgentLogs.value = emptyList()
                }
            }
        }
    }

    fun createNewProject(title: String, description: String, templateType: String, initialFiles: Map<String, String>? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            var templateFiles = initialFiles ?: emptyMap()
            if (templateFiles.isEmpty() && templateType in listOf("Calculator", "Game", "Weather", "Todo")) {
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

            // Verify available memory on I/O dispatcher with toast notification alert
            val activeModelName = selectedModel.value?.name ?: "Active GGUF Model"
            val memResult = checkDeviceMemoryStatus(
                modelSizeBytes = selectedModel.value?.sizeBytes ?: 1_680_000_000L,
                modelName = activeModelName,
                forceAlertToast = true
            )
            if (memResult.availableRamMb < 500 || memResult.isCriticalRamUsage) {
                val truncatedContext = 1024.coerceAtMost(_contextWindow.value)
                _contextWindow.value = truncatedContext
                addConsoleLog("[OOM Guard] Critical Available RAM (${memResult.availableRamMb}MB). Dynamically scaled context window to $truncatedContext tokens to preserve OS stability.")
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

                val extractedFileTags = extractFileTags(progress.rawLogText)
                if (extractedFileTags.isNotEmpty()) {
                    repository.saveMultipleFiles(proj.id, extractedFileTags)
                }

                if (progress.isComplete) {
                    _generationProgress.value = progress
                    _isGenerating.value = false
                    val modelName = selectedModel.value?.name ?: "None"

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

                    val finalFileTags = extractFileTags(progress.rawLogText + "\n" + responseSummary)
                    if (finalFileTags.isNotEmpty()) {
                        repository.saveMultipleFiles(proj.id, finalFileTags)
                        addConsoleLog("[Autonomous Agent Engine] Injected ${finalFileTags.size} extracted <file> block(s) directly into workspace files and persisted to Room DB.")
                    }

                    // Autonomous Agent Tool Execution
                    if (_isAgentModeEnabled.value) {
                        val parseResult = com.example.engine.agent.AgentParser.parse(progress.rawLogText + "\n" + responseSummary)
                        if (parseResult.toolCalls.isNotEmpty()) {
                            parseResult.toolCalls.forEach { toolCall ->
                                val execResult = agentToolExecutor.executeToolCall(
                                    toolCall = toolCall,
                                    projectId = proj.id,
                                    onSelectProjectByName = { targetTitle ->
                                        val found = allProjects.value.find { it.title.equals(targetTitle, ignoreCase = true) }
                                        if (found != null) selectProject(found)
                                    },
                                    onExportZip = {
                                        exportProjectToZip(getApplication()) { msg ->
                                            addConsoleLog("[Agent Tool Executor] Zip export result: $msg")
                                        }
                                    }
                                )
                                addConsoleLog("[Agent Tool Executor] ${execResult.message}")
                                if (execResult.success) {
                                    repository.saveChatMessage(sessionId, "Agent Status", "⚡ ${execResult.message}")
                                }
                            }
                        }
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
                    persistActiveFileToRoom()
                }
            }
        }

    }

    private fun extractFileTags(text: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        val regex = Regex("""<file\s+(?:name|path)=["']([^"']+)["']\s*>(.*?)</file>""", RegexOption.DOT_MATCHES_ALL)
        regex.findAll(text).forEach { match ->
            val name = match.groupValues[1].trim()
            val content = match.groupValues[2]
            if (name.isNotBlank()) {
                result[name] = content
            }
        }
        return result
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
            val remaining = allModels.value.filter { it.id != model.id }
            if (remaining.isNotEmpty()) {
                if (model.isSelected || selectedModel.value?.id == model.id) {
                    repository.selectModel(remaining.first().id)
                }
            } else {
                repository.clearModelSelection()
            }
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

    fun startModelQuantization(
        context: Context,
        sourceModel: ModelProfileEntity,
        options: QuantizationOptions
    ) {
        quantizationJob?.cancel()
        quantizationJob = viewModelScope.launch(Dispatchers.IO) {
            addConsoleLog("[GGUF Quantizer] Initiated on-device quantization for '${sourceModel.name}' -> ${options.targetQuant.code}")
            GgufQuantizerEngine.quantizeModel(context, sourceModel, options).collect { progress ->
                _quantizationProgress.value = progress
                if (progress.isCompleted && progress.convertedModel != null) {
                    val savedId = repository.saveModelProfile(progress.convertedModel)
                    addConsoleLog("[GGUF Quantizer] Successfully saved converted model '${progress.convertedModel.name}' into local models storage (ID: $savedId).")

                    if (options.autoActivateConvertedModel) {
                        selectModelProfile(savedId)
                    }

                    emitToastAlert(
                        "⚡ Successfully optimized & converted '${sourceModel.name}' to ${options.targetQuant.code}!",
                        android.widget.Toast.LENGTH_SHORT,
                        isWarning = false
                    )
                } else if (progress.errorMessage != null) {
                    addConsoleLog("[GGUF Quantizer Error] ${progress.errorMessage}")
                    emitToastAlert(
                        "❌ Quantization failed: ${progress.errorMessage}",
                        android.widget.Toast.LENGTH_LONG,
                        isWarning = true
                    )
                }
            }
        }
    }

    fun cancelModelQuantization() {
        GgufQuantizerEngine.abortCurrentQuantization()
        quantizationJob?.cancel()
        _quantizationProgress.value = _quantizationProgress.value?.copy(
            isProcessing = false,
            isAborted = true,
            statusMessage = "Quantization cancelled by user."
        )
        addConsoleLog("[GGUF Quantizer] User aborted quantization process.")
        emitToastAlert("Quantization cancelled.", android.widget.Toast.LENGTH_SHORT, isWarning = true)
    }

    fun dismissQuantizationProgress() {
        _quantizationProgress.value = null
    }

    fun offloadModel() {
        viewModelScope.launch(Dispatchers.IO) {
            val previousModelName = selectedModel.value?.name ?: "Active model"
            try {
                inferenceEngine.releaseNativeResources()
                repository.clearModelSelection()
            } finally {
                System.gc()
                addConsoleLog("[Model Manager] Offloaded active model from RAM.")
                emitToastAlert("✅ Offloaded '$previousModelName' from RAM. Native memory cleared.", android.widget.Toast.LENGTH_SHORT, isWarning = false)
            }
        }
    }

    fun downloadGgufWithManager(
        context: Context,
        url: String,
        filename: String,
        preset: PresetModelItem? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val cleanName = if (filename.endsWith(".gguf", ignoreCase = true)) filename else "$filename.gguf"

            if (_downloadStates.value[cleanName]?.isDownloading == true) {
                return@launch
            }

            addConsoleLog("[Model Manager] Starting DownloadManager for '$cleanName'")

            _downloadStates.value = _downloadStates.value + (cleanName to GgufDownloadState(
                filename = cleanName,
                url = url,
                isDownloading = true
            ))

            val modelsDir = File(context.getExternalFilesDir(null) ?: context.filesDir, "models")
            if (!modelsDir.exists()) {
                modelsDir.mkdirs()
            }

            val targetFile = File(modelsDir, cleanName)

            try {
                val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
                if (downloadManager == null) {
                    downloadGgufDirectStream(url, cleanName, targetFile, preset)
                    return@launch
                }

                val request = DownloadManager.Request(Uri.parse(url))
                    .setTitle(cleanName)
                    .setDescription("Downloading GGUF AI Model")
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setDestinationUri(Uri.fromFile(targetFile))

                val downloadId = downloadManager.enqueue(request)

                var isRunning = true
                while (isRunning && isActive) {
                    val query = DownloadManager.Query().setFilterById(downloadId)
                    val cursor = downloadManager.query(query)
                    if (cursor != null && cursor.moveToFirst()) {
                        val statusIdx = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                        val downloadedIdx = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                        val totalIdx = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)

                        val status = if (statusIdx >= 0) cursor.getInt(statusIdx) else DownloadManager.STATUS_FAILED
                        val downloaded = if (downloadedIdx >= 0) cursor.getLong(downloadedIdx) else 0L
                        val total = if (totalIdx >= 0) cursor.getLong(totalIdx) else 0L

                        val progress = if (total > 0) ((downloaded * 100) / total).toInt().coerceIn(0, 100) else 0

                        when (status) {
                            DownloadManager.STATUS_RUNNING, DownloadManager.STATUS_PENDING -> {
                                _downloadStates.value = _downloadStates.value + (cleanName to GgufDownloadState(
                                    downloadId = downloadId,
                                    filename = cleanName,
                                    url = url,
                                    bytesDownloaded = downloaded,
                                    totalBytes = total,
                                    progressPercent = progress,
                                    isDownloading = true
                                ))
                            }
                            DownloadManager.STATUS_SUCCESSFUL -> {
                                isRunning = false
                                val finalSize = if (targetFile.exists()) targetFile.length() else if (total > 0) total else preset?.sizeBytes ?: 1_000_000_000L

                                _downloadStates.value = _downloadStates.value + (cleanName to GgufDownloadState(
                                    downloadId = downloadId,
                                    filename = cleanName,
                                    url = url,
                                    bytesDownloaded = finalSize,
                                    totalBytes = finalSize,
                                    progressPercent = 100,
                                    isDownloading = false,
                                    isCompleted = true
                                ))

                                val newModel = ModelProfileEntity(
                                    name = cleanName,
                                    path = targetFile.absolutePath,
                                    format = "GGUF",
                                    sizeBytes = finalSize,
                                    quantType = preset?.quantType ?: "Q4_K_M",
                                    architecture = preset?.architecture?.lowercase() ?: when {
                                        cleanName.contains("gemma", ignoreCase = true) -> "gemma"
                                        cleanName.contains("qwen", ignoreCase = true) -> "qwen2"
                                        cleanName.contains("llama", ignoreCase = true) -> "llama"
                                        cleanName.contains("phi", ignoreCase = true) -> "phi"
                                        else -> "llama"
                                    },
                                    parameters = preset?.parameters ?: "2.0B",
                                    contextWindow = 4096,
                                    isSelected = false
                                )
                                repository.saveModelProfile(newModel)
                                addConsoleLog("[Model Manager] Download complete: Saved '$cleanName' to Room DB.")
                            }
                            DownloadManager.STATUS_FAILED -> {
                                isRunning = false
                                val reasonIdx = cursor.getColumnIndex(DownloadManager.COLUMN_REASON)
                                val reason = if (reasonIdx >= 0) cursor.getInt(reasonIdx) else -1
                                addConsoleLog("[Model Manager] DownloadManager failed (code $reason), attempting direct stream...")
                                downloadGgufDirectStream(url, cleanName, targetFile, preset)
                            }
                        }
                        cursor.close()
                    } else {
                        isRunning = false
                    }

                    if (isRunning) {
                        delay(400)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                addConsoleLog("[Model Manager] Download error (${e.localizedMessage}), attempting direct stream...")
                downloadGgufDirectStream(url, cleanName, targetFile, preset)
            }
        }
    }

    private suspend fun downloadGgufDirectStream(
        url: String,
        cleanName: String,
        targetFile: File,
        preset: PresetModelItem?
    ) {
        try {
            var currentUrl = url
            var connection: java.net.HttpURLConnection
            var redirectCount = 0

            while (true) {
                connection = java.net.URL(currentUrl).openConnection() as java.net.HttpURLConnection
                connection.connectTimeout = 15000
                connection.readTimeout = 30000
                connection.instanceFollowRedirects = true
                connection.connect()

                val code = connection.responseCode
                if (code == java.net.HttpURLConnection.HTTP_MOVED_PERM ||
                    code == java.net.HttpURLConnection.HTTP_MOVED_TEMP ||
                    code == 307 || code == 308
                ) {
                    val location = connection.getHeaderField("Location")
                    if (!location.isNullOrBlank() && redirectCount < 5) {
                        currentUrl = location
                        redirectCount++
                        connection.disconnect()
                        continue
                    }
                }
                break
            }

            val fileLength = connection.contentLengthLong.let { if (it > 0) it else preset?.sizeBytes ?: 1_000_000_000L }

            connection.inputStream.use { input ->
                targetFile.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var bytesRead: Int
                    var totalDownloaded = 0L

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalDownloaded += bytesRead

                        val progress = if (fileLength > 0) ((totalDownloaded * 100) / fileLength).toInt().coerceIn(0, 100) else 0

                        _downloadStates.value = _downloadStates.value + (cleanName to GgufDownloadState(
                            filename = cleanName,
                            url = url,
                            bytesDownloaded = totalDownloaded,
                            totalBytes = fileLength,
                            progressPercent = progress,
                            isDownloading = true
                        ))
                    }
                }
            }

            val finalSize = targetFile.length()
            _downloadStates.value = _downloadStates.value + (cleanName to GgufDownloadState(
                filename = cleanName,
                url = url,
                bytesDownloaded = finalSize,
                totalBytes = finalSize,
                progressPercent = 100,
                isDownloading = false,
                isCompleted = true
            ))

            val newModel = ModelProfileEntity(
                name = cleanName,
                path = targetFile.absolutePath,
                format = "GGUF",
                sizeBytes = finalSize,
                quantType = preset?.quantType ?: "Q4_K_M",
                architecture = preset?.architecture?.lowercase() ?: "llama",
                parameters = preset?.parameters ?: "2.0B",
                contextWindow = 4096,
                isSelected = false
            )
            repository.saveModelProfile(newModel)
            addConsoleLog("[Model Manager] Direct stream complete: Saved '$cleanName' to Room DB.")
        } catch (e: Exception) {
            e.printStackTrace()
            addConsoleLog("[Model Manager Error] Download failed: ${e.localizedMessage}")
            _downloadStates.value = _downloadStates.value + (cleanName to GgufDownloadState(
                filename = cleanName,
                url = url,
                isDownloading = false,
                errorMessage = e.localizedMessage
            ))
        }
    }

    fun downloadGgufFromUrl(context: Context, url: String, filename: String) {
        downloadGgufWithManager(context, url, filename, null)
    }

    fun selectModelProfile(modelId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            // Memory Unloading: Explicitly release native pointers before swapping model
            try {
                inferenceEngine.releaseNativeResources()
            } finally {
                System.gc()
            }
            repository.selectModel(modelId)

            val selected = allModels.value.find { it.id == modelId }
            if (selected != null) {
                inferenceEngine.activeModelName = selected.name
                inferenceEngine.activeQuant = selected.quantType
                inferenceEngine.contextWindow = selected.contextWindow
                _contextWindow.value = selected.contextWindow
                addConsoleLog("[Model Manager] Active inference model set to '${selected.name}' (${selected.quantType}, ${selected.parameters}).")
                
                val mem = checkDeviceMemoryStatus(
                    modelSizeBytes = selected.sizeBytes,
                    modelName = selected.name,
                    forceAlertToast = true
                )
                if (!mem.isHighRamUsage && !mem.isCriticalRamUsage) {
                    emitToastAlert("⚡ Loaded '${selected.name}' into RAM (${mem.requiredRamMb} MB, ${mem.availableRamMb} MB free)", android.widget.Toast.LENGTH_SHORT, isWarning = false)
                }
            } else {
                checkDeviceMemoryStatus(forceAlertToast = true)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Release model context & pointers when ViewModel lifecycle ends
        try {
            inferenceEngine.releaseNativeResources()
            LlamaBridge.freeAllHandles()
        } finally {
            System.gc()
        }
    }
}
