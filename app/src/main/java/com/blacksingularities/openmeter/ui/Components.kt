package com.blacksingularities.openmeter.ui

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.FiberManualRecord
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blacksingularities.openmeter.core.Prefs
import com.blacksingularities.openmeter.core.Recorder
import com.blacksingularities.openmeter.core.Ring
import com.blacksingularities.openmeter.core.Ticker
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max

val CardShape = RoundedCornerShape(24.dp)
val PillShape = RoundedCornerShape(50)

/** Ekran miernika: pasek z powrotem, tytułem i nagrywaniem, przewijana treść. */
@Composable
fun MeterScaffold(
    id: String,
    title: String,
    onBack: () -> Unit,
    recordColumns: List<String>? = null,
    recordInterval: Long = 50L,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    val context = LocalContext.current
    // Wyjście z miernika kończy jego nagrywanie (czujniki przestają wtedy działać).
    DisposableEffect(id) {
        onDispose {
            if (Recorder.isRecording(id)) {
                val f = Recorder.stop(context)
                if (f != null) Toast.makeText(context, "Zapisano ${f.name}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    Column(
        Modifier
            .fillMaxSize()
            .background(Palette.Ink)
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoundIcon(Icons.AutoMirrored.Rounded.ArrowBack, "Wstecz", onClick = onBack)
            Spacer(Modifier.width(12.dp))
            Text(
                title,
                color = Palette.Text,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            actions()
            if (recordColumns != null) {
                Spacer(Modifier.width(8.dp))
                RecordButton(id, title, recordColumns, recordInterval)
            }
        }
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

@Composable
private fun RecordButton(id: String, title: String, columns: List<String>, interval: Long) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val recording = Recorder.isRecording(id)
    var now by remember { mutableLongStateOf(0L) }
    LaunchedEffect(recording) {
        while (recording) {
            now = android.os.SystemClock.elapsedRealtime()
            delay(250)
        }
    }
    val bg by animateColorAsState(if (recording) Palette.Record else Palette.CardHigh, tween(200), label = "rec")
    val pulse = rememberInfiniteTransition(label = "pulse")
    val alpha by pulse.animateFloat(1f, 0.3f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "a")
    Row(
        Modifier
            .clip(PillShape)
            .background(bg)
            .clickable {
                if (Prefs.haptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                if (recording) {
                    val f = Recorder.stop(context)
                    Toast.makeText(
                        context,
                        if (f != null) "Zapisano ${f.name}\n(także w Pobrane/OpenMeter)" else "Brak danych do zapisania",
                        Toast.LENGTH_LONG,
                    ).show()
                } else {
                    Recorder.start(id, title, columns, interval)
                }
            }
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (recording) Icons.Rounded.Stop else Icons.Rounded.FiberManualRecord,
            null,
            tint = if (recording) Color.White else Palette.Record,
            modifier = Modifier.size(18.dp).let { if (recording) it else it },
        )
        Spacer(Modifier.width(6.dp))
        if (recording) {
            val s = ((now - Recorder.startedAt).coerceAtLeast(0) / 1000)
            Text(
                String.format(Locale.US, "%d:%02d", s / 60, s % 60),
                color = Color.White.copy(alpha = if (s % 2 == 0L) 1f else alpha.coerceAtLeast(0.6f)),
                style = Digits,
                fontSize = 14.sp,
            )
        } else {
            Text("REC", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

@Composable
fun RoundIcon(icon: ImageVector, description: String?, active: Boolean = false, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val bg by animateColorAsState(if (active) Color.White else Palette.CardHigh, tween(160), label = "ri")
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(bg)
            .clickable {
                if (Prefs.haptics) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, description, tint = if (active) Palette.Ink else Color.White, modifier = Modifier.size(22.dp))
    }
}

/** Płaska karta z delikatną obwódką dookoła. */
@Composable
fun Panel(modifier: Modifier = Modifier, padding: Dp = 16.dp, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(Palette.Card)
            .border(1.dp, Palette.Line, CardShape)
            .padding(padding),
        content = content,
    )
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        color = Palette.TextFaint,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.2.sp,
        modifier = modifier.padding(bottom = 8.dp),
    )
}

/** Duży odczyt: wartość + jednostka + opis. */
@Composable
fun Readout(
    value: String,
    unit: String,
    caption: String? = null,
    color: Color = Palette.Text,
    size: TextUnit = 64.sp,
    modifier: Modifier = Modifier,
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, color = color, style = Digits, fontSize = size, fontWeight = FontWeight.Light, maxLines = 1)
            if (unit.isNotEmpty()) {
                Spacer(Modifier.width(6.dp))
                Text(unit, color = Palette.TextDim, fontSize = (size.value * 0.3f).sp, modifier = Modifier.padding(bottom = (size.value * 0.16f).dp))
            }
        }
        if (caption != null) {
            Text(caption, color = Palette.TextDim, fontSize = 13.sp, textAlign = TextAlign.Center)
        }
    }
}

/** Siatka par „etykieta / wartość”. */
@Composable
fun StatGrid(items: List<Pair<String, String>>, columns: Int = 3) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items.chunked(columns).forEach { row ->
            Row(Modifier.fillMaxWidth()) {
                row.forEach { (label, value) ->
                    Column(Modifier.weight(1f)) {
                        Text(label, color = Palette.TextFaint, fontSize = 11.sp, maxLines = 1)
                        Text(value, color = Palette.Text, style = Digits, fontSize = 17.sp, maxLines = 1)
                    }
                }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/** Przełącznik segmentowy w kształcie pigułek. */
@Composable
fun Segmented(options: List<String>, selected: Int, modifier: Modifier = Modifier, onSelect: (Int) -> Unit) {
    val haptic = LocalHapticFeedback.current
    Row(
        modifier.clip(PillShape).background(Palette.CardHigh).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEachIndexed { i, label ->
            val active = i == selected
            val bg by animateColorAsState(if (active) Color.White else Color.Transparent, tween(160), label = "seg")
            Box(
                Modifier
                    .weight(1f)
                    .clip(PillShape)
                    .background(bg)
                    .clickable {
                        if (Prefs.haptics) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSelect(i)
                    }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, color = if (active) Palette.Ink else Palette.TextDim, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            }
        }
    }
}

/** Przycisk-pigułka z tekstem. */
@Composable
fun Pill(text: String, modifier: Modifier = Modifier, icon: ImageVector? = null, active: Boolean = false, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val bg by animateColorAsState(if (active) Color.White else Palette.CardHigh, tween(160), label = "pill")
    Row(
        modifier
            .clip(PillShape)
            .background(bg)
            .clickable {
                if (Prefs.haptics) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = if (active) Palette.Ink else Color.White, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(text, color = if (active) Palette.Ink else Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

/** Seria danych na wykresie liniowym. */
class Series(val ring: Ring, val color: Color, val label: String = "")

/**
 * Wykres liniowy z buforów cyklicznych. Zakres osi Y automatyczny (z zapasem) albo stały.
 * [ticker] wymusza przerysowanie po dopisaniu próbek.
 */
@Composable
fun LineChart(
    series: List<Series>,
    ticker: Ticker,
    modifier: Modifier = Modifier,
    height: Dp = 160.dp,
    minY: Float? = null,
    maxY: Float? = null,
    symmetric: Boolean = false,
    minSpan: Float = 1f,
    fill: Boolean = false,
    yLabel: (Float) -> String = { formatSmart(it) },
) {
    Column(modifier) {
        Box(Modifier.fillMaxWidth().height(height)) {
            val lo: Float
            val hi: Float
            @Suppress("UNUSED_VARIABLE") val t = ticker.state.intValue
            run {
                var a = Float.POSITIVE_INFINITY
                var b = Float.NEGATIVE_INFINITY
                for (s in series) if (s.ring.size > 0) {
                    a = minOf(a, s.ring.min()); b = maxOf(b, s.ring.max())
                }
                if (a == Float.POSITIVE_INFINITY) { a = 0f; b = 1f }
                if (symmetric) {
                    val m = maxOf(abs(a), abs(b), minSpan / 2)
                    a = -m; b = m
                } else if (b - a < minSpan) {
                    val c = (a + b) / 2
                    a = c - minSpan / 2; b = c + minSpan / 2
                }
                val pad = (b - a) * 0.08f
                lo = minY ?: (a - pad)
                hi = maxY ?: (b + pad)
            }
            Canvas(Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                for (i in 0..4) {
                    val y = h * i / 4f
                    drawLine(Palette.Grid, Offset(0f, y), Offset(w, y), 1f)
                }
                if (symmetric || (lo < 0 && hi > 0)) {
                    val y0 = h * (hi / (hi - lo))
                    drawLine(Palette.Line, Offset(0f, y0), Offset(w, y0), 1.5f)
                }
                for (s in series) {
                    val r = s.ring
                    if (r.size < 2) continue
                    val path = Path()
                    val step = w / (r.capacity - 1)
                    val x0 = w - (r.size - 1) * step
                    for (i in 0 until r.size) {
                        val x = x0 + i * step
                        val y = h - (r[i] - lo) / (hi - lo) * h
                        val yc = y.coerceIn(-2f, h + 2f)
                        if (i == 0) path.moveTo(x, yc) else path.lineTo(x, yc)
                    }
                    if (fill) {
                        val area = Path().apply {
                            addPath(path)
                            lineTo(w, h)
                            lineTo(x0, h)
                            close()
                        }
                        drawPath(area, s.color.copy(alpha = 0.12f))
                    }
                    drawPath(path, s.color, style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round))
                }
            }
            Text(yLabel(hi), color = Palette.TextFaint, fontSize = 10.sp, modifier = Modifier.align(Alignment.TopEnd))
            Text(yLabel(lo), color = Palette.TextFaint, fontSize = 10.sp, modifier = Modifier.align(Alignment.BottomEnd))
        }
        val labelled = series.filter { it.label.isNotEmpty() }
        if (labelled.isNotEmpty()) {
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                labelled.forEach { s ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(s.color))
                        Spacer(Modifier.width(5.dp))
                        Text(s.label, color = Palette.TextDim, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

/** Łukowa skala 240° z wypełnieniem [fraction] i treścią pośrodku. */
@Composable
fun ArcGauge(
    fraction: Float,
    color: Color,
    modifier: Modifier = Modifier,
    ticks: Int = 24,
    content: @Composable BoxScope.() -> Unit,
) {
    val f by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(180), label = "gauge")
    Box(modifier.fillMaxWidth().aspectRatio(1.25f), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().padding(8.dp)) {
            val stroke = 14.dp.toPx()
            val d = minOf(size.width, size.height * 1.2f) - stroke
            val topLeft = Offset((size.width - d) / 2, stroke / 2 + 4.dp.toPx())
            val arcSize = Size(d, d)
            drawArc(Palette.CardHigh, 150f, 240f, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            if (f > 0.001f) {
                drawArc(color, 150f, 240f * f, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            }
            val c = Offset(topLeft.x + d / 2, topLeft.y + d / 2)
            val rOut = d / 2 - stroke - 6.dp.toPx()
            for (i in 0..ticks) {
                val a = Math.toRadians(150.0 + 240.0 * i / ticks)
                val major = i % 4 == 0
                val rIn = rOut - (if (major) 10.dp.toPx() else 5.dp.toPx())
                val cos = kotlin.math.cos(a).toFloat()
                val sin = kotlin.math.sin(a).toFloat()
                drawLine(
                    if (major) Palette.TextDim else Palette.TextFaint,
                    Offset(c.x + cos * rIn, c.y + sin * rIn),
                    Offset(c.x + cos * rOut, c.y + sin * rOut),
                    strokeWidth = if (major) 2.dp.toPx() else 1.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }
        }
        content()
    }
}

/** Słupki widma (wartości 0..1). */
@Composable
fun Bars(values: FloatArray, ticker: Ticker, modifier: Modifier = Modifier, height: Dp = 140.dp, colorOf: (Float) -> Color = { Color.White }) {
    Canvas(modifier.fillMaxWidth().height(height)) {
        @Suppress("UNUSED_VARIABLE") val t = ticker.state.intValue
        val n = values.size
        if (n == 0) return@Canvas
        val gap = 2.dp.toPx()
        val bw = (size.width - gap * (n - 1)) / n
        for (i in 0 until n) {
            val v = values[i].coerceIn(0f, 1f)
            val bh = max(2.dp.toPx(), v * size.height)
            drawRoundRect(
                colorOf(v),
                Offset(i * (bw + gap), size.height - bh),
                Size(bw, bh),
                androidx.compose.ui.geometry.CornerRadius(bw / 2, bw / 2),
            )
        }
    }
}

/** Ostrzeżenie o braku czujnika lub uprawnienia. */
@Composable
fun Notice(text: String, action: String? = null, onAction: () -> Unit = {}) {
    Panel {
        Text(text, color = Palette.TextDim, fontSize = 14.sp, lineHeight = 20.sp)
        if (action != null) {
            Spacer(Modifier.height(12.dp))
            Pill(action, active = true, onClick = onAction)
        }
    }
}

fun formatSmart(v: Float): String = when {
    v.isNaN() -> "—"
    abs(v) >= 1000 -> String.format(Locale.US, "%.0f", v)
    abs(v) >= 100 -> String.format(Locale.US, "%.1f", v)
    abs(v) >= 10 -> String.format(Locale.US, "%.1f", v)
    abs(v) >= 1 -> String.format(Locale.US, "%.2f", v)
    else -> String.format(Locale.US, "%.3f", v)
}

fun fmt(v: Float, digits: Int): String = if (v.isNaN() || v.isInfinite()) "—" else String.format(Locale.US, "%.${digits}f", v)
fun fmt(v: Double, digits: Int): String = fmt(v.toFloat(), digits)
