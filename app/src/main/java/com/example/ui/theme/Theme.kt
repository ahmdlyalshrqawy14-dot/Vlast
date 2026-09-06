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

private val VlastDarkColorScheme = darkColorScheme(
    primary = VlastTokens.BrandRed,
    onPrimary = VlastTokens.TextOnAccent,
    primaryContainer = VlastTokens.BrandRedDark,
    onPrimaryContainer = VlastTokens.TextPrimary,
    secondary = VlastTokens.BrandAmber,
    onSecondary = VlastTokens.DarkBackground,
    tertiary = VlastTokens.BrandCyan,
    onTertiary = VlastTokens.DarkBackground,
    background = VlastTokens.DarkBackground,
    onBackground = VlastTokens.TextPrimary,
    surface = VlastTokens.DarkSurface,
    onSurface = VlastTokens.TextPrimary,
    surfaceVariant = VlastTokens.DarkSurfaceVariant,
    onSurfaceVariant = VlastTokens.TextSecondary,
    outline = VlastTokens.DarkBorder,
    outlineVariant = VlastTokens.DarkBorderActive
)

private val VlastLightColorScheme = lightColorScheme(
    primary = VlastTokens.BrandRed,
    onPrimary = VlastTokens.TextOnAccent,
    primaryContainer = VlastTokens.LightSurfaceVariant,
    onPrimaryContainer = VlastTokens.LightTextPrimary,
    secondary = VlastTokens.BrandAmber,
    onSecondary = VlastTokens.DarkBackground,
    tertiary = VlastTokens.BrandCyan,
    onTertiary = VlastTokens.DarkBackground,
    background = VlastTokens.LightBackground,
    onBackground = VlastTokens.LightTextPrimary,
    surface = VlastTokens.LightSurface,
    onSurface = VlastTokens.LightTextPrimary,
    surfaceVariant = VlastTokens.LightSurfaceVariant,
    onSurfaceVariant = VlastTokens.LightTextSecondary,
    outline = VlastTokens.LightBorder,
    outlineVariant = VlastTokens.LightBorderActive
)

@Composable
fun VlastTheme(
    darkTheme: Boolean = true, // Item 2: Dark Mode default at first run
    dynamicColor: Boolean = false, // Preserve authoritative identity by default
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> VlastDarkColorScheme
        else -> VlastLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = VlastTypography,
        content = content
    )
}
