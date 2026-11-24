package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val HayatDarkColorScheme = darkColorScheme(
    primary = CyberCyan,
    onPrimary = ObsidianVoid,
    primaryContainer = DeepCyanContainer,
    onPrimaryContainer = SoftCyanGlow,
    secondary = AuroraViolet,
    onSecondary = TextPrimary,
    secondaryContainer = VioletContainer,
    onSecondaryContainer = TextPrimary,
    tertiary = RosePulse,
    onTertiary = TextPrimary,
    background = ObsidianVoid,
    onBackground = TextPrimary,
    surface = MidnightGlass,
    onSurface = TextPrimary,
    surfaceVariant = CyberSlate,
    onSurfaceVariant = TextSecondary,
    surfaceContainerHigh = ElevatedSlate,
    outline = OutlineSubtle,
    error = RosePulse,
    onError = TextPrimary
)

val HayatShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun HayatAiTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = HayatDarkColorScheme,
        typography = HayatTypography,
        shapes = HayatShapes,
        content = content
    )
}
