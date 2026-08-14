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
    val recommendedContextWindow: Int = 4096,
    val isHighRamUsage: Boolean = false,
    val isCriticalRamUsage: Boolean = false,
    val ramPressurePercent: Int = 0,
    val modelName: String = "",
    val systemThresholdMb: Long = 400L
)

object MemoryCheckUtil {

    /**
     * Checks device total and available RAM along with JVM heap status.
     * Calculates required memory for a model and returns a diagnostic result.
     */
    fun verifyAvailableRam(
        context: Context,
        modelSizeBytes: Long = 1_680_000_000L, // Default ~1.68 GB for Q4_K_M model
        requestedContextWindow: Int = 4096,
        modelName: String = "Active GGUF Model"
    ): MemoryCheckResult {
        val memoryInfo = ActivityManager.MemoryInfo()
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        activityManager?.getMemoryInfo(memoryInfo)

        val totalRamMb = (memoryInfo.totalMem) / (1024 * 1024)
        val availableRamMb = (memoryInfo.availMem) / (1024 * 1024)
        val systemThresholdMb = (memoryInfo.threshold) / (1024 * 1024)
        val requiredRamMb = (modelSizeBytes) / (1024 * 1024)

        // Low memory state flagged by system OS or free RAM < 800MB or near threshold
        val isSystemLowMemory = memoryInfo.lowMemory || availableRamMb < 800 || (availableRamMb - systemThresholdMb) < 400

        val runtime = Runtime.getRuntime()
        val maxJvmHeapMb = runtime.maxMemory() / (1024 * 1024)
        val allocatedJvmHeapMb = runtime.totalMemory() / (1024 * 1024)
        val freeJvmHeapMb = (maxJvmHeapMb - allocatedJvmHeapMb) + (runtime.freeMemory() / (1024 * 1024))

        // Calculate RAM pressure ratio of model relative to available RAM and threshold
        val ramPressurePercent = if (availableRamMb > 0) {
            ((requiredRamMb.toFloat() / availableRamMb.toFloat()) * 100f).toInt().coerceAtMost(999)
        } else {
            100
        }

        // Determine if model RAM usage is High or Critical relative to system threshold
        val isCritical = availableRamMb < 500 || memoryInfo.lowMemory || (availableRamMb - systemThresholdMb) < 200 || ramPressurePercent >= 90
        val isHigh = isCritical || availableRamMb < 800 || ramPressurePercent >= 70 || (requiredRamMb * 0.75f) > availableRamMb

        // Determine if available memory is sufficient for model + inference context
        val isSufficient = availableRamMb >= (requiredRamMb * 0.60) && freeJvmHeapMb >= 256 && !memoryInfo.lowMemory

        val recommendedContext = when {
            isCritical -> 1024
            availableRamMb < 1000 || freeJvmHeapMb < 300 || isHigh -> 2048
            else -> requestedContextWindow
        }

        val warningMessage = when {
            isCritical -> "CRITICAL RAM ALERT: '$modelName' memory footprint exceeds safe threshold ($requiredRamMb MB required, $availableRamMb MB free, system limit: $systemThresholdMb MB). Context dynamically scaled to $recommendedContext tokens to avert OOM."
            isHigh -> "HIGH RAM WARNING: '$modelName' is using $requiredRamMb MB ($ramPressurePercent% of available system memory). Free RAM: $availableRamMb MB."
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
            recommendedContextWindow = recommendedContext,
            isHighRamUsage = isHigh,
            isCriticalRamUsage = isCritical,
            ramPressurePercent = ramPressurePercent,
            modelName = modelName,
            systemThresholdMb = systemThresholdMb
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
