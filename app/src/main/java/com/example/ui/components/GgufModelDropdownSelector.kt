package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.ModelProfileEntity

fun formatGgufSizeBytes(sizeBytes: Long): String {
    if (sizeBytes <= 0) return "Unknown size"
    val gb = sizeBytes / 1_073_741_824.0
    if (gb >= 1.0) {
        return "%.2f GB".format(gb)
    }
    val mb = sizeBytes / 1_048_576.0
    return "%.1f MB".format(mb)
}

/**
 * Dropdown component for selecting and switching between different local GGUF model files
 * stored in the device's internal storage.
 */
@Composable
fun GgufModelDropdownSelector(
    models: List<ModelProfileEntity>,
    selectedModel: ModelProfileEntity?,
    onModelSelected: (Long) -> Unit,
    modifier: Modifier = Modifier,
    onImportRequested: (() -> Unit)? = null,
    label: String = "Active GGUF Model File",
    showDetailsSupportingText: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = selectedModel?.name ?: if (models.isEmpty()) "Tap to import .gguf file" else "Select GGUF Model",
            onValueChange = {},
            readOnly = true,
            label = { Text(label, fontSize = 11.sp) },
            supportingText = if (showDetailsSupportingText) {
                {
                    selectedModel?.let {
                        Text(
                            text = "Size: ${formatGgufSizeBytes(it.sizeBytes)} • Quant: ${it.quantType} • Arch: ${it.architecture}",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } ?: if (models.isEmpty()) {
                        Text(
                            text = "No models available. Click to select a .gguf file from device storage.",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    } else null
                }
            } else null,
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onImportRequested != null) {
                        IconButton(onClick = { onImportRequested() }) {
                            Icon(
                                imageVector = Icons.Default.FileOpen,
                                contentDescription = "Import GGUF File",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    IconButton(onClick = {
                        if (models.isEmpty() && onImportRequested != null) {
                            onImportRequested()
                        } else {
                            expanded = !expanded
                        }
                    }) {
                        Icon(
                            imageVector = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                            contentDescription = "Select GGUF Model Dropdown"
                        )
                    }
                }
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.SdStorage,
                    contentDescription = "GGUF Model Storage",
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            enabled = true,
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    if (models.isEmpty() && onImportRequested != null) {
                        onImportRequested()
                    } else {
                        expanded = !expanded
                    }
                }
                .testTag("gguf_model_dropdown_field"),
            shape = RoundedCornerShape(12.dp)
        )

        DropdownMenu(
            expanded = expanded && (models.isNotEmpty() || onImportRequested != null),
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            Text(
                text = "SELECT LOCAL GGUF MODEL FILE",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            models.forEach { model ->
                val isSelected = model.id == selectedModel?.id
                DropdownMenuItem(
                    text = {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = model.name,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.sp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                                if (isSelected) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .background(Color(0xFF15803D), shape = RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 1.dp)
                                    ) {
                                        Text("ACTIVE", fontSize = 8.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            Text(
                                text = "Size: ${formatGgufSizeBytes(model.sizeBytes)} • Quant: ${model.quantType} • Arch: ${model.architecture}",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    leadingIcon = {
                        RadioButton(
                            selected = isSelected,
                            onClick = null
                        )
                    },
                    onClick = {
                        onModelSelected(model.id)
                        expanded = false
                    },
                    modifier = Modifier.testTag("gguf_dropdown_item_${model.id}")
                )
            }

            if (onImportRequested != null) {
                if (models.isNotEmpty()) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                }
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "➕ Import .gguf File from Device Storage...",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Import Local File",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    onClick = {
                        expanded = false
                        onImportRequested()
                    },
                    modifier = Modifier.testTag("gguf_dropdown_import_item")
                )
            }
        }
    }
}
