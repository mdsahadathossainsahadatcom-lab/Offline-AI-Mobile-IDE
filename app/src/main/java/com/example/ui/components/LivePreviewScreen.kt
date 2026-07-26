package com.example.ui.components

import android.annotation.SuppressLint
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tablet
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 1. Top Bar: Viewport Switcher & Reload Button
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
                IconButton(onClick = { isConsoleVisible = !isConsoleVisible }) {
                    Icon(
                        imageVector = Icons.Default.BugReport,
                        contentDescription = "Toggle Console",
                        tint = if (isConsoleVisible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(onClick = {
                    currentLoadedHtml = combinedHtml
                    webViewRef?.loadDataWithBaseURL(baseUrl, combinedHtml, "text/html", "UTF-8", null)
                }) {
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
                .weight(if (isConsoleVisible) 0.55f else 1f)
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
                                            val prefix = if (level == "ERROR") "[ERROR]" else "[$level]"
                                            onAddConsoleLog("$prefix ${consoleMessage.message()} (line ${consoleMessage.lineNumber()})")
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
        }

        // 3. DevTools / Console Log Drawer with Autonomous Auto-Fix AI Button
        if (isConsoleVisible) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.45f)
                    .padding(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🐛 Developer Console (${consoleLogs.size})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(onClick = onClearLogs, modifier = Modifier.padding(2.dp)) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear logs",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF0F172A), shape = RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(consoleLogs) { log ->
                            val isError = log.contains("ERROR") || log.contains("Error") || log.contains("TypeError") || log.contains("SyntaxError") || log.contains("ReferenceError")

                            if (isError) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2A0F1A)),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF43F5E))
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text(
                                            text = log,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = Color(0xFFF43F5E)
                                        )

                                        Spacer(modifier = Modifier.height(6.dp))

                                        Button(
                                            onClick = { onAutoFixError(log) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF43F5E)),
                                            modifier = Modifier.height(28.dp),
                                            shape = RoundedCornerShape(14.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "Auto Fix", modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("⚡ Auto-Fix with Local AI", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            } else {
                                Text(
                                    text = log,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = when {
                                        log.contains("WARNING") -> Color(0xFFFBBF24)
                                        else -> Color(0xFF38BDF8)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun buildCombinedWebPage(filesMap: Map<String, String>): String {
    val rawHtml = filesMap["index.html"] ?: "<h1>No index.html found</h1>"
    val cssContent = filesMap["style.css"] ?: ""
    val jsContent = filesMap["script.js"] ?: ""

    // Inject Error Interceptor Bridge
    val jsInterceptor = """
        <script>
        (function() {
          window.onerror = function(msg, url, line, col, error) {
            if (window.AndroidBridge && window.AndroidBridge.reportError) {
              window.AndroidBridge.reportError('[ERROR] ' + msg + ' (line ' + line + ':' + col + ')', error ? error.stack : '');
            }
          };
          var oldErr = console.error;
          console.error = function() {
            var args = Array.prototype.slice.call(arguments).join(' ');
            if (window.AndroidBridge && window.AndroidBridge.reportError) {
              window.AndroidBridge.reportError('[ERROR] ' + args, '');
            }
            if (oldErr) oldErr.apply(console, arguments);
          };
        })();
        </script>
    """.trimIndent()

    var resultHtml = rawHtml
    if (!resultHtml.contains("AndroidBridge")) {
        val headIdx = resultHtml.indexOf("<head>", ignoreCase = true)
        resultHtml = if (headIdx != -1) {
            resultHtml.substring(0, headIdx + 6) + "\n$jsInterceptor\n" + resultHtml.substring(headIdx + 6)
        } else {
            "$jsInterceptor\n$resultHtml"
        }
    }

    // Inject CSS into head if not already external linked
    if (cssContent.isNotBlank()) {
        val styleTag = "\n<style>\n$cssContent\n</style>\n"
        val closeHeadIdx = resultHtml.indexOf("</head>", ignoreCase = true)
        resultHtml = if (closeHeadIdx != -1) {
            resultHtml.substring(0, closeHeadIdx) + styleTag + resultHtml.substring(closeHeadIdx)
        } else {
            styleTag + resultHtml
        }
    }

    // Inject JS into body
    if (jsContent.isNotBlank()) {
        val scriptTag = "\n<script>\n$jsContent\n</script>\n"
        val closeBodyIdx = resultHtml.indexOf("</body>", ignoreCase = true)
        resultHtml = if (closeBodyIdx != -1) {
            resultHtml.substring(0, closeBodyIdx) + scriptTag + resultHtml.substring(closeBodyIdx)
        } else {
            resultHtml + scriptTag
        }
    }

    return resultHtml
}
