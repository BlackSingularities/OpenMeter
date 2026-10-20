package com.blacksingularities.openmeter.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Interfejs AMOLED: czysta czerń, kolor akcentu zależny od kategorii miernika, czerwień dla nagrywania. */
object Palette {
    val Ink = Color(0xFF000000)
    val Card = Color(0xFF0D0D10)
    val CardHigh = Color(0xFF1B1B21)
    val CardTop = Color(0xFF26262E)
    val Line = Color(0x1FFFFFFF)
    val Grid = Color(0x14FFFFFF)
    val Text = Color(0xFFFFFFFF)
    val TextDim = Color(0xB3FFFFFF)
    val TextFaint = Color(0x66FFFFFF)
    val Record = Color(0xFFFF3B47)

    /** Kolory serii na wykresach: osie X / Y / Z i wypadkowa. */
    val AxisX = Color(0xFFFF6B6B)
    val AxisY = Color(0xFF5CE1A6)
    val AxisZ = Color(0xFF6FA8FF)
    val Accent = Color(0xFFFFFFFF)

    /** Skala ostrzegawcza: spokojnie → ostrzeżenie → niebezpiecznie. */
    val Ok = Color(0xFF5CE1A6)
    val Warn = Color(0xFFFFC34D)
    val Danger = Color(0xFFFF3B47)

    /** Akcenty kategorii mierników. */
    val Amber = Color(0xFFFFB547)
    val Violet = Color(0xFFB69CFF)
    val Cyan = Color(0xFF4FD8EB)
    val Rose = Color(0xFFFF7EB6)
    val Mint = Color(0xFF5CE1A6)
    val Sky = Color(0xFF6FA8FF)
    val Silver = Color(0xFFC9CCD6)
}

/** Kolor akcentu bieżącego ekranu (kategoria miernika); domyślnie biel. */
val LocalAccent = compositionLocalOf { Color.White }

private val scheme = darkColorScheme(
    primary = Color.White,
    onPrimary = Palette.Ink,
    secondary = Color(0xFFE0E0E0),
    onSecondary = Palette.Ink,
    background = Palette.Ink,
    onBackground = Color.White,
    surface = Palette.Ink,
    onSurface = Color.White,
    surfaceVariant = Palette.CardHigh,
    onSurfaceVariant = Palette.TextDim,
    surfaceContainerLow = Palette.Card,
    surfaceContainer = Palette.Card,
    surfaceContainerHigh = Palette.CardHigh,
    surfaceContainerHighest = Palette.CardHigh,
    outline = Color(0x33FFFFFF),
    error = Palette.Record,
)

val Digits = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium)

private val base = Typography()
private val typography = Typography(
    headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.2).sp),
)

@Composable
fun OpenMeterTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, typography = typography, content = content)
}

/** Kolor wartości na skali 0..1 (zielony → żółty → czerwony). */
fun levelColor(t: Float): Color = when {
    t < 0.6f -> Palette.Ok
    t < 0.85f -> Palette.Warn
    else -> Palette.Danger
}
