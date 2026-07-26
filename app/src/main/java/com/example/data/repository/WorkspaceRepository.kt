package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.data.db.AppDatabase
import com.example.data.db.ChatHistoryEntity
import com.example.data.db.ChatSessionEntity
import com.example.data.db.ChatMessageEntity
import com.example.data.db.AgentLogEntity
import com.example.data.db.FileEntity

import com.example.data.db.ModelProfileEntity
import com.example.data.db.ProjectEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.io.File

class WorkspaceRepository(private val context: Context) {
    private val db = AppDatabase.getInstance(context)
    private val projectDao = db.projectDao()
    private val fileDao = db.fileDao()
    private val modelProfileDao = db.modelProfileDao()
    private val chatHistoryDao = db.chatHistoryDao()
    private val chatSessionDao = db.chatSessionDao()
    private val chatMessageDao = db.chatMessageDao()
    private val agentLogDao = db.agentLogDao()

    val allProjects: Flow<List<ProjectEntity>> = projectDao.getAllProjects()
    val allModels: Flow<List<ModelProfileEntity>> = modelProfileDao.getAllModels()
    val selectedModel: Flow<ModelProfileEntity?> = modelProfileDao.getSelectedModel()

    fun getFilesForProject(projectId: Long): Flow<List<FileEntity>> {
        return fileDao.getFilesForProject(projectId)
    }

    fun getChatHistoryForProject(projectId: Long): Flow<List<ChatHistoryEntity>> {
        return chatHistoryDao.getHistoryForProject(projectId)
    }

    fun getSessionsForProject(projectId: Long): Flow<List<ChatSessionEntity>> {
        return chatSessionDao.getSessionsForProject(projectId)
    }

    fun getMessagesForSession(sessionId: Long): Flow<List<ChatMessageEntity>> {
        return chatMessageDao.getMessagesForSession(sessionId)
    }

    fun getAgentLogsForSession(sessionId: Long): Flow<List<AgentLogEntity>> {
        return agentLogDao.getLogsForSession(sessionId)
    }

    suspend fun createOrGetActiveSession(projectId: Long, sessionName: String): Long {
        val project = projectDao.getProjectById(projectId)
        val name = sessionName.ifBlank { project?.title ?: "Web Project Chat" }
        val session = com.example.data.db.ChatSessionEntity(
            projectId = projectId,
            sessionName = name
        )
        return chatSessionDao.insertSession(session)
    }

    suspend fun saveChatMessage(sessionId: Long, sender: String, content: String): Long {
        val session = chatSessionDao.getSessionById(sessionId)
        if (session != null) {
            chatSessionDao.updateSession(session.copy(lastModified = System.currentTimeMillis()))
        }
        return chatMessageDao.insertMessage(
            com.example.data.db.ChatMessageEntity(
                sessionId = sessionId,
                sender = sender,
                content = content
            )
        )
    }

    suspend fun saveAgentLog(
        sessionId: Long,
        stepIndex: Int,
        thought: String,
        toolName: String,
        targetFile: String?,
        stepStatus: String,
        observation: String
    ) {
        agentLogDao.insertLog(
            com.example.data.db.AgentLogEntity(
                sessionId = sessionId,
                stepIndex = stepIndex,
                thought = thought,
                toolName = toolName,
                targetFile = targetFile,
                stepStatus = stepStatus,
                observation = observation
            )
        )
    }

    suspend fun updateSessionName(sessionId: Long, newName: String) {
        val session = chatSessionDao.getSessionById(sessionId)
        if (session != null) {
            chatSessionDao.updateSession(session.copy(sessionName = newName, lastModified = System.currentTimeMillis()))
        }
    }

    suspend fun clearHistoryForProject(projectId: Long) {
        chatHistoryDao.clearHistoryForProject(projectId)
        chatSessionDao.deleteSessionsForProject(projectId)
    }

    suspend fun deleteSession(session: com.example.data.db.ChatSessionEntity) {
        chatSessionDao.deleteSession(session)
    }


    suspend fun getProjectById(id: Long): ProjectEntity? {
        return projectDao.getProjectById(id)
    }

    suspend fun createProject(
        title: String,
        description: String,
        theme: String = "NIGHT",
        initialFiles: Map<String, String> = emptyMap()
    ): Long {
        val project = ProjectEntity(
            title = title,
            description = description,
            activeTheme = theme
        )
        val projectId = projectDao.insertProject(project)

        if (initialFiles.isNotEmpty()) {
            val fileEntities = initialFiles.map { (path, content) ->
                FileEntity(
                    projectId = projectId,
                    path = path,
                    content = content,
                    language = determineLanguage(path)
                )
            }
            fileDao.insertFiles(fileEntities)
        }

        return projectId
    }

    suspend fun saveFile(projectId: Long, path: String, content: String) {
        val existing = fileDao.getFileByPath(projectId, path)
        if (existing != null) {
            fileDao.updateFile(existing.copy(content = content, updatedAt = System.currentTimeMillis()))
        } else {
            fileDao.insertFile(
                FileEntity(
                    projectId = projectId,
                    path = path,
                    content = content,
                    language = determineLanguage(path)
                )
            )
        }
        // Save to workspace directory on device storage
        writeToStorageFile(projectId, path, content)
    }

    suspend fun saveMultipleFiles(projectId: Long, files: Map<String, String>) {
        files.forEach { (path, content) ->
            saveFile(projectId, path, content)
        }
        // Update project timestamp
        val p = projectDao.getProjectById(projectId)
        if (p != null) {
            projectDao.updateProject(p.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    suspend fun deleteFile(projectId: Long, path: String) {
        fileDao.deleteFileByPath(projectId, path)
        deleteFromStorageFile(projectId, path)
    }

    suspend fun deleteProject(project: ProjectEntity) {
        projectDao.deleteProject(project)
        val dir = File(context.filesDir, "workspace_${project.id}")
        if (dir.exists()) {
            dir.deleteRecursively()
        }
    }

    suspend fun addChatHistory(projectId: Long, prompt: String, aiResponse: String, tokens: Int, speed: Float, modelUsed: String) {
        chatHistoryDao.insertChat(
            ChatHistoryEntity(
                projectId = projectId,
                prompt = prompt,
                aiResponse = aiResponse,
                tokensGenerated = tokens,
                speedTokensPerSec = speed,
                modelUsed = modelUsed
            )
        )
    }

    suspend fun addModelProfile(
        name: String,
        path: String,
        sizeBytes: Long,
        quantType: String,
        architecture: String,
        parameters: String,
        contextWindow: Int
    ): Long {
        modelProfileDao.clearSelection()
        val model = ModelProfileEntity(
            name = name,
            path = path,
            sizeBytes = sizeBytes,
            quantType = quantType,
            architecture = architecture,
            parameters = parameters,
            contextWindow = contextWindow,
            isSelected = true
        )
        return modelProfileDao.insertModel(model)
    }

    suspend fun importGgufFileToStorage(
        context: Context,
        uri: Uri,
        onProgress: (Float, Long, Long) -> Unit
    ): ModelProfileEntity {
        val contentResolver = context.contentResolver
        var displayName = "Model_${System.currentTimeMillis()}.gguf"

        try {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    val name = cursor.getString(nameIndex)
                    if (!name.isNullOrBlank()) {
                        displayName = name
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (!displayName.endsWith(".gguf", ignoreCase = true)) {
            displayName += ".gguf"
        }

        val modelsDir = File(context.filesDir, "models")
        if (!modelsDir.exists()) {
            modelsDir.mkdirs()
        }

        var targetFile = File(modelsDir, displayName)
        if (targetFile.exists()) {
            val baseName = displayName.removeSuffix(".gguf")
            displayName = "${baseName}_${System.currentTimeMillis().toString().takeLast(4)}.gguf"
            targetFile = File(modelsDir, displayName)
        }

        var fileSize = 0L
        try {
            val descriptor = contentResolver.openFileDescriptor(uri, "r")
            fileSize = descriptor?.statSize ?: 0L
            descriptor?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val inputStream = contentResolver.openInputStream(uri)
            ?: throw java.io.IOException("Unable to open input stream from $uri")

        inputStream.use { input ->
            targetFile.outputStream().use { output ->
                val buffer = ByteArray(64 * 1024)
                var bytesRead: Int
                var totalBytesRead = 0L

                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    totalBytesRead += bytesRead
                    val progress = if (fileSize > 0) totalBytesRead.toFloat() / fileSize else 0.5f
                    onProgress(progress, totalBytesRead, fileSize)
                }
            }
        }

        if (fileSize <= 0) {
            fileSize = targetFile.length()
        }

        val metadata = com.example.engine.gguf.GgufHeaderParser.parseGgufUri(context, Uri.fromFile(targetFile))

        val modelId = addModelProfile(
            name = displayName,
            path = targetFile.absolutePath,
            sizeBytes = if (metadata.sizeBytes > 0) metadata.sizeBytes else fileSize,
            quantType = metadata.quantType,
            architecture = metadata.architecture,
            parameters = metadata.estimatedParams,
            contextWindow = metadata.contextWindow
        )

        return ModelProfileEntity(
            id = modelId,
            name = displayName,
            path = targetFile.absolutePath,
            sizeBytes = if (metadata.sizeBytes > 0) metadata.sizeBytes else fileSize,
            quantType = metadata.quantType,
            architecture = metadata.architecture,
            parameters = metadata.estimatedParams,
            contextWindow = metadata.contextWindow,
            isSelected = true
        )
    }

    suspend fun deleteModelProfile(model: ModelProfileEntity) {
        try {
            if (model.path.isNotBlank() && !model.path.startsWith("internal://")) {
                val file = File(model.path)
                if (file.exists()) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        modelProfileDao.deleteModel(model)
    }

    suspend fun renameModelProfile(id: Long, newName: String) {
        modelProfileDao.renameModel(id, newName)
    }

    suspend fun selectModel(id: Long) {
        modelProfileDao.clearSelection()
        modelProfileDao.selectModel(id)
    }

    private fun writeToStorageFile(projectId: Long, relativePath: String, content: String) {
        try {
            val projectDir = File(context.filesDir, "workspace_$projectId")
            if (!projectDir.exists()) projectDir.mkdirs()
            val targetFile = File(projectDir, relativePath)
            targetFile.parentFile?.mkdirs()
            targetFile.writeText(content)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun deleteFromStorageFile(projectId: Long, relativePath: String) {
        try {
            val projectDir = File(context.filesDir, "workspace_$projectId")
            val targetFile = File(projectDir, relativePath)
            if (targetFile.exists()) targetFile.delete()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun determineLanguage(path: String): String {
        return when {
            path.endsWith(".html", ignoreCase = true) || path.endsWith(".htm", ignoreCase = true) -> "html"
            path.endsWith(".css", ignoreCase = true) -> "css"
            path.endsWith(".js", ignoreCase = true) || path.endsWith(".ts", ignoreCase = true) -> "javascript"
            path.endsWith(".py", ignoreCase = true) -> "python"
            path.endsWith(".json", ignoreCase = true) -> "json"
            path.endsWith(".md", ignoreCase = true) -> "markdown"
            path.endsWith(".svg", ignoreCase = true) -> "svg"
            else -> "text"
        }
    }

    suspend fun exportProjectToZip(projectId: Long): File? {
        val project = projectDao.getProjectById(projectId) ?: return null
        val sourceDir = File(context.filesDir, "workspace_$projectId")
        if (!sourceDir.exists()) return null

        val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
            ?: context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS)
            ?: context.cacheDir

        if (!downloadsDir.exists()) downloadsDir.mkdirs()

        val safeTitle = project.title.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val zipFile = File(downloadsDir, "${safeTitle}_export.zip")
        com.example.util.FileUtils.zipDirectory(sourceDir, zipFile)
        return zipFile
    }

    suspend fun importProjectFromZip(inputStream: java.io.InputStream, zipFileName: String): Long {
        val cleanTitle = zipFileName.removeSuffix(".zip").replace("_", " ").ifBlank { "Imported Web Project" }
        val projectId = createProject(
            title = cleanTitle,
            description = "Imported workspace archive ($zipFileName)",
            theme = "NIGHT"
        )
        val targetDir = File(context.filesDir, "workspace_$projectId")
        if (!targetDir.exists()) targetDir.mkdirs()

        val extractedFiles = com.example.util.FileUtils.unzipToDirectory(inputStream, targetDir)
        if (extractedFiles.isNotEmpty()) {
            saveMultipleFiles(projectId, extractedFiles)
        } else {
            saveFile(projectId, "index.html", "<!-- Unzipped Project -->\n<h1>$cleanTitle</h1>")
        }

        return projectId
    }

    suspend fun ensureDefaultDataCreated() {
        val existingProjects = allProjects.first()
        if (existingProjects.isEmpty()) {
            val pId = createProject(
                title = "Smart Scientific Calculator",
                description = "Multi-file web app with scientific functions and responsive design.",
                theme = "NIGHT",
                initialFiles = mapOf(
                    "index.html" to """
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Smart Scientific Calculator</title>
  <link rel="stylesheet" href="style.css">
</head>
<body>
  <div class="calculator">
    <div class="display-container">
      <div id="history" class="history"></div>
      <input type="text" id="display" class="display" readonly value="0">
    </div>
    <div class="keypad">
      <button class="btn btn-action" onclick="clearDisplay()">AC</button>
      <button class="btn btn-action" onclick="deleteChar()">DEL</button>
      <button class="btn btn-func" onclick="appendFunc('Math.sqrt(')">√</button>
      <button class="btn btn-op" onclick="appendOp('/')">÷</button>

      <button class="btn" onclick="appendNum('7')">7</button>
      <button class="btn" onclick="appendNum('8')">8</button>
      <button class="btn" onclick="appendNum('9')">9</button>
      <button class="btn btn-op" onclick="appendOp('*')">×</button>

      <button class="btn" onclick="appendNum('4')">4</button>
      <button class="btn" onclick="appendNum('5')">5</button>
      <button class="btn" onclick="appendNum('6')">6</button>
      <button class="btn btn-op" onclick="appendOp('-')">-</button>

      <button class="btn" onclick="appendNum('1')">1</button>
      <button class="btn" onclick="appendNum('2')">2</button>
      <button class="btn" onclick="appendNum('3')">3</button>
      <button class="btn btn-op" onclick="appendOp('+')">+</button>

      <button class="btn" onclick="appendNum('0')">0</button>
      <button class="btn" onclick="appendNum('.')">.</button>
      <button class="btn btn-func" onclick="appendFunc('Math.pow(')">xʸ</button>
      <button class="btn btn-equals" onclick="calculate()">=</button>
    </div>
  </div>
  <script src="script.js"></script>
</body>
</html>
""".trimIndent(),
                    "style.css" to """
* { box-sizing: border-box; margin: 0; padding: 0; font-family: system-ui, sans-serif; }
body { background: #0f172a; min-height: 100vh; display: flex; justify-content: center; align-items: center; padding: 20px; color: #f8fafc; }
.calculator { background: #1e293b; width: 100%; max-width: 360px; border-radius: 24px; padding: 24px; border: 1px solid rgba(255,255,255,0.1); }
.display-container { background: #0f172a; border-radius: 16px; padding: 16px; margin-bottom: 20px; text-align: right; border: 1px solid rgba(56, 189, 248, 0.3); }
.history { font-size: 14px; color: #94a3b8; min-height: 20px; }
.display { width: 100%; background: transparent; border: none; color: #38bdf8; font-size: 36px; font-weight: 600; text-align: right; outline: none; }
.keypad { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; }
.btn { background: #334155; color: #f8fafc; border: none; height: 56px; border-radius: 16px; font-size: 18px; font-weight: 600; cursor: pointer; }
.btn-action { background: #f43f5e; color: #fff; }
.btn-op { background: #38bdf8; color: #0f172a; }
.btn-func { background: #818cf8; color: #fff; }
.btn-equals { background: #4ade80; color: #0f172a; }
""".trimIndent(),
                    "script.js" to """
let display = document.getElementById('display');
let history = document.getElementById('history');
let currentExpr = '';

function appendNum(num) {
  if (display.value === '0') currentExpr = num;
  else currentExpr += num;
  display.value = currentExpr;
}

function appendOp(op) {
  currentExpr += ' ' + op + ' ';
  display.value = currentExpr;
}

function appendFunc(funcStr) {
  currentExpr += funcStr;
  display.value = currentExpr;
}

function clearDisplay() {
  currentExpr = '';
  display.value = '0';
  history.innerText = '';
}

function deleteChar() {
  currentExpr = currentExpr.slice(0, -1);
  display.value = currentExpr || '0';
}

function calculate() {
  try {
    history.innerText = currentExpr + ' =';
    let result = eval(currentExpr.replace(/×/g, '*').replace(/÷/g, '/'));
    display.value = result;
    currentExpr = String(result);
  } catch (err) {
    display.value = 'Error';
    currentExpr = '';
  }
}
console.log('App initialized on Local AI IDE.');
""".trimIndent()
                )
            )
        }

        val existingModels = allModels.first()
        if (existingModels.isEmpty()) {
            addModelProfile(
                name = "Gemma-2B-it-Q4_K_M.gguf",
                path = "internal://models/gemma-4-2b-it.gguf",
                sizeBytes = 1_680_000_000L,
                quantType = "Q4_K_M",
                architecture = "gemma",
                parameters = "2.5B",
                contextWindow = 4096
            )
            addModelProfile(
                name = "Llama-3-8B-Instruct.Q4_K_M.gguf",
                path = "internal://models/llama-3-8b.gguf",
                sizeBytes = 4_580_000_000L,
                quantType = "Q4_K_M",
                architecture = "llama",
                parameters = "8B",
                contextWindow = 8192
            )
        }
    }
}
