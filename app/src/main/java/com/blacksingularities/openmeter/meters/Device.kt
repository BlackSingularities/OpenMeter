package com.blacksingularities.openmeter.meters

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.blacksingularities.openmeter.R
import com.blacksingularities.openmeter.core.Recorder
import com.blacksingularities.openmeter.core.Ring
import com.blacksingularities.openmeter.core.Stats
import com.blacksingularities.openmeter.core.Units
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
import com.blacksingularities.openmeter.ui.Tip
import com.blacksingularities.openmeter.ui.fmt
import kotlinx.coroutines.delay
import kotlin.math.abs

/** Odczyt z komunikatu ACTION_BATTERY_CHANGED. */
private class BatteryInfo(
    val level: Float,
    val status: Int,
    val plugged: Int,
    val health: Int,
    val tempC: Float,
    val voltage: Float,
    val technology: String,
)

private fun parse(i: Intent): BatteryInfo {
    val level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
    val scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
    val mv = i.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0)
    return BatteryInfo(
        level = if (level < 0) Float.NaN else level * 100f / scale,
        status = i.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN),
        plugged = i.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0),
        health = i.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN),
        tempC = i.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE).let { if (it == Int.MIN_VALUE) Float.NaN else it / 10f },
        // Część telefonów podaje wolty zamiast miliwoltów.
        voltage = if (mv > 100) mv / 1000f else mv.toFloat(),
        technology = i.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY).orEmpty(),
    )
}

private fun statusRes(s: Int) = when (s) {
    BatteryManager.BATTERY_STATUS_CHARGING -> R.string.bat_charging
    BatteryManager.BATTERY_STATUS_DISCHARGING -> R.string.bat_discharging
    BatteryManager.BATTERY_STATUS_FULL -> R.string.bat_full
    BatteryManager.BATTERY_STATUS_NOT_CHARGING -> R.string.bat_not_charging
    else -> R.string.bat_unknown
}

private fun pluggedRes(p: Int) = when (p) {
    BatteryManager.BATTERY_PLUGGED_AC -> R.string.bat_ac
    BatteryManager.BATTERY_PLUGGED_USB -> R.string.bat_usb
    BatteryManager.BATTERY_PLUGGED_WIRELESS -> R.string.bat_wireless
    else -> R.string.bat_battery
}

private fun healthRes(h: Int) = when (h) {
    BatteryManager.BATTERY_HEALTH_GOOD -> R.string.bat_good
    BatteryManager.BATTERY_HEALTH_OVERHEAT -> R.string.bat_overheat
    BatteryManager.BATTERY_HEALTH_DEAD -> R.string.bat_dead
    BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> R.string.bat_overvoltage
    BatteryManager.BATTERY_HEALTH_COLD -> R.string.bat_cold
    else -> R.string.bat_unknown
}

@Composable
fun BatteryMeter(onBack: () -> Unit) {
    val context = LocalContext.current
    var info by remember { mutableStateOf<BatteryInfo?>(null) }
    var currentMa by remember { mutableFloatStateOf(Float.NaN) }
    var chargeMah by remember { mutableFloatStateOf(Float.NaN) }
    var tick by remember { mutableIntStateOf(0) }
    val stats = remember { Stats() }
    val history = remember { Ring(300) }
    val ticker = rememberTicker()

    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, i: Intent) {
                info = parse(i)
            }
        }
        val sticky = ContextCompat.registerReceiver(context, receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED), ContextCompat.RECEIVER_NOT_EXPORTED)
        if (sticky != null) info = parse(sticky)
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }

    LaunchedEffect(Unit) {
        val bm = context.getSystemService(BatteryManager::class.java)
        while (true) {
            if (bm != null) {
                val raw = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
                // Większość telefonów podaje µA, niektóre mA.
                currentMa = when {
                    raw == Int.MIN_VALUE || raw == 0 -> Float.NaN
                    abs(raw) > 20_000 -> raw / 1000f
                    else -> raw.toFloat()
                }
                val counter = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
                chargeMah = if (counter == Int.MIN_VALUE || counter <= 0) Float.NaN else counter / 1000f
            }
            if (!currentMa.isNaN()) {
                stats.add(currentMa)
                history.add(currentMa)
                ticker.tick()
            }
            val i = info
            Recorder.log(Meter.Battery.id, i?.level, if (currentMa.isNaN()) null else currentMa, i?.voltage, i?.tempC)
            tick++
            delay(1000)
        }
    }

    val i = info
    val power = if (i == null || currentMa.isNaN()) Float.NaN else abs(currentMa) / 1000f * i.voltage
    val capacity = if (i == null || chargeMah.isNaN() || i.level.isNaN() || i.level < 5f) Float.NaN else chargeMah / (i.level / 100f)
    MeterScaffold(
        Meter.Battery.id,
        stringResource(Meter.Battery.title),
        onBack,
        recordColumns = listOf("level_pct", "current_ma", "voltage_v", "temperature_c"),
        recordInterval = 900,
    ) {
        Panel {
            val level = i?.level ?: Float.NaN
            val color = when {
                level.isNaN() -> Palette.Fg
                level < 15f -> Palette.Danger
                level < 30f -> Palette.Warn
                else -> Palette.Ok
            }
            ArcGauge(if (level.isNaN()) 0f else level / 100f, color) {
                Readout(fmt(level, 0), "%", if (i == null) stringResource(R.string.waiting) else stringResource(statusRes(i.status)))
            }
            StatGrid(
                listOf(
                    stringResource(R.string.bat_current) to (if (currentMa.isNaN()) "—" else "${fmt(currentMa, 0)} mA"),
                    stringResource(R.string.bat_power) to (if (power.isNaN()) "—" else "${fmt(power, 2)} W"),
                    stringResource(R.string.bat_voltage) to (if (i == null) "—" else "${fmt(i.voltage, 2)} V"),
                    stringResource(R.string.temperature) to (if (i == null) "—" else "${fmt(Units.temp(i.tempC), 1)} ${Units.tempUnit}"),
                    stringResource(R.string.bat_source) to (if (i == null) "—" else stringResource(pluggedRes(i.plugged))),
                    stringResource(R.string.bat_health) to (if (i == null) "—" else stringResource(healthRes(i.health))),
                    stringResource(R.string.bat_charge) to (if (chargeMah.isNaN()) "—" else "${fmt(chargeMah, 0)} mAh"),
                    stringResource(R.string.bat_capacity) to (if (capacity.isNaN()) "—" else "≈${fmt(capacity, 0)} mAh"),
                    stringResource(R.string.bat_tech) to (i?.technology?.ifBlank { "—" } ?: "—"),
                ),
            )
        }
        Panel {
            SectionLabel(stringResource(R.string.bat_history))
            LineChart(listOf(Series(history, Palette.Fg)), ticker, fill = true, minSpan = 50f, yLabel = { fmt(it, 0) })
            Spacer(Modifier.height(12.dp))
            StatGrid(
                listOf(
                    stringResource(R.string.min) to "${fmt(stats.min, 0)} mA",
                    stringResource(R.string.avg) to "${fmt(stats.avg, 0)} mA",
                    stringResource(R.string.max) to "${fmt(stats.max, 0)} mA",
                ),
            )
            Spacer(Modifier.height(12.dp))
            Pill(stringResource(R.string.reset), icon = Icons.Rounded.RestartAlt) { stats.reset(); history.clear(); ticker.tick() }
        }
        Tip(stringResource(R.string.bat_tip))
    }
}
