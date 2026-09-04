package com.goldenv2.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF6C9AFF),
    onPrimary = Color(0xFF00336D),
    primaryContainer = Color(0xFF4B73BF),
    onPrimaryContainer = Color(0xFFE8EEFF),
    secondary = Color(0xFFB0C9FF),
    onSecondary = Color(0xFF1D3D6D),
    secondaryContainer = Color(0xFF335184),
    onSecondaryContainer = Color(0xFFD6E3FF),
    tertiary = Color(0xFFD0BCFF),
    onTertiary = Color(0xFF4A3B6F),
    tertiaryContainer = Color(0xFF634F8A),
    onTertiaryContainer = Color(0xFFE8DDFF),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF13161B),
    onBackground = Color(0xFFE3E3E6),
    surface = Color(0xFF13161B),
    onSurface = Color(0xFFE3E3E6),
    surfaceVariant = Color(0xFF3F4851),
    onSurfaceVariant = Color(0xFFBFC9D4),
    outline = Color(0xFF828E9B),
    outlineVariant = Color(0xFF3F4851),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFFE3E3E6),
    inverseOnSurface = Color(0xFF13161B),
    inversePrimary = Color(0xFF2E5FB4),
    surfaceTint = Color(0xFF6C9AFF)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF2E5FB4),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD6E3FF),
    onPrimaryContainer = Color(0xFF001A3A),
    secondary = Color(0xFF4A638D),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD6E3FF),
    onSecondaryContainer = Color(0xFF001D3B),
    tertiary = Color(0xFF70599B),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFE8DDFF),
    onTertiaryContainer = Color(0xFF251347),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFAFAFA),
    onBackground = Color(0xFF13161B),
    surface = Color(0xFFFAFAFA),
    onSurface = Color(0xFF13161B),
    surfaceVariant = Color(0xFFDDE3EB),
    onSurfaceVariant = Color(0xFF3F4851),
    outline = Color(0xFF6D7783),
    outlineVariant = Color(0xFFBFC9D4),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFF13161B),
    inverseOnSurface = Color(0xFFE3E3E6),
    inversePrimary = Color(0xFF6C9AFF),
    surfaceTint = Color(0xFF2E5FB4)
)

@Composable
fun GoldenV2Theme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

val Typography = androidx.compose.material3.Typography()