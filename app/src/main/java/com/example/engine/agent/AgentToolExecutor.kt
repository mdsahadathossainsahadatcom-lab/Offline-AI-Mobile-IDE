package com.example.engine.agent

import com.example.data.db.ProjectEntity
import com.example.data.repository.WorkspaceRepository
import kotlinx.coroutines.flow.first

data class ToolExecutionResult(
    val success: Boolean,
    val actionName: String,
    val message: String,
    val affectedFile: String? = null
)

class AgentToolExecutor(
    private val repository: WorkspaceRepository
) {

    suspend fun createFile(projectId: Long, path: String, content: String): ToolExecutionResult {
        return try {
            val cleanPath = path.trim().removePrefix("/")
            if (cleanPath.isBlank()) {
                return ToolExecutionResult(false, "CREATE_FILE", "Error: Filename path cannot be blank.")
            }
            repository.saveFile(projectId, cleanPath, content)
            ToolExecutionResult(
                success = true,
                actionName = "CREATE_FILE",
                message = "Agent created file '$cleanPath'",
                affectedFile = cleanPath
            )
        } catch (e: Exception) {
            ToolExecutionResult(false, "CREATE_FILE", "Failed to create file '$path': ${e.localizedMessage}")
        }
    }

    suspend fun updateFile(
        projectId: Long,
        path: String,
        newContent: String?,
        searchBlock: String?,
        replaceBlock: String?
    ): ToolExecutionResult {
        return try {
            val cleanPath = path.trim().removePrefix("/")
            if (cleanPath.isBlank()) {
                return ToolExecutionResult(false, "UPDATE_FILE", "Error: Filename path cannot be blank.")
            }

            val files = repository.getFilesForProject(projectId).first()
            val existingFile = files.find { it.path.equals(cleanPath, ignoreCase = true) }

            if (existingFile == null && newContent == null) {
                return ToolExecutionResult(false, "UPDATE_FILE", "File '$cleanPath' not found in workspace.")
            }

            val finalContent = when {
                !searchBlock.isNullOrEmpty() && !replaceBlock.isNullOrEmpty() && existingFile != null -> {
                    if (existingFile.content.contains(searchBlock)) {
                        existingFile.content.replace(searchBlock, replaceBlock)
                    } else {
                        return ToolExecutionResult(false, "UPDATE_FILE", "Target search block not found in '$cleanPath'.")
                    }
                }
                newContent != null -> newContent
                else -> existingFile?.content ?: ""
            }

            repository.saveFile(projectId, cleanPath, finalContent)
            ToolExecutionResult(
                success = true,
                actionName = "UPDATE_FILE",
                message = "Agent updated file '$cleanPath'",
                affectedFile = cleanPath
            )
        } catch (e: Exception) {
            ToolExecutionResult(false, "UPDATE_FILE", "Failed to update file '$path': ${e.localizedMessage}")
        }
    }

    suspend fun deleteFile(projectId: Long, path: String): ToolExecutionResult {
        return try {
            val cleanPath = path.trim().removePrefix("/")
            val files = repository.getFilesForProject(projectId).first()
            val fileEntity = files.find { it.path.equals(cleanPath, ignoreCase = true) }
            if (fileEntity != null) {
                // Delete from room and storage
                repository.deleteFile(projectId, cleanPath)
                ToolExecutionResult(true, "DELETE_FILE", "Agent deleted file '$cleanPath'", cleanPath)
            } else {
                ToolExecutionResult(false, "DELETE_FILE", "File '$cleanPath' not found.")
            }
        } catch (e: Exception) {
            ToolExecutionResult(false, "DELETE_FILE", "Failed to delete '$path': ${e.localizedMessage}")
        }
    }

    suspend fun executeToolCall(
        toolCall: AgentToolCall,
        projectId: Long,
        onSelectProjectByName: suspend (String) -> Unit = {},
        onExportZip: () -> Unit = {}
    ): ToolExecutionResult {
        return when (toolCall.action.uppercase()) {
            "CREATE_FILE" -> {
                val path = toolCall.filename ?: "index.html"
                val content = toolCall.content ?: ""
                createFile(projectId, path, content)
            }

            "UPDATE_FILE", "EDIT_FILE" -> {
                val path = toolCall.filename ?: "index.html"
                updateFile(
                    projectId = projectId,
                    path = path,
                    newContent = toolCall.content,
                    searchBlock = toolCall.searchBlock,
                    replaceBlock = toolCall.replaceBlock
                )
            }

            "DELETE_FILE" -> {
                val path = toolCall.filename ?: ""
                deleteFile(projectId, path)
            }

            "SWITCH_WORKSPACE", "SELECT_PROJECT" -> {
                val targetName = toolCall.workspaceName ?: ""
                if (targetName.isNotBlank()) {
                    onSelectProjectByName(targetName)
                    ToolExecutionResult(true, "SWITCH_WORKSPACE", "Switched workspace to '$targetName'")
                } else {
                    ToolExecutionResult(false, "SWITCH_WORKSPACE", "Workspace name missing.")
                }
            }

            "EXPORT_ZIP", "EXPORT_PROJECT" -> {
                onExportZip()
                ToolExecutionResult(true, "EXPORT_ZIP", "Triggered workspace ZIP export.")
            }

            else -> {
                ToolExecutionResult(false, toolCall.action, "Unknown tool action: ${toolCall.action}")
            }
        }
    }
}
