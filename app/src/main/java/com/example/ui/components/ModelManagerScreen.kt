package com.example.ui.components

import android.app.ActivityManager
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Eject
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import com.example.engine.gguf.GgufQuantType
import com.example.engine.gguf.QuantizationOptions
import com.example.engine.gguf.QuantizationProgress
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.CircularProgressIndicator
import com.example.ui.viewmodel.GgufDownloadState
import androidx.compose.ui.unit.sp
import com.example.data.db.ModelProfileEntity
import com.example.ui.viewmodel.GgufImportProgress
import com.example.util.MemoryCheckResult

private fun formatByteSize(bytes: Long): String {
    return when {
        bytes >= 1_073_741_824L -> String.format("%.2f GB", bytes.toDouble() / 1_073_741_824L)
        bytes >= 1_048_576L -> String.format("%.1f MB", bytes.toDouble() / 1_048_576L)
        bytes >= 1024L -> String.format("%.0f KB", bytes.toDouble() / 1024L)
        else -> "$bytes B"
    }
}

data class PresetModelItem(
    val name: String,
    val filename: String,
    val architecture: String,
    val parameters: String,
    val quantType: String,
    val sizeDisplay: String,
    val sizeBytes: Long,
    val description: String,
    val downloadUrl: String,
    val category: String = "General"
)

private val PRESET_MODELS = listOf(
    // Coding Category
    PresetModelItem(
        name = "Qwen 2.5 Coder 1.5B Instruct",
        filename = "qwen2.5-coder-1.5b-instruct-q4_k_m.gguf",
        architecture = "Qwen2",
        parameters = "1.5B",
        quantType = "Q4_K_M",
        sizeDisplay = "1.10 GB",
        sizeBytes = 1_100_000_000L,
        description = "Alibaba's specialized code generation model trained on vast code repositories.",
        downloadUrl = "https://huggingface.co/Qwen/Qwen2.5-Coder-1.5B-Instruct-GGUF/resolve/main/qwen2.5-coder-1.5b-instruct-q4_k_m.gguf",
        category = "Coding"
    ),
    PresetModelItem(
        name = "DeepSeek Coder 1.3B Instruct",
        filename = "deepseek-coder-1.3b-instruct.Q4_K_M.gguf",
        architecture = "DeepSeek",
        parameters = "1.3B",
        quantType = "Q4_K_M",
        sizeDisplay = "0.88 GB",
        sizeBytes = 880_000_000L,
        description = "Lightweight coding assistant optimized for code completion and debugging.",
        downloadUrl = "https://huggingface.co/TheBloke/deepseek-coder-1.3b-instruct-GGUF/resolve/main/deepseek-coder-1.3b-instruct.Q4_K_M.gguf",
        category = "Coding"
    ),

    // General Category
    PresetModelItem(
        name = "Gemma 2 2B Instruction",
        filename = "gemma-2-2b-it-Q4_K_M.gguf",
        architecture = "Gemma",
        parameters = "2.5B",
        quantType = "Q4_K_M",
        sizeDisplay = "1.68 GB",
        sizeBytes = 1_680_000_000L,
        description = "Google's lightweight instruction-tuned 2B model optimized for Android ARM64 CPU & NPU.",
        downloadUrl = "https://huggingface.co/bartowski/gemma-2-2b-it-GGUF/resolve/main/gemma-2-2b-it-Q4_K_M.gguf",
        category = "General"
    ),
    PresetModelItem(
        name = "Qwen 2.5 1.5B Instruct",
        filename = "qwen2.5-1.5b-instruct-q4_k_m.gguf",
        architecture = "Qwen",
        parameters = "1.5B",
        quantType = "Q4_K_M",
        sizeDisplay = "1.10 GB",
        sizeBytes = 1_100_000_000L,
        description = "Alibaba's ultra-compact high-speed code & general conversation model.",
        downloadUrl = "https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct-GGUF/resolve/main/qwen2.5-1.5b-instruct-q4_k_m.gguf",
        category = "General"
    ),
    PresetModelItem(
        name = "Llama 3.2 1B Instruct",
        filename = "Llama-3.2-1B-Instruct-Q4_K_M.gguf",
        architecture = "Llama",
        parameters = "1.2B",
        quantType = "Q4_K_M",
        sizeDisplay = "0.85 GB",
        sizeBytes = 850_000_000L,
        description = "Meta's lightweight 1B model with state-of-the-art reasoning for mobile devices.",
        downloadUrl = "https://huggingface.co/bartowski/Llama-3.2-1B-Instruct-GGUF/resolve/main/Llama-3.2-1B-Instruct-Q4_K_M.gguf",
        category = "General"
    ),
    PresetModelItem(
        name = "Phi-3 Mini 4K Instruct",
        filename = "Phi-3-mini-4k-instruct-q4.gguf",
        architecture = "Phi",
        parameters = "3.8B",
        quantType = "Q4_K_M",
        sizeDisplay = "2.30 GB",
        sizeBytes = 2_300_000_000L,
        description = "Microsoft's high-reasoning compact model with 4K context length.",
        downloadUrl = "https://huggingface.co/microsoft/Phi-3-mini-4k-instruct-gguf/resolve/main/Phi-3-mini-4k-instruct-q4.gguf",
        category = "General"
    ),

    // Math Category
    PresetModelItem(
        name = "Qwen 2.5 Math 1.5B Instruct",
        filename = "qwen2.5-math-1.5b-instruct-q4_k_m.gguf",
        architecture = "Qwen2",
        parameters = "1.5B",
        quantType = "Q4_K_M",
        sizeDisplay = "1.12 GB",
        sizeBytes = 1_120_000_000L,
        description = "Specialized math problem-solving LLM trained for step-by-step mathematical reasoning.",
        downloadUrl = "https://huggingface.co/Qwen/Qwen2.5-Math-1.5B-Instruct-GGUF/resolve/main/qwen2.5-math-1.5b-instruct-q4_k_m.gguf",
        category = "Math"
    ),
    PresetModelItem(
        name = "DeepSeek Math 7B Instruct",
        filename = "deepseek-math-7b-instruct.Q4_K_M.gguf",
        architecture = "DeepSeek",
        parameters = "7.0B",
        quantType = "Q4_K_M",
        sizeDisplay = "4.10 GB",
        sizeBytes = 4_100_000_000L,
        description = "Advanced mathematical reasoning model for algebra, calculus, and theorem proving.",
        downloadUrl = "https://huggingface.co/TheBloke/deepseek-math-7b-instruct-GGUF/resolve/main/deepseek-math-7b-instruct.Q4_K_M.gguf",
        category = "Math"
    )
)

private data class PresetCategoryConfig(
    val categoryKey: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val color: Color
)

private val PRESET_CATEGORIES = listOf(
    PresetCategoryConfig(
        categoryKey = "Coding",
        title = "Coding Models",
        subtitle = "Specialized in code generation, debugging & syntax",
        icon = Icons.Default.Code,
        color = Color(0xFF00E676)
    ),
    PresetCategoryConfig(
        categoryKey = "General",
        title = "General Models",
        subtitle = "Versatile models for general chat, Q&A & reasoning",
        icon = Icons.Default.Psychology,
        color = Color(0xFF29B6F6)
    ),
    PresetCategoryConfig(
        categoryKey = "Math",
        title = "Math Models",
        subtitle = "Trained for math reasoning, step-by-step & equations",
        icon = Icons.Default.Category,
        color = Color(0xFFFFB74D)
    )
)

@Composable
private fun CategorySectionHeader(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    modelCount: Int,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() },
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E293B).copy(alpha = 0.65f)
        ),
        border = androidx.compose.foundation.BorderStroke(0.8.dp, Color.White.copy(alpha = 0.15f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = iconTint.copy(alpha = 0.15f)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(22.dp)
                    )
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = "$modelCount",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            IconButton(onClick = onToggle) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ModelManagerScreen(
    models: List<ModelProfileEntity>,
    selectedModel: ModelProfileEntity?,
    downloadStates: Map<String, GgufDownloadState> = emptyMap(),
    expandedCategories: Set<String> = setOf("Installed Models", "Coding", "General", "Math"),
    onToggleCategory: (String) -> Unit = {},
    importProgress: GgufImportProgress? = null,
    quantizationProgress: QuantizationProgress? = null,
    memoryCheckResult: MemoryCheckResult? = null,
    onModelSelected: (Long) -> Unit,
    onOffloadModel: () -> Unit,
    onImportGgufFile: (Uri) -> Unit,
    onDownloadFromUrl: (String, String) -> Unit,
    onDeleteModel: (ModelProfileEntity) -> Unit,
    onDismissImportProgress: () -> Unit,
    onStartQuantization: (ModelProfileEntity, QuantizationOptions) -> Unit = { _, _ -> },
    onCancelQuantization: () -> Unit = {},
    onDismiss: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var localExpandedCategories by remember { mutableStateOf(setOf("Installed Models", "Coding", "General", "Math")) }
    val currentExpandedCategories = remember(expandedCategories, localExpandedCategories) {
        if (expandedCategories.isNotEmpty()) expandedCategories else localExpandedCategories
    }
    val handleToggleCategory: (String) -> Unit = { cat ->
        onToggleCategory(cat)
        localExpandedCategories = if (localExpandedCategories.contains(cat)) {
            localExpandedCategories - cat
        } else {
            localExpandedCategories + cat
        }
    }

    // Memory Info calculation
    val (freeRamMb, totalRamMb) = remember(memoryCheckResult) {
        if (memoryCheckResult != null) {
            Pair(memoryCheckResult.availableRamMb, memoryCheckResult.totalRamMb)
        } else {
            val mi = ActivityManager.MemoryInfo()
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            am?.getMemoryInfo(mi)
            val freeMb = (mi?.availMem ?: (3500L * 1024 * 1024)) / (1024 * 1024)
            val totalMb = (mi?.totalMem ?: (8000L * 1024 * 1024)) / (1024 * 1024)
            Pair(freeMb, totalMb)
        }
    }

    val freeRamGbStr = remember(freeRamMb) {
        String.format("%.1f GB", freeRamMb / 1024f)
    }

    // State for Search and Filter
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("All") }

    val isSearchOrFilterActive = remember(searchQuery, selectedCategoryFilter) {
        searchQuery.isNotBlank() || selectedCategoryFilter != "All"
    }

    val filteredInstalledModels = remember(models, searchQuery, selectedCategoryFilter) {
        models.filter { model ->
            val categoryMatches = selectedCategoryFilter == "All" || selectedCategoryFilter == "Installed"
            val queryMatches = searchQuery.isBlank() ||
                    model.name.contains(searchQuery, ignoreCase = true) ||
                    model.quantType.contains(searchQuery, ignoreCase = true) ||
                    model.parameters.contains(searchQuery, ignoreCase = true) ||
                    model.path.contains(searchQuery, ignoreCase = true)
            categoryMatches && queryMatches
        }
    }

    // State for Dialogs
    var showAddFabDialog by remember { mutableStateOf(false) }
    var showHuggingFaceUrlDialog by remember { mutableStateOf(false) }
    var showQuantizerModal by remember { mutableStateOf(false) }
    var modelToQuantize by remember { mutableStateOf<ModelProfileEntity?>(null) }
    var huggingFaceUrlInput by remember { mutableStateOf("") }
    var hfModelNameInput by remember { mutableStateOf("") }
    var modelToDelete by remember { mutableStateOf<ModelProfileEntity?>(null) }
    var presetToDownload by remember { mutableStateOf<PresetModelItem?>(null) }

    // SAF Document Picker launcher
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            onImportGgufFile(uri)
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddFabDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = "Add Model") },
                text = { Text("Add Model", fontWeight = FontWeight.Bold) },
                containerColor = Color(0xFF6366F1).copy(alpha = 0.85f),
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                elevation = androidx.compose.material3.FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                    .padding(bottom = 16.dp)
                    .testTag("add_model_fab")
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Section
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF1E293B).copy(alpha = 0.65f)
                        ),
                        border = androidx.compose.foundation.BorderStroke(0.8.dp, Color.White.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
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
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Psychology,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }

                                Column {
                                    Text(
                                        text = "Models",
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "GGUF Model Hub & RAM Manager",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // RAM Usage Indicator Badge & Close Button
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (freeRamMb < 1500) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Memory,
                                            contentDescription = "RAM",
                                            tint = if (freeRamMb < 1500) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "Free RAM: $freeRamGbStr",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (freeRamMb < 1500) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }

                                if (onDismiss != null) {
                                    IconButton(
                                        onClick = onDismiss,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Close",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

            // Import Progress Card (if importing or error)
            if (importProgress != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (importProgress.errorMessage != null)
                                MaterialTheme.colorScheme.errorContainer
                            else
                                MaterialTheme.colorScheme.secondaryContainer
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
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
                                    Icon(
                                        imageVector = if (importProgress.errorMessage != null) Icons.Default.Warning else Icons.Default.SdStorage,
                                        contentDescription = null,
                                        tint = if (importProgress.errorMessage != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
                                    )
                                    Text(
                                        text = if (importProgress.errorMessage != null) "Import Error" else "Importing Model File...",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = if (importProgress.errorMessage != null) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }

                                IconButton(onClick = onDismissImportProgress) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Dismiss",
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }

                            if (importProgress.errorMessage != null) {
                                Text(
                                    text = importProgress.errorMessage,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            } else {
                                LinearProgressIndicator(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 10.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                )
                            }
                        }
                    }
                }
            }

            // Real-Time System RAM Telemetry Chart
            item(key = "real_time_ram_chart") {
                RealTimeRamChartCard()
            }

            // Search & Filter Bar Section
            item(key = "search_filter_bar") {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("model_search_filter_card"),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF1E293B).copy(alpha = 0.65f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(0.8.dp, Color.White.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Search TextField
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("model_search_input"),
                            placeholder = {
                                Text(
                                    text = "Search models by name or quantization (e.g. Q4_K_M)...",
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.5f)
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = Color(0xFF818CF8)
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = { searchQuery = "" },
                                        modifier = Modifier.testTag("clear_search_btn")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Clear search",
                                            tint = Color.White.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 13.sp),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF0F172A).copy(alpha = 0.6f),
                                unfocusedContainerColor = Color(0xFF0F172A).copy(alpha = 0.4f),
                                focusedBorderColor = Color(0xFF818CF8),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                            )
                        )

                        // Category Filter Chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val categoryFilters = listOf("All", "Installed", "Coding", "General", "Math")
                            categoryFilters.forEach { cat ->
                                val isSelected = selectedCategoryFilter == cat
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (isSelected) Color(0xFF6366F1).copy(alpha = 0.35f) else Color.White.copy(alpha = 0.05f),
                                    contentColor = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
                                    border = androidx.compose.foundation.BorderStroke(
                                        0.5.dp,
                                        if (isSelected) Color(0xFF818CF8).copy(alpha = 0.6f) else Color.White.copy(alpha = 0.15f)
                                    ),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .clickable { selectedCategoryFilter = cat }
                                        .testTag("filter_chip_$cat")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color(0xFF818CF8),
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                        Text(
                                            text = cat,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Section 1: Installed Local Models (Collapsible)
            val showInstalledSection = selectedCategoryFilter == "All" || selectedCategoryFilter == "Installed"
            if (showInstalledSection) {
                item(key = "header_installed") {
                    val isInstalledExpanded = isSearchOrFilterActive || currentExpandedCategories.contains("Installed Models")
                    CategorySectionHeader(
                        title = "Installed Local Models",
                        subtitle = "Models stored on device storage & active RAM",
                        icon = Icons.Default.SdStorage,
                        iconTint = MaterialTheme.colorScheme.primary,
                        modelCount = filteredInstalledModels.size,
                        isExpanded = isInstalledExpanded,
                        onToggle = { handleToggleCategory("Installed Models") }
                    )
                }

                val isInstalledExpanded = isSearchOrFilterActive || currentExpandedCategories.contains("Installed Models")
                if (isInstalledExpanded) {
                    if (filteredInstalledModels.isEmpty()) {
                        item(key = "installed_empty") {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SdStorage,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Text(
                                        text = if (searchQuery.isNotBlank()) "No local models matching '$searchQuery'" else "No local models found",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Tap '+' to import or download preset models below.",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        items(filteredInstalledModels, key = { "installed_${it.id}" }) { model ->
                        val isActiveInRam = model.isSelected || selectedModel?.id == model.id
                        val sizeMb = model.sizeBytes / (1024 * 1024)
                        val sizeGb = sizeMb / 1024f
                        val formattedSize = if (sizeGb >= 1.0f) String.format("%.2f GB", sizeGb) else "$sizeMb MB"

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("model_card_${model.id}"),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF1E293B).copy(alpha = if (isActiveInRam) 0.85f else 0.65f)
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (isActiveInRam) 1.dp else 0.8.dp,
                                color = if (isActiveInRam) Color(0xFF22C55E) else Color.White.copy(alpha = 0.15f)
                            ),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Top Row: Model Name + Active Badge
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Psychology,
                                            contentDescription = null,
                                            tint = if (isActiveInRam) Color(0xFF22C55E) else Color.White.copy(alpha = 0.7f),
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Text(
                                            text = model.name,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    // Active RAM Badge
                                    if (isActiveInRam) {
                                        Surface(
                                            shape = CircleShape,
                                            color = Color(0xFF22C55E).copy(alpha = 0.15f),
                                            border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0xFF22C55E).copy(alpha = 0.5f))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(0xFF22C55E))
                                                )
                                                Text(
                                                    text = "Loaded in RAM",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF22C55E)
                                                )
                                            }
                                        }
                                    } else {
                                        Surface(
                                            shape = CircleShape,
                                            color = Color.White.copy(alpha = 0.08f)
                                        ) {
                                            Text(
                                                text = "Offloaded",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = Color.White.copy(alpha = 0.6f),
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }

                                // Middle Info Pills
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color.White.copy(alpha = 0.08f)
                                    ) {
                                        Text(
                                            text = formattedSize,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White.copy(alpha = 0.9f),
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }

                                    Surface(
                                        shape = CircleShape,
                                        color = Color.White.copy(alpha = 0.08f)
                                    ) {
                                        Text(
                                            text = model.quantType,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White.copy(alpha = 0.9f),
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }

                                    if (model.parameters.isNotBlank()) {
                                        Surface(
                                            shape = CircleShape,
                                            color = Color.White.copy(alpha = 0.08f)
                                        ) {
                                            Text(
                                                text = model.parameters,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = Color.White.copy(alpha = 0.9f),
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }

                                HorizontalDivider(
                                    color = Color.White.copy(alpha = 0.12f)
                                )

                                // Action Row: Load/Offload Toggle + Quantize Button + Delete Icon
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (isActiveInRam) {
                                            Button(
                                                onClick = onOffloadModel,
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = Color(0xFFEF4444).copy(alpha = 0.85f),
                                                    contentColor = Color.White
                                                ),
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier
                                                    .height(38.dp)
                                                    .testTag("offload_model_btn_${model.id}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Eject,
                                                    contentDescription = "Offload",
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Offload from RAM", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                            }
                                        } else {
                                            Button(
                                                onClick = { onModelSelected(model.id) },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = Color(0xFF6366F1),
                                                    contentColor = Color.White
                                                ),
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier
                                                    .height(38.dp)
                                                    .testTag("load_model_btn_${model.id}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.PlayArrow,
                                                    contentDescription = "Load",
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Load Model", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                            }
                                        }

                                        // Quantize / Hardware Optimize Button
                                        OutlinedButton(
                                            onClick = {
                                                modelToQuantize = model
                                                showQuantizerModal = true
                                            },
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                contentColor = Color(0xFF818CF8)
                                            ),
                                            border = BorderStroke(1.dp, Color(0xFF6366F1).copy(alpha = 0.5f)),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .height(38.dp)
                                                .testTag("quantize_model_btn_${model.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Tune,
                                                contentDescription = "Quantize",
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Text("Quantize", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                    }

                                    IconButton(
                                        onClick = { modelToDelete = model },
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(Color(0xFFEF4444).copy(alpha = 0.15f))
                                            .testTag("delete_model_btn_${model.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete Model",
                                            tint = Color(0xFFEF4444)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            }

            // Section 2: Preset Model Categories Collapsible Sections ("Coding", "General", "Math")
            PRESET_CATEGORIES.forEach { categoryConfig ->
                val isCategoryAllowed = selectedCategoryFilter == "All" || selectedCategoryFilter == categoryConfig.categoryKey
                if (isCategoryAllowed) {
                    val categoryPresets = PRESET_MODELS.filter { preset ->
                        preset.category == categoryConfig.categoryKey &&
                        (searchQuery.isBlank() ||
                            preset.name.contains(searchQuery, ignoreCase = true) ||
                            preset.architecture.contains(searchQuery, ignoreCase = true) ||
                            preset.parameters.contains(searchQuery, ignoreCase = true) ||
                            preset.quantType.contains(searchQuery, ignoreCase = true) ||
                            preset.description.contains(searchQuery, ignoreCase = true) ||
                            preset.filename.contains(searchQuery, ignoreCase = true))
                    }
                    val isCategoryExpanded = isSearchOrFilterActive || currentExpandedCategories.contains(categoryConfig.categoryKey)

                    item(key = "header_${categoryConfig.categoryKey}") {
                        Spacer(modifier = Modifier.height(4.dp))
                        CategorySectionHeader(
                            title = categoryConfig.title,
                            subtitle = categoryConfig.subtitle,
                            icon = categoryConfig.icon,
                            iconTint = categoryConfig.color,
                            modelCount = categoryPresets.size,
                            isExpanded = isCategoryExpanded,
                            onToggle = { handleToggleCategory(categoryConfig.categoryKey) }
                        )
                    }

                    if (isCategoryExpanded) {
                        if (categoryPresets.isEmpty()) {
                            item(key = "empty_preset_${categoryConfig.categoryKey}") {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = "No ${categoryConfig.title.lowercase()} matching '$searchQuery'",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(14.dp)
                                    )
                                }
                            }
                        } else {
                            items(categoryPresets, key = { "preset_${it.filename}" }) { preset ->
                        val cleanFilename = if (preset.filename.endsWith(".gguf", ignoreCase = true)) preset.filename else "${preset.filename}.gguf"

                        val isAlreadyDownloaded = remember(models, cleanFilename) {
                            models.any {
                                it.name.equals(cleanFilename, ignoreCase = true) ||
                                it.path.endsWith(cleanFilename, ignoreCase = true) ||
                                it.name.contains(preset.filename.removeSuffix(".gguf"), ignoreCase = true)
                            }
                        }

                        val downloadState = downloadStates[cleanFilename]
                        val isDownloading = downloadState?.isDownloading == true

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF1E293B).copy(alpha = 0.65f)
                            ),
                            border = androidx.compose.foundation.BorderStroke(0.8.dp, Color.White.copy(alpha = 0.15f)),
                            shape = RoundedCornerShape(20.dp)
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
                                    Text(
                                        text = preset.name,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.weight(1f)
                                    )

                                    Surface(
                                        shape = CircleShape,
                                        color = categoryConfig.color.copy(alpha = 0.15f),
                                        border = androidx.compose.foundation.BorderStroke(0.8.dp, categoryConfig.color.copy(alpha = 0.3f))
                                    ) {
                                        Text(
                                            text = preset.category,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = categoryConfig.color,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color.White.copy(alpha = 0.08f)
                                    ) {
                                        Text(
                                            text = preset.architecture,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White.copy(alpha = 0.9f),
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }

                                    Surface(
                                        shape = CircleShape,
                                        color = Color.White.copy(alpha = 0.08f)
                                    ) {
                                        Text(
                                            text = preset.parameters,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White.copy(alpha = 0.9f),
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }

                                    Surface(
                                        shape = CircleShape,
                                        color = Color.White.copy(alpha = 0.08f)
                                    ) {
                                        Text(
                                            text = preset.quantType,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color.White.copy(alpha = 0.9f),
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = preset.description,
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.7f),
                                    lineHeight = 16.sp
                                )

                                if (isDownloading && downloadState != null) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "Downloading... ${downloadState.progressPercent}%",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF6366F1)
                                            )
                                            if (downloadState.totalBytes > 0) {
                                                Text(
                                                    text = "${formatByteSize(downloadState.bytesDownloaded)} / ${formatByteSize(downloadState.totalBytes)}",
                                                    fontSize = 11.sp,
                                                    color = Color.White.copy(alpha = 0.7f)
                                                )
                                            }
                                        }
                                        LinearProgressIndicator(
                                            progress = { downloadState.progressPercent / 100f },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(8.dp)
                                                .clip(RoundedCornerShape(4.dp)),
                                            color = Color(0xFF6366F1),
                                            trackColor = Color.White.copy(alpha = 0.1f)
                                        )
                                    }
                                }

                                HorizontalDivider(
                                    color = Color.White.copy(alpha = 0.12f),
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isAlreadyDownloaded) {
                                        val matchingModel = models.firstOrNull {
                                            it.name.equals(cleanFilename, ignoreCase = true) ||
                                            it.path.endsWith(cleanFilename, ignoreCase = true) ||
                                            it.name.contains(preset.filename.removeSuffix(".gguf"), ignoreCase = true)
                                        }

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                shape = CircleShape,
                                                color = Color(0xFF22C55E).copy(alpha = 0.15f),
                                                border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0xFF22C55E).copy(alpha = 0.5f))
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = Color(0xFF22C55E),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Text(
                                                        text = "Ready to Use",
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF22C55E)
                                                    )
                                                }
                                            }

                                            if (matchingModel != null) {
                                                Button(
                                                    onClick = { onModelSelected(matchingModel.id) },
                                                    shape = RoundedCornerShape(12.dp),
                                                    modifier = Modifier.height(36.dp),
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = if (matchingModel.isSelected) Color(0xFF10B981) else Color(0xFF6366F1),
                                                        contentColor = Color.White
                                                    )
                                                ) {
                                                    Text(
                                                        text = if (matchingModel.isSelected) "Active Model" else "Load Model",
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    } else if (isDownloading) {
                                        Button(
                                            onClick = { },
                                            enabled = false,
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.height(36.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                disabledContainerColor = Color.White.copy(alpha = 0.1f)
                                            )
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(14.dp),
                                                strokeWidth = 2.dp,
                                                color = Color.White
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Downloading...",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color.White.copy(alpha = 0.7f)
                                            )
                                        }
                                    } else {
                                        Button(
                                            onClick = { presetToDownload = preset },
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.height(36.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFF6366F1),
                                                contentColor = Color.White
                                            )
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Download,
                                                contentDescription = "Download",
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Download (${preset.sizeDisplay})",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

            item {
                Spacer(modifier = Modifier.height(80.dp)) // Padding for FAB and bottom nav
            }
        }
    }

    // FAB Choice Dialog
    if (showAddFabDialog) {
        AlertDialog(
            onDismissRequest = { showAddFabDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text("Add GGUF Model", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Select how you would like to add a quantized model file:",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Option 1: Import Local GGUF File
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showAddFabDialog = false
                                try {
                                    documentPickerLauncher.launch(arrayOf("*/*"))
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.FolderOpen,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "Import Local .gguf File",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Pick file from storage or SD card",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Option 2: Download from HuggingFace URL
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showAddFabDialog = false
                                showHuggingFaceUrlDialog = true
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.CloudDownload,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "Download from HuggingFace URL",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Enter direct download link for GGUF model",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Option 3: Quantize & Optimize Local Model
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showAddFabDialog = false
                                showQuantizerModal = true
                            }
                            .testTag("fab_quantize_option_card"),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF6366F1).copy(alpha = 0.20f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Tune,
                                        contentDescription = null,
                                        tint = Color(0xFF818CF8),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "Quantize & Optimize Model",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Compress larger GGUF to Q4_K_M or Q2_K on device",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAddFabDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // HuggingFace URL Download Input Dialog
    if (showHuggingFaceUrlDialog) {
        AlertDialog(
            onDismissRequest = { showHuggingFaceUrlDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    Text("HuggingFace URL Download", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Paste direct .gguf model download link from Hugging Face repository:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = huggingFaceUrlInput,
                        onValueChange = { huggingFaceUrlInput = it },
                        label = { Text("Model Download URL") },
                        placeholder = { Text("https://huggingface.co/...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = hfModelNameInput,
                        onValueChange = { hfModelNameInput = it },
                        label = { Text("Custom Model Name (Optional)") },
                        placeholder = { Text("Custom-Model-Q4_K_M.gguf") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (huggingFaceUrlInput.isNotBlank()) {
                            val name = hfModelNameInput.ifBlank { "HF-Model.gguf" }
                            onDownloadFromUrl(huggingFaceUrlInput.trim(), name.trim())
                            showHuggingFaceUrlDialog = false
                            huggingFaceUrlInput = ""
                            hfModelNameInput = ""
                        }
                    },
                    enabled = huggingFaceUrlInput.isNotBlank()
                ) {
                    Text("Start Download")
                }
            },
            dismissButton = {
                TextButton(onClick = { showHuggingFaceUrlDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Download Preset Confirmation Dialog
    if (presetToDownload != null) {
        val preset = presetToDownload!!
        AlertDialog(
            onDismissRequest = { presetToDownload = null },
            title = {
                Text("Download ${preset.name}?", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Download ${preset.filename} (${preset.sizeDisplay}) from HuggingFace into device storage for local AI inference?",
                        fontSize = 13.sp
                    )
                    Text(
                        text = preset.description,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    onDownloadFromUrl(preset.downloadUrl, preset.filename)
                    presetToDownload = null
                }) {
                    Text("Download (${preset.sizeDisplay})")
                }
            },
            dismissButton = {
                TextButton(onClick = { presetToDownload = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Model Confirmation Dialog
    if (modelToDelete != null) {
        val model = modelToDelete!!
        AlertDialog(
            onDismissRequest = { modelToDelete = null },
            title = { Text("Delete Model?", fontSize = 17.sp, fontWeight = FontWeight.Bold) },
            text = {
                Text("Are you sure you want to delete '${model.name}'? This will free up storage space on your device.", fontSize = 13.sp)
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteModel(model)
                        modelToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { modelToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // In-App GGUF Hardware Quantizer & Compression Dialog
    if (showQuantizerModal || modelToQuantize != null || quantizationProgress?.isProcessing == true) {
        GgufQuantizerDialog(
            allModels = models,
            initialSelectedModel = modelToQuantize,
            quantizationProgress = quantizationProgress,
            onStartQuantization = { model, options ->
                onStartQuantization(model, options)
            },
            onCancelQuantization = onCancelQuantization,
            onDismiss = {
                showQuantizerModal = false
                modelToQuantize = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelManagerDialog(
    models: List<ModelProfileEntity>,
    selectedModel: ModelProfileEntity?,
    downloadStates: Map<String, GgufDownloadState> = emptyMap(),
    expandedCategories: Set<String> = setOf("Installed Models", "Coding", "General", "Math"),
    onToggleCategory: (String) -> Unit = {},
    importProgress: GgufImportProgress? = null,
    quantizationProgress: QuantizationProgress? = null,
    memoryCheckResult: MemoryCheckResult? = null,
    onModelSelected: (Long) -> Unit,
    onOffloadModel: () -> Unit,
    onImportGgufFile: (Uri) -> Unit,
    onDownloadFromUrl: (String, String) -> Unit,
    onDeleteModel: (ModelProfileEntity) -> Unit,
    onDismissImportProgress: () -> Unit,
    onStartQuantization: (ModelProfileEntity, QuantizationOptions) -> Unit = { _, _ -> },
    onCancelQuantization: () -> Unit = {},
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxHeight(0.92f)
    ) {
        ModelManagerScreen(
            models = models,
            selectedModel = selectedModel,
            downloadStates = downloadStates,
            expandedCategories = expandedCategories,
            onToggleCategory = onToggleCategory,
            importProgress = importProgress,
            quantizationProgress = quantizationProgress,
            memoryCheckResult = memoryCheckResult,
            onModelSelected = onModelSelected,
            onOffloadModel = onOffloadModel,
            onImportGgufFile = onImportGgufFile,
            onDownloadFromUrl = onDownloadFromUrl,
            onDeleteModel = onDeleteModel,
            onDismissImportProgress = onDismissImportProgress,
            onStartQuantization = onStartQuantization,
            onCancelQuantization = onCancelQuantization,
            onDismiss = onDismiss
        )
    }
}

@Composable
fun RealTimeRamChartCard(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val ramHistory = remember { androidx.compose.runtime.mutableStateListOf<Float>() }
    var availRamMbState by remember { androidx.compose.runtime.mutableStateOf(2048L) }
    var isOomWarning by remember { androidx.compose.runtime.mutableStateOf(false) }

    // Pulsing animation for OOM Warning
    val infiniteTransition = rememberInfiniteTransition(label = "OomPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 550, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha"
    )

    LaunchedEffect(Unit) {
        if (ramHistory.isEmpty()) {
            repeat(15) {
                ramHistory.add(1.8f + (Math.random() * 0.3f).toFloat())
            }
        }
        while (true) {
            kotlinx.coroutines.delay(1000L)
            var currentAvailMb = 2048L
            val currentUsed = try {
                val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
                val memoryInfo = ActivityManager.MemoryInfo()
                activityManager?.getMemoryInfo(memoryInfo)
                if (memoryInfo != null) {
                    val totalGb = (memoryInfo.totalMem / (1024f * 1024f * 1024f))
                    val availGb = (memoryInfo.availMem / (1024f * 1024f * 1024f))
                    currentAvailMb = (memoryInfo.availMem / (1024 * 1024))
                    (totalGb - availGb).coerceAtLeast(0.5f)
                } else {
                    val runtime = Runtime.getRuntime()
                    val freeMemMb = (runtime.maxMemory() - (runtime.totalMemory() - runtime.freeMemory())) / (1024 * 1024)
                    currentAvailMb = freeMemMb
                    ((runtime.totalMemory() - runtime.freeMemory()) / (1024f * 1024f * 1024f)).coerceAtLeast(0.5f)
                }
            } catch (e: Exception) {
                1.8f
            }
            availRamMbState = currentAvailMb
            // OOM threshold: < 500 MB available system RAM matches LocalGgufInferenceEngine threshold
            isOomWarning = currentAvailMb < 500L

            if (ramHistory.size >= 25) {
                ramHistory.removeAt(0)
            }
            ramHistory.add(currentUsed)
        }
    }

    val latestUsed = ramHistory.lastOrNull() ?: 1.8f
    val chartColor = if (isOomWarning) Color(0xFFEF4444) else Color(0xFF38BDF8)
    val glowColor = if (isOomWarning) Color(0xFFEF4444).copy(alpha = pulseAlpha) else Color(0xFF38BDF8).copy(alpha = 0.4f)

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.65f)),
        border = BorderStroke(
            1.dp,
            if (isOomWarning) Color(0xFFEF4444).copy(alpha = pulseAlpha) else Color.White.copy(alpha = 0.12f)
        ),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = glowColor,
                            modifier = Modifier.size(8.dp)
                        ) {}
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "RAM Usage & Telemetry",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Text(
                        text = if (isOomWarning) "CRITICAL: Avail RAM < 500MB (OOM Risk)" else "System RAM Available: ${availRamMbState} MB",
                        fontSize = 11.sp,
                        color = if (isOomWarning) Color(0xFFF87171) else Color.White.copy(alpha = 0.6f)
                    )
                }

                if (isOomWarning) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFEF4444).copy(alpha = 0.25f * pulseAlpha),
                        border = BorderStroke(0.8.dp, Color(0xFFEF4444).copy(alpha = pulseAlpha))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "OOM Warning",
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "OOM WARNING",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF87171)
                            )
                        }
                    }
                } else {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = String.format(java.util.Locale.US, "%.2f GB", latestUsed),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8)
                        )
                        Text(
                            text = "Status: Nominal",
                            fontSize = 10.sp,
                            color = Color(0xFF34D399)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(Color(0xFF020617).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(8.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    if (ramHistory.size < 2) return@Canvas

                    val width = size.width
                    val height = size.height
                    val maxVal = ((ramHistory.maxOrNull() ?: 2.0f) * 1.25f).coerceAtLeast(3.0f)
                    val minVal = 0f

                    val dx = width / (ramHistory.size - 1)

                    val strokePath = Path()
                    val fillPath = Path()

                    ramHistory.forEachIndexed { i, valPoint ->
                        val x = i * dx
                        val normalized = ((valPoint - minVal) / (maxVal - minVal)).coerceIn(0f, 1f)
                        val y = height - (normalized * height)

                        if (i == 0) {
                            strokePath.moveTo(x, y)
                            fillPath.moveTo(x, height)
                            fillPath.lineTo(x, y)
                        } else {
                            val prevX = (i - 1) * dx
                            val prevVal = ramHistory[i - 1]
                            val prevNorm = ((prevVal - minVal) / (maxVal - minVal)).coerceIn(0f, 1f)
                            val prevY = height - (prevNorm * height)

                            val cx = (prevX + x) / 2f
                            strokePath.cubicTo(cx, prevY, cx, y, x, y)
                            fillPath.cubicTo(cx, prevY, cx, y, x, y)
                        }
                    }

                    fillPath.lineTo(width, height)
                    fillPath.close()

                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                chartColor.copy(alpha = if (isOomWarning) 0.45f * pulseAlpha else 0.35f),
                                Color(0xFF0F172A).copy(alpha = 0.05f)
                            )
                        )
                    )

                    drawPath(
                        path = strokePath,
                        color = chartColor,
                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )

                    val lastX = width
                    val lastVal = ramHistory.last()
                    val lastNorm = ((lastVal - minVal) / (maxVal - minVal)).coerceIn(0f, 1f)
                    val lastY = height - (lastNorm * height)

                    drawCircle(
                        color = glowColor,
                        radius = (if (isOomWarning) 9.dp else 7.dp).toPx(),
                        center = androidx.compose.ui.geometry.Offset(lastX, lastY)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 3.dp.toPx(),
                        center = androidx.compose.ui.geometry.Offset(lastX, lastY)
                    )
                }
            }
        }
    }
}
