/*
 * VibeTube
 * Copyright (C) 2026 VibeTube contributors
 *
 * Licensed under GPL-3.0-or-later
 */
package com.rahul.vibetube1.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = VibeTubeRed,
    onPrimary = Color.White,
    primaryContainer = DarkRed,
    onPrimaryContainer = Color.White,
    secondary = DarkRed,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF400000),
    onSecondaryContainer = Color.White,
    tertiary = DarkOnSurfaceVariant,
    onTertiary = DarkOnSurface,
    tertiaryContainer = Color(0xFF333333),
    onTertiaryContainer = Color.White,
    background = DarkBackground,
    surface = DarkBackground,
    surfaceContainer = DarkSurface,
    surfaceContainerHigh = Color(0xFF2C2C2C),
    onBackground = DarkOnSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurface,
    onSurfaceVariant = DarkOnSurfaceVariant,
    error = Color(0xFFCF6679),
    onError = Color.Black,
    errorContainer = Color(0xFFB3261E),
    onErrorContainer = Color.White,
    outline = DarkOnSurfaceVariant.copy(alpha = 0.5f),
    outlineVariant = DarkOnSurfaceVariant.copy(alpha = 0.2f)
)

private val LightColorScheme = lightColorScheme(
    primary = VibeTubeRed,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDAD6),
    onPrimaryContainer = Color(0xFF410002),
    secondary = DarkRed,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDAD6),
    onSecondaryContainer = Color(0xFF410002),
    tertiary = LightOnSurfaceVariant,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE5E5E5),
    onTertiaryContainer = LightOnSurface,
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightBackground,
    surfaceContainer = LightSurface,
    surfaceContainerHigh = Color(0xFFE0E0E0),
    onSurface = LightOnSurface,
    surfaceVariant = LightSurface,
    onSurfaceVariant = LightOnSurfaceVariant,
    error = Color(0xFFB3261E),
    onError = Color.White,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410002),
    outline = LightOnSurfaceVariant.copy(alpha = 0.5f),
    outlineVariant = LightOnSurfaceVariant.copy(alpha = 0.2f)
)

@Composable
fun ZyvoTubeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    isDynamicColorEnabled: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        isDynamicColorEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun VibeTubeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    isDynamicColorEnabled: Boolean = false,
    content: @Composable () -> Unit
) = ZyvoTubeTheme(
    darkTheme = darkTheme,
    isDynamicColorEnabled = isDynamicColorEnabled,
    content = content
)
