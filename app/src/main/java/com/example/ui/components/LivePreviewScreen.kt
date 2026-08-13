package com.example.ui.components

import android.annotation.SuppressLint
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Tablet
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

class JsConsoleInterceptor(private val onLog: (String) -> Unit) {
    @JavascriptInterface
    fun reportError(errorMsg: String, stackTrace: String) {
        val fullMsg = if (errorMsg.contains("[ERROR]")) errorMsg else "[ERROR] $errorMsg"
        val extra = if (stackTrace.isNotBlank()) "\nStack: $stackTrace" else ""
        onLog("$fullMsg$extra")
    }

    @JavascriptInterface
    fun reportLog(logMsg: String) {
        onLog(logMsg)
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LivePreviewScreen(
    filesMap: Map<String, String>,
    viewportMode: String,
    consoleLogs: List<String>,
    activeProjectId: Long = 1L,
    onViewportChange: (String) -> Unit,
    onAddConsoleLog: (String) -> Unit,
    onClearLogs: () -> Unit,
    onAutoFixError: (String) -> Unit = {}
) {
    var isConsoleVisible by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isReadyToRender by remember { mutableStateOf(false) }
    var currentLoadedHtml by remember { mutableStateOf("") }

    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, ERRORS, WARNINGS, LOGS
    var searchQuery by remember { mutableStateOf("") }
    var jsInputText by remember { mutableStateOf("") }
    val clipboardManager = LocalClipboardManager.current

    androidx.compose.runtime.LaunchedEffect(Unit) {
        // Defer heavy WebView initialization until tab navigation transition completes (100ms)
        kotlinx.coroutines.delay(100L)
        isReadyToRender = true
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    val baseUrl = remember(activeProjectId) {
        "file://${context.filesDir.absolutePath}/workspace_${activeProjectId}/"
    }

    val combinedHtml = remember(filesMap) {
        buildCombinedWebPage(filesMap)
    }

    val errorCount = remember(consoleLogs) {
        consoleLogs.count {
            it.contains("ERROR", ignoreCase = true) ||
                    it.contains("TypeError", ignoreCase = true) ||
                    it.contains("SyntaxError", ignoreCase = true) ||
                    it.contains("ReferenceError", ignoreCase = true)
        }
    }

    val warningCount = remember(consoleLogs) {
        consoleLogs.count {
            it.contains("WARN", ignoreCase = true) || it.contains("WARNING", ignoreCase = true)
        }
    }

    val filteredLogs = remember(consoleLogs, selectedFilter, searchQuery) {
        consoleLogs.filter { log ->
            val matchesFilter = when (selectedFilter) {
                "ERRORS" -> log.contains("ERROR", ignoreCase = true) || log.contains("TypeError", ignoreCase = true) || log.contains("SyntaxError", ignoreCase = true) || log.contains("ReferenceError", ignoreCase = true)
                "WARNINGS" -> log.contains("WARN", ignoreCase = true) || log.contains("WARNING", ignoreCase = true)
                "LOGS" -> log.contains("LOG", ignoreCase = true) || log.contains("INFO", ignoreCase = true) || log.startsWith(">") || log.startsWith("<-")
                else -> true
            }
            val matchesSearch = searchQuery.isBlank() || log.contains(searchQuery, ignoreCase = true)
            matchesFilter && matchesSearch
        }
    }

    fun submitReplCommand() {
        val cmd = jsInputText.trim()
        if (cmd.isNotBlank()) {
            onAddConsoleLog("> $cmd")
            jsInputText = ""
            webViewRef?.evaluateJavascript(cmd) { result ->
                if (result != null && result != "null" && result != "undefined") {
                    onAddConsoleLog("<- $result")
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 1. Top Bar: Viewport Switcher & Reload/Console Toggle Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            SingleChoiceSegmentedButtonRow {
                SegmentedButton(
                    selected = viewportMode == "MOBILE",
                    onClick = { onViewportChange("MOBILE") },
                    shape = SegmentedButtonDefaults.itemShape(0, 3)
                ) {
                    Icon(imageVector = Icons.Default.PhoneAndroid, contentDescription = "Mobile View", modifier = Modifier.padding(2.dp))
                }
                SegmentedButton(
                    selected = viewportMode == "TABLET",
                    onClick = { onViewportChange("TABLET") },
                    shape = SegmentedButtonDefaults.itemShape(1, 3)
                ) {
                    Icon(imageVector = Icons.Default.Tablet, contentDescription = "Tablet View", modifier = Modifier.padding(2.dp))
                }
                SegmentedButton(
                    selected = viewportMode == "DESKTOP",
                    onClick = { onViewportChange("DESKTOP") },
                    shape = SegmentedButtonDefaults.itemShape(2, 3)
                ) {
                    Icon(imageVector = Icons.Default.DesktopWindows, contentDescription = "Desktop View", modifier = Modifier.padding(2.dp))
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { isConsoleVisible = !isConsoleVisible },
                    modifier = Modifier.testTag("toggle_console_button")
                ) {
                    if (errorCount > 0) {
                        BadgedBox(
                            badge = {
                                Badge(containerColor = MaterialTheme.colorScheme.error) {
                                    Text("$errorCount", color = MaterialTheme.colorScheme.onError, fontSize = 9.sp)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Terminal,
                                contentDescription = "Toggle Console",
                                tint = if (isConsoleVisible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Default.Terminal,
                            contentDescription = "Toggle Console",
                            tint = if (isConsoleVisible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                IconButton(
                    onClick = {
                        currentLoadedHtml = combinedHtml
                        webViewRef?.loadDataWithBaseURL(baseUrl, combinedHtml, "text/html", "UTF-8", null)
                    },
                    modifier = Modifier.testTag("reload_preview_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reload Preview",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }

        // 2. WebView Canvas Area with Viewport Framing
        val viewportWidthModifier = when (viewportMode) {
            "MOBILE" -> Modifier.width(360.dp)
            "TABLET" -> Modifier.width(600.dp)
            else -> Modifier.fillMaxWidth()
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(if (isConsoleVisible) 0.50f else 1f)
                .padding(8.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Box(
                modifier = viewportWidthModifier
                    .fillMaxHeight()
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .background(Color.White, shape = RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (!isReadyToRender) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        androidx.compose.material3.CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Initializing Live Preview Engine...",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                } else {
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.allowFileAccess = true
                                settings.allowContentAccess = true
                                settings.loadWithOverviewMode = true
                                settings.useWideViewPort = true

                                // Module 2: JS Console Interceptor Bridge
                                addJavascriptInterface(JsConsoleInterceptor(onAddConsoleLog), "AndroidBridge")

                                webChromeClient = object : WebChromeClient() {
                                    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                                        if (consoleMessage != null) {
                                            val level = consoleMessage.messageLevel().name
                                            val msg = consoleMessage.message()
                                            if (!msg.startsWith("[ERROR]") && !msg.startsWith("[LOG]") && !msg.startsWith("[WARN]") && !msg.startsWith("[INFO]")) {
                                                val prefix = when (level) {
                                                    "ERROR" -> "[ERROR]"
                                                    "WARNING" -> "[WARN]"
                                                    "TIP", "DEBUG" -> "[DEBUG]"
                                                    else -> "[LOG]"
                                                }
                                                onAddConsoleLog("$prefix $msg (line ${consoleMessage.lineNumber()})")
                                            }
                                        }
                                        return true
                                    }
                                }

                                webViewClient = object : WebViewClient() {}
                                loadDataWithBaseURL(baseUrl, combinedHtml, "text/html", "UTF-8", null)
                                currentLoadedHtml = combinedHtml
                                webViewRef = this
                            }
                        },
                        update = { view ->
                            if (currentLoadedHtml != combinedHtml) {
                                currentLoadedHtml = combinedHtml
                                view.loadDataWithBaseURL(baseUrl, combinedHtml, "text/html", "UTF-8", null)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Small Expandable Log Console Overlay Bar (when collapsed)
            if (!isConsoleVisible) {
                val lastLog = consoleLogs.lastOrNull() ?: "Console ready. Waiting for log output..."
                Surface(
                    onClick = { isConsoleVisible = true },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(0.96f)
                        .padding(bottom = 8.dp)
                        .testTag("expandable_console_bar"),
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF0F172A).copy(alpha = 0.92f),
                    tonalElevation = 6.dp,
                    border = BorderStroke(1.dp, Color(0xFF334155))
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Terminal,
                                contentDescription = "Console",
                                tint = if (errorCount > 0) Color(0xFFF43F5E) else Color(0xFF38BDF8),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            if (errorCount > 0) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFFF43F5E).copy(alpha = 0.2f),
                                    modifier = Modifier.padding(end = 6.dp)
                                ) {
                                    Text(
                                        text = "$errorCount errors",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFF43F5E),
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = lastLog,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = when {
                                    lastLog.contains("ERROR", ignoreCase = true) -> Color(0xFFF43F5E)
                                    lastLog.contains("WARN", ignoreCase = true) -> Color(0xFFFBBF24)
                                    else -> Color(0xFF94A3B8)
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${consoleLogs.size} logs",
                                fontSize = 10.sp,
                                color = Color(0xFF64748B),
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowUp,
                                contentDescription = "Expand Console",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // 3. Dedicated DevTools Console Overlay (Expanded)
        if (isConsoleVisible) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.50f)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .testTag("preview_console_overlay"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                    // Header Bar with Title, Filter Chips, Search, and Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Terminal,
                                contentDescription = "Console",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "DEVTOOLS CONSOLE (${consoleLogs.size})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = onClearLogs,
                                modifier = Modifier.size(28.dp).testTag("console_clear_logs_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear logs",
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            IconButton(
                                onClick = { isConsoleVisible = false },
                                modifier = Modifier.size(28.dp).testTag("console_collapse_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Collapse console",
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = { isConsoleVisible = false },
                                modifier = Modifier.size(28.dp).testTag("console_close_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close console",
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Filter Chips & Search Bar Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilterChip(
                                selected = selectedFilter == "ALL",
                                onClick = { selectedFilter = "ALL" },
                                label = { Text("All", fontSize = 10.sp) },
                                modifier = Modifier.height(26.dp).testTag("console_filter_all")
                            )
                            FilterChip(
                                selected = selectedFilter == "ERRORS",
                                onClick = { selectedFilter = "ERRORS" },
                                label = { Text("Errors ($errorCount)", fontSize = 10.sp) },
                                modifier = Modifier.height(26.dp).testTag("console_filter_errors"),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer
                                )
                            )
                            FilterChip(
                                selected = selectedFilter == "WARNINGS",
                                onClick = { selectedFilter = "WARNINGS" },
                                label = { Text("Warn ($warningCount)", fontSize = 10.sp) },
                                modifier = Modifier.height(26.dp).testTag("console_filter_warnings")
                            )
                            FilterChip(
                                selected = selectedFilter == "LOGS",
                                onClick = { selectedFilter = "LOGS" },
                                label = { Text("Logs", fontSize = 10.sp) },
                                modifier = Modifier.height(26.dp).testTag("console_filter_logs")
                            )
                        }

                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Filter...", fontSize = 10.sp) },
                            modifier = Modifier.width(110.dp).height(32.dp).testTag("console_search_input"),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", modifier = Modifier.size(12.dp)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Log Output Console Window
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(Color(0xFF0F172A), shape = RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (filteredLogs.isEmpty()) {
                            item {
                                Text(
                                    text = if (consoleLogs.isEmpty()) "Console output is empty. Run code or log messages to debug." else "No log entries match the active filter.",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B),
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(4.dp)
                                )
                            }
                        }

                        items(filteredLogs) { log ->
                            val isError = log.contains("ERROR", ignoreCase = true) ||
                                    log.contains("TypeError", ignoreCase = true) ||
                                    log.contains("SyntaxError", ignoreCase = true) ||
                                    log.contains("ReferenceError", ignoreCase = true)
                            val isReplInput = log.startsWith(">")
                            val isReplOutput = log.startsWith("<-")

                            when {
                                isError -> {
                                    Card(
                                        modifier = Modifier.fillMaxWidth().testTag("console_error_log_item"),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A0F1A)),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF43F5E))
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = log,
                                                    fontSize = 11.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = Color(0xFFF43F5E),
                                                    modifier = Modifier.weight(1f)
                                                )
                                                IconButton(
                                                    onClick = { clipboardManager.setText(AnnotatedString(log)) },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.ContentCopy,
                                                        contentDescription = "Copy log",
                                                        tint = Color(0xFFF43F5E).copy(alpha = 0.7f),
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(6.dp))

                                            Button(
                                                onClick = { onAutoFixError(log) },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF43F5E)),
                                                modifier = Modifier.height(28.dp).testTag("auto_fix_error_button"),
                                                shape = RoundedCornerShape(14.dp)
                                            ) {
                                                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "Auto Fix", modifier = Modifier.size(12.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("⚡ Auto-Fix with AI", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                                isReplInput -> {
                                    Text(
                                        text = log,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFA855F7)
                                    )
                                }
                                isReplOutput -> {
                                    Text(
                                        text = log,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color(0xFF4ADE80)
                                    )
                                }
                                else -> {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = log,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = when {
                                                log.contains("WARN", ignoreCase = true) -> Color(0xFFFBBF24)
                                                log.contains("DEBUG", ignoreCase = true) -> Color(0xFF94A3B8)
                                                else -> Color(0xFF38BDF8)
                                            },
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(
                                            onClick = { clipboardManager.setText(AnnotatedString(log)) },
                                            modifier = Modifier.size(20.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ContentCopy,
                                                contentDescription = "Copy log",
                                                tint = Color(0xFF64748B),
                                                modifier = Modifier.size(10.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Interactive JS REPL Input Prompt Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = jsInputText,
                            onValueChange = { jsInputText = it },
                            placeholder = { Text("Run JS e.g. document.title, console.log('hi')", fontSize = 11.sp) },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("console_repl_input"),
                            singleLine = true,
                            leadingIcon = {
                                Text(">", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 8.dp))
                            },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { submitReplCommand() }),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                            )
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        IconButton(
                            onClick = { submitReplCommand() },
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(10.dp))
                                .size(40.dp)
                                .testTag("console_repl_send_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Run JavaScript",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun buildCombinedWebPage(filesMap: Map<String, String>): String {
    val mainHtmlEntry = filesMap["index.html"]
        ?: filesMap["/index.html"]
        ?: filesMap.entries.firstOrNull { it.key.lowercase().endsWith(".html") }?.value
        ?: """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body { font-family: system-ui, -apple-system, sans-serif; display: flex; flex-direction: column; align-items: center; justify-content: center; height: 100vh; margin: 0; background-color: #0f172a; color: #f8fafc; text-align: center; padding: 20px; }
                    h1 { color: #38bdf8; font-size: 24px; margin-bottom: 8px; }
                    p { color: #94a3b8; font-size: 14px; max-width: 400px; line-height: 1.5; }
                    .card { background: #1e293b; border: 1px solid #334155; padding: 24px; border-radius: 12px; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.3); }
                </style>
            </head>
            <body>
                <div class="card">
                    <h1>Live Preview Ready</h1>
                    <p>Create an <code>index.html</code> file in your workspace editor to start rendering web projects live!</p>
                </div>
            </body>
            </html>
        """.trimIndent()

    // Aggregate CSS contents from all .css files in workspace
    val aggregatedCss = filesMap.entries
        .filter { it.key.lowercase().endsWith(".css") }
        .joinToString("\n\n") { entry -> "/* --- ${entry.key} --- */\n${entry.value}" }

    // Aggregate JS contents from all .js files in workspace
    val aggregatedJs = filesMap.entries
        .filter { it.key.lowercase().endsWith(".js") }
        .joinToString("\n\n") { entry -> "/* --- ${entry.key} --- */\n${entry.value}" }

    // Inject Error Interceptor Bridge
    val jsInterceptor = """
        <script>
        (function() {
          function serializeArg(arg) {
            if (arg === null) return 'null';
            if (arg === undefined) return 'undefined';
            if (typeof arg === 'object') {
              try { return JSON.stringify(arg); } catch(e) { return String(arg); }
            }
            return String(arg);
          }
          function formatArgs(args) {
            return Array.prototype.slice.call(args).map(serializeArg).join(' ');
          }

          window.onerror = function(msg, url, line, col, error) {
            var stack = error && error.stack ? error.stack : '';
            if (window.AndroidBridge && window.AndroidBridge.reportError) {
              window.AndroidBridge.reportError('[ERROR] ' + msg + ' (' + (url || 'script.js') + ':' + line + ':' + col + ')', stack);
            }
          };

          var origLog = console.log;
          console.log = function() {
            var str = formatArgs(arguments);
            if (window.AndroidBridge && window.AndroidBridge.reportLog) {
              window.AndroidBridge.reportLog('[LOG] ' + str);
            }
            if (origLog) origLog.apply(console, arguments);
          };

          var origWarn = console.warn;
          console.warn = function() {
            var str = formatArgs(arguments);
            if (window.AndroidBridge && window.AndroidBridge.reportLog) {
              window.AndroidBridge.reportLog('[WARN] ' + str);
            }
            if (origWarn) origWarn.apply(console, arguments);
          };

          var origInfo = console.info;
          console.info = function() {
            var str = formatArgs(arguments);
            if (window.AndroidBridge && window.AndroidBridge.reportLog) {
              window.AndroidBridge.reportLog('[INFO] ' + str);
            }
            if (origInfo) origInfo.apply(console, arguments);
          };

          var origErr = console.error;
          console.error = function() {
            var str = formatArgs(arguments);
            if (window.AndroidBridge && window.AndroidBridge.reportError) {
              window.AndroidBridge.reportError('[ERROR] ' + str, '');
            }
            if (origErr) origErr.apply(console, arguments);
          };
        })();
        </script>
    """.trimIndent()

    var resultHtml = mainHtmlEntry
    if (!resultHtml.contains("AndroidBridge")) {
        val headIdx = resultHtml.indexOf("<head>", ignoreCase = true)
        resultHtml = if (headIdx != -1) {
            resultHtml.substring(0, headIdx + 6) + "\n$jsInterceptor\n" + resultHtml.substring(headIdx + 6)
        } else {
            "$jsInterceptor\n$resultHtml"
        }
    }

    // Inject CSS into head if style tags/links not present or to ensure custom CSS is applied
    if (aggregatedCss.isNotBlank()) {
        val styleTag = "\n<style>\n$aggregatedCss\n</style>\n"
        val closeHeadIdx = resultHtml.indexOf("</head>", ignoreCase = true)
        resultHtml = if (closeHeadIdx != -1) {
            resultHtml.substring(0, closeHeadIdx) + styleTag + resultHtml.substring(closeHeadIdx)
        } else {
            styleTag + resultHtml
        }
    }

    // Inject JS into body
    if (aggregatedJs.isNotBlank()) {
        val scriptTag = "\n<script>\n$aggregatedJs\n</script>\n"
        val closeBodyIdx = resultHtml.indexOf("</body>", ignoreCase = true)
        resultHtml = if (closeBodyIdx != -1) {
            resultHtml.substring(0, closeBodyIdx) + scriptTag + resultHtml.substring(closeBodyIdx)
        } else {
            resultHtml + scriptTag
        }
    }

    return resultHtml
}

