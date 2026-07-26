package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import androidx.compose.ui.text.withStyle
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
    onTabSelected: (String) -> Unit,
    onTabClosed: (String) -> Unit,
    onCodeChanged: (String) -> Unit,
    onRunPreview: () -> Unit,
    onToggleFullScreen: () -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var isSearchOpen by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .background(MaterialTheme.colorScheme.background)
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

        // 2. Compact Quick Action Toolbar (Max 40dp height)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Horizontal scrollable snippet chips
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
                if (lastAutoSaveTime != null) {
                    Text(
                        text = "💾 $lastAutoSaveTime",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                        modifier = Modifier.padding(end = 2.dp)
                    )
                }
                IconButton(
                    onClick = { isSearchOpen = !isSearchOpen },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
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

        // Search Bar if toggled
        if (isSearchOpen) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Find in code...", fontSize = 11.sp) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )
        }

        // 3. Code Editor Area with Line Numbers (consumes all remaining height via weight(1f))
        val lines = remember(codeContent) { codeContent.split("\n") }
        val verticalScrollState = rememberScrollState()
        val horizontalScrollState = rememberScrollState()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            // Line Numbers Column (Single-Text layout optimization for large files)
            val lineNumberString = remember(lines.size) {
                val sb = StringBuilder()
                for (i in 1..lines.size) {
                    sb.append(i).append(" \n")
                }
                sb.toString()
            }

            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(36.dp)
                    .background(MaterialTheme.colorScheme.surface)
                    .verticalScroll(verticalScrollState)
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = lineNumberString,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    lineHeight = 18.sp
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Code Text Input Field with Prism-Style Native Syntax Coloration & AI Ghost Text Engine
            var ghostTextPrediction by remember { mutableStateOf("") }

            androidx.compose.runtime.LaunchedEffect(codeContent, activeTabPath) {
                if (codeContent.isNotBlank()) {
                    kotlinx.coroutines.delay(400) // 400ms pause in typing
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
                val syntaxTransformation = remember(activeTabPath) {
                    SyntaxHighlightTransformation(activeTabPath)
                }

                Column(modifier = Modifier.fillMaxSize()) {
                    BasicTextField(
                        value = codeContent,
                        onValueChange = onCodeChanged,
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
                    )

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
    }
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

class SyntaxHighlightTransformation(private val path: String) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val highlighted = highlightSyntax(text.text, path)
        return TransformedText(
            text = highlighted,
            offsetMapping = OffsetMapping.Identity
        )
    }
}

private fun highlightSyntax(code: String, path: String): AnnotatedString {
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
    // 1. Comments <!-- ... -->
    Regex("<!--[\\s\\S]*?-->").findAll(code).forEach { m ->
        spans.add(StyleSpan(m.range.first, m.range.last + 1, SpanStyle(color = SyntaxComment, fontStyle = FontStyle.Italic)))
    }

    // 2. Tags
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
    // Comments
    Regex("#.*$", RegexOption.MULTILINE).findAll(code).forEach { m ->
        spans.add(StyleSpan(m.range.first, m.range.last + 1, SpanStyle(color = SyntaxComment, fontStyle = FontStyle.Italic)))
    }

    // Triple quotes / Single quotes / Double quotes
    Regex("\"\"\"[\\s\\S]*?\"\"\"|'''[\\s\\S]*?'''|\"[^\"]*\"|'[^']*'").findAll(code).forEach { m ->
        spans.add(StyleSpan(m.range.first, m.range.last + 1, SpanStyle(color = SyntaxString)))
    }

    // Decorators
    Regex("@[a-zA-Z_][a-zA-Z0-9_]*").findAll(code).forEach { m ->
        spans.add(StyleSpan(m.range.first, m.range.last + 1, SpanStyle(color = SyntaxTag, fontWeight = FontWeight.Bold)))
    }

    // Keywords
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

    // Numbers
    Regex("\\b\\d+(\\.\\d+)?\\b").findAll(code).forEach { m ->
        spans.add(StyleSpan(m.range.first, m.range.last + 1, SpanStyle(color = SyntaxNumber)))
    }
}

private fun highlightMarkdown(code: String, spans: MutableList<StyleSpan>) {
    // Headers (#, ##, ###)
    Regex("^#{1,6}\\s+.*$", RegexOption.MULTILINE).findAll(code).forEach { m ->
        spans.add(StyleSpan(m.range.first, m.range.last + 1, SpanStyle(color = SyntaxKeyword, fontWeight = FontWeight.Bold)))
    }

    // Bold (**text** or __text__)
    Regex("\\*\\*.*?\\*\\*|__.*?__").findAll(code).forEach { m ->
        spans.add(StyleSpan(m.range.first, m.range.last + 1, SpanStyle(color = SyntaxTag, fontWeight = FontWeight.Bold)))
    }

    // Code blocks ```...```
    Regex("```[\\s\\S]*?```").findAll(code).forEach { m ->
        spans.add(StyleSpan(m.range.first, m.range.last + 1, SpanStyle(color = SyntaxString, fontFamily = FontFamily.Monospace)))
    }

    // Inline code `...`
    Regex("`[^`]+`").findAll(code).forEach { m ->
        spans.add(StyleSpan(m.range.first, m.range.last + 1, SpanStyle(color = SyntaxAttribute)))
    }

    // Links [title](url)
    Regex("\\[.*?\\]\\(.*?\\)").findAll(code).forEach { m ->
        spans.add(StyleSpan(m.range.first, m.range.last + 1, SpanStyle(color = SyntaxSelector)))
    }
}

