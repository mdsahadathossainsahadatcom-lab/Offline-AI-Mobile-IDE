package com.example.data.db

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Immutable
@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String,
    val activeTheme: String = "NIGHT",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Immutable
@Entity(
    tableName = "files",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["projectId"]), Index(value = ["projectId", "path"], unique = true)]
)
data class FileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val path: String, // e.g., "index.html", "style.css", "script.js"
    val content: String,
    val language: String, // "html", "css", "javascript", "json"
    val updatedAt: Long = System.currentTimeMillis()
)

@Immutable
@Entity(tableName = "model_profiles")
data class ModelProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val path: String, // File path or Uri
    val format: String = "GGUF",
    val sizeBytes: Long = 0,
    val quantType: String = "Q4_K_M",
    val architecture: String = "gemma",
    val parameters: String = "2.5B",
    val contextWindow: Int = 4096,
    val isSelected: Boolean = false,
    val dateAdded: Long = System.currentTimeMillis()
)

@Immutable
@Entity(
    tableName = "chat_history",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["projectId"])]
)
data class ChatHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val prompt: String,
    val aiResponse: String,
    val timestamp: Long = System.currentTimeMillis(),
    val tokensGenerated: Int = 0,
    val speedTokensPerSec: Float = 18.5f,
    val modelUsed: String = "Gemma-2B-Q4_K_M.gguf"
)

@Immutable
@Entity(tableName = "chat_sessions")
data class ChatSessionEntity(
    @PrimaryKey(autoGenerate = true) val sessionId: Long = 0,
    val projectId: Long = 1L,
    val sessionName: String,
    val createdAtTimestamp: Long = System.currentTimeMillis(),
    val lastModified: Long = System.currentTimeMillis()
)

@Immutable
@Entity(
    tableName = "chat_messages",
    foreignKeys = [
        ForeignKey(
            entity = ChatSessionEntity::class,
            parentColumns = ["sessionId"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["sessionId"])]
)
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val messageId: Long = 0,
    val sessionId: Long,
    val sender: String, // "User" or "AI" or "Agent"
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Immutable
@Entity(
    tableName = "agent_logs",
    foreignKeys = [
        ForeignKey(
            entity = ChatSessionEntity::class,
            parentColumns = ["sessionId"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["sessionId"])]
)
data class AgentLogEntity(
    @PrimaryKey(autoGenerate = true) val logId: Long = 0,
    val sessionId: Long,
    val stepIndex: Int,
    val thought: String,
    val toolName: String,
    val targetFile: String?,
    val stepStatus: String,
    val observation: String,
    val timestamp: Long = System.currentTimeMillis()
)

