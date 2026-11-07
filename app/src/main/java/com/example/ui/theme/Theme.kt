package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = EmeraldActionLight,
    onPrimary = Color.White,
    primaryContainer = DeepGreenPrimaryLight,
    onPrimaryContainer = Color.White,
    secondary = DeepGreenPrimaryLight,
    onSecondary = Color.White,
    secondaryContainer = SoftMintPillLight,
    onSecondaryContainer = VeryDarkGreenTextLight,
    tertiary = WarmAmberBadgeTextLight,
    onTertiary = Color.White,
    tertiaryContainer = WarmAmberBadgeBgLight,
    onTertiaryContainer = WarmAmberBadgeTextLight,
    background = OffWhiteBackgroundLight,
    onBackground = VeryDarkGreenTextLight,
    surface = PureWhiteSurfaceLight,
    onSurface = VeryDarkGreenTextLight,
    surfaceVariant = SoftMintSurfaceLight,
    onSurfaceVariant = MutedGreenTextLight,
    outline = SubtleCardBorderLight
)

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldActionDark,
    onPrimary = Color(0xFF062119),
    primaryContainer = DeepGreenPrimaryDark,
    onPrimaryContainer = CrispMintTextDark,
    secondary = SoftMintTextDark,
    onSecondary = Color(0xFF07291F),
    secondaryContainer = SoftMintPillDark,
    onSecondaryContainer = CrispMintTextDark,
    tertiary = WarmAmberBadgeTextDark,
    onTertiary = Color(0xFF2B1C06),
    tertiaryContainer = WarmAmberBadgeBgDark,
    onTertiaryContainer = WarmAmberBadgeTextDark,
    background = DeepForestBackgroundDark,
    onBackground = CrispMintTextDark,
    surface = CardSurfaceDark,
    onSurface = CrispMintTextDark,
    surfaceVariant = SoftMintSurfaceDark,
    onSurfaceVariant = MutedGreenTextDark,
    outline = SubtleCardBorderDark
)

val CapZeroShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun CapZeroTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = CapZeroShapes,
        content = content
    )
}
