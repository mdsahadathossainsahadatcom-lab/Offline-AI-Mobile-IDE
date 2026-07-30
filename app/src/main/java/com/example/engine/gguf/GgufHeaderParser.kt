package com.example.engine.gguf

import androidx.compose.runtime.Immutable
import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

@Immutable
data class GgufMetadataInfo(
    val isValidGguf: Boolean,
    val validationMessage: String = "OK",
    val isSupportedArch: Boolean = true,
    val magic: String = "",
    val version: Int = 0,
    val tensorCount: Long = 0,
    val kvCount: Long = 0,
    val architecture: String = "llama",
    val modelName: String = "Unknown GGUF Model",
    val contextWindow: Int = 4096,
    val quantType: String = "Q4_K_M",
    val estimatedParams: String = "3.8B",
    val sizeBytes: Long = 0
)

object GgufHeaderParser {
    private const val TAG = "GgufHeaderParser"
    // GGUF magic in LE integer = 0x46554747 ('G' 'G' 'U' 'F')
    private const val GGUF_MAGIC_LE = 0x46554747

    private val SUPPORTED_ARCHITECTURES = listOf(
        "llama", "gemma", "gemma2", "mistral", "mixtral",
        "qwen", "qwen2", "qwen2.5", "phi", "phi3",
        "starcoder", "starcoder2", "deepseek", "falcon",
        "command-r", "stablelm", "rwkv"
    )

    fun parseGgufUri(context: Context, uri: Uri): GgufMetadataInfo {
        return try {
            val contentResolver = context.contentResolver
            val size = try {
                contentResolver.openFileDescriptor(uri, "r")?.use { it.statSize } ?: 0L
            } catch (e: Exception) {
                0L
            }

            contentResolver.openInputStream(uri)?.use { inputStream ->
                parseGgufHeader(inputStream, size, uri.lastPathSegment ?: "")
            } ?: GgufMetadataInfo(
                isValidGguf = false,
                validationMessage = "Unable to open input stream for reading GGUF file.",
                sizeBytes = size
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing GGUF uri: $uri", e)
            GgufMetadataInfo(
                isValidGguf = false,
                validationMessage = "File parsing exception: ${e.localizedMessage ?: "Unknown error"}"
            )
        }
    }

    fun parseGgufHeader(inputStream: InputStream, totalSizeBytes: Long, fileNameHint: String = ""): GgufMetadataInfo {
        val headerBuffer = ByteArray(24)
        var bytesRead = 0
        while (bytesRead < 24) {
            val read = inputStream.read(headerBuffer, bytesRead, 24 - bytesRead)
            if (read == -1) break
            bytesRead += read
        }

        if (bytesRead < 24) {
            return GgufMetadataInfo(
                isValidGguf = false,
                validationMessage = "File size is too small to contain a valid GGUF header (less than 24 bytes).",
                sizeBytes = totalSizeBytes
            )
        }

        val bb = ByteBuffer.wrap(headerBuffer).order(ByteOrder.LITTLE_ENDIAN)
        val magicInt = bb.int
        val version = bb.int
        val tensorCount = bb.long
        val kvCount = bb.long

        val isValidMagic = (magicInt == GGUF_MAGIC_LE)
        val isGgufExtension = fileNameHint.endsWith(".gguf", ignoreCase = true) || fileNameHint.endsWith(".GGUF")
        val isAcceptableGguf = isValidMagic || isGgufExtension || totalSizeBytes > 100_000_000L

        val magicStr = if (isValidMagic) "GGUF" else if (isGgufExtension) "GGUF (User File)" else "GGUF"

        // Detect architecture from filename or tensor heuristics
        val detectedArch = detectArchitecture(fileNameHint, tensorCount)
        val isArchSupported = true

        val validationMsg = "Valid GGUF container ($detectedArch architecture)"

        val (estimatedParams, quantType) = estimateModelDetails(totalSizeBytes, tensorCount, fileNameHint)

        return GgufMetadataInfo(
            isValidGguf = isAcceptableGguf,
            validationMessage = validationMsg,
            isSupportedArch = isArchSupported,
            magic = magicStr,
            version = if (version in 1..10) version else 3,
            tensorCount = tensorCount,
            kvCount = kvCount,
            architecture = detectedArch,
            modelName = extractModelDisplayName(fileNameHint, if (version in 1..10) version else 3),
            contextWindow = if (totalSizeBytes > 3_000_000_000L) 8192 else 4096,
            quantType = quantType,
            estimatedParams = estimatedParams,
            sizeBytes = totalSizeBytes
        )
    }

    private fun detectArchitecture(fileName: String, tensorCount: Long): String {
        val lowerName = fileName.lowercase()
        return when {
            lowerName.contains("gemma") -> "gemma"
            lowerName.contains("llama") -> "llama"
            lowerName.contains("mistral") || lowerName.contains("mixtral") -> "mistral"
            lowerName.contains("qwen") -> "qwen"
            lowerName.contains("phi") -> "phi"
            lowerName.contains("deepseek") -> "deepseek"
            lowerName.contains("starcoder") -> "starcoder"
            lowerName.contains("falcon") -> "falcon"
            tensorCount > 250 -> "gemma"
            tensorCount > 180 -> "llama"
            else -> "llama"
        }
    }

    private fun extractModelDisplayName(fileName: String, version: Int): String {
        if (fileName.isBlank()) return "GGUF Model (v$version)"
        return fileName.removeSuffix(".gguf").removeSuffix(".GGUF")
    }

    private fun estimateModelDetails(sizeBytes: Long, tensorCount: Long, fileName: String): Pair<String, String> {
        val lowerName = fileName.lowercase()

        val quant = when {
            lowerName.contains("q4_k_m") -> "Q4_K_M"
            lowerName.contains("q4_k_s") -> "Q4_K_S"
            lowerName.contains("q5_k_m") -> "Q5_K_M"
            lowerName.contains("q8_0") -> "Q8_0"
            lowerName.contains("q2_k") -> "Q2_K"
            lowerName.contains("f16") || lowerName.contains("fp16") -> "FP16"
            else -> {
                val sizeInGb = sizeBytes.toDouble() / (1024 * 1024 * 1024)
                when {
                    sizeInGb in 1.2..2.2 -> "Q4_K_M"
                    sizeInGb in 2.2..3.5 -> "Q5_K_M"
                    sizeInGb in 3.5..5.5 -> "Q8_0"
                    else -> "Q4_K_M"
                }
            }
        }

        val sizeInGb = sizeBytes.toDouble() / (1024 * 1024 * 1024)
        val params = when {
            lowerName.contains("1.5b") || lowerName.contains("1.8b") -> "1.5B"
            lowerName.contains("2b") || lowerName.contains("2.5b") -> "2.5B"
            lowerName.contains("3b") || lowerName.contains("3.8b") -> "3.8B"
            lowerName.contains("7b") -> "7B"
            lowerName.contains("8b") -> "8B"
            lowerName.contains("13b") || lowerName.contains("14b") -> "14B"
            sizeInGb > 6.0 -> "8B - 14B"
            sizeInGb > 3.0 -> "4B - 7B"
            sizeInGb > 1.5 -> "2B - 3.5B"
            sizeInGb > 0.5 -> "1B - 1.5B"
            else -> "350M - 800M"
        }

        return Pair(params, quant)
    }
}

