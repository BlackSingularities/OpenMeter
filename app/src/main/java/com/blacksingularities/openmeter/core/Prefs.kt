package com.blacksingularities.openmeter.core

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateOf

/** Ustawienia aplikacji – trwałe, a jednocześnie obserwowalne przez Compose. */
object Prefs {
    private lateinit var sp: SharedPreferences

    fun init(context: Context) {
        if (::sp.isInitialized) return
        sp = context.getSharedPreferences("openmeter", Context.MODE_PRIVATE)
        soundOffsetState.value = sp.getFloat("soundOffset", 0f)
        aWeightingState.value = sp.getBoolean("aWeighting", true)
        keepScreenOnState.value = sp.getBoolean("keepScreenOn", true)
        hapticsState.value = sp.getBoolean("haptics", true)
        detectorSoundState.value = sp.getBoolean("detectorSound", true)
        seaLevelHpaState.value = sp.getFloat("seaLevelHpa", 1013.25f)
        autoUpdateState.value = sp.getBoolean("autoUpdate", true)
        homeListState.value = sp.getBoolean("homeList", false)
        favoritesState.value = sp.getString("favorites", "")!!.split(',').filter { it.isNotBlank() }.toSet()
        recentState.value = sp.getString("recent", "")!!.split(',').filter { it.isNotBlank() }
    }

    private val homeListState = mutableStateOf(false)
    /** Ekran główny jako lista zamiast siatki kafelków. */
    var homeList: Boolean
        get() = homeListState.value
        set(v) {
            homeListState.value = v
            sp.edit().putBoolean("homeList", v).apply()
        }

    private val favoritesState = mutableStateOf<Set<String>>(emptySet())
    val favorites: Set<String> get() = favoritesState.value

    fun toggleFavorite(id: String) {
        val next = if (id in favorites) favorites - id else favorites + id
        favoritesState.value = next
        sp.edit().putString("favorites", next.joinToString(",")).apply()
    }

    private val recentState = mutableStateOf<List<String>>(emptyList())
    /** Ostatnio otwierane mierniki, najnowszy pierwszy. */
    val recent: List<String> get() = recentState.value

    fun pushRecent(id: String) {
        val next = (listOf(id) + recent.filter { it != id }).take(6)
        recentState.value = next
        sp.edit().putString("recent", next.joinToString(",")).apply()
    }

    fun clearPersonalization() {
        favoritesState.value = emptySet()
        recentState.value = emptyList()
        sp.edit().remove("favorites").remove("recent").apply()
    }

    private val soundOffsetState = mutableStateOf(0f)
    var soundOffset: Float
        get() = soundOffsetState.value
        set(v) {
            soundOffsetState.value = v
            sp.edit().putFloat("soundOffset", v).apply()
        }

    private val aWeightingState = mutableStateOf(true)
    var aWeighting: Boolean
        get() = aWeightingState.value
        set(v) {
            aWeightingState.value = v
            sp.edit().putBoolean("aWeighting", v).apply()
        }

    private val keepScreenOnState = mutableStateOf(true)
    var keepScreenOn: Boolean
        get() = keepScreenOnState.value
        set(v) {
            keepScreenOnState.value = v
            sp.edit().putBoolean("keepScreenOn", v).apply()
        }

    private val hapticsState = mutableStateOf(true)
    var haptics: Boolean
        get() = hapticsState.value
        set(v) {
            hapticsState.value = v
            sp.edit().putBoolean("haptics", v).apply()
        }

    private val detectorSoundState = mutableStateOf(true)
    var detectorSound: Boolean
        get() = detectorSoundState.value
        set(v) {
            detectorSoundState.value = v
            sp.edit().putBoolean("detectorSound", v).apply()
        }

    private val seaLevelHpaState = mutableStateOf(1013.25f)
    var seaLevelHpa: Float
        get() = seaLevelHpaState.value
        set(v) {
            seaLevelHpaState.value = v
            sp.edit().putFloat("seaLevelHpa", v).apply()
        }

    private val autoUpdateState = mutableStateOf(true)
    var autoUpdate: Boolean
        get() = autoUpdateState.value
        set(v) {
            autoUpdateState.value = v
            sp.edit().putBoolean("autoUpdate", v).apply()
        }

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
