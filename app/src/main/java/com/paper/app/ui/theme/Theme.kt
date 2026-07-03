package com.paper.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Minimalist, paper-like: near-black ink on warm off-white, no accent noise.
private val Ink = Color(0xFF1A1A1A)
private val PaperWhite = Color(0xFFFAF8F5)
private val FaintLine = Color(0xFFE5E1DB)
private val SoftGray = Color(0xFF8A857E)

private val NightInk = Color(0xFFE8E6E3)
private val NightPaper = Color(0xFF141414)
private val NightLine = Color(0xFF2A2A2A)

private val LightColors = lightColorScheme(
    primary = Ink,
    onPrimary = PaperWhite,
    background = PaperWhite,
    onBackground = Ink,
    surface = PaperWhite,
    onSurface = Ink,
    surfaceVariant = FaintLine,
    onSurfaceVariant = SoftGray,
    outline = FaintLine
)

private val DarkColors = darkColorScheme(
    primary = NightInk,
    onPrimary = NightPaper,
    background = NightPaper,
    onBackground = NightInk,
    surface = NightPaper,
    onSurface = NightInk,
    surfaceVariant = NightLine,
    onSurfaceVariant = SoftGray,
    outline = NightLine
)

private val PaperTypography = Typography(
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Normal,
        fontSize = 28.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 16.sp,
        lineHeight = 26.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 14.sp,
        lineHeight = 22.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        letterSpacing = 0.5.sp
    )
)

@Composable
fun PaperTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = PaperTypography,
        content = content
    )
}
