package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = CoralPrimaryDark,
    onPrimary = Color(0xFF280800),
    primaryContainer = CoralContainerDark,
    onPrimaryContainer = OnCoralContainerDark,
    secondary = MintSecondaryDark,
    onSecondary = Color(0xFF00201C),
    secondaryContainer = MintContainerDark,
    onSecondaryContainer = OnMintContainerDark,
    tertiary = AmberTertiaryDark,
    onTertiary = Color(0xFF2B1800),
    tertiaryContainer = AmberContainerDark,
    onTertiaryContainer = Color(0xFFFDE68A),
    background = ObsidianBackground,
    onBackground = SnowInk,
    surface = ObsidianSurface,
    onSurface = SnowInk,
    surfaceVariant = ObsidianSurfaceVariant,
    onSurfaceVariant = SnowMuted,
    outline = OutlineDark,
    outlineVariant = OutlineDark
)

private val LightColorScheme = lightColorScheme(
    primary = CoralPrimary,
    onPrimary = Color.White,
    primaryContainer = CoralContainerLight,
    onPrimaryContainer = OnCoralContainerLight,
    secondary = MintSecondary,
    onSecondary = Color.White,
    secondaryContainer = MintContainerLight,
    onSecondaryContainer = OnMintContainerLight,
    tertiary = AmberTertiary,
    onTertiary = Color.White,
    tertiaryContainer = AmberContainerLight,
    onTertiaryContainer = Color(0xFF451A03),
    background = AlabasterBackground,
    onBackground = SlateInk,
    surface = AlabasterSurface,
    onSurface = SlateInk,
    surfaceVariant = AlabasterSurfaceVariant,
    onSurfaceVariant = SlateMuted,
    outline = OutlineLight,
    outlineVariant = OutlineLight
)

val PulseGoalShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun PulseGoalTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = PulseGoalTypography,
        shapes = PulseGoalShapes,
        content = content
    )
}
