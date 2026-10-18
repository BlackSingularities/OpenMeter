package com.blacksingularities.openmeter.meters

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blacksingularities.openmeter.core.Prefs
import com.blacksingularities.openmeter.core.Recorder
import com.blacksingularities.openmeter.core.SensorEffect
import com.blacksingularities.openmeter.core.hasSensor
import com.blacksingularities.openmeter.ui.MeterScaffold
import com.blacksingularities.openmeter.ui.Palette
import com.blacksingularities.openmeter.ui.Panel
import com.blacksingularities.openmeter.ui.Pill
import com.blacksingularities.openmeter.ui.Readout
import com.blacksingularities.openmeter.ui.SectionLabel
import com.blacksingularities.openmeter.ui.Segmented
import com.blacksingularities.openmeter.ui.StatGrid
import com.blacksingularities.openmeter.ui.fmt
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/** Wektor grawitacji: czujnik grawitacji, a gdy go brak – wygładzony akcelerometr. */
private class GravityState {
    val g = FloatArray(3)
    var ready = false
    fun update(v: FloatArray, filtered: Boolean) {
        if (!ready || !filtered) {
            v.copyInto(g, 0, 0, 3); ready = true
        } else {
            for (i in 0..2) g[i] += 0.15f * (v[i] - g[i])
        }
    }
}

private fun Context.gravityType() = if (hasSensor(Sensor.TYPE_GRAVITY)) Sensor.TYPE_GRAVITY else Sensor.TYPE_ACCELEROMETER

private fun deg(r: Float) = Math.toDegrees(r.toDouble()).toFloat()

@Composable
fun LevelMeter(onBack: () -> Unit) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val type = remember { context.gravityType() }
    val grav = remember { GravityState() }
    var tx by remember { mutableFloatStateOf(0f) }
    var ty by remember { mutableFloatStateOf(0f) }
    var edge by remember { mutableFloatStateOf(0f) }
    var zeroX by remember { mutableFloatStateOf(0f) }
    var zeroY by remember { mutableFloatStateOf(0f) }
    var zeroEdge by remember { mutableFloatStateOf(0f) }
    var mode by remember { mutableIntStateOf(0) }
    var wasLevel by remember { mutableStateOf(false) }

    SensorEffect(type, SensorManager.SENSOR_DELAY_GAME) { v, _ ->
        grav.update(v, type == Sensor.TYPE_ACCELEROMETER)
        val (x, y, z) = grav.g
        tx = deg(atan2(x, sqrt(y * y + z * z))) - zeroX
        ty = deg(atan2(y, sqrt(x * x + z * z))) - zeroY
        val a = deg(atan2(x, y))
        edge = (a - (a / 90f).roundToInt() * 90f) - zeroEdge
        val level = if (mode == 0) abs(tx) < 0.2f && abs(ty) < 0.2f else abs(edge) < 0.2f
        if (level && !wasLevel && Prefs.haptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        wasLevel = level
        Recorder.log(Meter.Level.id, tx, ty, edge)
    }

    MeterScaffold(Meter.Level.id, Meter.Level.title, onBack, recordColumns = listOf("tilt_x_deg", "tilt_y_deg", "edge_deg"), recordInterval = 100) {
        Segmented(listOf("Na płasko", "Na krawędzi"), mode) { mode = it }
        Panel {
            val levelColor = if (wasLevel) Palette.Ok else Color.White
            if (mode == 0) {
                BubbleLevel(tx, ty, levelColor)
                Spacer(Modifier.height(16.dp))
                StatGrid(listOf("Oś X" to "${fmt(tx, 1)}°", "Oś Y" to "${fmt(ty, 1)}°", "Razem" to "${fmt(sqrt(tx * tx + ty * ty), 1)}°"))
            } else {
                Readout("${fmt(edge, 1)}°", "", if (abs(edge) < 0.2f) "Poziomo" else "Przechyl, aż pęcherzyk będzie na środku", color = levelColor)
                Spacer(Modifier.height(16.dp))
                TubeLevel(edge, levelColor)
                Spacer(Modifier.height(12.dp))
                val pct = Math.tan(Math.toRadians(edge.toDouble())) * 100
                StatGrid(listOf("Spadek" to "${fmt(pct, 1)} %", "mm na metr" to fmt(pct * 10, 0), "Stopnie" to fmt(edge, 2)))
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill("Kalibruj tutaj", icon = Icons.Rounded.MyLocation) {
                    zeroX += tx; zeroY += ty; zeroEdge += edge
                }
                Pill("Reset", icon = Icons.Rounded.RestartAlt) { zeroX = 0f; zeroY = 0f; zeroEdge = 0f }
            }
        }
        Text(
            "Połóż telefon ekranem do góry na badanej powierzchni. „Kalibruj tutaj” na idealnie płaskim blacie usuwa krzywiznę obudowy i aparatu.",
            color = Palette.TextFaint,
            fontSize = 12.sp,
            lineHeight = 16.sp,
        )
    }
}

@Composable
private fun BubbleLevel(tx: Float, ty: Float, color: Color) {
    Canvas(Modifier.fillMaxWidth().aspectRatio(1f)) {
        val c = center
        val r = size.minDimension / 2 - 4.dp.toPx()
        val full = 10f // stopni do krawędzi
        drawCircle(Palette.CardHigh, r, c)
        for (d in listOf(1f, 2f, 5f)) drawCircle(Palette.Line, r * d / full, c, style = Stroke(1.dp.toPx()))
        drawCircle(Palette.TextFaint, r, c, style = Stroke(1.5.dp.toPx()))
        drawLine(Palette.Line, Offset(c.x - r, c.y), Offset(c.x + r, c.y), 1.dp.toPx())
        drawLine(Palette.Line, Offset(c.x, c.y - r), Offset(c.x, c.y + r), 1.dp.toPx())
        var bx = tx / full * r
        var by = -ty / full * r
        val m = sqrt(bx * bx + by * by)
        val maxR = r - 26.dp.toPx()
        if (m > maxR) { bx *= maxR / m; by *= maxR / m }
        val p = Offset(c.x + bx, c.y + by)
        drawCircle(color.copy(alpha = 0.18f), 34.dp.toPx(), p)
        drawCircle(color, 24.dp.toPx(), p)
        drawCircle(Palette.Ink, 24.dp.toPx() * 0.3f, p)
        drawCircle(color, r * 1f / full + 24.dp.toPx(), c, style = Stroke(1.5.dp.toPx()), alpha = 0.6f)
    }
}

@Composable
private fun TubeLevel(angle: Float, color: Color) {
    Canvas(Modifier.fillMaxWidth().height(64.dp)) {
        val h = size.height
        val w = size.width
        drawRoundRect(Palette.CardHigh, Offset.Zero, Size(w, h), androidx.compose.ui.geometry.CornerRadius(h / 2))
        val mid = w / 2
        for (s in listOf(-1, 1)) drawLine(Palette.TextFaint, Offset(mid + s * 34.dp.toPx(), 8f), Offset(mid + s * 34.dp.toPx(), h - 8f), 2.dp.toPx())
        val off = (angle / 10f).coerceIn(-1f, 1f) * (w / 2 - h / 2 - 4.dp.toPx())
        drawRoundRect(color, Offset(mid + off - 28.dp.toPx(), 10.dp.toPx()), Size(56.dp.toPx(), h - 20.dp.toPx()), androidx.compose.ui.geometry.CornerRadius(h / 2))
    }
}

@Composable
fun ProtractorMeter(onBack: () -> Unit) {
    val context = LocalContext.current
    val type = remember { context.gravityType() }
    val grav = remember { GravityState() }
    var mode by remember { mutableIntStateOf(0) }
    var angle by remember { mutableFloatStateOf(0f) }
    var zero by remember { mutableFloatStateOf(0f) }
    var hold by remember { mutableStateOf(false) }

    SensorEffect(type, SensorManager.SENSOR_DELAY_GAME) { v, _ ->
        if (hold) return@SensorEffect
        grav.update(v, type == Sensor.TYPE_ACCELEROMETER)
        val (x, y, z) = grav.g
        val gNorm = sqrt(x * x + y * y + z * z).coerceAtLeast(0.01f)
        val raw = if (mode == 0) {
            // Obrót telefonu w płaszczyźnie ekranu: 0° = pionowo.
            (deg(atan2(x, y)) + 360f) % 360f
        } else {
            // Nachylenie ekranu względem poziomu: 0° = płasko, 90° = pionowo.
            deg(acos((z / gNorm).coerceIn(-1f, 1f)))
        }
        var a = raw - zero
        if (mode == 0) a = ((a + 540f) % 360f) - 180f
        angle = a
        Recorder.log(Meter.Protractor.id, if (mode == 0) "edge" else "tilt", a)
    }

    MeterScaffold(Meter.Protractor.id, Meter.Protractor.title, onBack, recordColumns = listOf("mode", "angle_deg"), recordInterval = 100) {
        Segmented(listOf("Krawędź", "Nachylenie"), mode) { mode = it; zero = 0f }
        Panel {
            Dial(angle, mode == 1)
            Readout("${fmt(abs(angle), 1)}°", "", if (mode == 0) "Przyłóż dłuższą krawędź telefonu do mierzonej powierzchni" else "Połóż telefon na mierzonej płaszczyźnie")
            Spacer(Modifier.height(10.dp))
            StatGrid(
                listOf(
                    "Dopełnienie" to "${fmt(90 - abs(angle) % 90, 1)}°",
                    "Spadek" to "${fmt(Math.tan(Math.toRadians(abs(angle).toDouble()).coerceAtMost(1.55)) * 100, 0)} %",
                    "Radiany" to fmt(Math.toRadians(angle.toDouble()), 3),
                ),
            )
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill(if (hold) "Puść" else "Zatrzymaj", icon = if (hold) Icons.Rounded.LockOpen else Icons.Rounded.Lock, active = hold) { hold = !hold }
                Pill("Zero tutaj", icon = Icons.Rounded.MyLocation) { zero += angle }
                Pill("Reset", icon = Icons.Rounded.RestartAlt) { zero = 0f }
            }
        }
    }
}

/** Tarcza kątomierza z obracającą się wskazówką. */
@Composable
private fun Dial(angle: Float, half: Boolean) {
    Canvas(Modifier.fillMaxWidth().aspectRatio(1.6f)) {
        val c = Offset(size.width / 2, size.height - 12.dp.toPx())
        val r = minOf(size.width / 2, size.height) - 16.dp.toPx()
        drawArc(Palette.CardHigh, 180f, 180f, true, Offset(c.x - r, c.y - r), Size(2 * r, 2 * r))
        for (d in 0..180 step 5) {
            val a = Math.toRadians(180.0 + d)
            val major = d % 30 == 0
            val mid = d % 10 == 0
            val len = if (major) 18.dp.toPx() else if (mid) 11.dp.toPx() else 6.dp.toPx()
            val ca = cos(a).toFloat()
            val sa = sin(a).toFloat()
            drawLine(
                if (major) Color.White else Palette.TextFaint,
                Offset(c.x + ca * r, c.y + sa * r),
                Offset(c.x + ca * (r - len), c.y + sa * (r - len)),
                if (major) 2.5f else 1.5f,
            )
        }
        // Wskazówka: od pionu (0°) w lewo lub w prawo.
        val shown = if (half) angle else angle
        rotate(shown.coerceIn(-90f, 90f) + 0f, pivot = c) {
            drawLine(Color.White, c, Offset(c.x, c.y - r + 6.dp.toPx()), 4.dp.toPx(), cap = StrokeCap.Round)
        }
        drawLine(Palette.TextFaint, c, Offset(c.x, c.y - r), 1.dp.toPx())
        val sweep = shown.coerceIn(-90f, 90f)
        drawArc(Palette.Ok.copy(alpha = 0.35f), 270f, sweep, true, Offset(c.x - r * 0.45f, c.y - r * 0.45f), Size(r * 0.9f, r * 0.9f))
        drawCircle(Color.White, 7.dp.toPx(), c)
    }
}

@Composable
fun HingeMeter(onBack: () -> Unit) {
    var angle by remember { mutableFloatStateOf(Float.NaN) }
    SensorEffect(Sensor.TYPE_HINGE_ANGLE, SensorManager.SENSOR_DELAY_UI) { v, _ ->
        angle = v[0]
        Recorder.log(Meter.Hinge.id, v[0])
    }
    MeterScaffold(Meter.Hinge.id, Meter.Hinge.title, onBack, recordColumns = listOf("hinge_deg"), recordInterval = 100) {
        Panel {
            SectionLabel("Rozłożenie")
            Canvas(Modifier.fillMaxWidth().aspectRatio(1.6f)) {
                val c = Offset(size.width / 2, size.height - 10.dp.toPx())
                val len = size.height - 30.dp.toPx()
                val a = if (angle.isNaN()) 0f else angle
                drawLine(Palette.TextDim, c, Offset(c.x - len, c.y), 8.dp.toPx(), cap = StrokeCap.Round)
                val rad = Math.toRadians((180.0 - a)).toFloat()
                drawLine(Color.White, c, Offset(c.x - len * cos(rad), c.y - len * sin(rad)), 8.dp.toPx(), cap = StrokeCap.Round)
                drawArc(Palette.Ok.copy(alpha = 0.3f), 180f, a, true, Offset(c.x - len * 0.4f, c.y - len * 0.4f), Size(len * 0.8f, len * 0.8f))
            }
            Readout("${fmt(angle, 0)}°", "", when {
                angle.isNaN() -> "Czekam na odczyt…"
                angle < 15 -> "Złożony"
                angle < 160 -> "Tryb namiotu / laptopa"
                else -> "Rozłożony"
            })
        }
    }
}
