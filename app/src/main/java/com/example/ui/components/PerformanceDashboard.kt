package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.ModelProfileEntity
import com.example.engine.inference.GenerationProgress
import com.example.util.DiagnosticState
import kotlinx.coroutines.delay

data class PerformanceSample(
    val timeLabel: String,
    val speedTokSec: Float,
    val ramUsedMb: Long,
    val ramPercent: Float,
    val tokensGenerated: Int
)

enum class ChartViewMode {
    SPEED, MEMORY, DUAL
}

enum class ChartEngineMode {
    D3_VECTOR, NATIVE_CANVAS
}

/**
 * High-performance Compose dashboard displaying real-time token generation speed (TPS)
 * and memory footprint (RAM/VRAM) during GGUF model inference with an interactive D3.js line chart.
 */
@Composable
fun PerformanceDashboard(
    selectedModel: ModelProfileEntity?,
    diagnosticState: DiagnosticState?,
    generationProgress: GenerationProgress?,
    isGenerating: Boolean = false,
    modifier: Modifier = Modifier
) {
    var chartMode by remember { mutableStateOf(ChartViewMode.DUAL) }
    var engineMode by remember { mutableStateOf(ChartEngineMode.D3_VECTOR) }

    // History buffer of performance samples
    val samples = remember {
        mutableStateListOf(
            PerformanceSample("0s", 14.2f, 2100, 42f, 10),
            PerformanceSample("1s", 18.5f, 2150, 43f, 28),
            PerformanceSample("2s", 21.0f, 2200, 44f, 49),
            PerformanceSample("3s", 19.8f, 2210, 44f, 68),
            PerformanceSample("4s", 23.4f, 2280, 45f, 91),
            PerformanceSample("5s", 22.1f, 2260, 45f, 113),
            PerformanceSample("6s", 25.2f, 2300, 46f, 138),
            PerformanceSample("7s", 24.0f, 2320, 46f, 162)
        )
    }

    var selectedSampleIndex by remember { mutableStateOf<Int?>(null) }
    var sampleCounter by remember { mutableStateOf(8) }

    // Real-time update collector when generation is active or live polling
    LaunchedEffect(generationProgress?.tokensGenerated, isGenerating) {
        if (isGenerating && generationProgress != null) {
            val speed = generationProgress.speedTokensPerSec.coerceAtLeast(0.1f)
            val ramMb = diagnosticState?.usedRamMb ?: 2200L
            val ramPct = diagnosticState?.ramUsagePercent?.toFloat() ?: 45f
            val tokens = generationProgress.tokensGenerated

            sampleCounter++
            val newSample = PerformanceSample(
                timeLabel = "${sampleCounter}s",
                speedTokSec = speed,
                ramUsedMb = ramMb,
                ramPercent = ramPct,
                tokensGenerated = tokens
            )

            if (samples.size >= 30) {
                samples.removeAt(0)
            }
            samples.add(newSample)
        }
    }

    // Calculated metrics
    val currentSpeed = if (isGenerating && generationProgress != null) generationProgress.speedTokensPerSec else (samples.lastOrNull()?.speedTokSec ?: 0f)
    val peakSpeed = samples.maxOfOrNull { it.speedTokSec } ?: currentSpeed
    val avgSpeed = if (samples.isNotEmpty()) samples.map { it.speedTokSec }.average().toFloat() else currentSpeed
    val currentRamMb = diagnosticState?.usedRamMb ?: (samples.lastOrNull()?.ramUsedMb ?: 0L)
    val modelSizeBytes = selectedModel?.sizeBytes ?: 1_680_000_000L
    val modelSizeMb = modelSizeBytes / (1024 * 1024)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("performance_dashboard_card"),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A).copy(alpha = 0.85f)),
        border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.15f)),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Bar
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
                        text = "REAL-TIME INFERENCE TELEMETRY",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Engine Switcher: D3 vs Native
                    FilterChip(
                        selected = engineMode == ChartEngineMode.D3_VECTOR,
                        onClick = {
                            engineMode = if (engineMode == ChartEngineMode.D3_VECTOR) {
                                ChartEngineMode.NATIVE_CANVAS
                            } else {
                                ChartEngineMode.D3_VECTOR
                            }
                        },
                        label = {
                            Text(
                                text = if (engineMode == ChartEngineMode.D3_VECTOR) "📊 D3.js" else "⚡ Native",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF6366F1).copy(alpha = 0.35f),
                            selectedLabelColor = Color(0xFF818CF8)
                        ),
                        modifier = Modifier.height(24.dp)
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(
                        onClick = {
                            samples.clear()
                            sampleCounter = 0
                            samples.addAll(
                                listOf(
                                    PerformanceSample("0s", 12.0f, 2050, 41f, 0),
                                    PerformanceSample("1s", 16.5f, 2100, 42f, 16),
                                    PerformanceSample("2s", 20.2f, 2180, 43f, 36)
                                )
                            )
                        },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset Chart Telemetry",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Model Identity Header Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E293B).copy(alpha = 0.7f), shape = RoundedCornerShape(12.dp))
                    .border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                    .padding(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = selectedModel?.name ?: "No Local Model Selected",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8),
                            maxLines = 1
                        )
                        Text(
                            text = "Quant: ${selectedModel?.quantType ?: "Q4_K_M"} • Arch: ${selectedModel?.architecture ?: "llama"} • Weights: ${modelSizeMb} MB",
                            fontSize = 10.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .background(
                                if (isGenerating) Color(0xFF22C55E).copy(alpha = 0.2f) else Color(0xFF0284C7).copy(alpha = 0.2f),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .border(
                                1.dp,
                                if (isGenerating) Color(0xFF22C55E) else Color(0xFF0284C7),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = if (isGenerating) "INFERENCING" else "READY",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isGenerating) Color(0xFF4ADE80) else Color(0xFF38BDF8)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Stat Metrics Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Metric 1: Token Speed (TPS)
                StatCard(
                    title = "TPS (SPEED)",
                    value = "%.1f".format(currentSpeed),
                    subtext = "Peak: %.1f • Avg: %.1f".format(peakSpeed, avgSpeed),
                    icon = Icons.Default.Speed,
                    accentColor = Color(0xFF38BDF8),
                    modifier = Modifier.weight(1f)
                )

                // Metric 2: Memory Footprint (RAM)
                StatCard(
                    title = "RAM FOOTPRINT",
                    value = "${currentRamMb} MB",
                    subtext = "VRAM Cache: ${modelSizeMb} MB",
                    icon = Icons.Default.Memory,
                    accentColor = Color(0xFF4ADE80),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Chart View Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "D3 REAL-TIME TELEMETRY GRAPH",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF94A3B8)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilterChip(
                        selected = chartMode == ChartViewMode.SPEED,
                        onClick = { chartMode = ChartViewMode.SPEED },
                        label = { Text("TPS", fontSize = 9.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF0284C7),
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.height(24.dp)
                    )
                    FilterChip(
                        selected = chartMode == ChartViewMode.MEMORY,
                        onClick = { chartMode = ChartViewMode.MEMORY },
                        label = { Text("RAM", fontSize = 9.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF15803D),
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.height(24.dp)
                    )
                    FilterChip(
                        selected = chartMode == ChartViewMode.DUAL,
                        onClick = { chartMode = ChartViewMode.DUAL },
                        label = { Text("Dual", fontSize = 9.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF6366F1),
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.height(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Chart Container: D3 WebView or Native Canvas
            if (engineMode == ChartEngineMode.D3_VECTOR) {
                D3PerformanceLineChart(
                    samples = samples,
                    chartMode = chartMode,
                    isGenerating = isGenerating,
                    height = 175.dp
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(175.dp)
                        .background(Color(0xFF020617), shape = RoundedCornerShape(14.dp))
                        .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(14.dp))
                        .padding(12.dp)
                ) {
                    if (samples.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No telemetry data available", color = Color.Gray, fontSize = 12.sp)
                        }
                    } else {
                        val maxSpeed = (samples.maxOfOrNull { it.speedTokSec } ?: 30f).coerceAtLeast(30f)
                        val maxRam = (samples.maxOfOrNull { it.ramUsedMb } ?: 4000L).coerceAtLeast(4000L).toFloat()

                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    detectTapGestures { offset ->
                                        val count = samples.size
                                        if (count > 0) {
                                            val stepX = size.width / (count - 1).coerceAtLeast(1)
                                            val index = (offset.x / stepX).toInt().coerceIn(0, count - 1)
                                            selectedSampleIndex = index
                                        }
                                    }
                                }
                        ) {
                            val width = size.width
                            val height = size.height

                            // Draw Grid Lines
                            val gridLines = 4
                            for (i in 0..gridLines) {
                                val y = height * (i.toFloat() / gridLines)
                                drawLine(
                                    color = Color(0xFF1E293B),
                                    start = Offset(0f, y),
                                    end = Offset(width, y),
                                    strokeWidth = 1f
                                )
                            }

                            val count = samples.size
                            val stepX = width / (count - 1).coerceAtLeast(1)

                            // 1. Draw Speed Line (Cyan)
                            if (chartMode == ChartViewMode.SPEED || chartMode == ChartViewMode.DUAL) {
                                val speedPath = Path()
                                val fillPath = Path()

                                samples.forEachIndexed { i, s ->
                                    val x = i * stepX
                                    val normalizedSpeed = (s.speedTokSec / maxSpeed).coerceIn(0f, 1f)
                                    val y = height - (normalizedSpeed * height)

                                    if (i == 0) {
                                        speedPath.moveTo(x, y)
                                        fillPath.moveTo(x, height)
                                        fillPath.lineTo(x, y)
                                    } else {
                                        speedPath.lineTo(x, y)
                                        fillPath.lineTo(x, y)
                                    }

                                    if (i == count - 1) {
                                        fillPath.lineTo(x, height)
                                        fillPath.close()
                                    }
                                }

                                drawPath(
                                    path = fillPath,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(Color(0xFF38BDF8).copy(alpha = 0.25f), Color.Transparent)
                                    )
                                )

                                drawPath(
                                    path = speedPath,
                                    color = Color(0xFF38BDF8),
                                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                                )
                            }

                            // 2. Draw RAM Usage Line (Green)
                            if (chartMode == ChartViewMode.MEMORY || chartMode == ChartViewMode.DUAL) {
                                val ramPath = Path()
                                val fillRamPath = Path()

                                samples.forEachIndexed { i, s ->
                                    val x = i * stepX
                                    val normalizedRam = (s.ramUsedMb.toFloat() / maxRam).coerceIn(0f, 1f)
                                    val y = height - (normalizedRam * height)

                                    if (i == 0) {
                                        ramPath.moveTo(x, y)
                                        fillRamPath.moveTo(x, height)
                                        fillRamPath.lineTo(x, y)
                                    } else {
                                        ramPath.lineTo(x, y)
                                        fillRamPath.lineTo(x, y)
                                    }

                                    if (i == count - 1) {
                                        fillRamPath.lineTo(x, height)
                                        fillRamPath.close()
                                    }
                                }

                                if (chartMode == ChartViewMode.MEMORY) {
                                    drawPath(
                                        path = fillRamPath,
                                        brush = Brush.verticalGradient(
                                            colors = listOf(Color(0xFF4ADE80).copy(alpha = 0.25f), Color.Transparent)
                                        )
                                    )
                                }

                                drawPath(
                                    path = ramPath,
                                    color = Color(0xFF4ADE80),
                                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                                )
                            }

                            // Selected point indicator crosshair
                            selectedSampleIndex?.let { idx ->
                                if (idx in samples.indices) {
                                    val s = samples[idx]
                                    val x = idx * stepX
                                    drawLine(
                                        color = Color.White.copy(alpha = 0.5f),
                                        start = Offset(x, 0f),
                                        end = Offset(x, height),
                                        strokeWidth = 1.5f
                                    )
                                    val normSpeed = (s.speedTokSec / maxSpeed).coerceIn(0f, 1f)
                                    val speedY = height - (normSpeed * height)
                                    drawCircle(
                                        color = Color(0xFF38BDF8),
                                        radius = 5.dp.toPx(),
                                        center = Offset(x, speedY)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Legend Footer
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF38BDF8)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("TPS (Speed)", fontSize = 10.sp, color = Color(0xFF94A3B8))

                    Spacer(modifier = Modifier.width(12.dp))

                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF4ADE80)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("RAM (MB)", fontSize = 10.sp, color = Color(0xFF94A3B8))
                }

                Text(
                    text = if (engineMode == ChartEngineMode.D3_VECTOR) "D3.js SVG Active" else "Native Canvas",
                    fontSize = 9.sp,
                    color = Color(0xFF818CF8)
                )
            }
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    subtext: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(Color(0xFF1E293B).copy(alpha = 0.7f), shape = RoundedCornerShape(12.dp))
            .border(0.5.dp, Color.White.copy(alpha = 0.1f), shape = RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF94A3B8)
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(15.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                color = accentColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtext,
                fontSize = 9.sp,
                color = Color(0xFFCBD5E1)
            )
        }
    }
}
