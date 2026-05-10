package com.example.prathamchikitse.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// --- REMOVED THE DUPLICATE VAL DEFINITIONS FROM HERE ---

private val DarkColorScheme = darkColorScheme(
    primary = MedicalRed,
    secondary = EmergencyBlue,
    tertiary = SuccessGreen,
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E)
)

private val LightColorScheme = lightColorScheme(
    primary = MedicalRed,
    secondary = EmergencyBlue,
    tertiary = SuccessGreen,
    background = LightGrey,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White
)

@Composable
fun PrathamChikitseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        val window = (view.context as Activity).window
        window.statusBarColor = colorScheme.primary.toArgb()
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}