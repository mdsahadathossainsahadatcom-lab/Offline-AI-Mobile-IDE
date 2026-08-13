package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.FileEntity
import com.example.data.repository.CodeFileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class CodeFileViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: CodeFileRepository = CodeFileRepository(application)
) : AndroidViewModel(application) {

    private val _editorContent = MutableStateFlow("")
    val editorContent: StateFlow<String> = _editorContent.asStateFlow()

    private val _activePath = MutableStateFlow("index.html")
    val activePath: StateFlow<String> = _activePath.asStateFlow()

    private val _projectId = MutableStateFlow(1L)
    val projectId: StateFlow<Long> = _projectId.asStateFlow()

    private val _saveStatus = MutableStateFlow("Saved")
    val saveStatus: StateFlow<String> = _saveStatus.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    val projectFiles: Flow<List<FileEntity>>
        get() = repository.getFilesForProject(_projectId.value)

    init {
        // Reactive Flow with 800ms debounce to automatically trigger Room DB save on editor text changes
        viewModelScope.launch(Dispatchers.IO) {
            _editorContent
                .debounce(800L)
                .distinctUntilChanged()
                .filter { _activePath.value.isNotBlank() }
                .collect { content ->
                    performSave(content)
                }
        }
    }

    /**
     * Called whenever user edits text in the editor.
     * Updates local state immediately and triggers Flow debounce pipeline.
     */
    fun onContentChanged(newContent: String) {
        if (_editorContent.value != newContent) {
            _editorContent.value = newContent
            _saveStatus.value = "Unsaved changes"
        }
    }

    /**
     * Switch active file in editor and load its content from Room database.
     */
    fun openFile(projectId: Long, path: String) {
        _projectId.value = projectId
        _activePath.value = path
        viewModelScope.launch(Dispatchers.IO) {
            val fileEntity = repository.getFileByPath(projectId, path)
            _editorContent.value = fileEntity?.content ?: ""
            _saveStatus.value = "Saved"
        }
    }

    /**
     * Force immediate manual save without waiting for debounce timer.
     */
    fun forceSave() {
        viewModelScope.launch(Dispatchers.IO) {
            performSave(_editorContent.value)
        }
    }

    private suspend fun performSave(content: String) {
        val currentPath = _activePath.value
        val currentProjId = _projectId.value
        if (currentPath.isBlank()) return

        _isSaving.value = true
        _saveStatus.value = "Saving..."
        try {
            repository.saveFile(
                projectId = currentProjId,
                path = currentPath,
                content = content
            )
            val timeStr = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
            _saveStatus.value = "Saved ($timeStr)"
        } catch (e: Exception) {
            _saveStatus.value = "Save failed: ${e.localizedMessage}"
        } finally {
            _isSaving.value = false
        }
    }
}
