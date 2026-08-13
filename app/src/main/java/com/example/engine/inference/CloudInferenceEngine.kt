package com.example.engine.inference

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.TimeUnit

enum class AiProviderMode { LOCAL_GGUF, CLOUD_API }

enum class CloudProvider(val displayName: String, val defaultModel: String) {
    GEMINI("Google Gemini", "gemini-1.5-flash"),
    OPENAI("OpenAI", "gpt-4o-mini"),
    GROQ("Groq (Fast Llama)", "llama-3.1-8b-instant"),
    CLAUDE("Anthropic Claude", "claude-3-5-sonnet-20240620")
}

data class AiProviderSettings(
    val mode: AiProviderMode = AiProviderMode.LOCAL_GGUF,
    val cloudProvider: CloudProvider = CloudProvider.GEMINI,
    val apiKey: String = "",
    val cloudModelName: String = CloudProvider.GEMINI.defaultModel
)

class CloudInferenceEngine {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun testConnection(settings: AiProviderSettings): Result<String> = withContext(Dispatchers.IO) {
        if (settings.apiKey.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("API key cannot be empty."))
        }

        try {
            val responseText = executePrompt(
                prompt = "Hello! Send back 'OK' to verify API key connection.",
                settings = settings
            )
            if (responseText.isNotBlank()) {
                Result.success("Connection successful! Response: ${responseText.take(60)}...")
            } else {
                Result.failure(Exception("Received empty response from ${settings.cloudProvider.displayName}."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun generateMultiFileCodeStream(
        prompt: String,
        settings: AiProviderSettings,
        existingFiles: Map<String, String> = emptyMap()
    ): Flow<GenerationProgress> = flow {
        if (settings.apiKey.isBlank()) {
            emit(
                GenerationProgress(
                    statusText = "Error: Cloud API Key missing",
                    tokensGenerated = 0,
                    speedTokensPerSec = 0f,
                    currentFile = null,
                    generatedFiles = emptyMap(),
                    isComplete = true,
                    rawLogText = "[Cloud API Error] No API key configured. Please set your ${settings.cloudProvider.displayName} API Key in Settings."
                )
            )
            return@flow
        }

        val fullPrompt = buildSystemPrompt(prompt, existingFiles)

        emit(
            GenerationProgress(
                statusText = "Connecting to ${settings.cloudProvider.displayName} (${settings.cloudModelName})...",
                tokensGenerated = 5,
                speedTokensPerSec = 12.5f,
                currentFile = "request.json",
                generatedFiles = emptyMap(),
                isComplete = false,
                rawLogText = "[Cloud Engine] Sending HTTP POST request to ${settings.cloudProvider.displayName} endpoint..."
            )
        )

        try {
            val aiResponse = executePrompt(fullPrompt, settings)
            val filesMap = parseCodeBlocks(aiResponse, existingFiles)

            val tokenEst = (aiResponse.length / 4).coerceAtLeast(12)
            emit(
                GenerationProgress(
                    statusText = "Completed response from ${settings.cloudProvider.displayName}",
                    tokensGenerated = tokenEst,
                    speedTokensPerSec = 45.0f,
                    currentFile = filesMap.keys.firstOrNull(),
                    generatedFiles = filesMap,
                    isComplete = true,
                    rawLogText = "[Cloud Engine] Received ${aiResponse.length} chars from ${settings.cloudModelName}."
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
            emit(
                GenerationProgress(
                    statusText = "Cloud Request Failed: ${e.localizedMessage}",
                    tokensGenerated = 0,
                    speedTokensPerSec = 0f,
                    currentFile = null,
                    generatedFiles = emptyMap(),
                    isComplete = true,
                    rawLogText = "[Cloud API Error] ${e.localizedMessage}"
                )
            )
        }
    }.flowOn(Dispatchers.IO)

    private fun executePrompt(prompt: String, settings: AiProviderSettings): String {
        val model = settings.cloudModelName.ifBlank { settings.cloudProvider.defaultModel }
        return when (settings.cloudProvider) {
            CloudProvider.GEMINI -> callGeminiApi(prompt, settings.apiKey, model)
            CloudProvider.OPENAI -> callOpenAiApi(prompt, settings.apiKey, model, "https://api.openai.com/v1/chat/completions")
            CloudProvider.GROQ -> callOpenAiApi(prompt, settings.apiKey, model, "https://api.groq.com/openai/v1/chat/completions")
            CloudProvider.CLAUDE -> callClaudeApi(prompt, settings.apiKey, model)
        }
    }

    private fun callGeminiApi(prompt: String, apiKey: String, modelName: String): String {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"
        val jsonPayload = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", prompt))
                    })
                })
            })
        }

        val request = Request.Builder()
            .url(url)
            .post(jsonPayload.toString().toRequestBody(jsonMediaType))
            .build()

        client.newCall(request).execute().use { response ->
            val bodyStr = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                val errObj = try { JSONObject(bodyStr) } catch (e: Exception) { null }
                val errMsg = errObj?.optJSONObject("error")?.optString("message") ?: response.message
                throw Exception("Gemini API Error ($response.code): $errMsg")
            }

            val json = JSONObject(bodyStr)
            val candidates = json.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val content = candidates.getJSONObject(0).optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                if (parts != null && parts.length() > 0) {
                    return parts.getJSONObject(0).optString("text", "")
                }
            }
            return bodyStr
        }
    }

    private fun callOpenAiApi(prompt: String, apiKey: String, modelName: String, endpointUrl: String): String {
        val jsonPayload = JSONObject().apply {
            put("model", modelName)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            })
        }

        val request = Request.Builder()
            .url(endpointUrl)
            .addHeader("Authorization", "Bearer $apiKey")
            .post(jsonPayload.toString().toRequestBody(jsonMediaType))
            .build()

        client.newCall(request).execute().use { response ->
            val bodyStr = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                val errObj = try { JSONObject(bodyStr) } catch (e: Exception) { null }
                val errMsg = errObj?.optJSONObject("error")?.optString("message") ?: response.message
                throw Exception("Cloud API Error ($response.code): $errMsg")
            }

            val json = JSONObject(bodyStr)
            val choices = json.optJSONArray("choices")
            if (choices != null && choices.length() > 0) {
                val message = choices.getJSONObject(0).optJSONObject("message")
                return message?.optString("content", "") ?: ""
            }
            return bodyStr
        }
    }

    private fun callClaudeApi(prompt: String, apiKey: String, modelName: String): String {
        val url = "https://api.anthropic.com/v1/messages"
        val jsonPayload = JSONObject().apply {
            put("model", modelName)
            put("max_tokens", 2048)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            })
        }

        val request = Request.Builder()
            .url(url)
            .addHeader("x-api-key", apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .post(jsonPayload.toString().toRequestBody(jsonMediaType))
            .build()

        client.newCall(request).execute().use { response ->
            val bodyStr = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                val errObj = try { JSONObject(bodyStr) } catch (e: Exception) { null }
                val errMsg = errObj?.optJSONObject("error")?.optString("message") ?: response.message
                throw Exception("Claude API Error ($response.code): $errMsg")
            }

            val json = JSONObject(bodyStr)
            val contentArr = json.optJSONArray("content")
            if (contentArr != null && contentArr.length() > 0) {
                return contentArr.getJSONObject(0).optString("text", "")
            }
            return bodyStr
        }
    }

    private fun buildSystemPrompt(prompt: String, existingFiles: Map<String, String>): String {
        val filesContext = if (existingFiles.isNotEmpty()) {
            "Existing Project Files:\n" + existingFiles.entries.joinToString("\n\n") { (path, content) ->
                "--- File: $path ---\n$content"
            }
        } else ""

        return """
            System Directive: You are an expert AI software developer embedded in a local IDE.
            You are fully optimized to understand and respond in Bengali when the user speaks Bengali. However, maintain all internal system tags (like <think> or <tool_call>) strictly in English.
            Provide clear code or answers. When updating files, wrap code blocks using markdown with filename hints like ```html:index.html or ```css:style.css or ```js:script.js.
            
            $filesContext
            
            User Instruction:
            $prompt
        """.trimIndent()
    }

    private fun parseCodeBlocks(aiResponse: String, existingFiles: Map<String, String>): Map<String, String> {
        val filesMap = mutableMapOf<String, String>()
        val codeBlockRegex = Regex("```(?:(\\w+)(?::([\\w./-]+))?)?\\n([\\s\\S]*?)```")

        val matches = codeBlockRegex.findAll(aiResponse)
        for (match in matches) {
            val lang = match.groupValues[1].lowercase(Locale.ROOT)
            val fileHint = match.groupValues[2]
            val code = match.groupValues[3]

            val fileName = when {
                fileHint.isNotBlank() -> fileHint
                lang == "html" -> "index.html"
                lang == "css" -> "style.css"
                lang == "js" || lang == "javascript" -> "script.js"
                lang == "py" || lang == "python" -> "main.py"
                lang == "json" -> "config.json"
                else -> existingFiles.keys.firstOrNull() ?: "index.html"
            }

            filesMap[fileName] = code.trim()
        }

        return filesMap
    }
}
