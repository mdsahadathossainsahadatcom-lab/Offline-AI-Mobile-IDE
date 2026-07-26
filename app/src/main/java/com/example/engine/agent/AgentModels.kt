package com.example.engine.agent

import androidx.compose.runtime.Immutable

enum class AgentStepStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    FAILED
}

@Immutable
data class AgentStep(
    val stepIndex: Int,
    val thought: String,
    val toolName: String,
    val targetFile: String? = null,
    val status: AgentStepStatus = AgentStepStatus.PENDING,
    val observation: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Immutable
data class AgentState(
    val isRunning: Boolean = false,
    val userGoal: String = "",
    val currentStepIndex: Int = 0,
    val maxSteps: Int = 10,
    val steps: List<AgentStep> = emptyList(),
    val statusMessage: String = "",
    val logs: List<String> = emptyList(),
    val isCancelled: Boolean = false
)
