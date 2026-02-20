package com.sam.audioroutine.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = InkBlack,
    onPrimary = Mist,
    primaryContainer = Charcoal,
    onPrimaryContainer = Mist,
    secondary = Slate,
    onSecondary = Mist,
    tertiary = GoldAccent,
    onTertiary = InkBlack,
    background = Mist,
    onBackground = InkBlack,
    surface = Mist,
    onSurface = InkBlack,
    surfaceVariant = Bone,
    onSurfaceVariant = Slate,
    outline = Slate,
    outlineVariant = Bone
)

@Composable
fun AudioRoutineTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
