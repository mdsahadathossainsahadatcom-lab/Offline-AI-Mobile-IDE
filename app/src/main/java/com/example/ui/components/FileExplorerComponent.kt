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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.FileEntity

private fun getFileEmoji(path: String): String {
    return when {
        path.endsWith(".html", ignoreCase = true) || path.endsWith(".htm", ignoreCase = true) -> "🌐"
        path.endsWith(".css", ignoreCase = true) -> "🎨"
        path.endsWith(".js", ignoreCase = true) || path.endsWith(".ts", ignoreCase = true) -> "⚡"
        path.endsWith(".py", ignoreCase = true) -> "🐍"
        path.endsWith(".json", ignoreCase = true) -> "📦"
        path.endsWith(".md", ignoreCase = true) -> "📝"
        path.endsWith(".txt", ignoreCase = true) -> "📄"
        else -> "📄"
    }
}

private data class CodeSnippetMatch(
    val lineNumber: Int,
    val lineText: String
)

private data class GlobalSearchResult(
    val file: FileEntity,
    val isPathMatch: Boolean,
    val codeMatches: List<CodeSnippetMatch>
)

@Composable
fun FileExplorerComponent(
    files: List<FileEntity>,
    activeTabPath: String,
    onSelectFile: (String) -> Unit,
    onCreateFile: (String, String?) -> Unit,
    onDeleteFile: (String) -> Unit,
    modifier: Modifier = Modifier,
    isCompact: Boolean = false,
    branches: List<com.example.ui.viewmodel.GitBranch> = emptyList(),
    currentBranchName: String = "main",
    onCreateBranch: (String, String) -> Boolean = { _, _ -> true },
    onSwitchBranch: (String) -> Unit = {},
    onDeleteBranch: (String) -> Boolean = { true },
    onGitClone: (String, String) -> Unit = { _, _ -> },
    onGitPull: () -> Unit = {},
    onGitPush: (String) -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var searchScope by remember { mutableStateOf("All") } // "All", "Files", "Code"
    var isCaseSensitive by remember { mutableStateOf(false) }

    var isNewFileDialogOpen by remember { mutableStateOf(false) }
    var newFileName by remember { mutableStateOf("") }
    var selectedFileExtensionPreset by remember { mutableStateOf(".html") }
    var fileToDelete by remember { mutableStateOf<String?>(null) }

    var expandedFolders by remember { mutableStateOf(setOf("Markup & Layout", "Styles & Theme", "Scripts & Logic", "Config & Assets")) }

    val globalSearchResults = remember(files, searchQuery, searchScope, isCaseSensitive) {
        if (searchQuery.isBlank()) emptyList()
        else {
            val query = searchQuery.trim()
            files.mapNotNull { file ->
                val isPathMatch = file.path.contains(query, ignoreCase = !isCaseSensitive)
                val codeMatches = if (searchScope != "Files") {
                    file.content.lines().mapIndexedNotNull { index, line ->
                        if (line.contains(query, ignoreCase = !isCaseSensitive)) {
                            CodeSnippetMatch(lineNumber = index + 1, lineText = line.trim())
                        } else null
                    }
                } else emptyList()

                val matchesScope = when (searchScope) {
                    "Files" -> isPathMatch
                    "Code" -> codeMatches.isNotEmpty()
                    else -> isPathMatch || codeMatches.isNotEmpty()
                }

                if (matchesScope) {
                    GlobalSearchResult(
                        file = file,
                        isPathMatch = isPathMatch,
                        codeMatches = codeMatches
                    )
                } else null
            }
        }
    }

    // Group files into logical categories for standard view
    val fileCategories = remember(files) {
        mapOf(
            "Markup & Layout" to files.filter { it.path.endsWith(".html") || it.path.endsWith(".htm") },
            "Styles & Theme" to files.filter { it.path.endsWith(".css") || it.path.endsWith(".scss") },
            "Scripts & Logic" to files.filter { it.path.endsWith(".js") || it.path.endsWith(".ts") || it.path.endsWith(".jsx") },
            "Config & Assets" to files.filter { !it.path.endsWith(".html") && !it.path.endsWith(".htm") && !it.path.endsWith(".css") && !it.path.endsWith(".js") && !it.path.endsWith(".ts") }
        ).filterValues { it.isNotEmpty() }
    }

    if (isNewFileDialogOpen) {
        AlertDialog(
            onDismissRequest = { isNewFileDialogOpen = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.InsertDriveFile, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Create New File", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Enter filename and select preset language extension:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    OutlinedTextField(
                        value = newFileName,
                        onValueChange = { newFileName = it },
                        label = { Text("Filename (e.g. app.js, main.css)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_file_name_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(".html", ".css", ".js", ".json").forEach { ext ->
                            val isSel = selectedFileExtensionPreset == ext
                            Surface(
                                onClick = {
                                    selectedFileExtensionPreset = ext
                                    if (newFileName.isNotBlank() && !newFileName.contains(".")) {
                                        newFileName = "$newFileName$ext"
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                    Text(
                                        text = ext,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSel) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        var nameToUse = newFileName.trim()
                        if (nameToUse.isNotBlank()) {
                            if (!nameToUse.contains(".")) {
                                nameToUse += selectedFileExtensionPreset
                            }
                            onCreateFile(nameToUse, null)
                            onSelectFile(nameToUse)
                            newFileName = ""
                            isNewFileDialogOpen = false
                        }
                    },
                    modifier = Modifier.testTag("confirm_create_file_button")
                ) {
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

    fileToDelete?.let { targetPath ->
        AlertDialog(
            onDismissRequest = { fileToDelete = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete File", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text("Are you sure you want to delete '$targetPath'? This action cannot be undone.", fontSize = 12.sp)
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteFile(targetPath)
                        fileToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_delete_file_button")
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.onError)
                }
            },
            dismissButton = {
                TextButton(onClick = { fileToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(if (isCompact) 8.dp else 12.dp)
            .testTag("file_explorer_component")
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = "Explorer",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "PROJECT FILES",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "${files.size}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (branches.isNotEmpty()) {
                    GitBranchSelectorChip(
                        currentBranchName = currentBranchName,
                        branches = branches,
                        onCreateBranch = onCreateBranch,
                        onSwitchBranch = onSwitchBranch,
                        onDeleteBranch = onDeleteBranch,
                        onGitClone = onGitClone,
                        onGitPull = onGitPull,
                        onGitPush = onGitPush
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }

                IconButton(
                    onClick = { isNewFileDialogOpen = true },
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("create_new_file_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New File",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Search Filter TextField
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Filter files or code...", fontSize = 12.sp) },
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search", modifier = Modifier.size(18.dp)) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(20.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(14.dp))
                    }
                }
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .testTag("file_explorer_search_input")
        )

        Spacer(modifier = Modifier.height(6.dp))

        if (searchQuery.isNotBlank()) {
            // Search Scope Selector & Case Sensitive Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("All", "Code", "Files").forEach { scope ->
                        val isScopeSel = searchScope == scope
                        Surface(
                            onClick = { searchScope = scope },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isScopeSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.testTag("search_scope_$scope")
                        ) {
                            Text(
                                text = scope,
                                fontSize = 10.sp,
                                fontWeight = if (isScopeSel) FontWeight.Bold else FontWeight.Medium,
                                color = if (isScopeSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Surface(
                    onClick = { isCaseSensitive = !isCaseSensitive },
                    shape = RoundedCornerShape(8.dp),
                    color = if (isCaseSensitive) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = "Aa",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isCaseSensitive) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            // Search Stats Summary Bar
            val totalCodeMatches = globalSearchResults.sumOf { it.codeMatches.size }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SEARCH RESULTS (${globalSearchResults.size} files)",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                if (totalCodeMatches > 0) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "$totalCodeMatches snippets",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Global Search Results List
            if (globalSearchResults.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🔍 No matches found", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("Try searching for other terms or adjust scope", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(globalSearchResults, key = { it.file.path }) { result ->
                        val isSelected = result.file.path == activeTabPath

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectFile(result.file.path) }
                                .testTag("search_result_file_${result.file.path}"),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                // File Header Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = getFileEmoji(result.file.path), fontSize = 14.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = result.file.path,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    if (result.isPathMatch) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "Name match",
                                                fontSize = 9.sp,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                // Code Snippets Matches List
                                if (result.codeMatches.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        result.codeMatches.take(6).forEach { snippet ->
                                            Surface(
                                                onClick = { onSelectFile(result.file.path) },
                                                shape = RoundedCornerShape(4.dp),
                                                color = MaterialTheme.colorScheme.surface,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "L${snippet.lineNumber}",
                                                        fontSize = 10.sp,
                                                        fontFamily = FontFamily.Monospace,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.width(32.dp)
                                                    )
                                                    Text(
                                                        text = snippet.lineText,
                                                        fontSize = 10.sp,
                                                        fontFamily = FontFamily.Monospace,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                        }
                                        if (result.codeMatches.size > 6) {
                                            Text(
                                                text = "+ ${result.codeMatches.size - 6} more code matches",
                                                fontSize = 9.sp,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Medium,
                                                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Standard Quick Switcher Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("index.html", "style.css", "script.js").forEach { corePath ->
                    val isSel = activeTabPath == corePath
                    val exists = files.any { it.path == corePath }

                    Surface(
                        onClick = { if (exists) onSelectFile(corePath) },
                        color = if (isSel) MaterialTheme.colorScheme.primary else if (exists) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(6.dp),
                        enabled = exists,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = when (corePath) {
                                    "index.html" -> "🌐 HTML"
                                    "style.css" -> "🎨 CSS"
                                    else -> "⚡ JS"
                                },
                                fontSize = 10.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Categorized File List Tree View
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                fileCategories.forEach { (categoryName, categoryFiles) ->
                    val isExpanded = categoryName in expandedFolders

                    item(key = categoryName) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    expandedFolders = if (isExpanded) {
                                        expandedFolders - categoryName
                                    } else {
                                        expandedFolders + categoryName
                                    }
                                }
                                .padding(vertical = 4.dp, horizontal = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                                contentDescription = "Toggle",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = categoryName,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "(${categoryFiles.size})",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }

                    if (isExpanded) {
                        items(categoryFiles, key = { it.path }) { file ->
                            val isSelected = file.path == activeTabPath
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 12.dp)
                                    .clickable { onSelectFile(file.path) }
                                    .testTag("file_explorer_item_${file.path}"),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(text = getFileEmoji(file.path), fontSize = 14.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = file.path,
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1
                                            )
                                            Text(
                                                text = "${file.content.lines().size} lines • ${file.content.length} chars",
                                                fontSize = 9.sp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                                            )
                                        }
                                    }

                                    if (files.size > 1) {
                                        IconButton(
                                            onClick = { fileToDelete = file.path },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete",
                                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                                                modifier = Modifier.size(14.dp)
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
}
