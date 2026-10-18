package com.blacksingularities.openmeter.core

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Ustawienia aplikacji – trwałe, a jednocześnie obserwowalne przez Compose. */
object Prefs {
    private lateinit var sp: SharedPreferences

    fun init(context: Context) {
        if (::sp.isInitialized) return
        sp = context.getSharedPreferences("openmeter", Context.MODE_PRIVATE)
        soundOffset = sp.getFloat("soundOffset", 0f)
        aWeighting = sp.getBoolean("aWeighting", true)
        keepScreenOn = sp.getBoolean("keepScreenOn", true)
        haptics = sp.getBoolean("haptics", true)
        detectorSound = sp.getBoolean("detectorSound", true)
        seaLevelHpa = sp.getFloat("seaLevelHpa", 1013.25f)
        autoUpdate = sp.getBoolean("autoUpdate", true)
    }

    var soundOffset by mutableFloatStateOf(0f)
        private set
    var aWeighting by mutableStateOf(true)
        private set
    var keepScreenOn by mutableStateOf(true)
        private set
    var haptics by mutableStateOf(true)
        private set
    var detectorSound by mutableStateOf(true)
        private set
    var seaLevelHpa by mutableFloatStateOf(1013.25f)
        private set
    var autoUpdate by mutableStateOf(true)
        private set

    fun setSoundOffset(v: Float) { soundOffset = v; sp.edit().putFloat("soundOffset", v).apply() }
    fun setAWeighting(v: Boolean) { aWeighting = v; sp.edit().putBoolean("aWeighting", v).apply() }
    fun setKeepScreenOn(v: Boolean) { keepScreenOn = v; sp.edit().putBoolean("keepScreenOn", v).apply() }
    fun setHaptics(v: Boolean) { haptics = v; sp.edit().putBoolean("haptics", v).apply() }
    fun setDetectorSound(v: Boolean) { detectorSound = v; sp.edit().putBoolean("detectorSound", v).apply() }
    fun setSeaLevelHpa(v: Float) { seaLevelHpa = v; sp.edit().putFloat("seaLevelHpa", v).apply() }
    fun setAutoUpdate(v: Boolean) { autoUpdate = v; sp.edit().putBoolean("autoUpdate", v).apply() }

    /** Historia ciśnienia: pary „czas;hPa” zapisywane co najmniej co minutę (do 48 h). */
    fun pressureHistory(): List<Pair<Long, Float>> =
        sp.getString("pressureHistory", "")!!.split('|').mapNotNull { e ->
            val p = e.split(';')
            if (p.size != 2) null else p[0].toLongOrNull()?.let { t -> p[1].toFloatOrNull()?.let { t to it } }
        }

    private var lastPressureAt = 0L

    /** Dopisuje próbkę (najwyżej raz na minutę); zwraca true, gdy została zapisana. */
    fun addPressure(time: Long, hpa: Float): Boolean {
        if (time - lastPressureAt < 60_000L) return false
        val list = pressureHistory()
        lastPressureAt = list.lastOrNull()?.first ?: 0L
        if (time - lastPressureAt < 60_000L) return false
        lastPressureAt = time
        val cut = time - 48 * 3600_000L
        val kept = (list.filter { it.first >= cut } + (time to hpa)).takeLast(2900)
        sp.edit().putString("pressureHistory", kept.joinToString("|") { "${it.first};${it.second}" }).apply()
        return true
    }

    fun clearPressureHistory() {
        sp.edit().remove("pressureHistory").apply()
        lastPressureAt = 0L
    }
}
