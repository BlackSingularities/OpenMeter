package com.blacksingularities.openmeter.meters

import android.hardware.Sensor
import android.hardware.SensorManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blacksingularities.openmeter.core.Fft
import com.blacksingularities.openmeter.core.Recorder
import com.blacksingularities.openmeter.core.Ring
import com.blacksingularities.openmeter.core.SensorEffect
import com.blacksingularities.openmeter.core.hasSensor
import com.blacksingularities.openmeter.core.peakBin
import com.blacksingularities.openmeter.core.rememberTicker
import com.blacksingularities.openmeter.ui.ArcGauge
import com.blacksingularities.openmeter.ui.Bars
import com.blacksingularities.openmeter.ui.LineChart
import com.blacksingularities.openmeter.ui.MeterScaffold
import com.blacksingularities.openmeter.ui.Palette
import com.blacksingularities.openmeter.ui.Panel
import com.blacksingularities.openmeter.ui.Pill
import com.blacksingularities.openmeter.ui.Readout
import com.blacksingularities.openmeter.ui.SectionLabel
import com.blacksingularities.openmeter.ui.Segmented
import com.blacksingularities.openmeter.ui.Series
import com.blacksingularities.openmeter.ui.StatGrid
import com.blacksingularities.openmeter.ui.fmt
import com.blacksingularities.openmeter.ui.levelColor
import kotlin.math.log10
import kotlin.math.sqrt

/**
 * Przyspieszenie liniowe (bez grawitacji) i kierunek grawitacji. Gdy telefon nie ma
 * czujnika przyspieszenia liniowego, grawitację wyznacza filtr dolnoprzepustowy.
 */
private class MotionState {
    val gravity = floatArrayOf(0f, 0f, 9.81f)
    val lin = FloatArray(3)
    private var init = false

    fun fromAccelerometer(v: FloatArray) {
        if (!init) { v.copyInto(gravity, 0, 0, 3); init = true }
        for (i in 0..2) {
            gravity[i] += 0.02f * (v[i] - gravity[i])
            lin[i] = v[i] - gravity[i]
        }
    }

    fun fromLinear(v: FloatArray) = v.copyInto(lin, 0, 0, 3)
    fun fromGravity(v: FloatArray) = v.copyInto(gravity, 0, 0, 3)

    /** Składowa pionowa (wzdłuż grawitacji) i pozioma przyspieszenia. */
    fun split(): Pair<Float, Float> {
        val gn = sqrt(gravity[0] * gravity[0] + gravity[1] * gravity[1] + gravity[2] * gravity[2]).coerceAtLeast(0.1f)
        val v = (lin[0] * gravity[0] + lin[1] * gravity[1] + lin[2] * gravity[2]) / gn
        val total = lin[0] * lin[0] + lin[1] * lin[1] + lin[2] * lin[2]
        return v to sqrt((total - v * v).coerceAtLeast(0f))
    }
}

@Composable
private fun MotionSensors(state: MotionState, onSample: (Long) -> Unit) {
    val context = LocalContext.current
    val hasLinear = remember { context.hasSensor(Sensor.TYPE_LINEAR_ACCELERATION) }
    val hasGravity = remember { context.hasSensor(Sensor.TYPE_GRAVITY) }
    if (hasLinear) {
        if (hasGravity) SensorEffect(Sensor.TYPE_GRAVITY, SensorManager.SENSOR_DELAY_GAME) { v, _ -> state.fromGravity(v) }
        SensorEffect(Sensor.TYPE_LINEAR_ACCELERATION, SensorManager.SENSOR_DELAY_FASTEST) { v, t ->
            state.fromLinear(v)
            onSample(t)
        }
    } else {
        SensorEffect(Sensor.TYPE_ACCELEROMETER, SensorManager.SENSOR_DELAY_FASTEST) { v, t ->
            state.fromAccelerometer(v)
            onSample(t)
        }
    }
}

private val ROMAN = listOf("I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X")
private val MERCALLI = listOf(
    "Nieodczuwalne", "Ledwo odczuwalne", "Słabe", "Lekkie", "Umiarkowane",
    "Silne", "Bardzo silne", "Niszczące", "Gwałtowne", "Ekstremalne",
)

/** Intensywność instrumentalna (Wald i in., 1999) z PGA w m/s². */
private fun mercalli(pga: Float): Int {
    val cms2 = pga * 100f
    if (cms2 < 1.7f) return 1
    val i = if (cms2 < 14f) 2.0 + (log10(cms2 / 1.7) / log10(14.0 / 1.7)) * 2.0 else 3.66 * log10(cms2.toDouble()) - 1.66
    return i.toInt().coerceIn(1, 10)
}

@Composable
fun Seismograph(onBack: () -> Unit) {
    val state = remember { MotionState() }
    val rx = remember { Ring(600) }
    val ry = remember { Ring(600) }
    val rz = remember { Ring(600) }
    val ticker = rememberTicker()
    var mag by remember { mutableFloatStateOf(0f) }
    var peak by remember { mutableFloatStateOf(0f) }
    var events by remember { mutableIntStateOf(0) }
    var lastEvent by remember { mutableLongStateOf(0L) }
    var scale by remember { mutableIntStateOf(0) }
    var rate by remember { mutableFloatStateOf(0f) }
    val times = remember { LongArray(2) }

    MotionSensors(state) { t ->
        val (x, y, z) = state.lin
        rx.add(x); ry.add(y); rz.add(z)
        val m = sqrt(x * x + y * y + z * z)
        mag = mag * 0.9f + m * 0.1f
        if (m > peak) peak = m
        if (m > 0.3f && t - lastEvent > 1_000_000_000L) {
            events++
            lastEvent = t
        }
        if (times[0] == 0L) times[0] = t
        times[1]++
        if (t - times[0] > 1_000_000_000L) {
            rate = times[1] * 1e9f / (t - times[0])
            times[0] = t
            times[1] = 0
        }
        ticker.tick()
        Recorder.log(Meter.Seismo.id, x, y, z, m)
    }

    val span = listOf(null, 0.2f, 2f, 20f)[scale]
    MeterScaffold(Meter.Seismo.id, Meter.Seismo.title, onBack, recordColumns = listOf("ax", "ay", "az", "magnitude_ms2"), recordInterval = 0) {
        Panel {
            SectionLabel("Przebieg · m/s²")
            LineChart(
                listOf(Series(rx, Palette.AxisX, "X"), Series(ry, Palette.AxisY, "Y"), Series(rz, Palette.AxisZ, "Z")),
                ticker,
                height = 240.dp,
                symmetric = true,
                minSpan = 0.02f,
                minY = span?.let { -it },
                maxY = span,
            )
            Spacer(Modifier.height(12.dp))
            Segmented(listOf("Auto", "±0,2", "±2", "±20"), scale) { scale = it }
        }
        Panel {
            val i = mercalli(peak)
            Row {
                Readout(fmt(mag, 3), "m/s²", "Bieżące drgania", modifier = Modifier.weight(1f), size = 36.sp)
                Readout(ROMAN[i - 1], "", MERCALLI[i - 1], color = levelColor(i / 10f), modifier = Modifier.weight(1f), size = 36.sp)
            }
            Spacer(Modifier.height(14.dp))
            StatGrid(
                listOf(
                    "Szczyt (PGA)" to fmt(peak, 3),
                    "Szczyt w g" to fmt(peak / 9.80665f, 4),
                    "Zdarzenia" to "$events",
                    "Próbkowanie" to "${fmt(rate, 0)} Hz",
                ),
                columns = 2,
            )
            Spacer(Modifier.height(14.dp))
            Pill("Zeruj", icon = Icons.Rounded.RestartAlt) { peak = 0f; events = 0; rx.clear(); ry.clear(); rz.clear() }
        }
        Text(
            "Połóż telefon na stabilnej, twardej powierzchni ekranem do góry. Skala intensywności (Mercalli) jest szacunkowa – akcelerometr telefonu nie zastąpi sejsmometru.",
            color = Palette.TextFaint,
            fontSize = 12.sp,
            lineHeight = 16.sp,
        )
    }
}

/** Ocena komfortu wg ISO 2631-1 (przyspieszenie ważone RMS, m/s²). */
private fun comfort(a: Float): Pair<String, Float> = when {
    a < 0.315f -> "Komfortowo" to 0.1f
    a < 0.63f -> "Lekko odczuwalne" to 0.4f
    a < 1f -> "Dość niekomfortowo" to 0.65f
    a < 1.6f -> "Niekomfortowo" to 0.8f
    a < 2.5f -> "Bardzo niekomfortowo" to 0.9f
    else -> "Skrajnie niekomfortowo" to 1f
}

private const val VIB_N = 256
private const val VIB_BANDS = 30

@Composable
fun VibrationMeter(onBack: () -> Unit) {
    val state = remember { MotionState() }
    val ticker = rememberTicker()
    val history = remember { Ring(300) }
    val vertHist = remember { Ring(300) }
    val fft = remember { Fft(VIB_N) }
    val buf = remember { DoubleArray(VIB_N) }
    val mags = remember { DoubleArray(VIB_N / 2) }
    val bands = remember { FloatArray(VIB_BANDS) }
    val acc = remember { DoubleArray(6) } // msqV, msqH, lastT, filled, sampleRate, sinceFft
    var rms by remember { mutableFloatStateOf(0f) }
    var rmsV by remember { mutableFloatStateOf(0f) }
    var rmsH by remember { mutableFloatStateOf(0f) }
    var maxRms by remember { mutableFloatStateOf(0f) }
    var bumps by remember { mutableIntStateOf(0) }
    var lastBump by remember { mutableLongStateOf(0L) }
    var domHz by remember { mutableFloatStateOf(Float.NaN) }
    var sumRms by remember { mutableFloatStateOf(0f) }
    var nRms by remember { mutableIntStateOf(0) }
    var lastHist by remember { mutableLongStateOf(0L) }

    MotionSensors(state) { t ->
        val (v, h) = state.split()
        val dt = if (acc[2] == 0.0) 0.005 else ((t - acc[2]) / 1e9).coerceIn(0.0005, 0.1)
        acc[2] = t.toDouble()
        acc[4] = if (acc[4] == 0.0) 1 / dt else acc[4] * 0.99 + (1 / dt) * 0.01
        // Średnia krocząca kwadratu przyspieszenia, stała czasowa 1 s.
        val k = (dt / 1.0).coerceAtMost(1.0)
        acc[0] += (v * v - acc[0]) * k
        acc[1] += (h * h - acc[1]) * k
        rmsV = sqrt(acc[0]).toFloat()
        rmsH = sqrt(acc[1]).toFloat()
        // Kierunek poziomy z współczynnikiem 1,4 wg ISO 2631.
        rms = sqrt(acc[0] + 1.96 * acc[1]).toFloat()
        if (rms > maxRms) maxRms = rms
        val peak = sqrt(v * v + h * h)
        if (peak > 2.5f && t - lastBump > 500_000_000L) {
            bumps++
            lastBump = t
        }
        // Bufor do FFT (składowa pionowa).
        System.arraycopy(buf, 1, buf, 0, VIB_N - 1)
        buf[VIB_N - 1] = v.toDouble()
        acc[3] = (acc[3] + 1).coerceAtMost(VIB_N.toDouble())
        acc[5]++
        if (acc[3] >= VIB_N && acc[5] >= 64) {
            acc[5] = 0.0
            fft.magnitudes(buf, mags)
            val sr = acc[4]
            val binHz = sr / VIB_N
            val pk = peakBin(mags, 2, VIB_N / 2 - 2)
            domHz = if (mags[pk.toInt()] > 0.02) (pk * binHz).toFloat() else Float.NaN
            // Pasma po 1 Hz od 1 do 30 Hz.
            for (b in 0 until VIB_BANDS) {
                val k0 = ((b + 1) / binHz).toInt().coerceIn(1, VIB_N / 2 - 1)
                val k1 = ((b + 2) / binHz).toInt().coerceIn(k0 + 1, VIB_N / 2)
                var m = 0.0
                for (i in k0 until k1) m = maxOf(m, mags[i])
                bands[b] = (bands[b] * 0.5f + (m / 0.5).toFloat().coerceIn(0f, 1f) * 0.5f)
            }
        }
        if (t - lastHist > 100_000_000L) {
            lastHist = t
            history.add(rms)
            vertHist.add(rmsV)
            sumRms += rms
            nRms++
            Recorder.log(Meter.Vibration.id, rms, rmsV, rmsH, peak, if (domHz.isNaN()) null else domHz)
        }
        ticker.tick()
    }

    val (label, frac) = comfort(rms)
    MeterScaffold(
        Meter.Vibration.id,
        Meter.Vibration.title,
        onBack,
        recordColumns = listOf("aw_rms_ms2", "vertical_rms", "horizontal_rms", "peak_ms2", "dominant_hz"),
        recordInterval = 0,
    ) {
        Panel {
            ArcGauge((rms / 3f).coerceIn(0f, 1f), levelColor(frac)) {
                Readout(fmt(rms, 2), "m/s²", label)
            }
            StatGrid(
                listOf(
                    "Pionowo" to fmt(rmsV, 2),
                    "Poziomo" to fmt(rmsH, 2),
                    "Częstotl." to if (domHz.isNaN()) "—" else "${fmt(domHz, 1)} Hz",
                    "Maks" to fmt(maxRms, 2),
                    "Średnio" to fmt(if (nRms == 0) Float.NaN else sumRms / nRms, 2),
                    "Wstrząsy" to "$bumps",
                ),
            )
            Spacer(Modifier.height(14.dp))
            Pill("Nowy przejazd", icon = Icons.Rounded.RestartAlt) {
                maxRms = 0f; bumps = 0; sumRms = 0f; nRms = 0; history.clear(); vertHist.clear()
            }
        }
        Panel {
            SectionLabel("Przebieg RMS · 30 s")
            LineChart(
                listOf(Series(history, Color.White, "Ważone"), Series(vertHist, Palette.AxisZ, "Pionowo")),
                ticker,
                minY = 0f,
                minSpan = 0.5f,
            )
        }
        Panel {
            SectionLabel("Widmo drgań pionowych · 1–30 Hz")
            Bars(bands, ticker, height = 110.dp, colorOf = { lerp(Color(0xFF3A3A3A), Palette.AxisZ, it) })
            Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
                listOf("1", "5", "10", "15", "20", "25", "30 Hz").forEach {
                    Text(it, color = Palette.TextFaint, fontSize = 10.sp, modifier = Modifier.weight(1f))
                }
            }
            Text(
                "Typowo: zawieszenie auta 1–2 Hz, koła i nierówności 10–20 Hz, wagon kolejowy 0,5–8 Hz.",
                color = Palette.TextFaint,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        Text(
            "Połóż telefon płasko na siedzeniu lub podłodze pojazdu. Komfort oceniany według ISO 2631-1 z przyspieszenia RMS (oś pozioma ×1,4).",
            color = Palette.TextFaint,
            fontSize = 12.sp,
            lineHeight = 16.sp,
        )
    }
}
