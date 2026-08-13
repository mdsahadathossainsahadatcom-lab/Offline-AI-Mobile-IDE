package com.example.engine.agent

import org.json.JSONObject

data class AgentToolCall(
    val action: String,
    val filename: String? = null,
    val content: String? = null,
    val searchBlock: String? = null,
    val replaceBlock: String? = null,
    val workspaceName: String? = null,
    val rawJson: String = ""
)

data class AgentParseResult(
    val cleanText: String,
    val toolCalls: List<AgentToolCall>
)

object AgentParser {

    private val TOOL_CALL_REGEX = Regex("""<tool_call>(.*?)</tool_call>""", RegexOption.DOT_MATCHES_ALL)

    fun parse(response: String): AgentParseResult {
        if (response.isBlank()) return AgentParseResult("", emptyList())

        val toolCalls = mutableListOf<AgentToolCall>()
        val matches = TOOL_CALL_REGEX.findAll(response)

        for (match in matches) {
            val jsonContent = match.groupValues[1].trim()
            try {
                val json = JSONObject(jsonContent)
                val action = json.optString("action", json.optString("tool_name", "")).uppercase()
                val filename = json.optString("filename", json.optString("path", json.optString("target_file", "")))
                val content = if (json.has("content")) json.getString("content") else null
                val searchBlock = if (json.has("search_block")) json.getString("search_block") else null
                val replaceBlock = if (json.has("replace_block")) json.getString("replace_block") else null
                val workspaceName = if (json.has("workspace_name")) json.getString("workspace_name") else if (json.has("project_title")) json.getString("project_title") else null

                toolCalls.add(
                    AgentToolCall(
                        action = action,
                        filename = filename.ifBlank { null },
                        content = content,
                        searchBlock = searchBlock,
                        replaceBlock = replaceBlock,
                        workspaceName = workspaceName?.ifBlank { null },
                        rawJson = jsonContent
                    )
                )
            } catch (e: Exception) {
                // Fallback for non-json tool call text
                toolCalls.add(
                    AgentToolCall(
                        action = "UNKNOWN",
                        rawJson = jsonContent
                    )
                )
            }
        }

        val cleanText = response.replace(TOOL_CALL_REGEX, "").trim()
        return AgentParseResult(cleanText = cleanText, toolCalls = toolCalls)
    }
}
