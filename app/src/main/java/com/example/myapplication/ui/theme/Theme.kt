package com.example.myapplication.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = NavyPrimary80,
    secondary = Navy80,
    tertiary = SkyAccent80
)

private val LightColorScheme = lightColorScheme(
    primary = RoyalBlue,
    onPrimary = Color.White,
    primaryContainer = PaleMint,
    onPrimaryContainer = DeepIndigo,
    secondary = SkyBlue,
    onSecondary = DeepIndigo,
    secondaryContainer = PaleMint,
    tertiary = DeepIndigo,
    onTertiary = Color.White,
    tertiaryContainer = DeepIndigo,
    background = Color(0xFFF6FAFD),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = PaleMint,
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
