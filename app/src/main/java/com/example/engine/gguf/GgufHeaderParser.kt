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

    fun parseGgufUri(context: Context, uri: Uri): GgufMetadataInfo {
        return try {
            val contentResolver = context.contentResolver
            val size = try {
                contentResolver.openFileDescriptor(uri, "r")?.use { it.statSize } ?: 0L
            } catch (e: Exception) {
                0L
            }

            contentResolver.openInputStream(uri)?.use { inputStream ->
                parseGgufHeader(inputStream, size)
            } ?: GgufMetadataInfo(isValidGguf = false, sizeBytes = size)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing GGUF uri: $uri", e)
            GgufMetadataInfo(isValidGguf = false)
        }
    }

    private fun parseGgufHeader(inputStream: InputStream, totalSizeBytes: Long): GgufMetadataInfo {
        val headerBuffer = ByteArray(24)
        var bytesRead = 0
        while (bytesRead < 24) {
            val read = inputStream.read(headerBuffer, bytesRead, 24 - bytesRead)
            if (read == -1) break
            bytesRead += read
        }

        if (bytesRead < 24) {
            return GgufMetadataInfo(isValidGguf = false, sizeBytes = totalSizeBytes)
        }

        val bb = ByteBuffer.wrap(headerBuffer).order(ByteOrder.LITTLE_ENDIAN)
        val magicInt = bb.int
        val version = bb.int
        val tensorCount = bb.long
        val kvCount = bb.long

        val isValid = (magicInt == GGUF_MAGIC_LE)
        val magicStr = if (isValid) "GGUF" else "UNKNOWN"

        // Determine estimated param size and quantization type based on size or version
        val (estimatedParams, quantType) = estimateModelDetails(totalSizeBytes, tensorCount)

        return GgufMetadataInfo(
            isValidGguf = isValid,
            magic = magicStr,
            version = version,
            tensorCount = tensorCount,
            kvCount = kvCount,
            architecture = if (tensorCount > 200) "gemma" else "llama",
            modelName = "GGUF Model (v$version)",
            contextWindow = if (totalSizeBytes > 3_000_000_000L) 8192 else 4096,
            quantType = quantType,
            estimatedParams = estimatedParams,
            sizeBytes = totalSizeBytes
        )
    }

    private fun estimateModelDetails(sizeBytes: Long, tensorCount: Long): Pair<String, String> {
        val sizeInGb = sizeBytes.toDouble() / (1024 * 1024 * 1024)
        val params = when {
            sizeInGb > 6.0 -> "8B - 13B"
            sizeInGb > 3.0 -> "4B - 7B"
            sizeInGb > 1.5 -> "2B - 3B"
            sizeInGb > 0.5 -> "1B - 1.5B"
            else -> "350M - 800M"
        }

        val quant = when {
            sizeInGb in 1.2..2.2 -> "Q4_K_M"
            sizeInGb in 2.2..3.5 -> "Q5_K_M"
            sizeInGb in 3.5..5.5 -> "Q8_0"
            else -> "Q4_K_S"
        }

        return Pair(params, quant)
    }
}
