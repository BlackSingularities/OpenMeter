package com.blacksingularities.openmeter.meters

import android.hardware.Sensor
import android.hardware.SensorManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blacksingularities.openmeter.core.LowPass
import com.blacksingularities.openmeter.core.Recorder
import com.blacksingularities.openmeter.core.Ring
import com.blacksingularities.openmeter.core.SensorEffect
import com.blacksingularities.openmeter.core.Stats
import com.blacksingularities.openmeter.core.rememberTicker
import com.blacksingularities.openmeter.ui.ArcGauge
import com.blacksingularities.openmeter.ui.LineChart
import com.blacksingularities.openmeter.ui.MeterScaffold
import com.blacksingularities.openmeter.ui.Palette
import com.blacksingularities.openmeter.ui.Panel
import com.blacksingularities.openmeter.ui.Pill
import com.blacksingularities.openmeter.ui.Readout
import com.blacksingularities.openmeter.ui.SectionLabel
import com.blacksingularities.openmeter.ui.Series
import com.blacksingularities.openmeter.ui.StatGrid
import com.blacksingularities.openmeter.ui.fmt
import com.blacksingularities.openmeter.ui.formatSmart
import kotlin.math.log10
import kotlin.math.log2

/** Zalecane natężenie oświetlenia (PN-EN 12464-1, w przybliżeniu). */
private val guide = listOf(
    50f to "Korytarz, oświetlenie nocne",
    150f to "Klatka schodowa, magazyn",
    300f to "Salon, kuchnia",
    500f to "Biuro, nauka, czytanie",
    750f to "Rysunek techniczny, precyzyjna praca",
    1000f to "Praca bardzo precyzyjna",
    10000f to "Dzień pochmurny na zewnątrz",
    100000f to "Pełne słońce",
)

private fun describe(lux: Float): String = when {
    lux < 1 -> "Ciemność"
    lux < 10 -> "Półmrok"
    lux < 50 -> "Słabe oświetlenie"
    lux < 200 -> "Przytulne światło"
    lux < 500 -> "Oświetlenie mieszkania"
    lux < 1000 -> "Jasne biuro"
    lux < 10000 -> "Światło dzienne w cieniu"
    lux < 40000 -> "Jasny dzień"
    else -> "Pełne słońce"
}

@Composable
fun LightMeter(onBack: () -> Unit) {
    var lux by remember { mutableFloatStateOf(Float.NaN) }
    var hold by remember { mutableStateOf(false) }
    val smooth = remember { LowPass(0.35f) }
    val stats = remember { Stats() }
    val history = remember { Ring(300) }
    val ticker = rememberTicker()

    SensorEffect(Sensor.TYPE_LIGHT, SensorManager.SENSOR_DELAY_UI) { v, _ ->
        if (hold) return@SensorEffect
        val l = smooth.filter(v[0])
        lux = l
        stats.add(l)
        history.add(log10(l.coerceAtLeast(0.1f)))
        ticker.tick()
        Recorder.log(Meter.Light.id, v[0], l)
    }

    MeterScaffold(Meter.Light.id, Meter.Light.title, onBack, recordColumns = listOf("lux_raw", "lux_smoothed"), recordInterval = 100) {
        Panel {
            // Skala logarytmiczna 0,1 lx … 100 000 lx.
            val frac = if (lux.isNaN()) 0f else ((log10(lux.coerceAtLeast(0.1f)) + 1f) / 6f)
            ArcGauge(frac, Color(0xFFFFD27A)) {
                Readout(if (lux.isNaN()) "—" else formatLux(lux), "lx", if (lux.isNaN()) "Czekam na odczyt…" else describe(lux))
            }
            val ev = if (lux.isNaN() || lux <= 0f) Float.NaN else log2(lux / 2.5f)
            StatGrid(
                listOf(
                    "Min" to formatLux(stats.min),
                    "Średnio" to formatLux(stats.avg),
                    "Maks" to formatLux(stats.max),
                    "Stopoświeca" to fmt(lux / 10.764f, 1),
                    "EV (ISO 100)" to fmt(ev, 1),
                    "Czas przy f/8" to shutter(ev),
                ),
            )
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill(if (hold) "Odblokuj" else "Zatrzymaj", icon = if (hold) Icons.Rounded.LockOpen else Icons.Rounded.Lock, active = hold) { hold = !hold }
                Pill("Zeruj", icon = Icons.Rounded.RestartAlt) { stats.reset(); history.clear(); ticker.tick() }
            }
        }
        Panel {
            SectionLabel("Historia · skala logarytmiczna")
            LineChart(listOf(Series(history, Color(0xFFFFD27A))), ticker, fill = true, minSpan = 1f, yLabel = { formatLux(Math.pow(10.0, it.toDouble()).toFloat()) + " lx" })
        }
        Panel {
            SectionLabel("Zalecane natężenie")
            guide.forEach { (l, label) ->
                val ok = !lux.isNaN() && lux >= l
                Row(Modifier.height(30.dp)) {
                    Text(label, color = if (ok) Color.White else Palette.TextFaint, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    Text("${formatLux(l)} lx", color = if (ok) Palette.Ok else Palette.TextFaint, fontSize = 14.sp)
                }
            }
        }
    }
}

private fun formatLux(v: Float): String = when {
    v.isNaN() -> "—"
    v >= 10000 -> "${fmt(v / 1000, 1)}k"
    else -> formatSmart(v)
}

/** Czas naświetlania przy przysłonie f/8 i ISO 100 dla danego EV. */
private fun shutter(ev: Float): String {
    if (ev.isNaN()) return "—"
    val t = 64.0 / Math.pow(2.0, ev.toDouble())
    return if (t >= 1) "${fmt(t, 1)} s" else "1/${Math.round(1 / t)}"
}
