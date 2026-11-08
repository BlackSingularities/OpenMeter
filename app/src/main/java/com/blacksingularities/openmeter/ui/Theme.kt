package com.blacksingularities.openmeter.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.blacksingularities.openmeter.core.Prefs
import com.blacksingularities.openmeter.core.ThemeMode

/** Bieżący tryb jasny/ciemny. Paleta czyta go w getterach, więc zmiana przerysowuje każdy ekran. */
object ThemeState {
    var dark by mutableStateOf(true)
}

/**
 * Czarno-biały interfejs: treść w kolorze tekstu (biel w ciemnym, czerń w jasnym), płaskie
 * tła bez obwódek. Kolor tylko tam, gdzie niesie znaczenie: nagrywanie, skala ostrzegawcza,
 * osie wykresów.
 */
object Palette {
    private val dark: Boolean get() = ThemeState.dark

    /** Tło aplikacji (a zarazem kolor treści na aktywnym, białym elemencie). */
    val Ink: Color get() = if (dark) Color.Black else Color.White
    /** Kolor treści: tekst i ikony. */
    val Fg: Color get() = if (dark) Color.White else Color(0xFF111111)
    val Text: Color get() = Fg
    /** Karta na tle aplikacji. */
    val Card: Color get() = if (dark) Color(0xFF101010) else Color(0xFFF3F3F3)
    /** Przyciski, pola, tory suwaków. */
    val CardHigh: Color get() = if (dark) Color(0xFF1C1C1C) else Color(0xFFE6E6E6)
    val Line: Color get() = Fg.copy(alpha = 0.12f)
    val Grid: Color get() = Fg.copy(alpha = 0.08f)
    val TextDim: Color get() = Fg.copy(alpha = if (dark) 0.70f else 0.66f)
    val TextFaint: Color get() = Fg.copy(alpha = 0.42f)

    val Record = Color(0xFFFF3B47)

    /** Osie X / Y / Z na wykresach. */
    val AxisX: Color get() = if (dark) Color(0xFFFF6B6B) else Color(0xFFD83A3A)
    val AxisY: Color get() = if (dark) Color(0xFF5CE1A6) else Color(0xFF14915A)
    val AxisZ: Color get() = if (dark) Color(0xFF6FA8FF) else Color(0xFF2F6FD6)

    /** Skala ostrzegawcza: spokojnie → ostrzeżenie → niebezpiecznie. */
    val Ok: Color get() = if (dark) Color(0xFF5CE1A6) else Color(0xFF14915A)
    val Warn: Color get() = if (dark) Color(0xFFFFC34D) else Color(0xFFC78300)
    val Danger = Color(0xFFFF3B47)
}

/** Kolor akcentu bieżącego ekranu – w czarno-białym interfejsie kolor treści. */
val LocalAccent = compositionLocalOf { Color.White }

private fun scheme(dark: Boolean) = if (dark) {
    darkColorScheme(
        primary = Color.White, onPrimary = Color.Black,
        secondary = Color(0xFFE0E0E0), onSecondary = Color.Black,
        background = Color.Black, onBackground = Color.White,
        surface = Color.Black, onSurface = Color.White,
        surfaceVariant = Color(0xFF1C1C1C), onSurfaceVariant = Color(0xB3FFFFFF),
        surfaceContainerLow = Color(0xFF101010), surfaceContainer = Color(0xFF141414),
        surfaceContainerHigh = Color(0xFF1C1C1C), surfaceContainerHighest = Color(0xFF242424),
        outline = Color(0x33FFFFFF), error = Palette.Record,
    )
} else {
    lightColorScheme(
        primary = Color(0xFF111111), onPrimary = Color.White,
        secondary = Color(0xFF333333), onSecondary = Color.White,
        background = Color.White, onBackground = Color(0xFF111111),
        surface = Color.White, onSurface = Color(0xFF111111),
        surfaceVariant = Color(0xFFEFEFEF), onSurfaceVariant = Color(0xA8111111),
        surfaceContainerLow = Color(0xFFF7F7F7), surfaceContainer = Color(0xFFF3F3F3),
        surfaceContainerHigh = Color(0xFFEFEFEF), surfaceContainerHighest = Color(0xFFE8E8E8),
        outline = Color(0x33111111), error = Palette.Record,
    )
}

val Digits = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium)

private val base = Typography()
private val typography = Typography(
    headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.2).sp),
)

/** Czy bieżące ustawienie daje ciemny wygląd. */
@Composable
fun isDarkTheme(): Boolean = when (Prefs.theme) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.DARK -> true
    ThemeMode.LIGHT -> false
}

@Composable
fun OpenMeterTheme(content: @Composable () -> Unit) {
    val dark = isDarkTheme()
    if (ThemeState.dark != dark) ThemeState.dark = dark
    MaterialTheme(colorScheme = scheme(dark), typography = typography, content = content)
}

/** Kolor wartości na skali 0..1 (zielony → żółty → czerwony). */
fun levelColor(t: Float): Color = when {
    t < 0.6f -> Palette.Ok
    t < 0.85f -> Palette.Warn
    else -> Palette.Danger
}
