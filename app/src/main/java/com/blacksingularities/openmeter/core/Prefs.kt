package com.blacksingularities.openmeter.core

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf

/** Motyw interfejsu. */
enum class ThemeMode { SYSTEM, DARK, LIGHT }

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
        themeState.value = runCatching { ThemeMode.valueOf(sp.getString("theme", ThemeMode.SYSTEM.name)!!) }.getOrDefault(ThemeMode.SYSTEM)
        imperialState.value = sp.getBoolean("imperial", false)
        pressureUnitState.value = sp.getInt("pressureUnit", 0)
        onboardedState.value = sp.getBoolean("onboarded", false)
        noiseAlarmState.value = sp.getFloat("noiseAlarm", 0f)
        stepLengthState.value = sp.getFloat("stepLength", 0.75f)
    }

    private fun <T> MutableState<T>.save(v: T, write: SharedPreferences.Editor.() -> Unit) {
        value = v
        sp.edit().apply(write).apply()
    }

    private val themeState = mutableStateOf(ThemeMode.SYSTEM)
    var theme: ThemeMode
        get() = themeState.value
        set(v) = themeState.save(v) { putString("theme", v.name) }

    private val imperialState = mutableStateOf(false)
    /** Jednostki imperialne: °F, stopy, mile. */
    var imperial: Boolean
        get() = imperialState.value
        set(v) = imperialState.save(v) { putBoolean("imperial", v) }

    private val pressureUnitState = mutableStateOf(0)
    /** Jednostka ciśnienia: 0 = hPa, 1 = mmHg, 2 = inHg. */
    var pressureUnit: Int
        get() = pressureUnitState.value
        set(v) = pressureUnitState.save(v) { putInt("pressureUnit", v) }

    private val onboardedState = mutableStateOf(false)
    /** Czy ekran powitalny został już pokazany. */
    var onboarded: Boolean
        get() = onboardedState.value
        set(v) = onboardedState.save(v) { putBoolean("onboarded", v) }

    private val noiseAlarmState = mutableStateOf(0f)
    /** Próg alarmu hałasu w dB; 0 = wyłączony. */
    var noiseAlarm: Float
        get() = noiseAlarmState.value
        set(v) = noiseAlarmState.save(v) { putFloat("noiseAlarm", v) }

    private val stepLengthState = mutableStateOf(0.75f)
    /** Długość kroku w metrach (krokomierz). */
    var stepLength: Float
        get() = stepLengthState.value
        set(v) = stepLengthState.save(v) { putFloat("stepLength", v) }

    private val homeListState = mutableStateOf(false)
    /** Ekran główny jako lista zamiast siatki kafelków. */
    var homeList: Boolean
        get() = homeListState.value
        set(v) = homeListState.save(v) { putBoolean("homeList", v) }

    private val favoritesState = mutableStateOf<Set<String>>(emptySet())
    val favorites: Set<String> get() = favoritesState.value

    fun toggleFavorite(id: String) {
        val next = if (id in favorites) favorites - id else favorites + id
        favoritesState.save(next) { putString("favorites", next.joinToString(",")) }
    }

    private val recentState = mutableStateOf<List<String>>(emptyList())
    /** Ostatnio otwierane mierniki, najnowszy pierwszy. */
    val recent: List<String> get() = recentState.value

    fun pushRecent(id: String) {
        val next = (listOf(id) + recent.filter { it != id }).take(6)
        recentState.save(next) { putString("recent", next.joinToString(",")) }
    }

    fun clearPersonalization() {
        favoritesState.value = emptySet()
        recentState.value = emptyList()
        sp.edit().remove("favorites").remove("recent").apply()
    }

    private val soundOffsetState = mutableStateOf(0f)
    var soundOffset: Float
        get() = soundOffsetState.value
        set(v) = soundOffsetState.save(v) { putFloat("soundOffset", v) }

    private val aWeightingState = mutableStateOf(true)
    var aWeighting: Boolean
        get() = aWeightingState.value
        set(v) = aWeightingState.save(v) { putBoolean("aWeighting", v) }

    private val keepScreenOnState = mutableStateOf(true)
    var keepScreenOn: Boolean
        get() = keepScreenOnState.value
        set(v) = keepScreenOnState.save(v) { putBoolean("keepScreenOn", v) }

    private val hapticsState = mutableStateOf(true)
    var haptics: Boolean
        get() = hapticsState.value
        set(v) = hapticsState.save(v) { putBoolean("haptics", v) }

    private val detectorSoundState = mutableStateOf(true)
    var detectorSound: Boolean
        get() = detectorSoundState.value
        set(v) = detectorSoundState.save(v) { putBoolean("detectorSound", v) }

    private val seaLevelHpaState = mutableStateOf(1013.25f)
    var seaLevelHpa: Float
        get() = seaLevelHpaState.value
        set(v) = seaLevelHpaState.save(v) { putFloat("seaLevelHpa", v) }

    private val autoUpdateState = mutableStateOf(true)
    var autoUpdate: Boolean
        get() = autoUpdateState.value
        set(v) = autoUpdateState.save(v) { putBoolean("autoUpdate", v) }

    /** Przywraca kalibracje i ustawienia mierników do wartości domyślnych. */
    fun resetCalibration() {
        soundOffset = 0f
        seaLevelHpa = 1013.25f
        noiseAlarm = 0f
        stepLength = 0.75f
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
