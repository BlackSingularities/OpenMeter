package com.blacksingularities.openmeter.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Interfejs AMOLED jak w OpenPDF: czysta czerń, biały akcent, czerwień dla nagrywania. */
object Palette {
    val Ink = Color(0xFF000000)
    val Card = Color(0xFF0E0E0E)
    val CardHigh = Color(0xFF1C1C1C)
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
}

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

@Composable
fun OpenMeterTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, typography = Typography(), content = content)
}

/** Kolor wartości na skali 0..1 (zielony → żółty → czerwony). */
fun levelColor(t: Float): Color = when {
    t < 0.6f -> Palette.Ok
    t < 0.85f -> Palette.Warn
    else -> Palette.Danger
}
