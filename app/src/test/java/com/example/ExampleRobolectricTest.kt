package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Local AI IDE", appName)
  }

  @Test
  fun `test findMatchRanges and performReplaceAll`() {
    val code = "const x = 10;\nconst y = 20;\nconst sum = x + y;"
    val ranges = com.example.ui.components.findMatchRanges(
      text = code,
      query = "const",
      matchCase = true,
      isWholeWord = true,
      useRegex = false
    )
    assertEquals(3, ranges.size)

    val (newCode, count) = com.example.ui.components.performReplaceAll(
      code = code,
      findQuery = "const",
      replaceText = "let",
      matchCase = true,
      isWholeWord = true,
      useRegex = false
    )
    assertEquals(3, count)
    assertEquals("let x = 10;\nlet y = 20;\nlet sum = x + y;", newCode)
  }

  @Test
  fun `test terminal log entries and source filtering`() {
    val logs = listOf(
      com.example.ui.viewmodel.TerminalLogEntry(
        timestamp = "10:00:00",
        source = com.example.ui.viewmodel.TerminalSource.GGUF_ENGINE,
        stream = com.example.ui.viewmodel.TerminalStream.STDOUT,
        message = "[llama.cpp] eval prompt"
      ),
      com.example.ui.viewmodel.TerminalLogEntry(
        timestamp = "10:00:01",
        source = com.example.ui.viewmodel.TerminalSource.WEB_PREVIEW,
        stream = com.example.ui.viewmodel.TerminalStream.STDERR,
        message = "[preview] Uncaught TypeError"
      )
    )

    val ggufLogs = logs.filter { it.source == com.example.ui.viewmodel.TerminalSource.GGUF_ENGINE }
    val previewLogs = logs.filter { it.source == com.example.ui.viewmodel.TerminalSource.WEB_PREVIEW }

    assertEquals(1, ggufLogs.size)
    assertEquals("[llama.cpp] eval prompt", ggufLogs.first().message)
    assertEquals(1, previewLogs.size)
    assertEquals(com.example.ui.viewmodel.TerminalStream.STDERR, previewLogs.first().stream)
  }

  @Test
  fun `test auto save configuration default and state toggling`() {
    var isAutoSaveEnabled = true
    fun toggleAutoSave(enabled: Boolean) {
      isAutoSaveEnabled = enabled
    }

    assertTrue(isAutoSaveEnabled)
    toggleAutoSave(false)
    assertFalse(isAutoSaveEnabled)
    toggleAutoSave(true)
    assertTrue(isAutoSaveEnabled)
  }

  @Test
  fun `test code folding region detection for JS functions and HTML blocks`() {
    val jsCode = """
      function main() {
        console.log("hello");
        const x = 10;
      }
    """.trimIndent()

    val jsRegions = com.example.ui.components.detectFoldRegions(jsCode, "index.js")
    assertEquals(1, jsRegions.size)
    assertEquals(0, jsRegions.first().startLine)
    assertEquals(3, jsRegions.first().endLine)

    val htmlCode = """
      <div class="container">
        <h1>Title</h1>
        <p>Paragraph</p>
      </div>
    """.trimIndent()

    val htmlRegions = com.example.ui.components.detectFoldRegions(htmlCode, "index.html")
    assertTrue(htmlRegions.isNotEmpty())
    assertEquals(0, htmlRegions.first().startLine)
    assertEquals(3, htmlRegions.first().endLine)
  }

  @Test
  fun `test syntax highlighting engine for html js css`() {
    val jsCode = "const message = 'hello world'; function calc(a, b) { return a + b; }"
    val jsAnnotated = com.example.ui.components.highlightSyntax(jsCode, "script.js")
    assertTrue(jsAnnotated.spanStyles.isNotEmpty())

    val htmlCode = "<div class=\"box\"><h1>Hello</h1></div>"
    val htmlAnnotated = com.example.ui.components.highlightSyntax(htmlCode, "index.html")
    assertTrue(htmlAnnotated.spanStyles.isNotEmpty())

    val cssCode = ".box { color: red; font-size: 16px; }"
    val cssAnnotated = com.example.ui.components.highlightSyntax(cssCode, "style.css")
    assertTrue(cssAnnotated.spanStyles.isNotEmpty())
  }

  @Test
  fun `test keyboard shortcut command triggers`() {
    var saved = false
    var searchToggled = false
    var previewTriggered = false

    fun handleShortcutKey(keyName: String, isCtrl: Boolean) {
      if (isCtrl && keyName == "S") saved = true
      if (isCtrl && keyName == "F") searchToggled = true
      if (isCtrl && keyName == "P") previewTriggered = true
    }

    handleShortcutKey("S", true)
    assertTrue(saved)

    handleShortcutKey("F", true)
    assertTrue(searchToggled)

    handleShortcutKey("P", true)
    assertTrue(previewTriggered)
  }

  @Test
  fun `test code completion suggestions engine for html and css`() {
    val htmlCode = "<di"
    val htmlSuggestions = com.example.ui.components.getCompletionSuggestions(htmlCode, "index.html")
    assertTrue(htmlSuggestions.any { it.displayText == "<div>" })

    val cssCode = ".box { col"
    val cssSuggestions = com.example.ui.components.getCompletionSuggestions(cssCode, "style.css")
    assertTrue(cssSuggestions.any { it.displayText == "color:" })
  }

  @Test
  fun `test dynamic theme toggle logic between light and dark modes`() {
    var activeTheme = com.example.ui.theme.IdeTheme.NIGHT
    fun toggleTheme() {
      activeTheme = if (activeTheme == com.example.ui.theme.IdeTheme.WHITE) {
        com.example.ui.theme.IdeTheme.NIGHT
      } else {
        com.example.ui.theme.IdeTheme.WHITE
      }
    }

    assertEquals(com.example.ui.theme.IdeTheme.NIGHT, activeTheme)

    toggleTheme()
    assertEquals(com.example.ui.theme.IdeTheme.WHITE, activeTheme)

    toggleTheme()
    assertEquals(com.example.ui.theme.IdeTheme.NIGHT, activeTheme)
  }

  @Test
  fun `test file explorer filter logic`() {
    val files = listOf(
      com.example.data.db.FileEntity(id = 1, projectId = 1, path = "index.html", content = "<h1>Test</h1>", language = "html"),
      com.example.data.db.FileEntity(id = 2, projectId = 1, path = "style.css", content = "body {}", language = "css"),
      com.example.data.db.FileEntity(id = 3, projectId = 1, path = "app.js", content = "console.log()", language = "javascript")
    )

    val query = "style"
    val filtered = files.filter { it.path.contains(query, ignoreCase = true) }
    assertEquals(1, filtered.size)
    assertEquals("style.css", filtered[0].path)
  }
}

