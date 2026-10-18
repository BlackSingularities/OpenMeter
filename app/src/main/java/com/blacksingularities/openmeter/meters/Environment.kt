package com.blacksingularities.openmeter.meters

import android.Manifest
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingFlat
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.blacksingularities.openmeter.core.LowPass
import com.blacksingularities.openmeter.core.Prefs
import com.blacksingularities.openmeter.core.Recorder
import com.blacksingularities.openmeter.core.Ring
import com.blacksingularities.openmeter.core.SensorEffect
import com.blacksingularities.openmeter.core.hasSensor
import com.blacksingularities.openmeter.core.rememberTicker
import com.blacksingularities.openmeter.core.sensorManager
import com.blacksingularities.openmeter.ui.ArcGauge
import com.blacksingularities.openmeter.ui.Digits
import com.blacksingularities.openmeter.ui.LineChart
import com.blacksingularities.openmeter.ui.MeterScaffold
import com.blacksingularities.openmeter.ui.Notice
import com.blacksingularities.openmeter.ui.Palette
import com.blacksingularities.openmeter.ui.Panel
import com.blacksingularities.openmeter.ui.Pill
import com.blacksingularities.openmeter.ui.Readout
import com.blacksingularities.openmeter.ui.RoundIcon
import com.blacksingularities.openmeter.ui.SectionLabel
import com.blacksingularities.openmeter.ui.Series
import com.blacksingularities.openmeter.ui.StatGrid
import com.blacksingularities.openmeter.ui.fmt
import com.blacksingularities.openmeter.ui.formatSmart
import com.blacksingularities.openmeter.ui.levelColor
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sqrt

// ───────────────────────────── Barometr ─────────────────────────────

private fun weather(hpa: Float) = when {
    hpa < 980 -> "Głęboki niż – burzowo"
    hpa < 1000 -> "Niż – możliwe opady"
    hpa < 1013 -> "Zmiennie"
    hpa < 1025 -> "Wyż – raczej pogodnie"
    else -> "Silny wyż – sucho i stabilnie"
}

@Composable
fun Barometer(onBack: () -> Unit) {
    var hpa by remember { mutableFloatStateOf(Float.NaN) }
    var zeroAlt by remember { mutableFloatStateOf(Float.NaN) }
    val smooth = remember { LowPass(0.1f) }
    val session = remember { Ring(600) }
    val longRing = remember { Ring(720) }
    val ticker = rememberTicker()
    val longTicker = rememberTicker()
    var lastSession by remember { mutableLongStateOf(0L) }
    var trend by remember { mutableFloatStateOf(Float.NaN) }

    fun reloadLong() {
        val h = Prefs.pressureHistory()
        longRing.clear()
        val cut = System.currentTimeMillis() - 24 * 3600_000L
        h.filter { it.first >= cut }.takeLast(longRing.capacity).forEach { longRing.add(it.second) }
        val now = System.currentTimeMillis()
        val old = h.lastOrNull { it.first <= now - 3 * 3600_000L } ?: h.firstOrNull()
        trend = if (old != null && h.isNotEmpty() && now - old.first > 30 * 60_000L) h.last().second - old.second else Float.NaN
        longTicker.tick()
    }
    LaunchedEffect(Unit) { reloadLong() }

    SensorEffect(Sensor.TYPE_PRESSURE, SensorManager.SENSOR_DELAY_UI) { v, _ ->
        val p = smooth.filter(v[0])
        hpa = p
        val now = SystemClock.elapsedRealtime()
        if (now - lastSession >= 1000) {
            lastSession = now
            session.add(p)
            ticker.tick()
            if (Prefs.addPressure(System.currentTimeMillis(), p)) reloadLong()
        }
        Recorder.log(Meter.Baro.id, v[0], SensorManager.getAltitude(Prefs.seaLevelHpa, p))
    }

    val alt = if (hpa.isNaN()) Float.NaN else SensorManager.getAltitude(Prefs.seaLevelHpa, hpa)
    val rel = if (zeroAlt.isNaN() || alt.isNaN()) Float.NaN else alt - zeroAlt
    MeterScaffold(Meter.Baro.id, Meter.Baro.title, onBack, recordColumns = listOf("pressure_hpa", "altitude_m"), recordInterval = 500) {
        Panel {
            val frac = if (hpa.isNaN()) 0f else ((hpa - 950f) / 100f)
            ArcGauge(frac, Color(0xFF6FA8FF)) {
                Readout(fmt(hpa, 1), "hPa", if (hpa.isNaN()) "Czekam na odczyt…" else weather(hpa))
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
                val icon = when {
                    trend.isNaN() || kotlin.math.abs(trend) < 0.5f -> Icons.AutoMirrored.Rounded.TrendingFlat
                    trend > 0 -> Icons.AutoMirrored.Rounded.TrendingUp
                    else -> Icons.AutoMirrored.Rounded.TrendingDown
                }
                Icon(icon, null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text(
                    when {
                        trend.isNaN() -> "Trend pojawi się po ok. 30 min pomiarów"
                        trend > 1.6f -> "Szybko rośnie (+${fmt(trend, 1)} hPa/3 h) – poprawa pogody"
                        trend > 0.5f -> "Rośnie (+${fmt(trend, 1)} hPa/3 h)"
                        trend < -1.6f -> "Szybko spada (${fmt(trend, 1)} hPa/3 h) – nadchodzi załamanie"
                        trend < -0.5f -> "Spada (${fmt(trend, 1)} hPa/3 h)"
                        else -> "Stabilnie"
                    },
                    color = Palette.TextDim,
                    fontSize = 13.sp,
                )
            }
            StatGrid(
                listOf(
                    "Wysokość" to "${fmt(alt, 0)} m",
                    "Względna" to (if (rel.isNaN()) "—" else "${fmt(rel, 1)} m"),
                    "Piętra" to (if (rel.isNaN()) "—" else fmt(rel / 3f, 1)),
                    "mmHg" to fmt(hpa * 0.750062f, 1),
                    "inHg" to fmt(hpa * 0.02953f, 2),
                    "Ciśn. odn." to fmt(Prefs.seaLevelHpa, 1),
                ),
            )
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Pill("Zero wysokości", icon = Icons.Rounded.MyLocation, active = !zeroAlt.isNaN()) { zeroAlt = if (zeroAlt.isNaN()) alt else Float.NaN }
                Spacer(Modifier.weight(1f))
                RoundIcon(Icons.Rounded.Remove, "Ciśnienie odniesienia −") { Prefs.setSeaLevelHpa(Prefs.seaLevelHpa - 1f) }
                RoundIcon(Icons.Rounded.Add, "Ciśnienie odniesienia +") { Prefs.setSeaLevelHpa(Prefs.seaLevelHpa + 1f) }
            }
            Text(
                "Ciśnienie odniesienia (QNH) z najbliższej stacji meteo poprawia wysokość bezwzględną.",
                color = Palette.TextFaint,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        Panel {
            SectionLabel("Bieżąca sesja · 10 min")
            LineChart(listOf(Series(session, Color(0xFF6FA8FF))), ticker, fill = true, minSpan = 0.5f, yLabel = { fmt(it, 1) })
        }
        Panel {
            SectionLabel("Ostatnie 24 h · gdy aplikacja była otwarta")
            if (longRing.size < 2) {
                Text("Za mało danych – historia zapisuje się co minutę podczas pracy barometru.", color = Palette.TextFaint, fontSize = 13.sp)
            } else {
                LineChart(listOf(Series(longRing, Color.White)), longTicker, fill = true, minSpan = 2f, yLabel = { fmt(it, 0) })
            }
            Spacer(Modifier.height(10.dp))
            Pill("Wyczyść historię", icon = Icons.Rounded.RestartAlt) { Prefs.clearPressureHistory(); reloadLong() }
        }
    }
}

// ───────────────────────────── Klimat ─────────────────────────────

/** Punkt rosy (wzór Magnusa). */
private fun dewPoint(t: Float, rh: Float): Float {
    val a = 17.62f
    val b = 243.12f
    val g = ln(rh / 100f) + a * t / (b + t)
    return b * g / (a - g)
}

/** Wilgotność bezwzględna w g/m³. */
private fun absoluteHumidity(t: Float, rh: Float): Float =
    (6.112f * exp(17.67f * t / (t + 243.5f)) * rh * 2.1674f / (273.15f + t))

@Composable
fun ClimateMeter(onBack: () -> Unit) {
    val context = LocalContext.current
    val hasT = remember { context.hasSensor(Sensor.TYPE_AMBIENT_TEMPERATURE) }
    val hasH = remember { context.hasSensor(Sensor.TYPE_RELATIVE_HUMIDITY) }
    var t by remember { mutableFloatStateOf(Float.NaN) }
    var rh by remember { mutableFloatStateOf(Float.NaN) }
    val tRing = remember { Ring(300) }
    val hRing = remember { Ring(300) }
    val ticker = rememberTicker()
    if (hasT) SensorEffect(Sensor.TYPE_AMBIENT_TEMPERATURE, SensorManager.SENSOR_DELAY_UI) { v, _ ->
        t = v[0]; tRing.add(v[0]); ticker.tick()
        Recorder.log(Meter.Climate.id, t, rh)
    }
    if (hasH) SensorEffect(Sensor.TYPE_RELATIVE_HUMIDITY, SensorManager.SENSOR_DELAY_UI) { v, _ ->
        rh = v[0]; hRing.add(v[0]); ticker.tick()
        if (!hasT) Recorder.log(Meter.Climate.id, t, rh)
    }
    MeterScaffold(Meter.Climate.id, Meter.Climate.title, onBack, recordColumns = listOf("temperature_c", "humidity_pct"), recordInterval = 1000) {
        Panel {
            Row {
                if (hasT) Readout(fmt(t, 1), "°C", "Temperatura", modifier = Modifier.weight(1f), size = 44.sp)
                if (hasH) Readout(fmt(rh, 0), "%", "Wilgotność", modifier = Modifier.weight(1f), size = 44.sp)
            }
            if (hasT && hasH && !t.isNaN() && !rh.isNaN()) {
                Spacer(Modifier.height(16.dp))
                val dp = dewPoint(t, rh)
                StatGrid(
                    listOf(
                        "Punkt rosy" to "${fmt(dp, 1)} °C",
                        "Wilg. bezwzgl." to "${fmt(absoluteHumidity(t, rh), 1)} g/m³",
                        "Odczucie" to when {
                            rh < 30 -> "Sucho"
                            rh > 65 && t > 24 -> "Parno"
                            rh > 70 -> "Wilgotno"
                            t < 18 -> "Chłodno"
                            t > 26 -> "Ciepło"
                            else -> "Komfortowo"
                        },
                    ),
                )
            }
        }
        Panel {
            SectionLabel("Historia")
            val list = buildList {
                if (hasT) add(Series(tRing, Palette.AxisX, "°C"))
                if (hasH) add(Series(hRing, Palette.AxisZ, "%"))
            }
            LineChart(list, ticker, minSpan = 2f)
        }
    }
}

// ───────────────────────────── Obrotomierz ─────────────────────────────

@Composable
fun Tachometer(onBack: () -> Unit) {
    var w by remember { mutableFloatStateOf(0f) }
    var maxRpm by remember { mutableFloatStateOf(0f) }
    val axes = remember { FloatArray(3) }
    val rx = remember { Ring(400) }
    val ry = remember { Ring(400) }
    val rz = remember { Ring(400) }
    val ticker = rememberTicker()
    SensorEffect(Sensor.TYPE_GYROSCOPE, SensorManager.SENSOR_DELAY_GAME) { v, _ ->
        v.copyInto(axes, 0, 0, 3)
        w = sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2])
        val rpm = w / (2 * PI.toFloat()) * 60f
        if (rpm > maxRpm) maxRpm = rpm
        rx.add(Math.toDegrees(v[0].toDouble()).toFloat())
        ry.add(Math.toDegrees(v[1].toDouble()).toFloat())
        rz.add(Math.toDegrees(v[2].toDouble()).toFloat())
        ticker.tick()
        Recorder.log(Meter.Tacho.id, v[0], v[1], v[2], rpm)
    }
    val rpm = w / (2 * PI.toFloat()) * 60f
    MeterScaffold(Meter.Tacho.id, Meter.Tacho.title, onBack, recordColumns = listOf("wx_rad_s", "wy_rad_s", "wz_rad_s", "rpm"), recordInterval = 20) {
        Panel {
            ArcGauge((rpm / 300f).coerceIn(0f, 1f), levelColor(rpm / 300f)) {
                Readout(fmt(rpm, 0), "obr/min", "${fmt(Math.toDegrees(w.toDouble()), 0)} °/s")
            }
            StatGrid(
                listOf(
                    "X °/s" to fmt(Math.toDegrees(axes[0].toDouble()), 0),
                    "Y °/s" to fmt(Math.toDegrees(axes[1].toDouble()), 0),
                    "Z °/s" to fmt(Math.toDegrees(axes[2].toDouble()), 0),
                    "Maks obr/min" to fmt(maxRpm, 0),
                    "Hz" to fmt(rpm / 60f, 2),
                    "rad/s" to fmt(w, 2),
                ),
            )
            Spacer(Modifier.height(14.dp))
            Pill("Zeruj maksimum", icon = Icons.Rounded.RestartAlt) { maxRpm = 0f }
        }
        Panel {
            SectionLabel("Prędkość kątowa · °/s")
            LineChart(listOf(Series(rx, Palette.AxisX, "X"), Series(ry, Palette.AxisY, "Y"), Series(rz, Palette.AxisZ, "Z")), ticker, symmetric = true, minSpan = 20f, yLabel = { fmt(it, 0) })
        }
        Text(
            "Połóż telefon na obracającym się przedmiocie (talerz gramofonu, obrotowe krzesło, karuzela) – pokaże prędkość obrotu. Żyroskop mierzy do kilku tysięcy °/s.",
            color = Palette.TextFaint,
            fontSize = 12.sp,
            lineHeight = 16.sp,
        )
    }
}

// ───────────────────────────── Krokomierz ─────────────────────────────

@Composable
fun StepsMeter(onBack: () -> Unit) {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED)
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    LaunchedEffect(Unit) { if (!granted) launcher.launch(Manifest.permission.ACTIVITY_RECOGNITION) }
    val hasDetector = remember { context.hasSensor(Sensor.TYPE_STEP_DETECTOR) }
    var session by remember { mutableIntStateOf(0) }
    var sinceBoot by remember { mutableFloatStateOf(Float.NaN) }
    var counterBase by remember { mutableFloatStateOf(Float.NaN) }
    val stamps = remember { mutableStateListOf<Long>() }

    fun step(t: Long) {
        stamps.add(t)
        while (stamps.size > 12) stamps.removeAt(0)
        Recorder.log(Meter.Steps.id, session)
    }
    if (granted && hasDetector) SensorEffect(Sensor.TYPE_STEP_DETECTOR, SensorManager.SENSOR_DELAY_FASTEST) { _, t ->
        session++
        step(t)
    }
    if (granted) SensorEffect(Sensor.TYPE_STEP_COUNTER, SensorManager.SENSOR_DELAY_UI) { v, t ->
        sinceBoot = v[0]
        if (counterBase.isNaN()) counterBase = v[0]
        if (!hasDetector) {
            val n = (v[0] - counterBase).toInt()
            if (n > session) { session = n; step(t) }
        }
    }
    val cadence = if (stamps.size >= 4) (stamps.size - 1) * 60e9f / (stamps.last() - stamps.first()).coerceAtLeast(1L) else Float.NaN
    MeterScaffold(Meter.Steps.id, Meter.Steps.title, onBack, recordColumns = listOf("steps"), recordInterval = 0) {
        if (!granted) {
            Notice("Krokomierz potrzebuje uprawnienia „Aktywność fizyczna”.", "Zezwól") { launcher.launch(Manifest.permission.ACTIVITY_RECOGNITION) }
            return@MeterScaffold
        }
        Panel {
            ArcGauge(((session % 1000) / 1000f), Palette.Ok) {
                Readout("$session", "", "kroków w tej sesji")
            }
            StatGrid(
                listOf(
                    "Kadencja" to "${fmt(cadence, 0)} /min",
                    "Dystans" to "${fmt(session * 0.75f / 1000f, 2)} km",
                    "Energia" to "${fmt(session * 0.04f, 0)} kcal",
                    "Od uruchom." to (if (sinceBoot.isNaN()) "—" else fmt(sinceBoot, 0)),
                    "Tempo" to when {
                        cadence.isNaN() -> "—"
                        cadence < 90 -> "Spacer"
                        cadence < 130 -> "Marsz"
                        else -> "Bieg"
                    },
                ),
            )
            Spacer(Modifier.height(14.dp))
            Pill("Zeruj", icon = Icons.Rounded.RestartAlt) { session = 0; counterBase = sinceBoot; stamps.clear() }
        }
        Text("Dystans i energia są szacowane (krok 0,75 m, 0,04 kcal/krok).", color = Palette.TextFaint, fontSize = 12.sp)
    }
}

// ───────────────────────────── Zbliżenie ─────────────────────────────

@Composable
fun ProximityMeter(onBack: () -> Unit) {
    val context = LocalContext.current
    val maxRange = remember { context.sensorManager().getDefaultSensor(Sensor.TYPE_PROXIMITY)?.maximumRange ?: 5f }
    var dist by remember { mutableFloatStateOf(Float.NaN) }
    var gestures by remember { mutableIntStateOf(0) }
    var nearSince by remember { mutableLongStateOf(0L) }
    var lastNearMs by remember { mutableLongStateOf(0L) }
    SensorEffect(Sensor.TYPE_PROXIMITY, SensorManager.SENSOR_DELAY_FASTEST) { v, _ ->
        val wasNear = !dist.isNaN() && dist < maxRange
        dist = v[0]
        val near = v[0] < maxRange
        if (near && !wasNear) {
            gestures++
            nearSince = SystemClock.elapsedRealtime()
        }
        if (!near && wasNear) lastNearMs = SystemClock.elapsedRealtime() - nearSince
        Recorder.log(Meter.Proximity.id, v[0], if (near) 1 else 0)
    }
    val near = !dist.isNaN() && dist < maxRange
    val fill by animateFloatAsState(if (near) 1f else 0f, tween(200), label = "near")
    MeterScaffold(Meter.Proximity.id, Meter.Proximity.title, onBack, recordColumns = listOf("distance_cm", "near"), recordInterval = 0) {
        Panel {
            Canvas(Modifier.fillMaxWidth().aspectRatio(1.4f)) {
                val r = size.minDimension / 2 - 10.dp.toPx()
                drawCircle(Palette.CardHigh, r, center)
                drawCircle(Color.White, r * (0.25f + 0.75f * fill), center, alpha = 0.15f + 0.85f * fill)
                drawCircle(Palette.TextFaint, r, center, style = Stroke(1.5.dp.toPx()))
            }
            Readout(if (near) "BLISKO" else "DALEKO", "", "Odczyt: ${formatSmart(dist)} cm (zakres ${formatSmart(maxRange)} cm)", size = 40.sp)
            Spacer(Modifier.height(14.dp))
            StatGrid(listOf("Gesty" to "$gestures", "Ostatnio zakryty" to "${fmt(lastNearMs / 1000f, 1)} s"), columns = 2)
            Spacer(Modifier.height(14.dp))
            Pill("Zeruj licznik", icon = Icons.Rounded.RestartAlt) { gestures = 0 }
        }
        Text(
            "Czujnik zbliżeniowy jest przy głośniku rozmów. Machnij nad nim dłonią – każde zakrycie to jeden gest. Wiele telefonów mówi tylko „blisko/daleko”.",
            color = Palette.TextFaint,
            fontSize = 12.sp,
            lineHeight = 16.sp,
        )
    }
}

// ───────────────────────────── Inspektor czujników ─────────────────────────────

@Composable
fun SensorInspector(onBack: () -> Unit) {
    val context = LocalContext.current
    val sensors = remember { context.sensorManager().getSensorList(Sensor.TYPE_ALL).sortedBy { it.type } }
    var open by remember { mutableStateOf<Sensor?>(null) }
    MeterScaffold(Meter.Inspector.id, Meter.Inspector.title, onBack) {
        Text("${sensors.size} czujników sprzętowych i wirtualnych. Dotknij, aby zobaczyć odczyt na żywo.", color = Palette.TextDim, fontSize = 13.sp)
        sensors.forEach { s ->
            val expanded = open == s
            Panel(Modifier.clickable { open = if (expanded) null else s }, padding = 14.dp) {
                Text(s.name, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    "${s.stringType.substringAfterLast('.')} · ${s.vendor}",
                    color = Palette.TextFaint,
                    fontSize = 12.sp,
                )
                if (expanded) {
                    Spacer(Modifier.height(10.dp))
                    StatGrid(
                        listOf(
                            "Zakres" to formatSmart(s.maximumRange),
                            "Rozdzielczość" to formatSmart(s.resolution),
                            "Pobór mA" to formatSmart(s.power),
                            "Min. odstęp" to "${s.minDelay} µs",
                            "Wersja" to "${s.version}",
                            "Wybudzający" to if (s.isWakeUpSensor) "tak" else "nie",
                        ),
                    )
                    Spacer(Modifier.height(10.dp))
                    LiveValues(s)
                }
            }
        }
    }
}

@Composable
private fun LiveValues(s: Sensor) {
    val context = LocalContext.current
    var values by remember(s) { mutableStateOf(FloatArray(0)) }
    androidx.compose.runtime.DisposableEffect(s) {
        val sm = context.sensorManager()
        val l = object : android.hardware.SensorEventListener {
            override fun onSensorChanged(e: android.hardware.SensorEvent) {
                values = e.values.copyOf()
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        sm.registerListener(l, s, SensorManager.SENSOR_DELAY_UI)
        onDispose { sm.unregisterListener(l) }
    }
    Column(Modifier.fillMaxWidth()) {
        if (values.isEmpty()) {
            Text("Czekam na zdarzenie… (niektóre czujniki reagują tylko na zmianę)", color = Palette.TextFaint, fontSize = 12.sp)
        }
        values.take(6).forEachIndexed { i, v ->
            Row {
                Text("[$i]", color = Palette.TextFaint, style = Digits, fontSize = 13.sp, modifier = Modifier.width(40.dp))
                Text(String.format(java.util.Locale.US, "%.4f", v), color = Color.White, style = Digits, fontSize = 13.sp)
            }
        }
    }
}
