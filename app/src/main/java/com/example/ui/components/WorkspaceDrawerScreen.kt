package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.IntegrationInstructions
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Workspaces
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.FileEntity
import com.example.data.db.ProjectEntity
import com.example.util.BoilerplateTemplate
import com.example.util.BoilerplateTemplates
import java.io.File

@Composable
fun WorkspaceDrawerScreen(
    activeProject: ProjectEntity?,
    allProjects: List<ProjectEntity>,
    files: List<FileEntity>,
    activeTabPath: String,
    onSelectProject: (ProjectEntity) -> Unit,
    onSelectFile: (String) -> Unit,
    onCreateFile: (String, String?) -> Unit,
    onDeleteFile: (String) -> Unit,
    onCreateProject: (String, String, String) -> Unit,
    onInsertTemplate: (String) -> Unit,
    onReplaceWithTemplate: (String) -> Unit,
    onExportZip: () -> Unit = {},
    onImportZip: () -> Unit = {}
) {
    var selectedDrawerTab by remember { mutableStateOf(0) } // 0 = Files & Workspaces, 1 = Boilerplate Library
    var selectedLanguageFilter by remember { mutableStateOf("ALL") } // ALL, HTML, CSS, JS
    var expandedTemplateId by remember { mutableStateOf<String?>(null) }

    var isNewFileDialogOpen by remember { mutableStateOf(false) }
    var newFileName by remember { mutableStateOf("") }

    var isNewProjectDialogOpen by remember { mutableStateOf(false) }
    var newProjectTitle by remember { mutableStateOf("") }
    var newProjectDesc by remember { mutableStateOf("") }
    var selectedTemplate by remember { mutableStateOf("Calculator") }

    val context = LocalContext.current
    val workspaceDir = remember(activeProject?.id) {
        activeProject?.let { File(context.filesDir, "workspace_${it.id}").apply { if (!exists()) mkdirs() } }
    }
    val diskFiles = remember(files, activeProject?.id) {
        workspaceDir?.listFiles()?.filter { it.isFile } ?: emptyList()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Top Tab Switcher: Workspaces vs Boilerplate Library
        TabRow(
            selectedTabIndex = selectedDrawerTab,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
        ) {
            Tab(
                selected = selectedDrawerTab == 0,
                onClick = { selectedDrawerTab = 0 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Folder, contentDescription = "Files", modifier = Modifier.padding(end = 4.dp))
                        Text("FILES & WORKSPACE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            )
            Tab(
                selected = selectedDrawerTab == 1,
                onClick = { selectedDrawerTab = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.IntegrationInstructions, contentDescription = "Templates", modifier = Modifier.padding(end = 4.dp))
                        Text("BOILERPLATE LIBRARY", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (selectedDrawerTab == 0) {
            // SECTION 0: FILES & WORKSPACES
            // 1. Current Workspace Header Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
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
                                imageVector = Icons.Default.Workspaces,
                                contentDescription = "Workspace",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = activeProject?.title ?: "Workspace",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Button(
                            onClick = { isNewProjectDialogOpen = true },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "New Project")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New Project", fontSize = 12.sp)
                        }
                    }

                    Text(
                        text = activeProject?.description ?: "Offline Local AI Workspace",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onExportZip,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("📦 Export .Zip", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = onImportZip,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("📥 Import .Zip", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Project Switcher Section
            Text(
                text = "PROJECT WORKSPACES",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                allProjects.forEach { proj ->
                    val isCurrent = proj.id == activeProject?.id
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onSelectProject(proj) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCurrent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = proj.title,
                            fontSize = 12.sp,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(12.dp),
                            maxLines = 1
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(12.dp))

            // 3. File Explorer Section
            FileExplorerComponent(
                files = files,
                activeTabPath = activeTabPath,
                onSelectFile = onSelectFile,
                onCreateFile = onCreateFile,
                onDeleteFile = onDeleteFile,
                modifier = Modifier.weight(1f)
            )
        } else {
            // SECTION 1: BOILERPLATE TEMPLATES LIBRARY
            Column(modifier = Modifier.fillMaxSize()) {
                // Header card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "⚡ COMMON HTML/CSS/JS BOILERPLATES",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Instantly insert battle-tested code snippets or replace active file content in $activeTabPath.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Filter Language Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("ALL", "HTML", "CSS", "JS").forEach { lang ->
                        val isSelected = selectedLanguageFilter == lang
                        Button(
                            onClick = { selectedLanguageFilter = lang },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = when(lang) {
                                    "HTML" -> "HTML 🌐"
                                    "CSS" -> "CSS 🎨"
                                    "JS" -> "JS ⚡"
                                    else -> "ALL 📦"
                                },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                val filteredTemplates = BoilerplateTemplates.allTemplates.filter { tmpl ->
                    selectedLanguageFilter == "ALL" || tmpl.language.equals(selectedLanguageFilter, ignoreCase = true)
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredTemplates) { template ->
                        val isExpanded = expandedTemplateId == template.id

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                // Title & Category Tag
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = when (template.language) {
                                                "html" -> "🌐"
                                                "css" -> "🎨"
                                                else -> "⚡"
                                            },
                                            fontSize = 16.sp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = template.title,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .background(
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(6.dp)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = template.category.uppercase(),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = template.description,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Code Preview Toggle Button
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(onClick = {
                                        expandedTemplateId = if (isExpanded) null else template.id
                                    }) {
                                        Icon(imageVector = Icons.Default.Code, contentDescription = "Code", modifier = Modifier.padding(end = 4.dp))
                                        Text(if (isExpanded) "Hide Preview" else "Preview Code", fontSize = 11.sp)
                                    }

                                    Text(
                                        text = "Target: ${template.suggestedFileName}",
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }

                                // Expanded Code Box
                                AnimatedVisibility(visible = isExpanded) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp)
                                            .background(Color(0xFF0F172A), shape = RoundedCornerShape(8.dp))
                                            .border(1.dp, Color(0xFF334155), shape = RoundedCornerShape(8.dp))
                                            .padding(10.dp)
                                    ) {
                                        Text(
                                            text = template.code,
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = Color(0xFFE2E8F0)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Quick Insertion Actions
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Button(
                                        onClick = { onInsertTemplate(template.code) },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Add, contentDescription = "Insert", modifier = Modifier.padding(end = 2.dp))
                                        Text("+ Insert", fontSize = 10.sp)
                                    }

                                    OutlinedButton(
                                        onClick = { onReplaceWithTemplate(template.code) },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Replace", fontSize = 10.sp)
                                    }

                                    OutlinedButton(
                                        onClick = { onCreateFile(template.suggestedFileName, template.code) },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("New File", fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // New File Dialog
    if (isNewFileDialogOpen) {
        AlertDialog(
            onDismissRequest = { isNewFileDialogOpen = false },
            title = { Text("Create New File / Sub-folder") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Supports nested folders e.g. 'css/style.css', 'js/script.js', 'main.py', 'data.json', 'README.md'",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    OutlinedTextField(
                        value = newFileName,
                        onValueChange = { newFileName = it },
                        placeholder = { Text("e.g. css/style.css, main.py") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (newFileName.isNotBlank()) {
                        onCreateFile(newFileName, null)
                        newFileName = ""
                        isNewFileDialogOpen = false
                    }
                }) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { isNewFileDialogOpen = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // New Project Dialog
    if (isNewProjectDialogOpen) {
        AlertDialog(
            onDismissRequest = { isNewProjectDialogOpen = false },
            title = { Text("Create Project Workspace") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newProjectTitle,
                        onValueChange = { newProjectTitle = it },
                        label = { Text("Project Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newProjectDesc,
                        onValueChange = { newProjectDesc = it },
                        label = { Text("Description") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Select Starter Template:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("Calculator", "Game", "Weather", "Todo").forEach { tmpl ->
                            Button(
                                onClick = { selectedTemplate = tmpl },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(tmpl, fontSize = 10.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (newProjectTitle.isNotBlank()) {
                        onCreateProject(newProjectTitle, newProjectDesc, selectedTemplate)
                        newProjectTitle = ""
                        newProjectDesc = ""
                        isNewProjectDialogOpen = false
                    }
                }) {
                    Text("Create Workspace")
                }
            },
            dismissButton = {
                TextButton(onClick = { isNewProjectDialogOpen = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
