package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.DiagnosticState

@Composable
fun DiagnosticPanel(
    diagnosticState: DiagnosticState,
    isGenerating: Boolean = false,
    modifier: Modifier = Modifier,
    isExpandedDefault: Boolean = true
) {
    val totalRamGb = "%.1f GB".format(diagnosticState.totalRamMb / 1024.0)
    val availRamGb = "%.1f GB".format(diagnosticState.availableRamMb / 1024.0)
    val usedRamGb = "%.1f GB".format(diagnosticState.usedRamMb / 1024.0)
    val ramProgress = (diagnosticState.ramUsagePercent / 100f).coerceIn(0f, 1f)
    val modelSizeMb = diagnosticState.modelSizeBytes / (1024 * 1024)

    val thermalColor = when (diagnosticState.thermalStatusCode) {
        0 -> if (diagnosticState.batteryTempCelsius > 42f) Color(0xFFEAB308) else Color(0xFF22C55E)
        1 -> Color(0xFFEAB308)
        2 -> Color(0xFFF97316)
        else -> Color(0xFFEF4444)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0F172A)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isGenerating) Color(0xFF38BDF8) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Panel Header Title & Active Status Dot
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isGenerating) Color(0xFF4ADE80) else Color(0xFF38BDF8))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isGenerating) "LLM DIAGNOSTIC MONITOR (ACTIVE)" else "HARDWARE & INFERENCE DIAGNOSTICS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White
                    )
                }

                Box(
                    modifier = Modifier
                        .background(thermalColor.copy(alpha = 0.2f), shape = RoundedCornerShape(6.dp))
                        .border(1.dp, thermalColor, shape = RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (diagnosticState.isThrottling) Icons.Default.Warning else Icons.Default.CheckCircle,
                            contentDescription = "Thermal Status",
                            tint = thermalColor,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = diagnosticState.thermalStatusText.uppercase(),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = thermalColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 1. GGUF Inference Speed Gauge Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E293B), shape = RoundedCornerShape(12.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = "Inference Speed",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "INFERENCE SPEED",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF94A3B8)
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "%.1f tokens/sec".format(diagnosticState.speedTokensPerSec),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF38BDF8)
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "TOKENS GENERATED",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8)
                    )
                    Text(
                        text = "${diagnosticState.tokensGenerated} tokens",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White
                    )
                    Text(
                        text = "Context: ${diagnosticState.tokensGenerated}/${diagnosticState.contextWindowTokens}",
                        fontSize = 9.sp,
                        color = Color(0xFFCBD5E1)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. Memory Usage Progress Bar & Metric Grid
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E293B), shape = RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Memory,
                            contentDescription = "Memory Usage",
                            tint = Color(0xFFA7F3D0),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "MEMORY USAGE (RAM)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF94A3B8)
                        )
                    }

                    Text(
                        text = "$usedRamGb / $totalRamGb (${diagnosticState.ramUsagePercent}%)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = if (diagnosticState.ramUsagePercent > 85) Color(0xFFEF4444) else Color(0xFF4ADE80)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { ramProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (diagnosticState.ramUsagePercent > 85) Color(0xFFEF4444) else Color(0xFF4ADE80),
                    trackColor = Color(0xFF334155)
                )

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Free RAM: $availRamGb",
                        fontSize = 10.sp,
                        color = Color(0xFFCBD5E1)
                    )
                    Text(
                        text = "JVM Heap: ${diagnosticState.jvmAllocatedMb}MB / ${diagnosticState.jvmMaxMb}MB",
                        fontSize = 10.sp,
                        color = Color(0xFFCBD5E1)
                    )
                    Text(
                        text = "Model Weights: ${modelSizeMb}MB",
                        fontSize = 10.sp,
                        color = Color(0xFF38BDF8)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 3. Thermal Throttling & Hardware Status
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E293B), shape = RoundedCornerShape(12.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Thermostat,
                        contentDescription = "Thermal Throttling",
                        tint = thermalColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "THERMAL THROTTLING STATUS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF94A3B8)
                        )
                        Text(
                            text = diagnosticState.thermalStatusText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = thermalColor
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    if (diagnosticState.batteryTempCelsius > 0f) {
                        Text(
                            text = "Temp: ${"%.1f".format(diagnosticState.batteryTempCelsius)} °C",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (diagnosticState.batteryTempCelsius > 42f) Color(0xFFEF4444) else Color.White
                        )
                    }
                    Text(
                        text = "CPU Threads: ${diagnosticState.cpuThreads}",
                        fontSize = 10.sp,
                        color = Color(0xFFCBD5E1)
                    )
                }
            }
        }
    }
}
