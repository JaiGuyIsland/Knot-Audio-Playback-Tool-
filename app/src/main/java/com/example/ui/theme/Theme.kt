package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = KnotTextPrimary,
    onPrimary = KnotObsidian,
    primaryContainer = KnotSurfaceCard,
    onPrimaryContainer = KnotTextPrimary,
    secondary = KnotAccent,
    onSecondary = KnotObsidian,
    secondaryContainer = KnotSurfaceHighlight,
    onSecondaryContainer = KnotAccent,
    tertiary = KnotAccentPurple,
    background = KnotObsidian,
    onBackground = KnotTextPrimary,
    surface = KnotSurface,
    onSurface = KnotTextPrimary,
    surfaceVariant = KnotSurfaceCard,
    onSurfaceVariant = KnotTextSecondary,
    outline = KnotBorder,
    outlineVariant = KnotBorderSubtle
)

private val LightColorScheme = lightColorScheme(
    primary = KnotLightTextPrimary,
    onPrimary = KnotLightSurface,
    primaryContainer = KnotLightSurfaceCard,
    onPrimaryContainer = KnotLightTextPrimary,
    secondary = KnotAccent,
    onSecondary = KnotLightSurface,
    secondaryContainer = KnotLightBorder,
    onSecondaryContainer = KnotAccent,
    tertiary = KnotAccentPurple,
    background = KnotLightBg,
    onBackground = KnotLightTextPrimary,
    surface = KnotLightSurface,
    onSurface = KnotLightTextPrimary,
    surfaceVariant = KnotLightSurfaceCard,
    onSurfaceVariant = KnotLightTextSecondary,
    outline = KnotLightBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep intentional minimalist design
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> DarkColorScheme // Knot shines best in serene minimalist dark aesthetic
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
