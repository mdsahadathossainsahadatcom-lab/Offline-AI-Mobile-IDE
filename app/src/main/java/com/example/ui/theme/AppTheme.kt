package com.example.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

enum class IdeTheme(val displayName: String, val icon: String) {
    NIGHT("Night (Dark)", "🌙"),
    WHITE("White (Light)", "⚪"),
    GREEN("Green (Matrix)", "🟢"),
    BLUE("Blue (Ocean)", "🔵"),
    YELLOW("Yellow (Warm)", "🟡")
}

val NightColorScheme = darkColorScheme(
    primary = NightPrimary,
    onPrimary = NightOnPrimary,
    primaryContainer = NightSurfaceVariant,
    onPrimaryContainer = NightOnSurface,
    secondary = NightAccent,
    background = NightBackground,
    surface = NightSurface,
    surfaceVariant = NightSurfaceVariant,
    onBackground = NightOnSurface,
    onSurface = NightOnSurface,
    onSurfaceVariant = NightOnSurface
)

val WhiteColorScheme = lightColorScheme(
    primary = WhitePrimary,
    onPrimary = WhiteOnPrimary,
    primaryContainer = WhiteSurfaceVariant,
    onPrimaryContainer = WhiteOnSurface,
    secondary = WhiteAccent,
    background = WhiteBackground,
    surface = WhiteSurface,
    surfaceVariant = WhiteSurfaceVariant,
    onBackground = WhiteOnSurface,
    onSurface = WhiteOnSurface,
    onSurfaceVariant = WhiteOnSurface
)

val GreenColorScheme = darkColorScheme(
    primary = GreenPrimary,
    onPrimary = GreenOnPrimary,
    primaryContainer = GreenSurfaceVariant,
    onPrimaryContainer = GreenOnSurface,
    secondary = GreenAccent,
    background = GreenBackground,
    surface = GreenSurface,
    surfaceVariant = GreenSurfaceVariant,
    onBackground = GreenOnSurface,
    onSurface = GreenOnSurface,
    onSurfaceVariant = GreenOnSurface
)

val BlueColorScheme = darkColorScheme(
    primary = BluePrimary,
    onPrimary = BlueOnPrimary,
    primaryContainer = BlueSurfaceVariant,
    onPrimaryContainer = BlueOnSurface,
    secondary = BlueAccent,
    background = BlueBackground,
    surface = BlueSurface,
    surfaceVariant = BlueSurfaceVariant,
    onBackground = BlueOnSurface,
    onSurface = BlueOnSurface,
    onSurfaceVariant = BlueOnSurface
)

val YellowColorScheme = darkColorScheme(
    primary = YellowPrimary,
    onPrimary = YellowOnPrimary,
    primaryContainer = YellowSurfaceVariant,
    onPrimaryContainer = YellowOnSurface,
    secondary = YellowAccent,
    background = YellowBackground,
    surface = YellowSurface,
    surfaceVariant = YellowSurfaceVariant,
    onBackground = YellowOnSurface,
    onSurface = YellowOnSurface,
    onSurfaceVariant = YellowOnSurface
)

fun getIdeColorScheme(theme: IdeTheme): ColorScheme = when (theme) {
    IdeTheme.NIGHT -> NightColorScheme
    IdeTheme.WHITE -> WhiteColorScheme
    IdeTheme.GREEN -> GreenColorScheme
    IdeTheme.BLUE -> BlueColorScheme
    IdeTheme.YELLOW -> YellowColorScheme
}
