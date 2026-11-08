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
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.blacksingularities.openmeter.R
import com.blacksingularities.openmeter.core.LowPass
import com.blacksingularities.openmeter.core.Prefs
import com.blacksingularities.openmeter.core.Recorder
import com.blacksingularities.openmeter.core.Ring
import com.blacksingularities.openmeter.core.SensorEffect
import com.blacksingularities.openmeter.core.Units
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
import com.blacksingularities.openmeter.ui.Tip
import com.blacksingularities.openmeter.ui.fmt
import com.blacksingularities.openmeter.ui.formatSmart
import com.blacksingularities.openmeter.ui.levelColor
import com.blacksingularities.openmeter.ui.reactive
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sqrt

// ───────────────────────────── Barometr ─────────────────────────────

private fun weather(hpa: Float) = when {
    hpa < 980 -> R.string.baro_w1
    hpa < 1000 -> R.string.baro_w2
    hpa < 1013 -> R.string.baro_w3
    hpa < 1025 -> R.string.baro_w4
    else -> R.string.baro_w5
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
    val pu = Units.pressureUnit
    val pd = Units.pressureDigits
    val lu = Units.lengthUnit
    MeterScaffold(Meter.Baro.id, stringResource(Meter.Baro.title), onBack, recordColumns = listOf("pressure_hpa", "altitude_m"), recordInterval = 500) {
        Panel {
            val frac = if (hpa.isNaN()) 0f else ((hpa - 950f) / 100f)
            ArcGauge(frac, Palette.Fg) {
                Readout(fmt(Units.pressure(hpa), pd), pu, if (hpa.isNaN()) stringResource(R.string.waiting) else stringResource(weather(hpa)))
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 14.dp)) {
                val icon = when {
                    trend.isNaN() || kotlin.math.abs(trend) < 0.5f -> Icons.AutoMirrored.Rounded.TrendingFlat
                    trend > 0 -> Icons.AutoMirrored.Rounded.TrendingUp
                    else -> Icons.AutoMirrored.Rounded.TrendingDown
                }
                Icon(icon, null, tint = Palette.Fg)
                Spacer(Modifier.width(8.dp))
                val t = fmt(trend, 1)
                Text(
                    when {
                        trend.isNaN() -> stringResource(R.string.baro_trend_wait)
                        trend > 1.6f -> stringResource(R.string.baro_trend_up_fast, t)
                        trend > 0.5f -> stringResource(R.string.baro_trend_up, t)
                        trend < -1.6f -> stringResource(R.string.baro_trend_down_fast, t)
                        trend < -0.5f -> stringResource(R.string.baro_trend_down, t)
                        else -> stringResource(R.string.baro_trend_stable)
                    },
                    color = Palette.TextDim,
                    fontSize = 13.sp,
                )
            }
            StatGrid(
                listOf(
                    stringResource(R.string.altitude) to "${fmt(Units.length(alt), 0)} $lu",
                    stringResource(R.string.baro_relative) to (if (rel.isNaN()) "—" else "${fmt(Units.length(rel), 1)} $lu"),
                    stringResource(R.string.baro_floors) to (if (rel.isNaN()) "—" else fmt(rel / 3f, 1)),
                    "hPa" to fmt(hpa, 1),
                    "mmHg" to fmt(hpa * 0.750062f, 1),
                    stringResource(R.string.baro_qnh) to "${fmt(Units.pressure(Prefs.seaLevelHpa), pd)} $pu",
                ),
            )
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Pill(stringResource(R.string.baro_zero_alt), icon = Icons.Rounded.MyLocation, active = !zeroAlt.isNaN()) { zeroAlt = if (zeroAlt.isNaN()) alt else Float.NaN }
                Spacer(Modifier.weight(1f))
                RoundIcon(Icons.Rounded.Remove, stringResource(R.string.baro_qnh_minus)) { Prefs.seaLevelHpa = (Prefs.seaLevelHpa - 1f) }
                RoundIcon(Icons.Rounded.Add, stringResource(R.string.baro_qnh_plus)) { Prefs.seaLevelHpa = (Prefs.seaLevelHpa + 1f) }
            }
            Text(stringResource(R.string.baro_qnh_hint), color = Palette.TextFaint, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp))
        }
        Panel {
            SectionLabel(stringResource(R.string.baro_session, pu))
            LineChart(listOf(Series(session, Palette.Fg)), ticker, fill = true, minSpan = 0.5f, yLabel = { fmt(Units.pressure(it), pd) })
        }
        Panel {
            SectionLabel(stringResource(R.string.baro_24h))
            if (longRing.size < 2) {
                Text(stringResource(R.string.baro_24h_empty), color = Palette.TextFaint, fontSize = 13.sp)
            } else {
                LineChart(listOf(Series(longRing, Palette.Fg)), longTicker, fill = true, minSpan = 2f, yLabel = { fmt(Units.pressure(it), pd) })
            }
            Spacer(Modifier.height(10.dp))
            Pill(stringResource(R.string.set_pressure_history), icon = Icons.Rounded.RestartAlt) { Prefs.clearPressureHistory(); reloadLong() }
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
    val tu = Units.tempUnit
    MeterScaffold(Meter.Climate.id, stringResource(Meter.Climate.title), onBack, recordColumns = listOf("temperature_c", "humidity_pct"), recordInterval = 1000) {
        Panel {
            Row {
                if (hasT) Readout(fmt(Units.temp(t), 1), tu, stringResource(R.string.temperature), modifier = Modifier.weight(1f), size = 44.sp)
                if (hasH) Readout(fmt(rh, 0), "%", stringResource(R.string.humidity), modifier = Modifier.weight(1f), size = 44.sp)
            }
            if (hasT && hasH && !t.isNaN() && !rh.isNaN()) {
                Spacer(Modifier.height(16.dp))
                val dp = dewPoint(t, rh)
                StatGrid(
                    listOf(
                        stringResource(R.string.clim_dew) to "${fmt(Units.temp(dp), 1)} $tu",
                        stringResource(R.string.clim_abs) to "${fmt(absoluteHumidity(t, rh), 1)} g/m³",
                        stringResource(R.string.clim_feel) to stringResource(
                            when {
                                rh < 30 -> R.string.clim_dry
                                rh > 65 && t > 24 -> R.string.clim_muggy
                                rh > 70 -> R.string.clim_humid
                                t < 18 -> R.string.clim_cool
                                t > 26 -> R.string.clim_warm
                                else -> R.string.clim_comfort
                            },
                        ),
                    ),
                )
            }
        }
        Panel {
            SectionLabel(stringResource(R.string.history))
            val list = buildList {
                if (hasT) add(Series(tRing, Palette.AxisX, tu))
                if (hasH) add(Series(hRing, Palette.AxisZ, "%"))
            }
            LineChart(list, ticker, minSpan = 2f)
        }
        Tip(stringResource(R.string.clim_tip))
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
    MeterScaffold(Meter.Tacho.id, stringResource(Meter.Tacho.title), onBack, recordColumns = listOf("wx_rad_s", "wy_rad_s", "wz_rad_s", "rpm"), recordInterval = 20) {
        Panel {
            ArcGauge((rpm / 300f).coerceIn(0f, 1f), levelColor(rpm / 300f)) {
                Readout(fmt(rpm, 0), stringResource(R.string.rpm), "${fmt(Math.toDegrees(w.toDouble()), 0)} °/s")
            }
            StatGrid(
                listOf(
                    "X °/s" to fmt(Math.toDegrees(axes[0].toDouble()), 0),
                    "Y °/s" to fmt(Math.toDegrees(axes[1].toDouble()), 0),
                    "Z °/s" to fmt(Math.toDegrees(axes[2].toDouble()), 0),
                    stringResource(R.string.tacho_max) to fmt(maxRpm, 0),
                    "Hz" to fmt(rpm / 60f, 2),
                    "rad/s" to fmt(w, 2),
                ),
            )
            Spacer(Modifier.height(14.dp))
            Pill(stringResource(R.string.tacho_reset), icon = Icons.Rounded.RestartAlt) { maxRpm = 0f }
        }
        Panel {
            SectionLabel(stringResource(R.string.tacho_chart))
            LineChart(listOf(Series(rx, Palette.AxisX, "X"), Series(ry, Palette.AxisY, "Y"), Series(rz, Palette.AxisZ, "Z")), ticker, symmetric = true, minSpan = 20f, yLabel = { fmt(it, 0) })
        }
        Tip(stringResource(R.string.tacho_tip))
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
    var goal by remember { mutableIntStateOf(1000) }

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
    val km = session * Prefs.stepLength / 1000f
    MeterScaffold(Meter.Steps.id, stringResource(Meter.Steps.title), onBack, recordColumns = listOf("steps"), recordInterval = 0) {
        if (!granted) {
            Notice(stringResource(R.string.steps_perm), stringResource(R.string.allow)) { launcher.launch(Manifest.permission.ACTIVITY_RECOGNITION) }
            return@MeterScaffold
        }
        Panel {
            ArcGauge((session.toFloat() / goal).coerceIn(0f, 1f), if (session >= goal) Palette.Ok else Palette.Fg) {
                Readout("$session", "", pluralStringResource(R.plurals.steps_session, session))
            }
            StatGrid(
                listOf(
                    stringResource(R.string.steps_cadence) to "${fmt(cadence, 0)} /min",
                    stringResource(R.string.steps_distance) to "${fmt(Units.distance(km), 2)} ${Units.distanceUnit}",
                    stringResource(R.string.steps_energy) to "${fmt(session * 0.04f, 0)} kcal",
                    stringResource(R.string.steps_boot) to (if (sinceBoot.isNaN()) "—" else fmt(sinceBoot, 0)),
                    stringResource(R.string.steps_pace) to stringResource(
                        when {
                            cadence.isNaN() -> R.string.dash
                            cadence < 90 -> R.string.steps_walk
                            cadence < 130 -> R.string.steps_march
                            else -> R.string.steps_run
                        },
                    ),
                    stringResource(R.string.steps_goal) to "$goal",
                ),
            )
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Pill(stringResource(R.string.reset), icon = Icons.Rounded.RestartAlt) { session = 0; counterBase = sinceBoot; stamps.clear() }
                Spacer(Modifier.weight(1f))
                RoundIcon(Icons.Rounded.Remove, stringResource(R.string.steps_goal_minus)) { goal = (goal - 500).coerceAtLeast(500) }
                RoundIcon(Icons.Rounded.Add, stringResource(R.string.steps_goal_plus)) { goal += 500 }
            }
        }
        Tip(stringResource(R.string.steps_tip, fmt(Prefs.stepLength * 100f, 0)))
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
    val card = Palette.CardHigh
    val fg = Palette.Fg
    val faint = Palette.TextFaint
    MeterScaffold(Meter.Proximity.id, stringResource(Meter.Proximity.title), onBack, recordColumns = listOf("distance_cm", "near"), recordInterval = 0) {
        Panel {
            Canvas(Modifier.fillMaxWidth().aspectRatio(1.4f)) {
                val r = size.minDimension / 2 - 10.dp.toPx()
                drawCircle(card, r, center)
                drawCircle(fg, r * (0.25f + 0.75f * fill), center, alpha = 0.15f + 0.85f * fill)
                drawCircle(faint, r, center, style = Stroke(1.5.dp.toPx()))
            }
            Readout(
                stringResource(if (near) R.string.prox_near else R.string.prox_far),
                "",
                stringResource(R.string.prox_reading, formatSmart(dist), formatSmart(maxRange)),
                size = 40.sp,
            )
            Spacer(Modifier.height(14.dp))
            StatGrid(
                listOf(stringResource(R.string.prox_gestures) to "$gestures", stringResource(R.string.prox_last) to "${fmt(lastNearMs / 1000f, 1)} s"),
                columns = 2,
            )
            Spacer(Modifier.height(14.dp))
            Pill(stringResource(R.string.prox_reset), icon = Icons.Rounded.RestartAlt) { gestures = 0 }
        }
        Tip(stringResource(R.string.prox_tip))
    }
}

// ───────────────────────────── Inspektor czujników ─────────────────────────────

@Composable
fun SensorInspector(onBack: () -> Unit) {
    val context = LocalContext.current
    val sensors = remember { context.sensorManager().getSensorList(Sensor.TYPE_ALL).sortedBy { it.type } }
    var open by remember { mutableStateOf<Sensor?>(null) }
    MeterScaffold(Meter.Inspector.id, stringResource(Meter.Inspector.title), onBack) {
        Text(pluralStringResource(R.plurals.insp_count, sensors.size, sensors.size), color = Palette.TextDim, fontSize = 13.sp, modifier = Modifier.padding(start = 4.dp))
        sensors.forEach { s ->
            val expanded = open == s
            Panel(Modifier.reactive(s.name, pressScale = 0.98f) { open = if (expanded) null else s }, padding = 14.dp) {
                Text(s.name, color = Palette.Fg, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text("${s.stringType.substringAfterLast('.')} · ${s.vendor}", color = Palette.TextFaint, fontSize = 12.sp)
                if (expanded) {
                    Spacer(Modifier.height(10.dp))
                    StatGrid(
                        listOf(
                            stringResource(R.string.insp_range) to formatSmart(s.maximumRange),
                            stringResource(R.string.insp_resolution) to formatSmart(s.resolution),
                            stringResource(R.string.insp_power) to formatSmart(s.power),
                            stringResource(R.string.insp_delay) to "${s.minDelay} µs",
                            stringResource(R.string.insp_version) to "${s.version}",
                            stringResource(R.string.insp_wakeup) to stringResource(if (s.isWakeUpSensor) R.string.yes else R.string.no),
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
    DisposableEffect(s) {
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
            Text(stringResource(R.string.insp_waiting), color = Palette.TextFaint, fontSize = 12.sp)
        }
        values.take(6).forEachIndexed { i, v ->
            Row {
                Text("[$i]", color = Palette.TextFaint, style = Digits, fontSize = 13.sp, modifier = Modifier.width(40.dp))
                Text(String.format(java.util.Locale.US, "%.4f", v), color = Palette.Fg, style = Digits, fontSize = 13.sp)
            }
        }
    }
}
