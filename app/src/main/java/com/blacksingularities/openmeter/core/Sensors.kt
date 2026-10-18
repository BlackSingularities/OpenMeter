package com.blacksingularities.openmeter.core

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext

fun Context.sensorManager(): SensorManager = getSystemService(SensorManager::class.java)

fun Context.hasSensor(type: Int): Boolean = sensorManager().getDefaultSensor(type) != null

fun Context.hasMicrophone(): Boolean = packageManager.hasSystemFeature(PackageManager.FEATURE_MICROPHONE)

/**
 * Nasłuch czujnika na czas obecności na ekranie. [onEvent] dostaje kopię wartości
 * i znacznik czasu w nanosekundach. Gdy czujnika brak – nic się nie dzieje.
 */
@Composable
fun SensorEffect(
    type: Int,
    delay: Int = SensorManager.SENSOR_DELAY_GAME,
    enabled: Boolean = true,
    onAccuracy: (Int) -> Unit = {},
    onEvent: (values: FloatArray, timestampNs: Long) -> Unit,
) {
    val context = LocalContext.current
    val acc = rememberUpdatedState(onAccuracy)
    val handler = rememberUpdatedState(onEvent)
    DisposableEffect(type, delay, enabled) {
        val sm = context.sensorManager()
        val sensor = sm.getDefaultSensor(type)
        if (!enabled || sensor == null) return@DisposableEffect onDispose { }
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                handler.value(event.values.copyOf(), event.timestamp)
            }

            override fun onAccuracyChanged(s: Sensor?, accuracy: Int) {
                acc.value(accuracy)
            }
        }
        sm.registerListener(listener, sensor, delay)
        onDispose { sm.unregisterListener(listener) }
    }
}

/** Licznik zmian do odświeżania płócien rysowanych z buforów (bez kopiowania danych do stanu). */
class Ticker {
    val state = mutableIntStateOf(0)
    fun tick() {
        state.intValue++
    }
}

@Composable
fun rememberTicker() = remember { Ticker() }

/** Bufor cykliczny próbek do wykresów. */
class Ring(val capacity: Int) {
    private val data = FloatArray(capacity)
    var size = 0
        private set
    private var head = 0

    fun add(v: Float) {
        data[head] = v
        head = (head + 1) % capacity
        if (size < capacity) size++
    }

    /** i = 0 to najstarsza próbka. */
    operator fun get(i: Int): Float = data[(head - size + i + capacity * 2) % capacity]

    fun last(): Float = if (size == 0) 0f else get(size - 1)

    fun clear() {
        size = 0
        head = 0
    }

    fun min(): Float {
        var m = Float.POSITIVE_INFINITY
        for (i in 0 until size) m = minOf(m, get(i))
        return m
    }

    fun max(): Float {
        var m = Float.NEGATIVE_INFINITY
        for (i in 0 until size) m = maxOf(m, get(i))
        return m
    }
}

/** Minimum, maksimum i średnia od początku pomiaru. */
class Stats {
    var min = Float.NaN
        private set
    var max = Float.NaN
        private set
    private var sum = 0.0
    var count = 0L
        private set
    val avg: Float get() = if (count == 0L) Float.NaN else (sum / count).toFloat()

    fun add(v: Float) {
        if (v.isNaN()) return
        min = if (min.isNaN()) v else minOf(min, v)
        max = if (max.isNaN()) v else maxOf(max, v)
        sum += v
        count++
    }

    fun reset() {
        min = Float.NaN
        max = Float.NaN
        sum = 0.0
        count = 0
    }
}

/** Prosty filtr dolnoprzepustowy (wygładzanie odczytów). */
class LowPass(private val alpha: Float) {
    private var v = Float.NaN
    fun filter(x: Float): Float {
        v = if (v.isNaN()) x else v + alpha * (x - v)
        return v
    }

    fun reset() {
        v = Float.NaN
    }
}
