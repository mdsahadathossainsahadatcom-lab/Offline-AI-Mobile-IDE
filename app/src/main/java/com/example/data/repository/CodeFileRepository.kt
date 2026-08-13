package com.example.data.repository

import android.content.Context
import com.example.data.db.AppDatabase
import com.example.data.db.FileDao
import com.example.data.db.FileEntity
import kotlinx.coroutines.flow.Flow

class CodeFileRepository(private val fileDao: FileDao) {

    constructor(context: Context) : this(AppDatabase.getInstance(context).fileDao())

    fun getFilesForProject(projectId: Long): Flow<List<FileEntity>> {
        return fileDao.getFilesForProject(projectId)
    }

    suspend fun getFileByPath(projectId: Long, path: String): FileEntity? {
        return fileDao.getFileByPath(projectId, path)
    }

    suspend fun saveFile(projectId: Long, path: String, content: String, language: String? = null): Long {
        val existingFile = fileDao.getFileByPath(projectId, path)
        val inferredLanguage = language ?: detectLanguageFromPath(path)

        return if (existingFile != null) {
            val updated = existingFile.copy(
                content = content,
                language = inferredLanguage,
                updatedAt = System.currentTimeMillis()
            )
            fileDao.updateFile(updated)
            updated.id
        } else {
            val newFile = FileEntity(
                projectId = projectId,
                path = path,
                content = content,
                language = inferredLanguage,
                updatedAt = System.currentTimeMillis()
            )
            fileDao.insertFile(newFile)
        }
    }

    suspend fun deleteFile(projectId: Long, path: String) {
        fileDao.deleteFileByPath(projectId, path)
    }

    private fun detectLanguageFromPath(path: String): String {
        return when {
            path.endsWith(".kt") || path.endsWith(".kts") -> "kotlin"
            path.endsWith(".java") -> "java"
            path.endsWith(".py") -> "python"
            path.endsWith(".js") -> "javascript"
            path.endsWith(".ts") || path.endsWith(".tsx") -> "typescript"
            path.endsWith(".html") -> "html"
            path.endsWith(".css") -> "css"
            path.endsWith(".json") -> "json"
            path.endsWith(".md") -> "markdown"
            path.endsWith(".xml") -> "xml"
            path.endsWith(".cpp") || path.endsWith(".c") || path.endsWith(".h") -> "cpp"
            else -> "text"
        }
    }
}
