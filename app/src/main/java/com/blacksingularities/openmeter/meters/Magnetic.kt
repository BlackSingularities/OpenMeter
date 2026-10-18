package com.blacksingularities.openmeter.meters

import android.hardware.Sensor
import android.hardware.SensorManager
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blacksingularities.openmeter.core.LowPass
import com.blacksingularities.openmeter.core.Prefs
import com.blacksingularities.openmeter.core.Recorder
import com.blacksingularities.openmeter.core.Ring
import com.blacksingularities.openmeter.core.SensorEffect
import com.blacksingularities.openmeter.core.hasSensor
import com.blacksingularities.openmeter.core.rememberTicker
import com.blacksingularities.openmeter.ui.ArcGauge
import com.blacksingularities.openmeter.ui.LineChart
import com.blacksingularities.openmeter.ui.MeterScaffold
import com.blacksingularities.openmeter.ui.Notice
import com.blacksingularities.openmeter.ui.Palette
import com.blacksingularities.openmeter.ui.Panel
import com.blacksingularities.openmeter.ui.Pill
import com.blacksingularities.openmeter.ui.Readout
import com.blacksingularities.openmeter.ui.SectionLabel
import com.blacksingularities.openmeter.ui.Series
import com.blacksingularities.openmeter.ui.StatGrid
import com.blacksingularities.openmeter.ui.fmt
import com.blacksingularities.openmeter.ui.levelColor
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun MetalDetector(onBack: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    var field by remember { mutableFloatStateOf(Float.NaN) }
    var baseline by remember { mutableFloatStateOf(Float.NaN) }
    var accuracy by remember { mutableIntStateOf(SensorManager.SENSOR_STATUS_ACCURACY_HIGH) }
    val xyz = remember { FloatArray(3) }
    val smooth = remember { LowPass(0.3f) }
    val history = remember { Ring(300) }
    val ticker = rememberTicker()

    SensorEffect(Sensor.TYPE_MAGNETIC_FIELD, SensorManager.SENSOR_DELAY_GAME, onAccuracy = { accuracy = it }) { v, _ ->
        v.copyInto(xyz, 0, 0, 3)
        val m = smooth.filter(sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]))
        field = m
        if (baseline.isNaN() && history.size > 20) baseline = m
        history.add(m)
        ticker.tick()
        Recorder.log(Meter.Metal.id, v[0], v[1], v[2], m)
    }

    val delta = if (field.isNaN() || baseline.isNaN()) 0f else abs(field - baseline)
    val currentDelta by rememberUpdatedState(delta)

    // Pikanie jak w prawdziwym wykrywaczu: im silniejsze zaburzenie pola, tym szybciej.
    val tone = remember { runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, 70) }.getOrNull() }
    DisposableEffect(tone) { onDispose { tone?.release() } }
    LaunchedEffect(Unit) {
        while (true) {
            val d = currentDelta
            if (d > 6f) {
                if (Prefs.detectorSound) tone?.startTone(ToneGenerator.TONE_PROP_BEEP, 35)
                if (Prefs.haptics && d > 25f) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                delay((700 - d * 8).toLong().coerceIn(70, 700))
            } else {
                delay(120)
            }
        }
    }

    MeterScaffold(
        Meter.Metal.id,
        Meter.Metal.title,
        onBack,
        recordColumns = listOf("bx_uT", "by_uT", "bz_uT", "magnitude_uT"),
        recordInterval = 50,
        actions = {
            com.blacksingularities.openmeter.ui.RoundIcon(
                if (Prefs.detectorSound) Icons.Rounded.VolumeUp else Icons.Rounded.VolumeOff,
                "Dźwięk",
            ) { Prefs.setDetectorSound(!Prefs.detectorSound) }
        },
    ) {
        if (accuracy <= SensorManager.SENSOR_STATUS_ACCURACY_LOW) {
            Notice("Magnetometr wymaga kalibracji – zakreśl telefonem kilka razy ósemkę w powietrzu.")
        }
        Panel {
            val frac = (delta / 100f).coerceIn(0f, 1f)
            ArcGauge(frac, levelColor(frac)) {
                Readout(fmt(field, 1), "µT", if (delta > 6f) "Wykryto metal · Δ ${fmt(delta, 1)} µT" else "Brak zakłóceń · Δ ${fmt(delta, 1)} µT")
            }
            StatGrid(listOf("X" to fmt(xyz[0], 1), "Y" to fmt(xyz[1], 1), "Z" to fmt(xyz[2], 1), "Tło" to fmt(baseline, 1), "Ziemia" to "25–65", "Jednostka" to "µT"))
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill("Ustaw tło", icon = Icons.Rounded.MyLocation, active = true) { baseline = field }
            }
        }
        Panel {
            SectionLabel("Natężenie pola")
            LineChart(listOf(Series(history, Color(0xFF6FA8FF))), ticker, fill = true, minSpan = 10f)
        }
        Text(
            "Magnetometr znajduje się zwykle przy górnej krawędzi telefonu. Wykrywa przedmioty ferromagnetyczne (stal, żelazo), magnesy i przewody z prądem – nie złoto ani aluminium. Ustaw tło z dala od metalu i powoli przesuwaj telefon.",
            color = Palette.TextFaint,
            fontSize = 12.sp,
            lineHeight = 16.sp,
        )
    }
}

private val DIRS = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
private fun dirName(a: Float) = DIRS[(((a + 22.5f) % 360f) / 45f).toInt().coerceIn(0, 7)]

@Composable
fun CompassMeter(onBack: () -> Unit) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val useRotation = remember { context.hasSensor(Sensor.TYPE_ROTATION_VECTOR) }
    var azimuth by remember { mutableFloatStateOf(Float.NaN) }
    var pitch by remember { mutableFloatStateOf(0f) }
    var roll by remember { mutableFloatStateOf(0f) }
    var fieldStrength by remember { mutableFloatStateOf(Float.NaN) }
    var target by remember { mutableFloatStateOf(Float.NaN) }
    var accuracy by remember { mutableIntStateOf(SensorManager.SENSOR_STATUS_ACCURACY_HIGH) }
    var lastCardinal by remember { mutableIntStateOf(-1) }
    val rot = remember { FloatArray(9) }
    val orient = remember { FloatArray(3) }
    val accel = remember { FloatArray(3) }
    val mag = remember { FloatArray(3) }

    fun apply() {
        SensorManager.getOrientation(rot, orient)
        val a = ((Math.toDegrees(orient[0].toDouble()).toFloat() + 360f) % 360f)
        azimuth = if (azimuth.isNaN()) a else {
            // Wygładzanie po okręgu (bez skoku 359° → 0°).
            var d = a - azimuth
            if (d > 180) d -= 360f
            if (d < -180) d += 360f
            (azimuth + d * 0.2f + 360f) % 360f
        }
        pitch = Math.toDegrees(orient[1].toDouble()).toFloat()
        roll = Math.toDegrees(orient[2].toDouble()).toFloat()
        val card = ((azimuth + 45f) % 360f / 90f).toInt()
        if (card != lastCardinal && lastCardinal != -1 && Prefs.haptics) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        lastCardinal = card
        Recorder.log(Meter.Compass.id, azimuth, pitch, roll, fieldStrength)
    }

    if (useRotation) {
        SensorEffect(Sensor.TYPE_ROTATION_VECTOR, SensorManager.SENSOR_DELAY_GAME) { v, _ ->
            SensorManager.getRotationMatrixFromVector(rot, v)
            apply()
        }
    } else {
        SensorEffect(Sensor.TYPE_ACCELEROMETER, SensorManager.SENSOR_DELAY_GAME) { v, _ -> v.copyInto(accel, 0, 0, 3) }
    }
    SensorEffect(Sensor.TYPE_MAGNETIC_FIELD, SensorManager.SENSOR_DELAY_GAME, onAccuracy = { accuracy = it }) { v, _ ->
        v.copyInto(mag, 0, 0, 3)
        fieldStrength = sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2])
        if (!useRotation && SensorManager.getRotationMatrix(rot, null, accel, mag)) apply()
    }

    MeterScaffold(Meter.Compass.id, Meter.Compass.title, onBack, recordColumns = listOf("azimuth_deg", "pitch_deg", "roll_deg", "field_uT"), recordInterval = 100) {
        if (accuracy <= SensorManager.SENSOR_STATUS_ACCURACY_LOW) {
            Notice("Kompas jest niedokładny – zakreśl telefonem kilka razy ósemkę w powietrzu i odsuń go od metalu.")
        }
        Panel {
            CompassDial(if (azimuth.isNaN()) 0f else azimuth, target)
            Spacer(Modifier.height(8.dp))
            Readout(
                if (azimuth.isNaN()) "—" else "${azimuth.roundToInt()}°",
                if (azimuth.isNaN()) "" else dirName(azimuth),
                if (!target.isNaN() && !azimuth.isNaN()) {
                    var d = target - azimuth
                    if (d > 180) d -= 360f
                    if (d < -180) d += 360f
                    if (abs(d) < 3) "Na kursie" else "Namiar ${target.roundToInt()}° · obróć ${if (d > 0) "w prawo" else "w lewo"} o ${abs(d).roundToInt()}°"
                } else "Azymut magnetyczny",
            )
            Spacer(Modifier.height(12.dp))
            StatGrid(listOf("Pochylenie" to "${fmt(pitch, 0)}°", "Przechył" to "${fmt(roll, 0)}°", "Pole" to "${fmt(fieldStrength, 0)} µT"))
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill("Zapamiętaj namiar", icon = Icons.Rounded.MyLocation, active = !target.isNaN()) { target = azimuth }
                if (!target.isNaN()) Pill("Usuń", icon = Icons.Rounded.Close) { target = Float.NaN }
            }
        }
    }
}

@Composable
private fun CompassDial(azimuth: Float, target: Float) {
    Canvas(Modifier.fillMaxWidth().aspectRatio(1f)) {
        val c = center
        val r = size.minDimension / 2 - 8.dp.toPx()
        drawCircle(Palette.Card, r, c)
        // Znacznik kierunku telefonu na górze.
        val tri = Path().apply {
            moveTo(c.x, c.y - r - 6.dp.toPx())
            lineTo(c.x - 9.dp.toPx(), c.y - r + 10.dp.toPx())
            lineTo(c.x + 9.dp.toPx(), c.y - r + 10.dp.toPx())
            close()
        }
        drawPath(tri, Palette.Record)
        rotate(-azimuth, c) {
            for (d in 0 until 360 step 5) {
                val a = Math.toRadians(d - 90.0)
                val major = d % 30 == 0
                val len = if (d % 90 == 0) 22.dp.toPx() else if (major) 14.dp.toPx() else 7.dp.toPx()
                val ca = cos(a).toFloat()
                val sa = sin(a).toFloat()
                drawLine(
                    if (major) Color.White else Palette.TextFaint,
                    Offset(c.x + ca * (r - 4.dp.toPx()), c.y + sa * (r - 4.dp.toPx())),
                    Offset(c.x + ca * (r - 4.dp.toPx() - len), c.y + sa * (r - 4.dp.toPx() - len)),
                    if (major) 2.5f else 1.5f,
                )
            }
            val paint = android.graphics.Paint().apply {
                isAntiAlias = true
                textAlign = android.graphics.Paint.Align.CENTER
                textSize = 22.dp.toPx()
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }
            listOf("N" to 0, "E" to 90, "S" to 180, "W" to 270).forEach { (label, d) ->
                val a = Math.toRadians(d - 90.0)
                val rr = r - 48.dp.toPx()
                paint.color = (if (label == "N") Palette.Record else Color.White).toArgb()
                drawContext.canvas.nativeCanvas.save()
                drawContext.canvas.nativeCanvas.rotate(d.toFloat(), c.x + cos(a).toFloat() * rr, c.y + sin(a).toFloat() * rr)
                drawContext.canvas.nativeCanvas.drawText(label, c.x + cos(a).toFloat() * rr, c.y + sin(a).toFloat() * rr + 8.dp.toPx(), paint)
                drawContext.canvas.nativeCanvas.restore()
            }
            if (!target.isNaN()) {
                val a = Math.toRadians(target - 90.0)
                drawCircle(Palette.Ok, 7.dp.toPx(), Offset(c.x + cos(a).toFloat() * (r - 70.dp.toPx()), c.y + sin(a).toFloat() * (r - 70.dp.toPx())))
            }
        }
        // Igła.
        val needleN = Path().apply {
            moveTo(c.x, c.y - r * 0.42f); lineTo(c.x - 8.dp.toPx(), c.y); lineTo(c.x + 8.dp.toPx(), c.y); close()
        }
        val needleS = Path().apply {
            moveTo(c.x, c.y + r * 0.42f); lineTo(c.x - 8.dp.toPx(), c.y); lineTo(c.x + 8.dp.toPx(), c.y); close()
        }
        rotate(-azimuth, c) {
            drawPath(needleN, Palette.Record)
            drawPath(needleS, Palette.TextDim)
        }
        drawCircle(Color.White, 5.dp.toPx(), c)
    }
}
