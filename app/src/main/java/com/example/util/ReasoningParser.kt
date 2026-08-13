package com.example.util

/**
 * Data class representing the parsed components of AI text containing reasoning/thinking tags.
 *
 * @property thinkingText The reasoning trace extracted from inside <think>...</think> tags.
 * @property finalAnswerText The main response text outside or after thinking tags.
 * @property isCurrentlyThinking True if <think> tag was opened but not yet closed (active streaming).
 * @property isThinkingFinished True if both <think> and </think> tags are present and complete.
 */
data class ReasoningParseResult(
    val thinkingText: String = "",
    val finalAnswerText: String = "",
    val isCurrentlyThinking: Boolean = false,
    val isThinkingFinished: Boolean = false
)

object ReasoningParser {
    /**
     * Parses raw AI output text to extract reasoning blocks enclosed in <think>...</think> tags.
     * Handles streaming states, completed generations, and standard responses without thinking tags.
     *
     * @param rawText Raw output text from AI model stream or response.
     * @return ReasoningParseResult containing extracted thinking trace and clean final answer.
     */
    fun parse(rawText: String): ReasoningParseResult {
        if (rawText.isBlank()) {
            return ReasoningParseResult()
        }

        val openTagIndex = rawText.indexOf("<think>")
        val closeTagIndex = rawText.indexOf("</think>")

        return when {
            openTagIndex != -1 -> {
                if (closeTagIndex != -1 && closeTagIndex > openTagIndex) {
                    // Completed thinking block: Both <think> and </think> exist
                    val thinking = rawText.substring(openTagIndex + 7, closeTagIndex).trim()
                    val textBefore = rawText.substring(0, openTagIndex).trim()
                    val textAfter = rawText.substring(closeTagIndex + 8).trim()

                    val finalAnswer = when {
                        textBefore.isNotEmpty() && textAfter.isNotEmpty() -> "$textBefore\n$textAfter"
                        textBefore.isNotEmpty() -> textBefore
                        else -> textAfter
                    }

                    ReasoningParseResult(
                        thinkingText = thinking,
                        finalAnswerText = finalAnswer,
                        isCurrentlyThinking = false,
                        isThinkingFinished = true
                    )
                } else {
                    // Streaming thinking block: <think> open but </think> tag not yet generated
                    val thinking = rawText.substring(openTagIndex + 7).trim()
                    val textBefore = rawText.substring(0, openTagIndex).trim()

                    ReasoningParseResult(
                        thinkingText = thinking,
                        finalAnswerText = textBefore,
                        isCurrentlyThinking = true,
                        isThinkingFinished = false
                    )
                }
            }
            closeTagIndex != -1 -> {
                // Edge case: </think> present without explicit <think> header
                val thinking = rawText.substring(0, closeTagIndex).trim()
                val textAfter = rawText.substring(closeTagIndex + 8).trim()

                ReasoningParseResult(
                    thinkingText = thinking,
                    finalAnswerText = textAfter,
                    isCurrentlyThinking = false,
                    isThinkingFinished = true
                )
            }
            else -> {
                // Standard text output with no reasoning block
                ReasoningParseResult(
                    thinkingText = "",
                    finalAnswerText = rawText,
                    isCurrentlyThinking = false,
                    isThinkingFinished = false
                )
            }
        }
    }
}
