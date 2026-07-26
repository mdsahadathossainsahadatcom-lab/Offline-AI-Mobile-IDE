package com.example.util

import androidx.compose.runtime.Immutable
import android.app.ActivityManager
import android.content.Context
import android.os.Process
import java.io.RandomAccessFile

@Immutable
data class MemoryCheckResult(
    val isSufficient: Boolean,
    val availableRamMb: Long,
    val totalRamMb: Long,
    val requiredRamMb: Long,
    val warningMessage: String? = null,
    val isLowMemoryState: Boolean = false,
    val recommendedContextWindow: Int = 4096
)

object MemoryCheckUtil {

    /**
     * Checks device total and available RAM along with JVM heap status.
     * Calculates required memory for a model and returns a diagnostic result.
     */
    fun verifyAvailableRam(
        context: Context,
        modelSizeBytes: Long = 1_680_000_000L, // Default ~1.68 GB for Q4_K_M model
        requestedContextWindow: Int = 4096
    ): MemoryCheckResult {
        val memoryInfo = ActivityManager.MemoryInfo()
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        activityManager?.getMemoryInfo(memoryInfo)

        val totalRamMb = (memoryInfo.totalMem) / (1024 * 1024)
        val availableRamMb = (memoryInfo.availMem) / (1024 * 1024)
        val requiredRamMb = (modelSizeBytes) / (1024 * 1024)

        // Low memory state flagged by system OS or free RAM < 800MB
        val isSystemLowMemory = memoryInfo.lowMemory || availableRamMb < 800

        val runtime = Runtime.getRuntime()
        val maxJvmHeapMb = runtime.maxMemory() / (1024 * 1024)
        val allocatedJvmHeapMb = runtime.totalMemory() / (1024 * 1024)
        val freeJvmHeapMb = (maxJvmHeapMb - allocatedJvmHeapMb) + (runtime.freeMemory() / (1024 * 1024))

        // Determine if available memory is sufficient for model + inference context
        val isSufficient = availableRamMb >= (requiredRamMb * 0.60) && freeJvmHeapMb >= 256

        val recommendedContext = when {
            availableRamMb < 1000 || freeJvmHeapMb < 300 -> 2048
            availableRamMb < 600 || isSystemLowMemory -> 1024
            else -> requestedContextWindow
        }

        val warningMessage = when {
            !isSufficient -> "Critical Memory Notice: Device has $availableRamMb MB free RAM ($totalRamMb MB Total). Model requires ~$requiredRamMb MB. Context scaled to $recommendedContext tokens to prevent OOM crash."
            isSystemLowMemory -> "Low RAM Alert: $availableRamMb MB RAM available. Auto RAM Guard active with context scaled to $recommendedContext tokens."
            availableRamMb < requiredRamMb -> "Memory Constraint: Free RAM ($availableRamMb MB) is lower than model weight file size ($requiredRamMb MB). Zero-copy mmap enabled."
            else -> null
        }

        return MemoryCheckResult(
            isSufficient = isSufficient,
            availableRamMb = availableRamMb,
            totalRamMb = totalRamMb,
            requiredRamMb = requiredRamMb,
            warningMessage = warningMessage,
            isLowMemoryState = isSystemLowMemory,
            recommendedContextWindow = recommendedContext
        )
    }

    /**
     * Reads PSS (Proportional Set Size) memory footprint of current process.
     */
    fun getProcessMemoryUsageMb(context: Context): Long {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            ?: return 0L
        val pids = intArrayOf(Process.myPid())
        val processMemoryInfo = activityManager.getProcessMemoryInfo(pids)
        return if (processMemoryInfo.isNotEmpty()) {
            processMemoryInfo[0].totalPss / 1024L
        } else {
            0L
        }
    }
}
