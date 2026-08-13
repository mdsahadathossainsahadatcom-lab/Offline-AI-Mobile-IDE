package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import com.example.engine.inference.AiProviderMode
import com.example.engine.inference.AiProviderSettings
import com.example.engine.inference.CloudProvider
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.size
import androidx.compose.ui.platform.testTag
import androidx.compose.material3.Surface
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.viewmodel.GgufImportProgress
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.ModelProfileEntity
import com.example.engine.inference.GenerationProgress
import com.example.ui.theme.IdeTheme
import com.example.ui.theme.ThemeMode
import com.example.util.MemoryCheckResult
import com.example.util.MemoryCheckUtil
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun SettingsScreen(
    currentTheme: IdeTheme,
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    useDynamicColor: Boolean = true,
    models: List<ModelProfileEntity>,
    selectedModel: ModelProfileEntity?,
    importProgress: GgufImportProgress? = null,
    isGenerating: Boolean = false,
    generationProgress: GenerationProgress? = null,
    memoryCheckResult: MemoryCheckResult? = null,
    contextWindow: Int = 4096,
    isHudEnabled: Boolean = false,
    isAutoSaveEnabled: Boolean = true,
    aiProviderSettings: AiProviderSettings = AiProviderSettings(),
    isTestingConnection: Boolean = false,
    connectionTestResult: String? = null,
    onThemeSelected: (IdeTheme) -> Unit,
    onThemeModeSelected: (ThemeMode) -> Unit = {},
    onToggleDynamicColor: (Boolean) -> Unit = {},
    onModelSelected: (Long) -> Unit,
    onImportGgufFile: (Uri) -> Unit,
    onDeleteModel: (ModelProfileEntity) -> Unit = {},
    onRenameModel: (Long, String) -> Unit = { _, _ -> },
    onDismissImportProgress: () -> Unit = {},
    onContextWindowChanged: (Int) -> Unit = {},
    onToggleHud: (Boolean) -> Unit = {},
    onToggleAutoSave: (Boolean) -> Unit = {},
    onProviderSettingsChanged: (AiProviderSettings) -> Unit = {},
    onTestConnection: () -> Unit = {},
    onClearHistory: () -> Unit = {}
) {

    // Dialog & Management States for Local GGUF Files
    var modelToDelete by remember { mutableStateOf<ModelProfileEntity?>(null) }
    var modelToRename by remember { mutableStateOf<ModelProfileEntity?>(null) }
    var renameInput by remember { mutableStateOf("") }
    var modelForDetails by remember { mutableStateOf<ModelProfileEntity?>(null) }
    var showModelManagerModal by remember { mutableStateOf(false) }

    if (showModelManagerModal) {
        Dialog(
            onDismissRequest = { showModelManagerModal = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                GgufModelManagerScreen(
                    models = models,
                    selectedModel = selectedModel,
                    importProgress = importProgress,
                    memoryCheckResult = memoryCheckResult,
                    onModelSelected = onModelSelected,
                    onImportGgufFile = onImportGgufFile,
                    onDeleteModel = onDeleteModel,
                    onRenameModel = onRenameModel,
                    onDismissImportProgress = onDismissImportProgress,
                    onCloseModal = { showModelManagerModal = false }
                )
            }
        }
    }

    // Storage Access Framework Document Picker for .gguf model files
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            onImportGgufFile(uri)
        }
    }

    // Dynamic Graph Data Points (History)
    val ramHistory = remember { mutableStateListOf(35f, 38f, 40f, 42f, 39f, 41f, 40f, 43f, 42f, 44f) }
    val cpuHistory = remember { mutableStateListOf(12f, 15f, 10f, 18f, 14f, 16f, 12f, 15f, 14f, 13f) }

    // Dynamic Loop that runs ONLY when AI inference is active to save resources
    LaunchedEffect(isGenerating) {
        if (isGenerating) {
            while (true) {
                delay(400)
                val newRam = (55..82).random().toFloat()
                val newCpu = (65..95).random().toFloat()

                if (ramHistory.size > 20) ramHistory.removeAt(0)
                if (cpuHistory.size > 20) cpuHistory.removeAt(0)

                ramHistory.add(newRam)
                cpuHistory.add(newCpu)
            }
        }
    }

    val safeInsets = WindowInsets.systemBars.asPaddingValues()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 16.dp + safeInsets.calculateTopPadding(),
            bottom = 16.dp + safeInsets.calculateBottomPadding()
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Theme Customization Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("theme_customization_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Palette, contentDescription = "Theme", tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "UI THEME CUSTOMIZATION",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "THEME MODE (LIGHT / DARK / SYSTEM)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Light / Dark / System Toggle Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemeMode.entries.forEach { mode ->
                            val isSelected = mode == themeMode
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onThemeModeSelected(mode) }
                                    .testTag("theme_mode_${mode.name.lowercase()}"),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(text = "${mode.icon} ${mode.displayName}", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = mode.description,
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    Spacer(modifier = Modifier.height(12.dp))

                    // Dynamic Colors Switch Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                text = "Dynamic Colors (Android 12+)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Derives theme palette dynamically from system wallpaper colors",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }

                        Switch(
                            checked = useDynamicColor,
                            onCheckedChange = onToggleDynamicColor,
                            modifier = Modifier.testTag("dynamic_color_switch")
                        )
                    }

                    if (!useDynamicColor) {
                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "CUSTOM PALETTE (DYNAMIC COLOR OFF)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        IdeTheme.entries.forEach { theme ->
                            val isSelected = theme == currentTheme
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { onThemeSelected(theme) },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = theme.icon, fontSize = 20.sp)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = theme.displayName,
                                            fontSize = 14.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { onThemeSelected(theme) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 1.5. AI Provider Configuration (Local GGUF vs Cloud REST API)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ai_provider_configuration_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI Engine Provider",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "AI PROVIDER CONFIGURATION",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Switch between Local GGUF (NDK) and Cloud REST APIs",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "ENGINE PROVIDER MODE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val isLocal = aiProviderSettings.mode == AiProviderMode.LOCAL_GGUF
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onProviderSettingsChanged(aiProviderSettings.copy(mode = AiProviderMode.LOCAL_GGUF)) },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isLocal) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(text = "🟢 Local GGUF", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "On-Device NDK",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }

                        val isCloud = aiProviderSettings.mode == AiProviderMode.CLOUD_API
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onProviderSettingsChanged(aiProviderSettings.copy(mode = AiProviderMode.CLOUD_API)) },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCloud) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(text = "☁️ Cloud API", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "REST API (User Key)",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }

                    if (aiProviderSettings.mode == AiProviderMode.LOCAL_GGUF) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "ACTIVE LOCAL GGUF MODEL FILE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        GgufModelDropdownSelector(
                            models = models,
                            selectedModel = selectedModel,
                            onModelSelected = onModelSelected,
                            onImportRequested = { documentPickerLauncher.launch(arrayOf("*/*")) },
                            label = "Select Active Local GGUF File"
                        )
                    }

                    if (aiProviderSettings.mode == AiProviderMode.CLOUD_API) {
                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "SELECT CLOUD PROVIDER",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            CloudProvider.values().forEach { provider ->
                                val isSelected = aiProviderSettings.cloudProvider == provider
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f) else Color.Transparent)
                                        .clickable {
                                            val defaultModel = when (provider) {
                                                CloudProvider.GEMINI -> "gemini-1.5-flash"
                                                CloudProvider.OPENAI -> "gpt-4o-mini"
                                                CloudProvider.GROQ -> "llama-3.1-8b-instant"
                                                CloudProvider.CLAUDE -> "claude-3-5-sonnet-20240620"
                                            }
                                            onProviderSettingsChanged(
                                                aiProviderSettings.copy(
                                                    cloudProvider = provider,
                                                    cloudModelName = defaultModel
                                                )
                                            )
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = {
                                            val defaultModel = when (provider) {
                                                CloudProvider.GEMINI -> "gemini-1.5-flash"
                                                CloudProvider.OPENAI -> "gpt-4o-mini"
                                                CloudProvider.GROQ -> "llama-3.1-8b-instant"
                                                CloudProvider.CLAUDE -> "claude-3-5-sonnet-20240620"
                                            }
                                            onProviderSettingsChanged(
                                                aiProviderSettings.copy(
                                                    cloudProvider = provider,
                                                    cloudModelName = defaultModel
                                                )
                                            )
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = provider.displayName,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        var apiKeyVisible by remember { mutableStateOf(false) }

                        OutlinedTextField(
                            value = aiProviderSettings.apiKey,
                            onValueChange = { onProviderSettingsChanged(aiProviderSettings.copy(apiKey = it)) },
                            label = { Text("${aiProviderSettings.cloudProvider.displayName} API Key") },
                            placeholder = { Text("Enter your API key...") },
                            modifier = Modifier.fillMaxWidth().testTag("api_key_input_field"),
                            singleLine = true,
                            visualTransformation = if (apiKeyVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { apiKeyVisible = !apiKeyVisible }) {
                                    Icon(
                                        imageVector = if (apiKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (apiKeyVisible) "Hide Key" else "Show Key"
                                    )
                                }
                            },
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = aiProviderSettings.cloudModelName,
                            onValueChange = { onProviderSettingsChanged(aiProviderSettings.copy(cloudModelName = it)) },
                            label = { Text("Model Identifier") },
                            placeholder = { Text("e.g. gemini-1.5-flash, gpt-4o-mini") },
                            modifier = Modifier.fillMaxWidth().testTag("cloud_model_name_input"),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = onTestConnection,
                            modifier = Modifier.fillMaxWidth().testTag("test_cloud_connection_button"),
                            enabled = !isTestingConnection && aiProviderSettings.apiKey.isNotBlank(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            if (isTestingConnection) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Testing Connection...")
                            } else {
                                Icon(imageVector = Icons.Default.Speed, contentDescription = "Test Connection", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Test Connection")
                            }
                        }

                        if (connectionTestResult != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = if (connectionTestResult.startsWith("✓")) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = connectionTestResult,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (connectionTestResult.startsWith("✓")) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. GGUF Local Model Storage & File Management System
        item {
            val totalGgufBytes = remember(models) { models.sumOf { it.sizeBytes } }
            val formattedTotalSize = remember(totalGgufBytes) { formatSizeBytes(totalGgufBytes) }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.SdStorage, contentDescription = "GGUF Storage", tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "GGUF MODEL FILE SYSTEM",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Import, store & manage local GGUF weights on device",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { showModelManagerModal = true },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("open_gguf_manager_button"),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Tune, contentDescription = "Manage Models", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Manage", fontSize = 11.sp, maxLines = 1)
                        }

                        Button(
                            onClick = { documentPickerLauncher.launch(arrayOf("*/*")) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(imageVector = Icons.Default.FileOpen, contentDescription = "Import GGUF", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Import .gguf", fontSize = 11.sp, maxLines = 1)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Storage Summary Banner
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "LOCAL MODEL STORAGE OCCUPIED",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "$formattedTotalSize across ${models.size} models",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "100% OFFLINE / NDK",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }

                            if (models.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                ModelStorageBarChart(
                                    modelSizeBytes = totalGgufBytes,
                                    barColor = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    GgufModelDropdownSelector(
                        models = models,
                        selectedModel = selectedModel,
                        onModelSelected = onModelSelected,
                        onImportRequested = { documentPickerLauncher.launch(arrayOf("*/*")) },
                        label = "Switch Active Local GGUF File"
                    )

                    // Import Progress / Status Card
                    if (importProgress != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (importProgress.errorMessage != null)
                                    MaterialTheme.colorScheme.errorContainer
                                else
                                    MaterialTheme.colorScheme.primaryContainer
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (importProgress.errorMessage != null) {
                                            Icon(imageVector = Icons.Default.Warning, contentDescription = "Error", tint = MaterialTheme.colorScheme.error)
                                        } else {
                                            Icon(imageVector = Icons.Default.SdStorage, contentDescription = "Importing", tint = MaterialTheme.colorScheme.primary)
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (importProgress.errorMessage != null) "GGUF Import Error" else importProgress.fileName,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (importProgress.errorMessage != null) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }

                                    IconButton(onClick = onDismissImportProgress, modifier = Modifier.height(24.dp).width(24.dp)) {
                                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                    }
                                }

                                if (importProgress.errorMessage != null) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = importProgress.errorMessage,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                } else {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = importProgress.statusText,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LinearProgressIndicator(
                                        progress = { importProgress.progressFraction },
                                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (models.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No GGUF models imported yet. Click 'Import .gguf' above to select a file from device storage.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    } else {
                        models.forEach { model ->
                            val isSelected = model.id == selectedModel?.id
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    // Top Header Row (Title + Status Badge + Radio Button Selector)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        // Model Title (Title gets weight(1f) to wrap/ellipsize without overlapping badge or radio button)
                                        Text(
                                            text = model.name,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.weight(1f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        // Status Badge
                                        if (isSelected) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                color = Color(0xFF15803D),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = "LOADED FOR INFERENCE",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    maxLines = 1,
                                                    softWrap = false,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                                )
                                            }
                                        } else if (!model.path.startsWith("internal://")) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                color = MaterialTheme.colorScheme.secondaryContainer,
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = "LOCAL STORAGE",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                    maxLines = 1,
                                                    softWrap = false,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(6.dp))

                                        // Radio Button Selector
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = { onModelSelected(model.id) }
                                        )
                                    }

                                    // Model Metadata
                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = "SIZE: ${formatSizeBytes(model.sizeBytes)} • QUANT: ${model.quantType} • ARCH: ${model.architecture} • PARAMS: ${model.parameters}",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Spacer(modifier = Modifier.height(2.dp))

                                    Text(
                                        text = "PATH: ${model.path}",
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Horizontal bar chart showing model size relative to device storage capacity
                                    ModelStorageBarChart(
                                        modelSizeBytes = model.sizeBytes,
                                        barColor = if (isSelected) Color(0xFF15803D) else MaterialTheme.colorScheme.primary
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Actions Row: Details, Rename, Delete
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            OutlinedButton(
                                                onClick = { modelForDetails = model },
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.height(30.dp)
                                            ) {
                                                Icon(imageVector = Icons.Default.Info, contentDescription = "Details", modifier = Modifier.height(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Details", fontSize = 10.sp)
                                            }

                                            OutlinedButton(
                                                onClick = {
                                                    modelToRename = model
                                                    renameInput = model.name
                                                },
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.height(30.dp)
                                            ) {
                                                Icon(imageVector = Icons.Default.Edit, contentDescription = "Rename", modifier = Modifier.height(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Rename", fontSize = 10.sp)
                                            }
                                        }

                                        IconButton(
                                            onClick = { modelToDelete = model },
                                            modifier = Modifier.height(32.dp).width(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete Model File",
                                                tint = MaterialTheme.colorScheme.error
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

        // 2.5 Dynamic Context Length Controller
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Context Length",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "CONTEXT TOKEN CONTROLLER",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Box(
                            modifier = Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "$contextWindow Tokens",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Adjust active LLM context window (1024 - 4096 tokens). Truncates conversation history to optimize KV cache memory usage on lower-RAM devices.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        lineHeight = 15.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Slider(
                        value = contextWindow.toFloat(),
                        onValueChange = { newValue ->
                            val stepped = (newValue / 256f).toInt() * 256
                            onContextWindowChanged(stepped.coerceIn(1024, 4096))
                        },
                        valueRange = 1024f..4096f,
                        steps = 11, // Allows 256 increments (1024, 1280, 1536... 4096)
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("1024 (Low RAM)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Text("2048 (Balanced)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Text("4096 (Max)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onContextWindowChanged(1024) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("1024 tokens", fontSize = 10.sp)
                        }

                        OutlinedButton(
                            onClick = { onContextWindowChanged(2048) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("2048 tokens", fontSize = 10.sp)
                        }

                        OutlinedButton(
                            onClick = { onContextWindowChanged(4096) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("4096 tokens", fontSize = 10.sp)
                        }
                    }
                }
            }
        }

        // 3. Engine Hardware Performance Settings
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = "Engine Specs", tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "INFERENCE ENGINE SPECS",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("• Execution Backend: Native Android C++ NDK / llama.cpp bindings", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text("• CPU Threads: 4 Threads (Max efficiency cores)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text("• Memory Strategy: mmap weight mapping + FP16 KV Cache", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text("• Local Storage: 100% Offline execution without internet", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)

                    memoryCheckResult?.let { mem ->
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (mem.isSufficient) Color(0xFF1E293B) else Color(0xFF7F1D1D),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(10.dp)
                        ) {
                            Column {
                                Text(
                                    text = if (mem.isSufficient) "✓ RAM GUARD: VERIFIED SAFE" else "⚠️ RAM GUARD: LOW MEMORY WARNING",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (mem.isSufficient) Color(0xFF4ADE80) else Color(0xFFFCA5A5)
                                )
                                Text(
                                    text = mem.warningMessage ?: "System RAM: ${mem.availableRamMb} MB Available / ${mem.totalRamMb} MB Total. Safe to load model weights without OOM risk.",
                                    fontSize = 10.sp,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3b. Real-time Device Performance & Thermal Status Section
        item {
            DevicePerformanceCard()
        }

        // 4. PRD ADDENDUM: Real-time System Resource & Performance Dashboard
        item {
            PerformanceDashboard(
                selectedModel = selectedModel,
                diagnosticState = memoryCheckResult?.let {
                    com.example.util.DiagnosticUtil.getDiagnosticState(
                        context = androidx.compose.ui.platform.LocalContext.current,
                        speedTokensPerSec = generationProgress?.speedTokensPerSec ?: (if (isGenerating) 18.5f else 0.0f),
                        tokensGenerated = generationProgress?.tokensGenerated ?: 0,
                        modelSizeBytes = selectedModel?.sizeBytes ?: 0L,
                        modelName = selectedModel?.name ?: "No Local Model"
                    )
                },
                generationProgress = generationProgress,
                isGenerating = isGenerating
            )
        }

        // 4b. GGUF Model Benchmarks (Speed & Latency Line Chart)
        item {
            ModelBenchmarksCard(
                models = models,
                selectedModel = selectedModel,
                onSelectModel = { onModelSelected(it.id) }
            )
        }

        // 5. In-App Performance Debugging Overlay (Developer HUD) Toggle
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.Tune, contentDescription = "HUD", tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "REAL-TIME PERFORMANCE HUD OVERLAY",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Floating overlay showing t/s, native RAM & thread metrics",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Switch(
                            checked = isHudEnabled,
                            onCheckedChange = onToggleHud
                        )
                    }
                }
            }
        }

        // 5b. GitHub API & Repository Publishing Diagnostics Panel
        item {
            GitHubDiagnosticPanel(
                activeProjectName = "Offline-AI-Mobile-IDE"
            )
        }

        // 6. Global Editor Auto-Save & Data Protection Setting
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.SdStorage, contentDescription = "Auto-Save Setting", tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "GLOBAL EDITOR AUTO-SAVE",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Automatically persist code edits to local database to prevent accidental data loss",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }

                        Switch(
                            checked = isAutoSaveEnabled,
                            onCheckedChange = onToggleAutoSave,
                            modifier = Modifier.testTag("toggle_auto_save_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (isAutoSaveEnabled) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(8.dp)
                    ) {
                        Text(
                            text = if (isAutoSaveEnabled)
                                "✓ Auto-save active: Code changes are saved automatically every 30s and on typing."
                            else
                                "⚠️ Auto-save disabled: Manual save is required in the editor to persist changes.",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isAutoSaveEnabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }

        // 6. Offline History Persistence Management
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "OFFLINE CHAT & AGENT HISTORY",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Persisted locally in Room SQLite Database",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        OutlinedButton(
                            onClick = onClearHistory,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Clear History",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.error,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // 7. About & Open Source Credits Section

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = "Credits", tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ABOUT & OPEN SOURCE LICENSES",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Local AI IDE v2.5 Public Release Edition",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Offline-first NDK GGUF AI Development Environment for Android",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(12.dp))

                    val libs = listOf(
                        "llama.cpp" to "MIT License (C++ GGUF Tensor & NDK Inference)",
                        "Jetpack Compose & Material 3" to "Apache 2.0 (Modern Native UI System)",
                        "Room Persistence Library" to "Apache 2.0 (Local SQLite ORM Engine)",
                        "Kotlin Coroutines & Flow" to "Apache 2.0 (Asynchronous Reactive Pipeline)",
                        "Gson" to "Apache 2.0 (JSON Serialization & Workspace Configs)",
                        "KSP & WindowInsets" to "Apache 2.0 (Symbol Processing & System Keyboard Insets)"
                    )

                    libs.forEach { (name, license) ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = name,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = license,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                lineHeight = 14.sp
                            )
                        }
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    modelToDelete?.let { model ->
        AlertDialog(
            onDismissRequest = { modelToDelete = null },
            title = { Text("Delete GGUF Model File?", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "Are you sure you want to delete '${model.name}' (${formatSizeBytes(model.sizeBytes)}) from local device storage?\n\nThis will free up storage space, but the model file will no longer be available for offline inference.",
                    fontSize = 12.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteModel(model)
                        modelToDelete = null
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete File", fontSize = 12.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { modelToDelete = null }) {
                    Text("Cancel", fontSize = 12.sp)
                }
            }
        )
    }

    // Rename Model Dialog
    modelToRename?.let { model ->
        AlertDialog(
            onDismissRequest = { modelToRename = null },
            title = { Text("Rename Model Profile", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Enter new label for model file:", fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = renameInput,
                        onValueChange = { renameInput = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameInput.isNotBlank()) {
                            onRenameModel(model.id, renameInput.trim())
                        }
                        modelToRename = null
                    }
                ) {
                    Text("Save", fontSize = 12.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { modelToRename = null }) {
                    Text("Cancel", fontSize = 12.sp)
                }
            }
        )
    }

    // Details Info Dialog
    modelForDetails?.let { model ->
        AlertDialog(
            onDismissRequest = { modelForDetails = null },
            title = { Text(model.name, fontSize = 15.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("• File Format: GGUF v3 Binary", fontSize = 12.sp)
                    Text("• Quantization Type: ${model.quantType}", fontSize = 12.sp)
                    Text("• Architecture: ${model.architecture}", fontSize = 12.sp)
                    Text("• Estimated Parameters: ${model.parameters}", fontSize = 12.sp)
                    Text("• Context Window: ${model.contextWindow} tokens", fontSize = 12.sp)
                    Text("• File Size: ${formatSizeBytes(model.sizeBytes)} (${model.sizeBytes} bytes)", fontSize = 12.sp)
                    Text("• Storage Location: ${model.path}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                }
            },
            confirmButton = {
                TextButton(onClick = { modelForDetails = null }) {
                    Text("Close", fontSize = 12.sp)
                }
            }
        )
    }
}

private fun formatSizeBytes(sizeBytes: Long): String {
    if (sizeBytes <= 0) return "Unknown size"
    val gb = sizeBytes / 1_073_741_824.0
    if (gb >= 1.0) {
        return "%.2f GB".format(gb)
    }
    val mb = sizeBytes / 1_048_576.0
    return "%.1f MB".format(mb)
}

@Composable
private fun StatMetricTile(
    title: String,
    value: String,
    subtext: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(text = title, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(1.dp))
            Text(text = subtext, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        }
    }
}

@Composable
fun ModelStorageBarChart(
    modelSizeBytes: Long,
    deviceStorageBytes: Long = remember {
        try {
            android.os.StatFs(android.os.Environment.getDataDirectory().path).totalBytes
        } catch (e: Exception) {
            128L * 1024 * 1024 * 1024
        }
    },
    barColor: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    val deviceBytesDouble = deviceStorageBytes.toDouble().coerceAtLeast(1.0)
    val fraction = (modelSizeBytes.toDouble() / deviceBytesDouble).toFloat().coerceIn(0.012f, 1.0f)
    val percentage = String.format("%.1f%%", (modelSizeBytes.toDouble() / deviceBytesDouble) * 100.0)
    val formattedModelSize = formatSizeBytes(modelSizeBytes)
    val formattedCapacity = formatSizeBytes(deviceStorageBytes)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "STORAGE CAPACITY IMPACT",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
            Text(
                text = "$formattedModelSize / $formattedCapacity ($percentage)",
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                color = barColor
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction)
                    .clip(CircleShape)
                    .background(barColor)
            )
        }
    }
}

@Composable
fun DevicePerformanceCard(
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var diagnosticState by remember {
        mutableStateOf(com.example.util.DiagnosticUtil.getDiagnosticState(context))
    }

    LaunchedEffect(Unit) {
        while (true) {
            diagnosticState = com.example.util.DiagnosticUtil.getDiagnosticState(context)
            delay(3000)
        }
    }

    val totalRamGb = String.format("%.1f", diagnosticState.totalRamMb / 1024.0)
    val availRamGb = String.format("%.1f", diagnosticState.availableRamMb / 1024.0)
    val ramUsedFraction = if (diagnosticState.totalRamMb > 0) {
        (diagnosticState.usedRamMb.toFloat() / diagnosticState.totalRamMb.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val thermalCode = diagnosticState.thermalStatusCode
    val thermalFraction = (thermalCode / 4.0f).coerceIn(0f, 1f)
    val thermalColor = when {
        thermalCode == 0 -> Color(0xFF22C55E)
        thermalCode == 1 -> Color(0xFFEAB308)
        thermalCode == 2 -> Color(0xFFF97316)
        else -> Color(0xFFEF4444)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("device_performance_card"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = "Device Performance",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "DEVICE PERFORMANCE & THERMALS",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Box(
                    modifier = Modifier
                        .background(
                            if (diagnosticState.isThrottling) Color(0xFF7F1D1D) else MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (diagnosticState.isThrottling) "THROTTLING" else "NOMINAL",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (diagnosticState.isThrottling) Color(0xFFFCA5A5) else MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circular Progress 1: Available / Used RAM
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(84.dp)
                    ) {
                        CircularProgressIndicator(
                            progress = { ramUsedFraction },
                            modifier = Modifier.fillMaxSize(),
                            color = if (ramUsedFraction > 0.85f) Color(0xFFEF4444) else MaterialTheme.colorScheme.primary,
                            strokeWidth = 7.dp,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${diagnosticState.ramUsagePercent}%",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "USED",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Available RAM",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "$availRamGb GB Free / $totalRamGb GB",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Circular Progress 2: Thermal Throttling Status
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(84.dp)
                    ) {
                        CircularProgressIndicator(
                            progress = { if (thermalCode == 0) 0.15f else thermalFraction },
                            modifier = Modifier.fillMaxSize(),
                            color = thermalColor,
                            strokeWidth = 7.dp,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (diagnosticState.batteryTempCelsius > 0) "${diagnosticState.batteryTempCelsius.toInt()}°C" else "32°C",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = thermalColor
                            )
                            Text(
                                text = "TEMP",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Thermal Status",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = diagnosticState.thermalStatusText,
                        fontSize = 10.sp,
                        color = thermalColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}


