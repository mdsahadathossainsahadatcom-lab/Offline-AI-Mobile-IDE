package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
fun LocalAiIdeTheme(
    ideTheme: IdeTheme = IdeTheme.NIGHT,
    content: @Composable () -> Unit
) {
    val colorScheme = getIdeColorScheme(ideTheme)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

