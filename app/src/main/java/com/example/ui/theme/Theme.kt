package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldMedium,
    secondary = Teal80,
    tertiary = AmberAccent,
    background = SlateExtraDark,
    surface = SlateDark,
    onPrimary = SlateExtraDark,
    onSecondary = SlateExtraDark,
    onBackground = GrayBackground,
    onSurface = GrayBackground
)

private val LightColorScheme = lightColorScheme(
    primary = EmeraldDark,
    secondary = SlateSecondary,
    tertiary = AmberAccent,
    background = GrayBackground,
    surface = CardBackground,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = SlateDark,
    onSurface = SlateDark
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep colors predictable and customized across devices
    content: @Composable () -> Unit,
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
        typography = Typography,
        content = content
    )
}
