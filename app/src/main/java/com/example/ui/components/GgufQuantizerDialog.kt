package com.example.ui.components

import android.app.ActivityManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.db.ModelProfileEntity
import com.example.engine.gguf.GgufQuantType
import com.example.engine.gguf.GgufQuantizerEngine
import com.example.engine.gguf.QuantizationOptions
import com.example.engine.gguf.QuantizationProgress
import java.util.Locale

private fun formatByteSize(bytes: Long): String {
    return when {
        bytes >= 1_073_741_824L -> String.format(Locale.US, "%.2f GB", bytes.toDouble() / 1_073_741_824L)
        bytes >= 1_048_576L -> String.format(Locale.US, "%.1f MB", bytes.toDouble() / 1_048_576L)
        bytes >= 1024L -> String.format(Locale.US, "%.0f KB", bytes.toDouble() / 1024L)
        else -> "$bytes B"
    }
}

/**
 * Premium Glassmorphic On-Device GGUF Quantizer Dialog.
 * Allows users to re-quantize larger models into smaller, hardware-optimized formats (e.g. Q4_K_M, Q2_K)
 * with live metrics, RAM optimization recommendations, and progress tracking.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GgufQuantizerDialog(
    allModels: List<ModelProfileEntity>,
    initialSelectedModel: ModelProfileEntity?,
    quantizationProgress: QuantizationProgress?,
    onStartQuantization: (ModelProfileEntity, QuantizationOptions) -> Unit,
    onCancelQuantization: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    // Memory info
    val (freeRamMb, totalRamMb) = remember {
        val mi = ActivityManager.MemoryInfo()
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        am?.getMemoryInfo(mi)
        val freeMb = (mi.availMem) / (1024 * 1024)
        val totalMb = (mi.totalMem) / (1024 * 1024)
        Pair(freeMb, totalMb)
    }

    val hardwareSuggestedQuant = remember(freeRamMb, totalRamMb) {
        GgufQuantType.getSuggestedForHardware(freeRamMb, totalRamMb)
    }

    var selectedSourceModel by remember {
        mutableStateOf(initialSelectedModel ?: allModels.firstOrNull())
    }

    var selectedTargetQuant by remember {
        mutableStateOf(
            if (selectedSourceModel != null && selectedSourceModel!!.quantType.contains("Q2")) {
                GgufQuantType.Q3_K_M
            } else {
                hardwareSuggestedQuant
            }
        )
    }

    val maxCores = remember { Runtime.getRuntime().availableProcessors() }
    var threadCount by remember { mutableIntStateOf((maxCores - 1).coerceAtLeast(1)) }
    var keepOriginal by remember { mutableStateOf(true) }
    var autoActivate by remember { mutableStateOf(true) }
    var customOutputName by remember {
        mutableStateOf(
            if (selectedSourceModel != null) {
                GgufQuantizerEngine.generateQuantizedFileName(selectedSourceModel!!.name, selectedTargetQuant)
            } else ""
        )
    }

    var showAdvancedSettings by remember { mutableStateOf(false) }
    var showLogConsole by remember { mutableStateOf(false) }

    LaunchedEffect(selectedSourceModel, selectedTargetQuant) {
        if (selectedSourceModel != null) {
            customOutputName = GgufQuantizerEngine.generateQuantizedFileName(
                selectedSourceModel!!.name,
                selectedTargetQuant
            )
        }
    }

    val isCurrentlyProcessing = quantizationProgress?.isProcessing == true
    val isCompleted = quantizationProgress?.isCompleted == true

    Dialog(
        onDismissRequest = {
            if (!isCurrentlyProcessing) {
                onDismiss()
            }
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = !isCurrentlyProcessing,
            dismissOnClickOutside = !isCurrentlyProcessing
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
                .padding(horizontal = 16.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.92f)
                    .testTag("gguf_quantizer_dialog_card"),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF0F172A).copy(alpha = 0.95f)
                ),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF6366F1).copy(alpha = 0.20f),
                                border = BorderStroke(1.dp, Color(0xFF6366F1).copy(alpha = 0.5f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = "Quantizer",
                                    tint = Color(0xFF818CF8),
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .size(24.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = "On-Device GGUF Quantizer",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Hardware Compression & RAM Optimization",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                            }
                        }

                        if (!isCurrentlyProcessing) {
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .testTag("quantizer_dialog_close_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.12f))

                    // Content Area: Either Active Progress View OR Configurator
                    if (isCurrentlyProcessing || isCompleted) {
                        QuantizationProgressView(
                            progress = quantizationProgress!!,
                            onCancel = onCancelQuantization,
                            onFinish = onDismiss
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Section 1: Source Model Selector
                            item {
                                Text(
                                    text = "1. SELECT SOURCE GGUF MODEL",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp,
                                    color = Color(0xFF818CF8)
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                if (allModels.isEmpty()) {
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(16.dp),
                                        color = Color.White.copy(alpha = 0.05f),
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f))
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "No GGUF models found in local storage.",
                                                color = Color.White.copy(alpha = 0.7f),
                                                fontSize = 13.sp
                                            )
                                            Text(
                                                text = "Please import a GGUF file first in the Models manager.",
                                                color = Color.White.copy(alpha = 0.5f),
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        allModels.forEach { model ->
                                            val isSelected = selectedSourceModel?.id == model.id
                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { selectedSourceModel = model }
                                                    .testTag("source_model_option_${model.id}"),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = if (isSelected) Color(0xFF6366F1).copy(alpha = 0.22f) else Color.White.copy(alpha = 0.04f)
                                                ),
                                                border = BorderStroke(
                                                    if (isSelected) 1.5.dp else 0.8.dp,
                                                    if (isSelected) Color(0xFF6366F1) else Color.White.copy(alpha = 0.12f)
                                                ),
                                                shape = RoundedCornerShape(16.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(12.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(
                                                        modifier = Modifier.weight(1f),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Psychology,
                                                            contentDescription = null,
                                                            tint = if (isSelected) Color(0xFF818CF8) else Color.White.copy(alpha = 0.6f),
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                        Column {
                                                            Text(
                                                                text = model.name,
                                                                fontSize = 13.sp,
                                                                fontWeight = FontWeight.SemiBold,
                                                                color = Color.White,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                            Text(
                                                                text = "Size: ${formatByteSize(model.sizeBytes)} • Current Quant: ${model.quantType} • Params: ${model.parameters}",
                                                                fontSize = 11.sp,
                                                                color = Color.White.copy(alpha = 0.6f)
                                                            )
                                                        }
                                                    }

                                                    if (isSelected) {
                                                        Icon(
                                                            imageVector = Icons.Default.CheckCircle,
                                                            contentDescription = "Selected",
                                                            tint = Color(0xFF6366F1),
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Section 2: Hardware Recommendation Banner
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color(0xFF0284C7).copy(alpha = 0.15f)
                                    ),
                                    border = BorderStroke(1.dp, Color(0xFF0284C7).copy(alpha = 0.4f)),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Speed,
                                            contentDescription = "Hardware Advisor",
                                            tint = Color(0xFF38BDF8),
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Column {
                                            Text(
                                                text = "Hardware Recommendation: ${hardwareSuggestedQuant.code}",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF38BDF8)
                                            )
                                            Text(
                                                text = "Device RAM: ${String.format(Locale.US, "%.1f", totalRamMb / 1024f)} GB Total (${String.format(Locale.US, "%.1f", freeRamMb / 1024f)} GB Free). Suggested for zero-lag local inference.",
                                                fontSize = 11.sp,
                                                color = Color.White.copy(alpha = 0.8f)
                                            )
                                        }
                                    }
                                }
                            }

                            // Section 3: Target Quantization Format Selection
                            item {
                                Text(
                                    text = "2. CHOOSE TARGET QUANTIZATION FORMAT",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp,
                                    color = Color(0xFF818CF8)
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                val sourceSizeBytes = selectedSourceModel?.sizeBytes ?: 3_000_000_000L
                                val sourceQuant = selectedSourceModel?.quantType ?: "FP16"

                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    GgufQuantType.entries.forEach { quantType ->
                                        val isSelected = selectedTargetQuant == quantType
                                        val estimatedSize = GgufQuantizerEngine.estimateQuantizedSizeBytes(
                                            sourceSizeBytes = sourceSizeBytes,
                                            sourceQuant = sourceQuant,
                                            targetQuant = quantType
                                        )
                                        val savingsPct = (((sourceSizeBytes - estimatedSize).toFloat() / sourceSizeBytes) * 100f).coerceAtLeast(0f)

                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { selectedTargetQuant = quantType }
                                                .testTag("target_quant_card_${quantType.code}"),
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (isSelected) Color(0xFF6366F1).copy(alpha = 0.20f) else Color.White.copy(alpha = 0.04f)
                                            ),
                                            border = BorderStroke(
                                                if (isSelected) 1.5.dp else 0.8.dp,
                                                if (isSelected) Color(0xFF6366F1) else Color.White.copy(alpha = 0.12f)
                                            ),
                                            shape = RoundedCornerShape(16.dp)
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(14.dp),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        Text(
                                                            text = quantType.code,
                                                            fontSize = 15.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color.White
                                                        )

                                                        Surface(
                                                            shape = CircleShape,
                                                            color = if (quantType.isRecommended) Color(0xFF22C55E).copy(alpha = 0.18f) else Color.White.copy(alpha = 0.08f),
                                                            border = BorderStroke(
                                                                0.8.dp,
                                                                if (quantType.isRecommended) Color(0xFF22C55E).copy(alpha = 0.5f) else Color.White.copy(alpha = 0.15f)
                                                            )
                                                        ) {
                                                            Text(
                                                                text = quantType.badgeLabel,
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = if (quantType.isRecommended) Color(0xFF22C55E) else Color.White.copy(alpha = 0.8f),
                                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                    }

                                                    // Estimated Output Size & Savings Badge
                                                    Surface(
                                                        shape = CircleShape,
                                                        color = Color(0xFF6366F1).copy(alpha = 0.15f)
                                                    ) {
                                                        Text(
                                                            text = "~${formatByteSize(estimatedSize)} (-${String.format(Locale.US, "%.0f", savingsPct)}%)",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFF818CF8),
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                        )
                                                    }
                                                }

                                                Text(
                                                    text = quantType.description,
                                                    fontSize = 11.sp,
                                                    color = Color.White.copy(alpha = 0.7f),
                                                    lineHeight = 15.sp
                                                )

                                                // Ratings Row
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "🎯 Accuracy: ${"%.1f".format(quantType.qualityScore)}/5.0 • ⚡ Speed: ${"%.1f".format(quantType.speedScore)}/5.0",
                                                        fontSize = 10.sp,
                                                        color = Color.White.copy(alpha = 0.5f)
                                                    )
                                                    Text(
                                                        text = "Min RAM: ${"%.1f".format(quantType.minRamGb)} GB",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = Color(0xFF38BDF8)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Section 4: Advanced Options Toggle
                            item {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showAdvancedSettings = !showAdvancedSettings },
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color.White.copy(alpha = 0.04f),
                                    border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.12f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Tune,
                                                contentDescription = null,
                                                tint = Color.White.copy(alpha = 0.7f),
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = "Conversion Settings & Thread Allocation",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = Color.White
                                            )
                                        }
                                        Icon(
                                            imageVector = if (showAdvancedSettings) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = null,
                                            tint = Color.White.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            }

                            if (showAdvancedSettings) {
                                item {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 4.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        // Output Filename Field
                                        OutlinedTextField(
                                            value = customOutputName,
                                            onValueChange = { customOutputName = it },
                                            label = { Text("Output Model Filename", fontSize = 11.sp) },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.White,
                                                focusedBorderColor = Color(0xFF6366F1),
                                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                                focusedContainerColor = Color.White.copy(alpha = 0.04f),
                                                unfocusedContainerColor = Color.White.copy(alpha = 0.04f)
                                            ),
                                            shape = RoundedCornerShape(12.dp),
                                            singleLine = true
                                        )

                                        // Thread Slider
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("Quantization CPU Threads", fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
                                                Text("$threadCount / $maxCores cores", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF818CF8))
                                            }
                                            Slider(
                                                value = threadCount.toFloat(),
                                                onValueChange = { threadCount = it.toInt() },
                                                valueRange = 1f..maxCores.toFloat(),
                                                steps = maxCores - 2,
                                                colors = SliderDefaults.colors(
                                                    thumbColor = Color(0xFF6366F1),
                                                    activeTrackColor = Color(0xFF6366F1),
                                                    inactiveTrackColor = Color.White.copy(alpha = 0.15f)
                                                )
                                            )
                                        }

                                        // Toggles
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { keepOriginal = !keepOriginal },
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Checkbox(
                                                checked = keepOriginal,
                                                onCheckedChange = { keepOriginal = it },
                                                colors = CheckboxDefaults.colors(checkedColor = Color(0xFF6366F1))
                                            )
                                            Text(
                                                text = "Keep original larger model file",
                                                fontSize = 12.sp,
                                                color = Color.White.copy(alpha = 0.9f)
                                            )
                                        }

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { autoActivate = !autoActivate },
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Checkbox(
                                                checked = autoActivate,
                                                onCheckedChange = { autoActivate = it },
                                                colors = CheckboxDefaults.colors(checkedColor = Color(0xFF6366F1))
                                            )
                                            Text(
                                                text = "Auto-load & activate converted model immediately",
                                                fontSize = 12.sp,
                                                color = Color.White.copy(alpha = 0.9f)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Bottom Action Button
                        Button(
                            onClick = {
                                if (selectedSourceModel != null) {
                                    val options = QuantizationOptions(
                                        targetQuant = selectedTargetQuant,
                                        customOutputName = customOutputName,
                                        threadCount = threadCount,
                                        keepOriginal = keepOriginal,
                                        autoActivateConvertedModel = autoActivate
                                    )
                                    onStartQuantization(selectedSourceModel!!, options)
                                }
                            },
                            enabled = selectedSourceModel != null,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF6366F1),
                                contentColor = Color.White,
                                disabledContainerColor = Color.White.copy(alpha = 0.1f)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("start_quantization_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Start On-Device Conversion to ${selectedTargetQuant.code}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Live progress viewer showing tensor conversion progress, speeds, savings, and logs.
 */
@Composable
private fun QuantizationProgressView(
    progress: QuantizationProgress,
    onCancel: () -> Unit,
    onFinish: () -> Unit
) {
    val listState = rememberLazyListState()

    LaunchedEffect(progress.logMessages.size) {
        if (progress.logMessages.isNotEmpty()) {
            listState.animateScrollToItem(progress.logMessages.size - 1)
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Column(
        modifier = Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = when {
                    progress.isCompleted -> Color(0xFF22C55E).copy(alpha = 0.15f)
                    progress.errorMessage != null -> Color(0xFFEF4444).copy(alpha = 0.15f)
                    else -> Color(0xFF6366F1).copy(alpha = 0.15f)
                }
            ),
            border = BorderStroke(
                1.dp,
                when {
                    progress.isCompleted -> Color(0xFF22C55E).copy(alpha = 0.4f)
                    progress.errorMessage != null -> Color(0xFFEF4444).copy(alpha = 0.4f)
                    else -> Color(0xFF6366F1).copy(alpha = 0.4f)
                }
            ),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (progress.isCompleted) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Done",
                                tint = Color(0xFF22C55E),
                                modifier = Modifier.size(24.dp)
                            )
                        } else if (progress.errorMessage != null) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = "Error",
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.5.dp,
                                color = Color(0xFF818CF8)
                            )
                        }

                        Text(
                            text = when {
                                progress.isCompleted -> "Optimization Complete!"
                                progress.errorMessage != null -> "Quantization Error"
                                else -> "Quantizing: ${progress.sourceQuant} -> ${progress.targetQuant}"
                            },
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Text(
                        text = "${(progress.progressFraction * 100).toInt()}%",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (progress.isCompleted) Color(0xFF22C55E) else Color(0xFF818CF8)
                    )
                }

                // Progress Bar
                LinearProgressIndicator(
                    progress = { progress.progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (progress.isCompleted) Color(0xFF22C55E) else Color(0xFF6366F1),
                    trackColor = Color.White.copy(alpha = 0.1f)
                )

                // Current Layer and Speed Metrics
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (progress.isProcessing) "Tensor ${progress.currentTensorIndex}/${progress.totalTensors}" else progress.statusMessage,
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (progress.isProcessing) {
                        Text(
                            text = "⚡ ${String.format(Locale.US, "%.1f", progress.speedMBPerSec)} MB/s",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF38BDF8)
                        )
                    }
                }
            }
        }

        // Live Tensor Log Box (Console)
        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.5f)),
            border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.12f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Terminal,
                            contentDescription = null,
                            tint = Color(0xFF818CF8),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Quantization Stream Log",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }

                    Text(
                        text = "${progress.logMessages.size} entries",
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.4f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(progress.logMessages) { logLine ->
                        Text(
                            text = logLine,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = when {
                                logLine.contains("ERROR") -> Color(0xFFF87171)
                                logLine.contains("Complete") || logLine.contains("Saved") -> Color(0xFF4ADE80)
                                logLine.contains("->") -> Color(0xFF93C5FD)
                                else -> Color.White.copy(alpha = 0.7f)
                            },
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }

        // Bottom Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (progress.isProcessing) {
                OutlinedButton(
                    onClick = onCancel,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                    border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .testTag("cancel_quantization_btn")
                ) {
                    Icon(imageVector = Icons.Default.Cancel, contentDescription = "Cancel", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Cancel Quantization", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            } else {
                Button(
                    onClick = onFinish,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .testTag("finish_quantization_btn")
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = "Done", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Done", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
