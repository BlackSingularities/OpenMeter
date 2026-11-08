package com.blacksingularities.openmeter.core

/** Przeliczanie jednostek według ustawień (metryczne / imperialne, jednostka ciśnienia). */
object Units {
    val tempUnit: String get() = if (Prefs.imperial) "°F" else "°C"
    fun temp(c: Float): Float = if (Prefs.imperial) c * 9f / 5f + 32f else c

    val lengthUnit: String get() = if (Prefs.imperial) "ft" else "m"
    fun length(m: Float): Float = if (Prefs.imperial) m * 3.28084f else m

    val distanceUnit: String get() = if (Prefs.imperial) "mi" else "km"
    fun distance(km: Float): Float = if (Prefs.imperial) km * 0.621371f else km

    val pressureUnit: String get() = when (Prefs.pressureUnit) {
        1 -> "mmHg"
        2 -> "inHg"
        else -> "hPa"
    }

    fun pressure(hpa: Float): Float = when (Prefs.pressureUnit) {
        1 -> hpa * 0.750062f
        2 -> hpa * 0.02953f
        else -> hpa
    }

    /** Liczba miejsc po przecinku dla bieżącej jednostki ciśnienia. */
    val pressureDigits: Int get() = if (Prefs.pressureUnit == 2) 2 else 1
}
