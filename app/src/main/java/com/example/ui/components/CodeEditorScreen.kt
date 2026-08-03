package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.filled.Save
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.focusable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material3.AlertDialog
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FindReplace
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.SyntaxAttribute
import com.example.ui.theme.SyntaxComment
import com.example.ui.theme.SyntaxKeyword
import com.example.ui.theme.SyntaxNumber
import com.example.ui.theme.SyntaxSelector
import com.example.ui.theme.SyntaxString
import com.example.ui.theme.SyntaxTag

@Composable
fun CodeEditorScreen(
    openTabs: List<String>,
    activeTabPath: String,
    codeContent: String,
    lastAutoSaveTime: String? = null,
    isFullScreen: Boolean = false,
    isAutoSaveEnabled: Boolean = true,
    terminalLogs: List<com.example.ui.viewmodel.TerminalLogEntry> = emptyList(),
    allProjectFiles: List<com.example.data.db.FileEntity> = emptyList(),
    onTabSelected: (String) -> Unit,
    onTabClosed: (String) -> Unit,
    onCodeChanged: (String) -> Unit,
    onRunPreview: () -> Unit,
    onSaveFile: () -> Unit = {},
    onToggleFullScreen: () -> Unit = {},
    onClearTerminal: () -> Unit = {},
    onSendTerminalCommand: (String) -> Unit = {},
    onCreateFile: (String, String?) -> Unit = { _, _ -> },
    onDeleteFile: (String) -> Unit = {},
    onExportZip: () -> Unit = {}
) {
    // File Explorer Side Panel State
    var isExplorerOpen by remember { mutableStateOf(false) }
    // Terminal Emulator View State
    var isTerminalOpen by remember { mutableStateOf(true) }
    var isTerminalExpanded by remember { mutableStateOf(false) }
    var selectedSourceFilter by remember { mutableStateOf(com.example.ui.viewmodel.TerminalSource.ALL) }
    var terminalInputText by remember { mutableStateOf("") }

    // Find & Replace Overlay State
    var searchQuery by remember { mutableStateOf("") }
    var replaceQuery by remember { mutableStateOf("") }
    var isSearchOpen by remember { mutableStateOf(false) }
    var isReplaceOpen by remember { mutableStateOf(false) }
    var matchCase by remember { mutableStateOf(false) }
    var isWholeWord by remember { mutableStateOf(false) }
    var useRegex by remember { mutableStateOf(false) }
    var currentMatchIndex by remember { mutableIntStateOf(0) }
    var replaceStatusMessage by remember { mutableStateOf<String?>(null) }

    // Code Folding State & Regions
    var isFoldingBarOpen by remember { mutableStateOf(false) }
    var foldedStartLines by remember { mutableStateOf(setOf<Int>()) }
    var selectedLineIndex by remember { mutableIntStateOf(-1) }
    var showShortcutsDialog by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    val foldRegions = remember(codeContent, activeTabPath) {
        detectFoldRegions(codeContent, activeTabPath)
    }
    val foldStartMap = remember(foldRegions) {
        foldRegions.associateBy { it.startLine }
    }

    // Match Calculation
    val matchRanges = remember(codeContent, searchQuery, matchCase, isWholeWord, useRegex) {
        findMatchRanges(codeContent, searchQuery, matchCase, isWholeWord, useRegex)
    }

    LaunchedEffect(matchRanges.size) {
        if (matchRanges.isNotEmpty()) {
            if (currentMatchIndex >= matchRanges.size) {
                currentMatchIndex = 0
            }
        } else {
            currentMatchIndex = 0
        }
    }

    if (showShortcutsDialog) {
        AlertDialog(
            modifier = Modifier.testTag("keyboard_shortcuts_dialog"),
            onDismissRequest = { showShortcutsDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Keyboard,
                        contentDescription = "Shortcuts",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Keyboard Shortcuts",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val shortcuts = listOf(
                        "Ctrl / Cmd + S" to "Save Active File",
                        "Ctrl / Cmd + F" to "Toggle Find & Replace Bar",
                        "Ctrl / Cmd + H" to "Open Replace Mode",
                        "Ctrl / Cmd + P" to "Run App Preview",
                        "Ctrl / Cmd + Shift + F" to "Toggle Fullscreen Editor",
                        "Ctrl / Cmd + K" to "Toggle Code Folding Controls",
                        "Ctrl / Cmd + J" to "Toggle Terminal Emulator",
                        "Ctrl / Cmd + ?" to "Show Keyboard Shortcuts",
                        "Escape" to "Close Active Overlays & Dialogs"
                    )

                    shortcuts.forEach { (keys, desc) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = keys,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = desc,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showShortcutsDialog = false },
                    modifier = Modifier.testTag("close_keyboard_shortcuts_button")
                ) {
                    Text("Close")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .background(MaterialTheme.colorScheme.background)
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    val isCtrlOrMeta = event.isCtrlPressed || event.isMetaPressed
                    when {
                        isCtrlOrMeta && event.key == Key.S -> {
                            onSaveFile()
                            true
                        }
                        isCtrlOrMeta && event.key == Key.F -> {
                            if (event.isShiftPressed) {
                                onToggleFullScreen()
                            } else {
                                isSearchOpen = !isSearchOpen
                                replaceStatusMessage = null
                            }
                            true
                        }
                        isCtrlOrMeta && (event.key == Key.H || event.key == Key.R) -> {
                            isSearchOpen = true
                            isReplaceOpen = true
                            replaceStatusMessage = null
                            true
                        }
                        isCtrlOrMeta && event.key == Key.P -> {
                            onRunPreview()
                            true
                        }
                        isCtrlOrMeta && (event.key == Key.K) -> {
                            isFoldingBarOpen = !isFoldingBarOpen
                            true
                        }
                        isCtrlOrMeta && (event.key == Key.Slash) -> {
                            showShortcutsDialog = !showShortcutsDialog
                            true
                        }
                        isCtrlOrMeta && (event.key == Key.J || event.key == Key.Grave) -> {
                            isTerminalOpen = !isTerminalOpen
                            true
                        }
                        event.key == Key.Escape -> {
                            if (showShortcutsDialog) {
                                showShortcutsDialog = false
                                true
                            } else if (isSearchOpen) {
                                isSearchOpen = false
                                true
                            } else if (isFoldingBarOpen) {
                                isFoldingBarOpen = false
                                true
                            } else if (isTerminalOpen) {
                                isTerminalOpen = false
                                true
                            } else {
                                false
                            }
                        }
                        else -> false
                    }
                } else {
                    false
                }
            }
    ) {
        // 1. File Tabs Row
        ScrollableTabRow(
            selectedTabIndex = openTabs.indexOf(activeTabPath).coerceAtLeast(0),
            edgePadding = 4.dp,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            divider = {}
        ) {
            openTabs.forEach { path ->
                val isSelected = path == activeTabPath
                Tab(
                    selected = isSelected,
                    onClick = { onTabSelected(path) },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Text(
                                text = getFileIcon(path) + " " + path,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            if (openTabs.size > 1) {
                                IconButton(
                                    onClick = { onTabClosed(path) },
                                    modifier = Modifier.size(18.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close tab",
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }
                )
            }
        }

        // 2. Quick Action Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val snippets = when {
                    activeTabPath.endsWith(".py") -> listOf("def ", "return ", "if ", "else:", "for ", "import ", "print()", "class ", "try:", "except:")
                    activeTabPath.endsWith(".json") -> listOf("\"\":", "{}", "[]", "\"true\"", "\"false\"", "1,")
                    activeTabPath.endsWith(".md") -> listOf("# ", "## ", "```", "**bold**", "- ", "[link]()")
                    else -> listOf("<div", "</div>", "class=\"\"", "function()", "{", "}", "=>", ";", "const", "let", "<style>", "<script>")
                }

                snippets.forEach { snippet ->
                    AssistChip(
                        onClick = { onCodeChanged(codeContent + snippet) },
                        label = { Text(snippet, fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                        modifier = Modifier.height(28.dp),
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            labelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isAutoSaveEnabled) {
                    if (lastAutoSaveTime != null) {
                        Text(
                            text = "💾 $lastAutoSaveTime",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                            modifier = Modifier.padding(end = 2.dp)
                        )
                    }
                } else {
                    Button(
                        onClick = onSaveFile,
                        modifier = Modifier
                            .height(26.dp)
                            .testTag("manual_save_button"),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "Save File",
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Save", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                IconButton(
                    onClick = { isExplorerOpen = !isExplorerOpen },
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("toggle_file_explorer_sidebar_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = "Toggle File Explorer Sidebar",
                        tint = if (isExplorerOpen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = onSaveFile,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("manual_save_icon_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = "Save Active File",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = {
                        isSearchOpen = !isSearchOpen
                        replaceStatusMessage = null
                    },
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("toggle_find_replace_overlay_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Find & Replace",
                        tint = if (isSearchOpen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = { isFoldingBarOpen = !isFoldingBarOpen },
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("toggle_code_folding_bar_button")
                ) {
                    Icon(
                        imageVector = if (foldedStartLines.isNotEmpty()) Icons.Default.UnfoldLess else Icons.Default.UnfoldMore,
                        contentDescription = "Code Folding Controls",
                        tint = if (isFoldingBarOpen || foldedStartLines.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = { showShortcutsDialog = true },
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("show_keyboard_shortcuts_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Keyboard,
                        contentDescription = "Keyboard Shortcuts Cheat Sheet",
                        tint = if (showShortcutsDialog) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = { isTerminalOpen = !isTerminalOpen },
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("toggle_terminal_overlay_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = "Terminal Emulator View",
                        tint = if (isTerminalOpen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = onExportZip,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("export_project_zip_toolbar_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Archive,
                        contentDescription = "Export Project as Zip",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = onRunPreview,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Run Preview",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = onToggleFullScreen,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isFullScreen) Icons.Default.FitScreen else Icons.Default.CropFree,
                        contentDescription = if (isFullScreen) "Exit Fullscreen" else "Maximize Canvas",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // 3. Find and Replace Floating / Docked Overlay
        AnimatedVisibility(
            visible = isSearchOpen,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .testTag("find_replace_overlay_card"),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Row 1: Find Input + Navigation & Mode Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )

                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = {
                                searchQuery = it
                                replaceStatusMessage = null
                            },
                            placeholder = { Text("Find in file...", fontSize = 12.sp) },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("find_input_field"),
                            singleLine = true,
                            textStyle = TextStyle(fontSize = 12.sp, fontFamily = FontFamily.Monospace),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )

                        // Match Count Badge
                        Text(
                            text = if (searchQuery.isEmpty()) {
                                "0/0"
                            } else if (matchRanges.isEmpty()) {
                                "No matches"
                            } else {
                                "${currentMatchIndex + 1}/${matchRanges.size}"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = if (matchRanges.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Previous Match Button
                        IconButton(
                            onClick = {
                                if (matchRanges.isNotEmpty()) {
                                    currentMatchIndex = if (currentMatchIndex > 0) currentMatchIndex - 1 else matchRanges.size - 1
                                }
                            },
                            enabled = matchRanges.isNotEmpty(),
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("find_previous_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = "Previous match",
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Next Match Button
                        IconButton(
                            onClick = {
                                if (matchRanges.isNotEmpty()) {
                                    currentMatchIndex = (currentMatchIndex + 1) % matchRanges.size
                                }
                            },
                            enabled = matchRanges.isNotEmpty(),
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("find_next_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = "Next match",
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Toggle Replace Mode Row
                        IconButton(
                            onClick = { isReplaceOpen = !isReplaceOpen },
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("toggle_replace_row_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapVert,
                                contentDescription = "Toggle Replace",
                                tint = if (isReplaceOpen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Close Overlay
                        IconButton(
                            onClick = { isSearchOpen = false },
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("close_find_replace_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close overlay",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Row 2: Replace Input & Bulk Action Buttons
                    AnimatedVisibility(visible = isReplaceOpen) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FindReplace,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(20.dp)
                            )

                            OutlinedTextField(
                                value = replaceQuery,
                                onValueChange = {
                                    replaceQuery = it
                                    replaceStatusMessage = null
                                },
                                placeholder = { Text("Replace with...", fontSize = 12.sp) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .testTag("replace_input_field"),
                                singleLine = true,
                                textStyle = TextStyle(fontSize = 12.sp, fontFamily = FontFamily.Monospace),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.secondary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                )
                            )

                            // Replace Single Button
                            Button(
                                onClick = {
                                    if (matchRanges.isNotEmpty()) {
                                        val (updatedCode, nextIdx) = performReplaceSingle(
                                            code = codeContent,
                                            replaceText = replaceQuery,
                                            matchRanges = matchRanges,
                                            targetIndex = currentMatchIndex
                                        )
                                        onCodeChanged(updatedCode)
                                        currentMatchIndex = nextIdx
                                        replaceStatusMessage = "Replaced 1 match"
                                    }
                                },
                                enabled = matchRanges.isNotEmpty(),
                                modifier = Modifier
                                    .height(38.dp)
                                    .testTag("replace_single_button"),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Text("Replace", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            // Replace All Button
                            Button(
                                onClick = {
                                    if (searchQuery.isNotEmpty()) {
                                        val (updatedCode, count) = performReplaceAll(
                                            code = codeContent,
                                            findQuery = searchQuery,
                                            replaceText = replaceQuery,
                                            matchCase = matchCase,
                                            isWholeWord = isWholeWord,
                                            useRegex = useRegex
                                        )
                                        onCodeChanged(updatedCode)
                                        replaceStatusMessage = "Replaced all $count occurrence(s)"
                                    }
                                },
                                enabled = matchRanges.isNotEmpty(),
                                modifier = Modifier
                                    .height(38.dp)
                                    .testTag("replace_all_button"),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("Replace All", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Row 3: Filter Chips (Match Case, Whole Word, Regex) & Status Message
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilterChip(
                                selected = matchCase,
                                onClick = { matchCase = !matchCase },
                                label = { Text("Aa", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                modifier = Modifier
                                    .height(30.dp)
                                    .testTag("toggle_match_case"),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            )

                            FilterChip(
                                selected = isWholeWord,
                                onClick = { isWholeWord = !isWholeWord },
                                label = { Text("\\b", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                modifier = Modifier
                                    .height(30.dp)
                                    .testTag("toggle_whole_word"),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            )

                            FilterChip(
                                selected = useRegex,
                                onClick = { useRegex = !useRegex },
                                label = { Text(".*", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                modifier = Modifier
                                    .height(30.dp)
                                    .testTag("toggle_regex"),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        }

                        if (replaceStatusMessage != null) {
                            Text(
                                text = replaceStatusMessage!!,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        // 3b. Code Folding Control Bar
        AnimatedVisibility(
            visible = isFoldingBarOpen,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    .testTag("code_folding_bar_card"),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.UnfoldMore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "CODE FOLDING (${foldRegions.size} blocks found)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // Fold All Button
                        Button(
                            onClick = {
                                foldedStartLines = foldRegions.map { it.startLine }.toSet()
                            },
                            enabled = foldRegions.isNotEmpty(),
                            modifier = Modifier
                                .height(28.dp)
                                .testTag("fold_all_button"),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Fold All", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        // Unfold All Button
                        OutlinedButton(
                            onClick = {
                                foldedStartLines = emptySet()
                            },
                            enabled = foldedStartLines.isNotEmpty(),
                            modifier = Modifier
                                .height(28.dp)
                                .testTag("unfold_all_button"),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Text("Unfold All", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (foldedStartLines.isNotEmpty()) {
                            AssistChip(
                                onClick = { },
                                label = { Text("${foldedStartLines.size} folded", fontSize = 10.sp) },
                                modifier = Modifier.height(26.dp)
                            )
                        }

                        IconButton(
                            onClick = { isFoldingBarOpen = false },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Folding Bar",
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }

        // 4. Code Editor Area with Foldable Line Numbers Gutter & Search Highlights
        val lines = remember(codeContent) { codeContent.split("\n") }
        val verticalScrollState = rememberScrollState()
        val horizontalScrollState = rememberScrollState()

        val hiddenLineIndices = remember(foldedStartLines, foldRegions) {
            val hidden = mutableSetOf<Int>()
            for (region in foldRegions) {
                if (region.startLine in foldedStartLines) {
                    for (lineIdx in (region.startLine + 1)..region.endLine) {
                        hidden.add(lineIdx)
                    }
                }
            }
            hidden
        }

        val visibleLineIndices = remember(lines.size, hiddenLineIndices) {
            (0 until lines.size).filter { it !in hiddenLineIndices }
        }

        val displayedCode = remember(codeContent, foldedStartLines, foldRegions, visibleLineIndices) {
            if (foldedStartLines.isEmpty()) {
                codeContent
            } else {
                visibleLineIndices.joinToString("\n") { i ->
                    if (i in foldedStartLines && i in foldStartMap) {
                        val region = foldStartMap[i]!!
                        lines.getOrElse(i) { "" } + " " + region.label
                    } else {
                        lines.getOrElse(i) { "" }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            // Collapsible File Explorer Side Panel
            AnimatedVisibility(
                visible = isExplorerOpen && allProjectFiles.isNotEmpty(),
                enter = expandHorizontally() + fadeIn(),
                exit = shrinkHorizontally() + fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(220.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        .padding(end = 4.dp)
                ) {
                    FileExplorerComponent(
                        files = allProjectFiles,
                        activeTabPath = activeTabPath,
                        onSelectFile = { path ->
                            onTabSelected(path)
                        },
                        onCreateFile = onCreateFile,
                        onDeleteFile = onDeleteFile,
                        isCompact = true
                    )
                }
            }

            // Line Numbers & Folding Gutter Column
            val maxLineDigits = remember(lines.size) { lines.size.toString().length }
            val dynamicGutterWidth = remember(maxLineDigits) { (34 + (maxLineDigits * 8)).dp.coerceAtLeast(48.dp) }

            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(dynamicGutterWidth)
                    .background(MaterialTheme.colorScheme.surface)
                    .verticalScroll(verticalScrollState)
                    .padding(vertical = 6.dp)
                    .testTag("editor_gutter_column"),
                horizontalAlignment = Alignment.End
            ) {
                visibleLineIndices.forEach { i ->
                    val isSelected = i == selectedLineIndex
                    val isFolded = i in foldedStartLines

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(18.dp)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else Color.Transparent,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .clickable { selectedLineIndex = i }
                            .testTag("gutter_line_number_${i + 1}"),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (i in foldStartMap) {
                            IconButton(
                                onClick = {
                                    foldedStartLines = if (isFolded) {
                                        foldedStartLines - i
                                    } else {
                                        foldedStartLines + i
                                    }
                                },
                                modifier = Modifier
                                    .size(16.dp)
                                    .testTag("fold_toggle_line_${i + 1}")
                            ) {
                                Icon(
                                    imageVector = if (isFolded) Icons.Default.ChevronRight else Icons.Default.ExpandMore,
                                    contentDescription = if (isFolded) "Expand line ${i + 1}" else "Fold line ${i + 1}",
                                    tint = if (isFolded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.width(16.dp))
                        }

                        Text(
                            text = "${i + 1}",
                            fontSize = 11.sp,
                            fontWeight = if (isSelected || isFolded) FontWeight.Bold else FontWeight.Normal,
                            fontFamily = FontFamily.Monospace,
                            color = when {
                                isSelected -> MaterialTheme.colorScheme.primary
                                isFolded -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                            },
                            modifier = Modifier.padding(start = 2.dp, end = 4.dp)
                        )
                    }
                }
            }

            // Gutter Vertical Separator
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            )

            Spacer(modifier = Modifier.width(4.dp))

            // Code Text Input Field with Syntax & Search Visual Transformation
            var ghostTextPrediction by remember { mutableStateOf("") }

            LaunchedEffect(codeContent, activeTabPath) {
                if (codeContent.isNotBlank()) {
                    kotlinx.coroutines.delay(400)
                    val lastLine = codeContent.trimEnd().lines().lastOrNull()?.trim() ?: ""
                    ghostTextPrediction = when {
                        lastLine.startsWith("function") && !lastLine.contains("{") -> " main() {\n  console.log('AI engine active');\n}"
                        lastLine.startsWith("const ") && !lastLine.contains("=") -> " = document.querySelector('.app');"
                        lastLine.startsWith("<div") && !lastLine.contains(">") -> " class=\"container\">\n  <h2>Title</h2>\n</div>"
                        lastLine.startsWith("def ") && !lastLine.contains(":") -> " main():\n    print('Running script')"
                        lastLine.endsWith("{") -> "\n  padding: 16px;\n  margin: 0;\n}"
                        else -> ""
                    }
                } else {
                    ghostTextPrediction = ""
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f), shape = RoundedCornerShape(6.dp))
                    .padding(6.dp)
            ) {
                val syntaxTransformation = remember(activeTabPath, searchQuery, matchCase, isWholeWord, useRegex, currentMatchIndex, matchRanges) {
                    SyntaxHighlightTransformation(
                        path = activeTabPath,
                        searchQuery = searchQuery,
                        matchCase = matchCase,
                        isWholeWord = isWholeWord,
                        useRegex = useRegex,
                        currentMatchIndex = currentMatchIndex,
                        matchRanges = matchRanges
                    )
                }

                Column(modifier = Modifier.fillMaxSize()) {
                    BasicTextField(
                        value = displayedCode,
                        onValueChange = { newText ->
                            if (foldedStartLines.isNotEmpty()) {
                                foldedStartLines = emptySet()
                            }
                            onCodeChanged(newText)
                        },
                        textStyle = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        visualTransformation = syntaxTransformation,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(verticalScrollState)
                            .horizontalScroll(horizontalScrollState)
                            .testTag("code_editor_text_field")
                    )

                    val completionSuggestions = remember(codeContent, activeTabPath) {
                        getCompletionSuggestions(codeContent, activeTabPath)
                    }

                    if (completionSuggestions.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF1E293B), shape = RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .horizontalScroll(rememberScrollState())
                                .testTag("code_completion_suggestions_bar"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "💡 Hints:",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            completionSuggestions.forEach { suggestion ->
                                AssistChip(
                                    onClick = {
                                        val newCode = if (codeContent.endsWith(suggestion.prefixToReplace)) {
                                            codeContent.dropLast(suggestion.prefixToReplace.length) + suggestion.insertText
                                        } else {
                                            codeContent + suggestion.insertText
                                        }
                                        onCodeChanged(newCode)
                                    },
                                    label = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = suggestion.displayText,
                                                fontSize = 11.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = suggestion.category,
                                                fontSize = 9.sp,
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                            )
                                        }
                                    },
                                    modifier = Modifier
                                        .height(26.dp)
                                        .testTag("completion_chip_${suggestion.displayText}")
                                )
                            }
                        }
                    }

                    if (ghostTextPrediction.isNotBlank()) {

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0F172A), shape = RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "👻 Ghost Text: ${ghostTextPrediction.replace("\n", " ").take(32)}...",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF94A3B8)
                            )

                            Button(
                                onClick = {
                                    onCodeChanged(codeContent + ghostTextPrediction)
                                    ghostTextPrediction = ""
                                },
                                modifier = Modifier.height(24.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Tab / Accept", fontSize = 9.sp)
                            }
                        }
                    }
                }
            }
        }

        // 5. Terminal Emulator View at the Bottom of Editor
        AnimatedVisibility(
            visible = isTerminalOpen,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            val terminalHeight = if (isTerminalExpanded) 280.dp else 160.dp
            val filteredLogs = remember(terminalLogs, selectedSourceFilter) {
                if (selectedSourceFilter == com.example.ui.viewmodel.TerminalSource.ALL) {
                    terminalLogs
                } else {
                    terminalLogs.filter { it.source == selectedSourceFilter }
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, start = 4.dp, end = 4.dp, bottom = 2.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF090D16)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 8.dp, bottomEnd = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    // Terminal Header & Filters
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF22C55E))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "TERMINAL (STDOUT / STDERR)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White
                            )
                        }

                        // Filter Chips Row
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilterChip(
                                selected = selectedSourceFilter == com.example.ui.viewmodel.TerminalSource.ALL,
                                onClick = { selectedSourceFilter = com.example.ui.viewmodel.TerminalSource.ALL },
                                label = { Text("ALL", fontSize = 9.sp) },
                                modifier = Modifier
                                    .height(24.dp)
                                    .testTag("terminal_filter_all")
                            )
                            FilterChip(
                                selected = selectedSourceFilter == com.example.ui.viewmodel.TerminalSource.GGUF_ENGINE,
                                onClick = { selectedSourceFilter = com.example.ui.viewmodel.TerminalSource.GGUF_ENGINE },
                                label = { Text("GGUF Engine", fontSize = 9.sp) },
                                modifier = Modifier
                                    .height(24.dp)
                                    .testTag("terminal_filter_gguf")
                            )
                            FilterChip(
                                selected = selectedSourceFilter == com.example.ui.viewmodel.TerminalSource.WEB_PREVIEW,
                                onClick = { selectedSourceFilter = com.example.ui.viewmodel.TerminalSource.WEB_PREVIEW },
                                label = { Text("Web Preview", fontSize = 9.sp) },
                                modifier = Modifier
                                    .height(24.dp)
                                    .testTag("terminal_filter_preview")
                            )
                            FilterChip(
                                selected = selectedSourceFilter == com.example.ui.viewmodel.TerminalSource.SYSTEM,
                                onClick = { selectedSourceFilter = com.example.ui.viewmodel.TerminalSource.SYSTEM },
                                label = { Text("System", fontSize = 9.sp) },
                                modifier = Modifier
                                    .height(24.dp)
                                    .testTag("terminal_filter_system")
                            )

                            // Expand / Compress Height
                            IconButton(
                                onClick = { isTerminalExpanded = !isTerminalExpanded },
                                modifier = Modifier
                                    .size(24.dp)
                                    .testTag("toggle_terminal_height_button")
                            ) {
                                Icon(
                                    imageVector = if (isTerminalExpanded) Icons.Default.FitScreen else Icons.Default.CropFree,
                                    contentDescription = "Resize Terminal",
                                    tint = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.size(14.dp)
                                )
                            }

                            // Clear Terminal Button
                            IconButton(
                                onClick = onClearTerminal,
                                modifier = Modifier
                                    .size(24.dp)
                                    .testTag("clear_terminal_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Clear Terminal",
                                    tint = Color(0xFFF87171),
                                    modifier = Modifier.size(14.dp)
                                )
                            }

                            // Close Terminal
                            IconButton(
                                onClick = { isTerminalOpen = false },
                                modifier = Modifier
                                    .size(24.dp)
                                    .testTag("close_terminal_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close Terminal",
                                    tint = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

                    // Terminal Logs Console Area
                    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
                    LaunchedEffect(filteredLogs.size) {
                        if (filteredLogs.isNotEmpty()) {
                            listState.animateScrollToItem(filteredLogs.size - 1)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(terminalHeight)
                            .background(Color(0xFF030712), shape = RoundedCornerShape(6.dp))
                            .border(1.dp, Color(0xFF1E293B), shape = RoundedCornerShape(6.dp))
                            .padding(6.dp)
                    ) {
                        if (filteredLogs.isEmpty()) {
                            Text(
                                text = "No terminal logs captured for selected filter.",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF64748B),
                                modifier = Modifier.align(Alignment.Center)
                            )
                        } else {
                            androidx.compose.foundation.lazy.LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                items(filteredLogs.size, key = { filteredLogs[it].id }) { index ->
                                    val log = filteredLogs[index]
                                    val (sourceTag, sourceColor) = when (log.source) {
                                        com.example.ui.viewmodel.TerminalSource.GGUF_ENGINE -> "GGUF" to Color(0xFF38BDF8)
                                        com.example.ui.viewmodel.TerminalSource.WEB_PREVIEW -> "PREVIEW" to Color(0xFF4ADE80)
                                        com.example.ui.viewmodel.TerminalSource.SYSTEM -> "SYS" to Color(0xFFC084FC)
                                        else -> "LOG" to Color(0xFF94A3B8)
                                    }

                                    val (streamTag, streamColor) = when (log.stream) {
                                        com.example.ui.viewmodel.TerminalStream.STDOUT -> "STDOUT" to Color(0xFF22C55E)
                                        com.example.ui.viewmodel.TerminalStream.STDERR -> "STDERR" to Color(0xFFEF4444)
                                        com.example.ui.viewmodel.TerminalStream.WARN -> "WARN" to Color(0xFFEAB308)
                                        com.example.ui.viewmodel.TerminalStream.ERROR -> "ERROR" to Color(0xFFF87171)
                                        com.example.ui.viewmodel.TerminalStream.INFO -> "INFO" to Color(0xFF38BDF8)
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Text(
                                            text = "[${log.timestamp}]",
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = Color(0xFF64748B)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "[$sourceTag]",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            color = sourceColor
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "[$streamTag]",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            color = streamColor
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = log.message,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = if (log.stream == com.example.ui.viewmodel.TerminalStream.STDERR || log.stream == com.example.ui.viewmodel.TerminalStream.ERROR) Color(0xFFFCA5A5) else Color(0xFFE2E8F0)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // CLI Input Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF030712), shape = RoundedCornerShape(6.dp))
                            .border(1.dp, Color(0xFF334155), shape = RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "dev@local-ai:~$ ",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF22C55E)
                        )

                        BasicTextField(
                            value = terminalInputText,
                            onValueChange = { terminalInputText = it },
                            singleLine = true,
                            textStyle = TextStyle(
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("terminal_command_input_field"),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                imeAction = androidx.compose.ui.text.input.ImeAction.Send
                            ),
                            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                                onSend = {
                                    if (terminalInputText.isNotBlank()) {
                                        onSendTerminalCommand(terminalInputText)
                                        terminalInputText = ""
                                    }
                                }
                            )
                        )

                        IconButton(
                            onClick = {
                                if (terminalInputText.isNotBlank()) {
                                    onSendTerminalCommand(terminalInputText)
                                    terminalInputText = ""
                                }
                            },
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("send_terminal_command_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Execute Command",
                                tint = Color(0xFF22C55E),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// Search Range Calculator
fun findMatchRanges(
    text: String,
    query: String,
    matchCase: Boolean,
    isWholeWord: Boolean,
    useRegex: Boolean
): List<IntRange> {
    if (query.isEmpty() || text.isEmpty()) return emptyList()
    return try {
        val pattern = when {
            useRegex -> query
            isWholeWord -> "\\b${Regex.escape(query)}\\b"
            else -> Regex.escape(query)
        }
        val options = if (matchCase) emptySet() else setOf(RegexOption.IGNORE_CASE)
        val regex = Regex(pattern, options)
        regex.findAll(text).map { it.range }.toList()
    } catch (e: Exception) {
        emptyList()
    }
}

// Replace Operations
fun performReplaceSingle(
    code: String,
    replaceText: String,
    matchRanges: List<IntRange>,
    targetIndex: Int
): Pair<String, Int> {
    if (matchRanges.isEmpty() || targetIndex !in matchRanges.indices) return Pair(code, targetIndex)
    val range = matchRanges[targetIndex]
    val newCode = code.replaceRange(range.first, range.last + 1, replaceText)
    val nextIndex = if (matchRanges.size > 1) {
        if (targetIndex < matchRanges.size - 1) targetIndex else 0
    } else 0
    return Pair(newCode, nextIndex)
}

fun performReplaceAll(
    code: String,
    findQuery: String,
    replaceText: String,
    matchCase: Boolean,
    isWholeWord: Boolean,
    useRegex: Boolean
): Pair<String, Int> {
    val ranges = findMatchRanges(code, findQuery, matchCase, isWholeWord, useRegex)
    if (ranges.isEmpty()) return Pair(code, 0)

    val newCode = try {
        val pattern = when {
            useRegex -> findQuery
            isWholeWord -> "\\b${Regex.escape(findQuery)}\\b"
            else -> Regex.escape(findQuery)
        }
        val options = if (matchCase) emptySet() else setOf(RegexOption.IGNORE_CASE)
        val regex = Regex(pattern, options)
        regex.replace(code, replaceText)
    } catch (e: Exception) {
        code
    }
    return Pair(newCode, ranges.size)
}

private fun getFileIcon(path: String): String {
    return when {
        path.endsWith(".html") || path.endsWith(".htm") -> "🌐"
        path.endsWith(".css") -> "🎨"
        path.endsWith(".js") || path.endsWith(".ts") -> "⚡"
        path.endsWith(".py") -> "🐍"
        path.endsWith(".json") -> "📦"
        path.endsWith(".md") -> "📝"
        else -> "📄"
    }
}

private class StyleSpan(val start: Int, val end: Int, val style: SpanStyle)

class SyntaxHighlightTransformation(
    private val path: String,
    private val searchQuery: String = "",
    private val matchCase: Boolean = false,
    private val isWholeWord: Boolean = false,
    private val useRegex: Boolean = false,
    private val currentMatchIndex: Int = -1,
    private val matchRanges: List<IntRange> = emptyList()
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val highlighted = highlightSyntax(
            code = text.text,
            path = path,
            searchQuery = searchQuery,
            matchCase = matchCase,
            isWholeWord = isWholeWord,
            useRegex = useRegex,
            currentMatchIndex = currentMatchIndex,
            matchRanges = matchRanges
        )
        return TransformedText(
            text = highlighted,
            offsetMapping = OffsetMapping.Identity
        )
    }
}

fun highlightSyntax(
    code: String,
    path: String,
    searchQuery: String = "",
    matchCase: Boolean = false,
    isWholeWord: Boolean = false,
    useRegex: Boolean = false,
    currentMatchIndex: Int = -1,
    matchRanges: List<IntRange> = emptyList()
): AnnotatedString {
    if (code.isEmpty()) return AnnotatedString("")

    val spans = mutableListOf<StyleSpan>()

    val isHtml = path.endsWith(".html") || path.endsWith(".htm")
    val isCss = path.endsWith(".css")
    val isJs = path.endsWith(".js") || path.endsWith(".ts")
    val isPython = path.endsWith(".py")
    val isJson = path.endsWith(".json")
    val isMarkdown = path.endsWith(".md")

    when {
        isHtml -> highlightHtml(code, spans)
        isCss -> highlightCss(code, spans)
        isJs -> highlightJs(code, spans)
        isPython -> highlightPython(code, spans)
        isJson -> highlightJson(code, spans)
        isMarkdown -> highlightMarkdown(code, spans)
    }

    // Add search highlights on top of syntax highlighting
    if (searchQuery.isNotEmpty() && matchRanges.isNotEmpty()) {
        matchRanges.forEachIndexed { index, range ->
            val isCurrent = (index == currentMatchIndex)
            val bg = if (isCurrent) Color(0xFFF59E0B) else Color(0x80EAB308)
            val fg = if (isCurrent) Color.Black else Color.Unspecified
            spans.add(
                StyleSpan(
                    start = range.first,
                    end = (range.last + 1).coerceAtMost(code.length),
                    style = SpanStyle(background = bg, color = fg, fontWeight = FontWeight.Bold)
                )
            )
        }
    }

    return buildAnnotatedString {
        append(code)
        spans.forEach { span ->
            if (span.start >= 0 && span.end <= code.length && span.start < span.end) {
                addStyle(span.style, span.start, span.end)
            }
        }
    }
}

private fun highlightHtml(code: String, spans: MutableList<StyleSpan>) {
    Regex("<!--[\\s\\S]*?-->").findAll(code).forEach { m ->
        spans.add(StyleSpan(m.range.first, m.range.last + 1, SpanStyle(color = SyntaxComment, fontStyle = FontStyle.Italic)))
    }

    Regex("</?[a-zA-Z0-9-]+[^>]*>").findAll(code).forEach { match ->
        val tagStr = match.value
        val start = match.range.first

        Regex("</?([a-zA-Z0-9-]+)").find(tagStr)?.let { tagMatch ->
            val tagStart = start + tagMatch.range.first
            val tagEnd = start + tagMatch.range.last + 1
            spans.add(StyleSpan(tagStart, tagEnd, SpanStyle(color = SyntaxTag, fontWeight = FontWeight.Bold)))
        }

        Regex("([a-zA-Z0-9-]+)=").findAll(tagStr).forEach { attrMatch ->
            val attrStart = start + attrMatch.groups[1]!!.range.first
            val attrEnd = start + attrMatch.groups[1]!!.range.last + 1
            spans.add(StyleSpan(attrStart, attrEnd, SpanStyle(color = SyntaxAttribute)))
        }

        Regex("\"[^\"]*\"|'[^']*'").findAll(tagStr).forEach { valMatch ->
            val valStart = start + valMatch.range.first
            val valEnd = start + valMatch.range.last + 1
            spans.add(StyleSpan(valStart, valEnd, SpanStyle(color = SyntaxString)))
        }
    }
}

private fun highlightCss(code: String, spans: MutableList<StyleSpan>) {
    Regex("/\\*[\\s\\S]*?\\*/").findAll(code).forEach { m ->
        spans.add(StyleSpan(m.range.first, m.range.last + 1, SpanStyle(color = SyntaxComment, fontStyle = FontStyle.Italic)))
    }

    Regex("\"[^\"]*\"|'[^']*'").findAll(code).forEach { m ->
        spans.add(StyleSpan(m.range.first, m.range.last + 1, SpanStyle(color = SyntaxString)))
    }

    Regex("([.#]?[a-zA-Z0-9_-]+|\\*|:[a-zA-Z-]+)\\s*\\{").findAll(code).forEach { m ->
        m.groups[1]?.let { g ->
            spans.add(StyleSpan(g.range.first, g.range.last + 1, SpanStyle(color = SyntaxSelector, fontWeight = FontWeight.Bold)))
        }
    }

    Regex("([a-zA-Z-]+)\\s*:").findAll(code).forEach { m ->
        m.groups[1]?.let { g ->
            spans.add(StyleSpan(g.range.first, g.range.last + 1, SpanStyle(color = SyntaxAttribute)))
        }
    }

    Regex("\\b\\d+(\\.\\d+)?(px|rem|em|vh|vw|%|s|ms)?\\b").findAll(code).forEach { m ->
        spans.add(StyleSpan(m.range.first, m.range.last + 1, SpanStyle(color = SyntaxNumber)))
    }
}

private fun highlightJs(code: String, spans: MutableList<StyleSpan>) {
    Regex("//.*$|/\\*[\\s\\S]*?\\*/", RegexOption.MULTILINE).findAll(code).forEach { m ->
        spans.add(StyleSpan(m.range.first, m.range.last + 1, SpanStyle(color = SyntaxComment, fontStyle = FontStyle.Italic)))
    }

    Regex("\"[^\"]*\"|'[^']*'|`[^`]*`").findAll(code).forEach { m ->
        spans.add(StyleSpan(m.range.first, m.range.last + 1, SpanStyle(color = SyntaxString)))
    }

    val keywords = setOf("const", "let", "var", "function", "async", "await", "return", "if", "else", "for", "while", "class", "import", "export", "from", "new", "this", "try", "catch", "throw", "typeof", "instanceof", "of", "in", "switch", "case", "default", "break", "continue", "yield")
    Regex("\\b[a-zA-Z_][a-zA-Z0-9_]*\\b").findAll(code).forEach { m ->
        val word = m.value
        if (keywords.contains(word)) {
            spans.add(StyleSpan(m.range.first, m.range.last + 1, SpanStyle(color = SyntaxKeyword, fontWeight = FontWeight.Bold)))
        } else if (setOf("true", "false", "null", "undefined", "NaN").contains(word)) {
            spans.add(StyleSpan(m.range.first, m.range.last + 1, SpanStyle(color = SyntaxNumber, fontWeight = FontWeight.Bold)))
        } else if (setOf("console", "document", "window", "Math", "JSON", "Array", "Object", "Promise", "fetch", "alert", "showToast", "setTimeout", "setInterval").contains(word)) {
            spans.add(StyleSpan(m.range.first, m.range.last + 1, SpanStyle(color = SyntaxTag)))
        }
    }

    Regex("\\b\\d+(\\.\\d+)?\\b").findAll(code).forEach { m ->
        spans.add(StyleSpan(m.range.first, m.range.last + 1, SpanStyle(color = SyntaxNumber)))
    }

    Regex("=>|===|!==|&&|\\|\\||\\?|:").findAll(code).forEach { m ->
        spans.add(StyleSpan(m.range.first, m.range.last + 1, SpanStyle(color = SyntaxAttribute)))
    }
}

private fun highlightJson(code: String, spans: MutableList<StyleSpan>) {
    Regex("\"([^\"]+)\"\\s*:").findAll(code).forEach { m ->
        m.groups[1]?.let { g ->
            spans.add(StyleSpan(g.range.first - 1, g.range.last + 2, SpanStyle(color = SyntaxAttribute, fontWeight = FontWeight.Bold)))
        }
    }
    Regex(":\\s*(\"[^\"]*\")").findAll(code).forEach { m ->
        m.groups[1]?.let { g ->
            spans.add(StyleSpan(g.range.first, g.range.last + 1, SpanStyle(color = SyntaxString)))
        }
    }
    Regex("\\b(true|false|null|\\d+(\\.\\d+)?)\\b").findAll(code).forEach { m ->
        spans.add(StyleSpan(m.range.first, m.range.last + 1, SpanStyle(color = SyntaxNumber)))
    }
}

private fun highlightPython(code: String, spans: MutableList<StyleSpan>) {
    Regex("#.*$", RegexOption.MULTILINE).findAll(code).forEach { m ->
        spans.add(StyleSpan(m.range.first, m.range.last + 1, SpanStyle(color = SyntaxComment, fontStyle = FontStyle.Italic)))
    }

    Regex("\"\"\"[\\s\\S]*?\"\"\"|'''[\\s\\S]*?'''|\"[^\"]*\"|'[^']*'").findAll(code).forEach { m ->
        spans.add(StyleSpan(m.range.first, m.range.last + 1, SpanStyle(color = SyntaxString)))
    }

    Regex("@[a-zA-Z_][a-zA-Z0-9_]*").findAll(code).forEach { m ->
        spans.add(StyleSpan(m.range.first, m.range.last + 1, SpanStyle(color = SyntaxTag, fontWeight = FontWeight.Bold)))
    }

    val pyKeywords = setOf(
        "def", "class", "import", "from", "as", "return", "if", "elif", "else", "for", "while",
        "try", "except", "finally", "with", "pass", "break", "continue", "lambda", "global",
        "nonlocal", "assert", "raise", "yield", "async", "await", "in", "is", "not", "and", "or"
    )
    Regex("\\b[a-zA-Z_][a-zA-Z0-9_]*\\b").findAll(code).forEach { m ->
        val word = m.value
        if (pyKeywords.contains(word)) {
            spans.add(StyleSpan(m.range.first, m.range.last + 1, SpanStyle(color = SyntaxKeyword, fontWeight = FontWeight.Bold)))
        } else if (setOf("True", "False", "None").contains(word)) {
            spans.add(StyleSpan(m.range.first, m.range.last + 1, SpanStyle(color = SyntaxNumber, fontWeight = FontWeight.Bold)))
        } else if (setOf("print", "len", "range", "type", "str", "int", "float", "list", "dict", "set", "tuple", "open", "super").contains(word)) {
            spans.add(StyleSpan(m.range.first, m.range.last + 1, SpanStyle(color = SyntaxSelector)))
        }
    }

    Regex("\\b\\d+(\\.\\d+)?\\b").findAll(code).forEach { m ->
        spans.add(StyleSpan(m.range.first, m.range.last + 1, SpanStyle(color = SyntaxNumber)))
    }
}

private fun highlightMarkdown(code: String, spans: MutableList<StyleSpan>) {
    Regex("^#{1,6}\\s+.*$", RegexOption.MULTILINE).findAll(code).forEach { m ->
        spans.add(StyleSpan(m.range.first, m.range.last + 1, SpanStyle(color = SyntaxKeyword, fontWeight = FontWeight.Bold)))
    }

    Regex("\\*\\*.*?\\*\\*|__.*?__").findAll(code).forEach { m ->
        spans.add(StyleSpan(m.range.first, m.range.last + 1, SpanStyle(color = SyntaxTag, fontWeight = FontWeight.Bold)))
    }

    Regex("```[\\s\\S]*?```").findAll(code).forEach { m ->
        spans.add(StyleSpan(m.range.first, m.range.last + 1, SpanStyle(color = SyntaxString, fontFamily = FontFamily.Monospace)))
    }

    Regex("`[^`]+`").findAll(code).forEach { m ->
        spans.add(StyleSpan(m.range.first, m.range.last + 1, SpanStyle(color = SyntaxAttribute)))
    }

    Regex("\\[.*?\\]\\(.*?\\)").findAll(code).forEach { m ->
        spans.add(StyleSpan(m.range.first, m.range.last + 1, SpanStyle(color = SyntaxSelector)))
    }
}

// Code Folding Models & Region Detection
data class FoldRegion(
    val startLine: Int,
    val endLine: Int,
    val label: String
)

fun detectFoldRegions(code: String, path: String): List<FoldRegion> {
    if (code.isBlank()) return emptyList()
    val lines = code.lines()
    val regions = mutableListOf<FoldRegion>()

    // 1. Detect curly brace blocks { ... } for JS, CSS, JSON, Java, Kotlin
    val stack = java.util.ArrayDeque<Pair<Int, String>>()
    lines.forEachIndexed { i, line ->
        for (ch in line) {
            if (ch == '{') {
                stack.push(Pair(i, "{"))
            } else if (ch == '}') {
                if (stack.isNotEmpty() && stack.peek()?.second == "{") {
                    val (startLine, _) = stack.pop()
                    if (i > startLine) {
                        regions.add(FoldRegion(startLine, i, "{ ... }"))
                    }
                }
            }
        }
    }

    // 2. Detect HTML / XML multi-line tags <tag> ... </tag>
    val isHtml = path.endsWith(".html") || path.endsWith(".htm")
    if (isHtml) {
        val tagStack = java.util.ArrayDeque<Pair<Int, String>>()
        val voidTags = setOf("img", "input", "br", "hr", "meta", "link", "source", "embed", "param", "base", "col")
        lines.forEachIndexed { i, line ->
            val openMatches = Regex("<([a-zA-Z0-9-]+)(?:\\s[^>]*)?>").findAll(line)
            for (m in openMatches) {
                val tag = m.groupValues[1].lowercase()
                if (!line.contains("</$tag>") && tag !in voidTags && !line.endsWith("/>")) {
                    tagStack.push(Pair(i, tag))
                }
            }
            val closeMatches = Regex("</([a-zA-Z0-9-]+)>").findAll(line)
            for (m in closeMatches) {
                val tag = m.groupValues[1].lowercase()
                if (tagStack.isNotEmpty() && tagStack.peek()?.second == tag) {
                    val (startLine, matchedTag) = tagStack.pop()
                    if (i > startLine) {
                        regions.add(FoldRegion(startLine, i, "<$matchedTag> ... </$matchedTag>"))
                    }
                }
            }
        }
    }

    // 3. Detect Python def / class blocks
    if (path.endsWith(".py")) {
        lines.forEachIndexed { i, line ->
            val trimmed = line.trim()
            if ((trimmed.startsWith("def ") || trimmed.startsWith("class ") || trimmed.startsWith("if ") || trimmed.startsWith("for ") || trimmed.startsWith("while ")) && trimmed.endsWith(":")) {
                val indent = line.indexOfFirst { !it.isWhitespace() }
                var endLine = i
                for (j in (i + 1) until lines.size) {
                    val next = lines[j]
                    if (next.isBlank()) continue
                    val nextIndent = next.indexOfFirst { !it.isWhitespace() }
                    if (nextIndent <= indent) break
                    endLine = j
                }
                if (endLine > i) {
                    regions.add(FoldRegion(i, endLine, "..."))
                }
            }
        }
    }

    return regions.distinctBy { Pair(it.startLine, it.endLine) }.sortedBy { it.startLine }
}

// Code Completion & Suggestion Engine
data class CompletionSuggestion(
    val displayText: String,
    val insertText: String,
    val category: String,
    val prefixToReplace: String
)

fun getCompletionSuggestions(code: String, path: String): List<CompletionSuggestion> {
    if (code.isEmpty()) return emptyList()

    val lastLine = code.lines().lastOrNull() ?: ""
    val trimmed = lastLine.trimEnd()
    if (trimmed.isEmpty()) return emptyList()

    val match = Regex("([<a-zA-Z0-9_:-]+)$").find(trimmed)
    val token = match?.value ?: ""
    if (token.isEmpty()) return emptyList()

    val lowerPath = path.lowercase()
    val suggestions = mutableListOf<CompletionSuggestion>()

    // 1. HTML tag & attribute suggestions
    if (lowerPath.endsWith(".html") || lowerPath.endsWith(".htm") || token.startsWith("<")) {
        val htmlTags = listOf(
            "div", "span", "p", "h1", "h2", "h3", "h4", "button", "input", "form",
            "section", "article", "header", "footer", "nav", "ul", "ol", "li", "a",
            "img", "table", "tr", "td", "script", "style", "link", "main", "aside", "canvas"
        )
        val htmlAttrs = listOf(
            "class=\"\"", "id=\"\"", "href=\"\"", "src=\"\"", "type=\"\"", "style=\"\"",
            "alt=\"\"", "placeholder=\"\"", "value=\"\"", "onclick=\"\"", "disabled", "required"
        )

        val cleanToken = if (token.startsWith("<")) token.drop(1).lowercase() else token.lowercase()
        htmlTags.filter { cleanToken.isEmpty() || it.startsWith(cleanToken) }.forEach { tag ->
            suggestions.add(CompletionSuggestion(
                displayText = "<$tag>",
                insertText = "<$tag></$tag>",
                category = "HTML Tag",
                prefixToReplace = token
            ))
        }

        if (!token.startsWith("<")) {
            htmlAttrs.filter { cleanToken.isEmpty() || it.lowercase().startsWith(cleanToken) }.forEach { attr ->
                suggestions.add(CompletionSuggestion(
                    displayText = attr,
                    insertText = attr,
                    category = "HTML Attr",
                    prefixToReplace = token
                ))
            }
        }
    }

    // 2. CSS property & value suggestions
    if (lowerPath.endsWith(".css") || lowerPath.endsWith(".html")) {
        val cssProps = listOf(
            "color", "background-color", "display", "flex-direction", "justify-content", "align-items",
            "margin", "padding", "border", "border-radius", "width", "height", "font-size", "font-weight",
            "font-family", "position", "top", "left", "right", "bottom", "opacity", "box-shadow",
            "grid-template-columns", "gap", "cursor", "transition", "overflow", "z-index", "text-align"
        )
        val cssValues = listOf(
            "flex", "grid", "block", "inline-block", "none", "center", "space-between", "space-around",
            "pointer", "100%", "auto", "relative", "absolute", "fixed", "border-box", "sans-serif", "hidden"
        )

        val cleanToken = token.lowercase().removePrefix(":")
        cssProps.filter { cleanToken.isNotEmpty() && it.startsWith(cleanToken) }.forEach { prop ->
            suggestions.add(CompletionSuggestion(
                displayText = "$prop:",
                insertText = "$prop: ",
                category = "CSS Prop",
                prefixToReplace = token
            ))
        }

        cssValues.filter { cleanToken.isNotEmpty() && it.startsWith(cleanToken) }.forEach { valStr ->
            suggestions.add(CompletionSuggestion(
                displayText = valStr,
                insertText = "$valStr;",
                category = "CSS Value",
                prefixToReplace = token
            ))
        }
    }

    // 3. JS Keyword suggestions
    if (lowerPath.endsWith(".js") || lowerPath.endsWith(".ts") || lowerPath.endsWith(".html")) {
        val jsKeywords = listOf(
            "function", "const", "let", "var", "return", "if", "else", "async", "await",
            "import", "export", "document.getElementById", "addEventListener", "querySelector",
            "console.log", "fetch", "JSON.stringify", "Math.random", "Promise"
        )

        val cleanToken = token.lowercase()
        jsKeywords.filter { cleanToken.length >= 2 && it.lowercase().startsWith(cleanToken) }.forEach { kw ->
            suggestions.add(CompletionSuggestion(
                displayText = kw,
                insertText = kw,
                category = "JS Keyword",
                prefixToReplace = token
            ))
        }
    }

    return suggestions.distinctBy { it.displayText }.take(8)
}

