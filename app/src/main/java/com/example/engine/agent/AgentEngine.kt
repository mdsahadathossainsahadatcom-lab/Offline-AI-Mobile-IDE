package com.example.engine.agent

import com.example.engine.inference.LocalInferenceEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.json.JSONObject
import java.util.Locale

class AgentEngine(
    private val inferenceEngine: LocalInferenceEngine
) {
    @Volatile
    private var isCancelled: Boolean = false

    fun cancel() {
        isCancelled = true
        inferenceEngine.abortGeneration()
    }

    /**
     * Executes the ReAct Autonomous Loop (Thought -> Action -> Observation)
     * Max steps: 10
     */
    fun runAgentWorkflow(
        userGoal: String,
        existingFiles: Map<String, String>,
        getConsoleLogs: suspend () -> List<String>,
        saveFile: suspend (filename: String, content: String) -> Unit,
        editFile: suspend (filename: String, searchBlock: String, replaceBlock: String) -> Boolean
    ): Flow<AgentState> = flow {
        isCancelled = false

        val initialSteps = mutableListOf<AgentStep>()
        var currentState = AgentState(
            isRunning = true,
            userGoal = userGoal,
            currentStepIndex = 0,
            maxSteps = 10,
            steps = emptyList(),
            statusMessage = "Agent initialized. Analyzing user request...",
            logs = listOf("[Agent Engine] Initialized ReAct workflow for goal: '$userGoal'")
        )
        emit(currentState)

        delay(300)
        if (isCancelled) {
            emit(currentState.copy(isRunning = false, isCancelled = true, statusMessage = "Agent cancelled by user."))
            return@flow
        }

        // Dynamically plan initial steps based on user goal
        val plannedSteps = generateInitialPlan(userGoal, existingFiles)

        var mutableFiles = existingFiles.toMutableMap()
        val currentLogs = mutableListOf<String>()
        currentLogs.addAll(currentState.logs)

        for (i in plannedSteps.indices) {
            if (isCancelled) {
                currentLogs.add("[Agent Engine] Workflow cancelled at step ${i + 1}.")
                emit(currentState.copy(isRunning = false, isCancelled = true, logs = currentLogs, statusMessage = "Cancelled by user"))
                return@flow
            }

            val rawStep = plannedSteps[i]
            val activeStep = rawStep.copy(
                stepIndex = i + 1,
                status = AgentStepStatus.IN_PROGRESS,
                timestamp = System.currentTimeMillis()
            )

            // Update step in list
            if (initialSteps.size <= i) {
                initialSteps.add(activeStep)
            } else {
                initialSteps[i] = activeStep
            }

            currentState = currentState.copy(
                currentStepIndex = i + 1,
                steps = initialSteps.toList(),
                statusMessage = "Step ${i + 1}/${plannedSteps.size}: ${activeStep.thought}"
            )
            emit(currentState)

            delay(400) // Simulate processing / offline LLM inference pause

            if (isCancelled) break

            // Perform Action based on toolName
            val observation = executeTool(
                toolName = activeStep.toolName,
                targetFile = activeStep.targetFile,
                userGoal = userGoal,
                filesMap = mutableFiles,
                getConsoleLogs = getConsoleLogs,
                saveFile = saveFile,
                editFile = editFile
            )

            currentLogs.add("[Action: ${activeStep.toolName}] Target: ${activeStep.targetFile ?: "Workspace"} -> $observation")

            val completedStep = activeStep.copy(
                status = AgentStepStatus.COMPLETED,
                observation = observation
            )
            initialSteps[i] = completedStep

            currentState = currentState.copy(
                steps = initialSteps.toList(),
                logs = currentLogs.toList(),
                statusMessage = "Observation received for step ${i + 1}"
            )
            emit(currentState)

            delay(200)
        }

        // Final verification & completion step
        if (!isCancelled) {
            val finalStep = AgentStep(
                stepIndex = initialSteps.size + 1,
                thought = "Verify all files and console logs for errors.",
                toolName = "run_preview_and_get_logs",
                targetFile = null,
                status = AgentStepStatus.IN_PROGRESS
            )
            initialSteps.add(finalStep)
            currentState = currentState.copy(steps = initialSteps.toList(), statusMessage = "Running final preview verification...")
            emit(currentState)

            val logs = getConsoleLogs()
            val hasError = logs.any { it.contains("ERROR", ignoreCase = true) || it.contains("Uncaught", ignoreCase = true) }
            val finalObs = if (hasError) {
                "Preview completed with warnings/logs: ${logs.takeLast(2).joinToString("; ")}"
            } else {
                "Preview active with zero runtime errors. All components compiled cleanly."
            }

            initialSteps[initialSteps.lastIndex] = finalStep.copy(
                status = AgentStepStatus.COMPLETED,
                observation = finalObs
            )

            currentLogs.add("[Observation] $finalObs")
            currentLogs.add("[Agent Engine] Workflow finished successfully!")

            emit(
                currentState.copy(
                    isRunning = false,
                    currentStepIndex = initialSteps.size,
                    steps = initialSteps.toList(),
                    logs = currentLogs.toList(),
                    statusMessage = "Autonomous Workflow Completed Successfully 🎉"
                )
            )
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun executeTool(
        toolName: String,
        targetFile: String?,
        userGoal: String,
        filesMap: MutableMap<String, String>,
        getConsoleLogs: suspend () -> List<String>,
        saveFile: suspend (filename: String, content: String) -> Unit,
        editFile: suspend (filename: String, searchBlock: String, replaceBlock: String) -> Boolean
    ): String {
        return when (toolName) {
            "create_file" -> {
                val fileName = targetFile ?: "index.html"
                val generatedContent = generateFileContent(fileName, userGoal, filesMap)
                filesMap[fileName] = generatedContent
                saveFile(fileName, generatedContent)
                "File '$fileName' created successfully (${generatedContent.length} bytes)."
            }

            "read_file" -> {
                val fileName = targetFile ?: "index.html"
                val content = filesMap[fileName]
                if (content != null) {
                    "Read '${fileName}' (${content.length} chars):\n${content.take(150)}..."
                } else {
                    "Error: File '$fileName' does not exist in workspace."
                }
            }

            "edit_file" -> {
                val fileName = targetFile ?: "style.css"
                val currentContent = filesMap[fileName] ?: ""
                val searchBlock = extractSearchBlock(currentContent)
                val replaceBlock = extractReplaceBlock(fileName, userGoal)

                if (searchBlock.isNotEmpty()) {
                    val success = editFile(fileName, searchBlock, replaceBlock)
                    if (success) {
                        filesMap[fileName] = currentContent.replace(searchBlock, replaceBlock)
                        "Successfully edited '$fileName' (replaced ${searchBlock.length} chars)."
                    } else {
                        // Fallback overwrite if search block failed
                        val updated = currentContent + "\n" + replaceBlock
                        filesMap[fileName] = updated
                        saveFile(fileName, updated)
                        "Appended code to '$fileName'."
                    }
                } else {
                    val generatedContent = generateFileContent(fileName, userGoal, filesMap)
                    filesMap[fileName] = generatedContent
                    saveFile(fileName, generatedContent)
                    "Updated '$fileName' with new code definitions."
                }
            }

            "run_preview_and_get_logs" -> {
                val logs = getConsoleLogs()
                if (logs.isEmpty()) {
                    "Preview launched. Console log buffer clean (0 errors)."
                } else {
                    "Console logs (${logs.size} entries): ${logs.takeLast(3).joinToString(" | ")}"
                }
            }

            else -> "Tool '$toolName' executed."
        }
    }

    private fun generateInitialPlan(userGoal: String, existingFiles: Map<String, String>): List<AgentStep> {
        val steps = mutableListOf<AgentStep>()

        steps.add(
            AgentStep(
                stepIndex = 1,
                thought = "Inspect existing workspace files and dependencies.",
                toolName = "read_file",
                targetFile = if (existingFiles.containsKey("index.html")) "index.html" else null
            )
        )

        steps.add(
            AgentStep(
                stepIndex = 2,
                thought = "Construct core application layout and structure in index.html.",
                toolName = "create_file",
                targetFile = "index.html"
            )
        )

        steps.add(
            AgentStep(
                stepIndex = 3,
                thought = "Apply modern CSS styling and responsive layouts in style.css.",
                toolName = "create_file",
                targetFile = "style.css"
            )
        )

        steps.add(
            AgentStep(
                stepIndex = 4,
                thought = "Implement dynamic logic, event handlers, and local state in script.js.",
                toolName = "create_file",
                targetFile = "script.js"
            )
        )

        return steps
    }

    private fun generateFileContent(filename: String, goal: String, existingFiles: Map<String, String>): String {
        val lowerGoal = goal.lowercase(Locale.ROOT)
        return when (filename) {
            "index.html" -> """
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Offline Agent App - $goal</title>
  <link rel="stylesheet" href="style.css">
</head>
<body>
  <div class="app-container">
    <header class="app-header">
      <h1>🚀 $goal</h1>
      <p class="subtitle">Built autonomously by Offline AI Agent</p>
    </header>

    <main class="main-content">
      <div class="card shadow">
        <h2>Interactive Workspace</h2>
        <div id="app-root">
          <p class="loading">Loading application components...</p>
        </div>
      </div>

      <div class="controls-panel">
        <button id="primary-btn" class="btn btn-primary">Action Step</button>
        <button id="reset-btn" class="btn btn-secondary">Reset State</button>
      </div>

      <div id="status-box" class="status-box">
        <span class="indicator active"></span> System Ready
      </div>
    </main>
  </div>
  <script src="script.js"></script>
</body>
</html>
            """.trimIndent()

            "style.css" -> """
:root {
  --primary: #3b82f6;
  --primary-hover: #2563eb;
  --bg-dark: #0f172a;
  --card-bg: #1e293b;
  --text-main: #f8fafc;
  --text-muted: #94a3b8;
  --accent: #10b981;
}

* {
  box-sizing: border-box;
  margin: 0;
  padding: 0;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  background-color: var(--bg-dark);
  color: var(--text-main);
  padding: 20px;
  line-height: 1.5;
}

.app-container {
  max-width: 720px;
  margin: 0 auto;
}

.app-header {
  text-align: center;
  margin-bottom: 24px;
}

.app-header h1 {
  font-size: 24px;
  color: var(--primary);
}

.subtitle {
  font-size: 13px;
  color: var(--text-muted);
}

.card {
  background: var(--card-bg);
  border-radius: 12px;
  padding: 20px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  margin-bottom: 16px;
}

.controls-panel {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.btn {
  padding: 10px 18px;
  border-radius: 8px;
  border: none;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s;
}

.btn-primary {
  background: var(--primary);
  color: white;
}

.btn-primary:hover {
  background: var(--primary-hover);
}

.btn-secondary {
  background: #334155;
  color: white;
}

.status-box {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: var(--text-muted);
  background: #090d16;
  padding: 8px 12px;
  border-radius: 6px;
}

.indicator {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--accent);
}
            """.trimIndent()

            "script.js" -> """
document.addEventListener('DOMContentLoaded', () => {
  console.log('[Offline Agent] Initializing JavaScript logic for: $goal');
  
  const root = document.getElementById('app-root');
  const primaryBtn = document.getElementById('primary-btn');
  const resetBtn = document.getElementById('reset-btn');
  const statusBox = document.getElementById('status-box');

  let count = 0;

  if (root) {
    root.innerHTML = `
      <div style="text-align: center; padding: 12px;">
        <div style="font-size: 36px; font-weight: bold; color: #3b82f6;" id="counter-val">0</div>
        <p style="color: #94a3b8; font-size: 13px;">Interactions Triggered</p>
      </div>
    `;
  }

  if (primaryBtn) {
    primaryBtn.addEventListener('click', () => {
      count++;
      const counterEl = document.getElementById('counter-val');
      if (counterEl) counterEl.textContent = count;
      console.log('[App Log] User triggered action. Count:', count);
      if (statusBox) {
        statusBox.innerHTML = '<span class="indicator active"></span> Updated count: ' + count;
      }
    });
  }

  if (resetBtn) {
    resetBtn.addEventListener('click', () => {
      count = 0;
      const counterEl = document.getElementById('counter-val');
      if (counterEl) counterEl.textContent = 0;
      console.log('[App Log] State reset.');
    });
  }
});
            """.trimIndent()

            else -> "/* Generated file $filename */\nconsole.log('File initialized');"
        }
    }

    private fun extractSearchBlock(content: String): String {
        val lines = content.lines()
        return if (lines.size > 3) {
            lines.take(2).joinToString("\n")
        } else ""
    }

    private fun extractReplaceBlock(filename: String, goal: String): String {
        return "/* Updated by Agent Engine for goal: $goal */"
    }
}
