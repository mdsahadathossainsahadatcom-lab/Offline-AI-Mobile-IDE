package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.ModelProfileEntity
import com.example.data.repository.WorkspaceRepository
import com.example.engine.inference.LlamaBridge
import com.example.engine.inference.LocalGgufInferenceEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * WorkspaceViewModel: Manages Workspace state, GGUF model lifecycles,
 * RAM guards, and safe native memory deallocation via LlamaBridge.
 */
class WorkspaceViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository = WorkspaceRepository(application.applicationContext)
    val ggufEngine = LocalGgufInferenceEngine(application.applicationContext)

    private val _isModelBusy = MutableStateFlow(false)
    val isModelBusy: StateFlow<Boolean> = _isModelBusy.asStateFlow()

    private val _toastEvents = MutableSharedFlow<ToastAlertEvent>(extraBufferCapacity = 64)
    val toastEvents: SharedFlow<ToastAlertEvent> = _toastEvents.asSharedFlow()

    /**
     * Safely switches to a new GGUF model with OOM safeguards, Mutex locking, and clean handle deallocation.
     */
    fun selectModel(model: ModelProfileEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            _isModelBusy.value = true
            try {
                // Check RAM safety before loading
                val memCheck = ggufEngine.checkSystemMemorySafe(getApplication())
                if (!memCheck.isSafe) {
                    _toastEvents.emit(
                        ToastAlertEvent(
                            message = "⚠️ Cannot load ${model.name}: Low RAM (${memCheck.availableRamMb}MB available)",
                            duration = android.widget.Toast.LENGTH_LONG,
                            isWarning = true
                        )
                    )
                    return@launch
                }

                // Explicitly unload previous model and flush native memory
                ggufEngine.unloadModel()

                val loadResult = ggufEngine.loadModel(
                    modelPath = model.path,
                    modelName = model.name,
                    quantType = model.quantType,
                    requestedContext = model.contextWindow,
                    context = getApplication()
                )

                if (loadResult.isSuccess) {
                    repository.selectModel(model.id)
                    _toastEvents.emit(
                        ToastAlertEvent(
                            message = "⚡ Loaded '${model.name}' into RAM (${memCheck.availableRamMb}MB free)",
                            duration = android.widget.Toast.LENGTH_SHORT,
                            isWarning = false
                        )
                    )
                } else {
                    _toastEvents.emit(
                        ToastAlertEvent(
                            message = "❌ Failed to load '${model.name}': ${loadResult.exceptionOrNull()?.message}",
                            duration = android.widget.Toast.LENGTH_LONG,
                            isWarning = true
                        )
                    )
                }
            } finally {
                _isModelBusy.value = false
            }
        }
    }

    /**
     * Offloads the active GGUF model, invokes LlamaBridge.nativeFree(), and hints garbage collection.
     */
    fun offloadModel() {
        viewModelScope.launch(Dispatchers.IO) {
            _isModelBusy.value = true
            try {
                ggufEngine.unloadModel()
                repository.clearModelSelection()
                _toastEvents.emit(
                    ToastAlertEvent(
                        message = "✅ Model offloaded. Native RAM/VRAM flushed.",
                        duration = android.widget.Toast.LENGTH_SHORT,
                        isWarning = false
                    )
                )
            } finally {
                _isModelBusy.value = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Release native llama.cpp handles on ViewModel destruction
        ggufEngine.releaseNativeResources()
        LlamaBridge.freeAllHandles()
    }
}
