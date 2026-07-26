package com.example.util

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.os.Process
import androidx.compose.runtime.Immutable

@Immutable
data class DiagnosticState(
    val availableRamMb: Long = 0,
    val totalRamMb: Long = 0,
    val usedRamMb: Long = 0,
    val ramUsagePercent: Int = 0,
    val jvmAllocatedMb: Long = 0,
    val jvmMaxMb: Long = 0,
    val processPssMb: Long = 0,
    val modelSizeBytes: Long = 0,
    val speedTokensPerSec: Float = 0f,
    val tokensGenerated: Int = 0,
    val contextWindowTokens: Int = 4096,
    val thermalStatusText: String = "Nominal / Normal",
    val thermalStatusCode: Int = 0, // 0=None, 1=Light, 2=Moderate, 3=Severe, 4=Critical
    val batteryTempCelsius: Float = 0f,
    val isThrottling: Boolean = false,
    val cpuThreads: Int = 4,
    val activeModelName: String = "Gemma-2B-Q4_K_M.gguf"
)

object DiagnosticUtil {

    fun getDiagnosticState(
        context: Context,
        speedTokensPerSec: Float = 0f,
        tokensGenerated: Int = 0,
        contextWindowTokens: Int = 4096,
        modelSizeBytes: Long = 1_680_000_000L,
        cpuThreads: Int = 4,
        modelName: String = "Gemma-2B-Q4_K_M.gguf"
    ): DiagnosticState {
        // 1. RAM / Memory Metrics
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager?.getMemoryInfo(memoryInfo)

        val totalRamMb = memoryInfo?.totalMem?.div(1024 * 1024) ?: 4096L
        val availableRamMb = memoryInfo?.availMem?.div(1024 * 1024) ?: 2048L
        val usedRamMb = (totalRamMb - availableRamMb).coerceAtLeast(0)
        val ramUsagePercent = if (totalRamMb > 0) ((usedRamMb.toDouble() / totalRamMb.toDouble()) * 100).toInt() else 0

        val runtime = Runtime.getRuntime()
        val jvmMaxMb = runtime.maxMemory() / (1024 * 1024)
        val jvmTotalMb = runtime.totalMemory() / (1024 * 1024)
        val jvmAllocatedMb = jvmTotalMb - (runtime.freeMemory() / (1024 * 1024))

        var processPssMb = 0L
        try {
            val pids = intArrayOf(Process.myPid())
            val procMemInfo = activityManager?.getProcessMemoryInfo(pids)
            if (procMemInfo != null && procMemInfo.isNotEmpty()) {
                processPssMb = procMemInfo[0].totalPss / 1024L
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Thermal Metrics
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val thermalStatusCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            powerManager?.currentThermalStatus ?: 0
        } else {
            0
        }

        var batteryTempCelsius = 0f
        try {
            val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val temp = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
            batteryTempCelsius = temp / 10.0f
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val thermalStatusText = when (thermalStatusCode) {
            0 -> if (batteryTempCelsius > 42f) "Elevated Temp (${batteryTempCelsius}°C)" else "Nominal / Normal"
            1 -> "Light Throttling"
            2 -> "Moderate Throttling"
            3 -> "Severe Throttling"
            4 -> "Critical Thermal State"
            5 -> "Emergency Thermal"
            6 -> "Shutdown Thermal"
            else -> "Nominal"
        }

        val isThrottling = thermalStatusCode >= 2 || batteryTempCelsius >= 43.0f

        return DiagnosticState(
            availableRamMb = availableRamMb,
            totalRamMb = totalRamMb,
            usedRamMb = usedRamMb,
            ramUsagePercent = ramUsagePercent,
            jvmAllocatedMb = jvmAllocatedMb,
            jvmMaxMb = jvmMaxMb,
            processPssMb = processPssMb,
            modelSizeBytes = modelSizeBytes,
            speedTokensPerSec = speedTokensPerSec,
            tokensGenerated = tokensGenerated,
            contextWindowTokens = contextWindowTokens,
            thermalStatusText = thermalStatusText,
            thermalStatusCode = thermalStatusCode,
            batteryTempCelsius = batteryTempCelsius,
            isThrottling = isThrottling,
            cpuThreads = cpuThreads,
            activeModelName = modelName
        )
    }
}
