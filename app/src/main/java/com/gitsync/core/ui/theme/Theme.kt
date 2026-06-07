package com.gitsync.core.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = GithubBlue,
    onPrimary = Color.White,
    primaryContainer = GithubBlueMuted,
    onPrimaryContainer = GithubBlueHover,
    secondary = GithubPurple,
    onSecondary = Color.White,
    tertiary = GithubOrange,
    background = GithubDarkBackground,
    onBackground = GithubDarkTextPrimary,
    surface = GithubDarkSurface,
    onSurface = GithubDarkTextPrimary,
    surfaceVariant = GithubDarkSurfaceVariant,
    onSurfaceVariant = GithubDarkTextSecondary,
    outline = GithubDarkBorder,
    outlineVariant = GithubDarkBorder,
    error = GithubRed,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = GithubBlue,
    onPrimary = Color.White,
    primaryContainer = GithubBlueHover,
    onPrimaryContainer = GithubDarkBackground,
    secondary = GithubPurple,
    onSecondary = Color.White,
    tertiary = GithubOrange,
    background = GithubLightBackground,
    onBackground = GithubLightTextPrimary,
    surface = GithubLightSurface,
    onSurface = GithubLightTextPrimary,
    surfaceVariant = GithubLightSurfaceVariant,
    onSurfaceVariant = GithubLightTextSecondary,
    outline = GithubLightBorder,
    outlineVariant = GithubLightBorder,
    error = GithubRed,
    onError = Color.White
)

@Composable
fun GitSyncTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
