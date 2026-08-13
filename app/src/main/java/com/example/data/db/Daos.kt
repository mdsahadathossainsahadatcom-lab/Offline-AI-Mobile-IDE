package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY updatedAt DESC")
    fun getAllProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    suspend fun getProjectById(id: Long): ProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity): Long

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Delete
    suspend fun deleteProject(project: ProjectEntity)
}

@Dao
interface FileDao {
    @Query("SELECT * FROM files WHERE projectId = :projectId ORDER BY path ASC")
    fun getFilesForProject(projectId: Long): Flow<List<FileEntity>>

    @Query("SELECT * FROM files WHERE projectId = :projectId AND path = :path LIMIT 1")
    suspend fun getFileByPath(projectId: Long, path: String): FileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: FileEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFiles(files: List<FileEntity>)

    @Update
    suspend fun updateFile(file: FileEntity)

    @Delete
    suspend fun deleteFile(file: FileEntity)

    @Query("DELETE FROM files WHERE projectId = :projectId AND path = :path")
    suspend fun deleteFileByPath(projectId: Long, path: String)

    @Query("DELETE FROM files WHERE projectId = :projectId")
    suspend fun deleteAllFilesForProject(projectId: Long)
}

@Dao
interface ModelProfileDao {
    @Query("SELECT * FROM model_profiles ORDER BY dateAdded DESC")
    fun getAllModels(): Flow<List<ModelProfileEntity>>

    @Query("SELECT * FROM model_profiles WHERE isSelected = 1 LIMIT 1")
    fun getSelectedModel(): Flow<ModelProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModel(model: ModelProfileEntity): Long

    @Query("UPDATE model_profiles SET isSelected = 0")
    suspend fun clearSelection()

    @Query("UPDATE model_profiles SET isSelected = 1 WHERE id = :id")
    suspend fun selectModel(id: Long)

    @Query("UPDATE model_profiles SET name = :newName WHERE id = :id")
    suspend fun renameModel(id: Long, newName: String)

    @Delete
    suspend fun deleteModel(model: ModelProfileEntity)
}

@Dao
interface ChatHistoryDao {
    @Query("SELECT * FROM chat_history WHERE projectId = :projectId ORDER BY timestamp ASC")
    fun getHistoryForProject(projectId: Long): Flow<List<ChatHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChat(chat: ChatHistoryEntity): Long

    @Query("DELETE FROM chat_history WHERE projectId = :projectId")
    suspend fun clearHistoryForProject(projectId: Long)
}

@Dao
interface ChatSessionDao {
    @Query("SELECT * FROM chat_sessions WHERE projectId = :projectId ORDER BY lastModified DESC")
    fun getSessionsForProject(projectId: Long): Flow<List<ChatSessionEntity>>

    @Query("SELECT * FROM chat_sessions ORDER BY lastModified DESC")
    fun getAllSessions(): Flow<List<ChatSessionEntity>>

    @Query("SELECT * FROM chat_sessions WHERE sessionId = :sessionId LIMIT 1")
    suspend fun getSessionById(sessionId: Long): ChatSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ChatSessionEntity): Long

    @Update
    suspend fun updateSession(session: ChatSessionEntity)

    @Delete
    suspend fun deleteSession(session: ChatSessionEntity)

    @Query("DELETE FROM chat_sessions WHERE projectId = :projectId")
    suspend fun deleteSessionsForProject(projectId: Long)
}

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getMessagesForSession(sessionId: Long): Flow<List<ChatMessageEntity>>

    @Query("SELECT COUNT(*) FROM chat_messages WHERE sessionId = :sessionId")
    suspend fun getMessageCountForSession(sessionId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity): Long

    @Delete
    suspend fun deleteMessage(message: ChatMessageEntity)

    @Query("DELETE FROM chat_messages WHERE sessionId = :sessionId")
    suspend fun clearMessagesForSession(sessionId: Long)
}

@Dao
interface AgentLogDao {
    @Query("SELECT * FROM agent_logs WHERE sessionId = :sessionId ORDER BY stepIndex ASC")
    fun getLogsForSession(sessionId: Long): Flow<List<AgentLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: AgentLogEntity): Long

    @Delete
    suspend fun deleteLog(log: AgentLogEntity)

    @Query("DELETE FROM agent_logs WHERE sessionId = :sessionId")
    suspend fun clearLogsForSession(sessionId: Long)
}

