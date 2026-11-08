package com.blacksingularities.openmeter.core

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.blacksingularities.openmeter.BuildConfig
import java.util.Locale

/**
 * Język aplikacji (per-app language, Android 13+ także w ustawieniach systemu).
 *
 * Teksty są w zasobach: `res/values/strings.xml` (angielski, domyślny) i `res/values-xx/strings.xml`
 * dla pozostałych języków. Lista języków do wyboru powstaje przy budowaniu z folderów
 * `values-xx` (patrz `app/build.gradle.kts`), więc nowy język to tylko nowy plik `strings.xml`.
 */
object AppLanguage {
    /** Tagi dostępnych języków, np. ["en", "pl"]. */
    val tags: List<String> get() = BuildConfig.LANGUAGES.split(',').map { it.trim() }.filter { it.isNotEmpty() }

    /** Tag wybranego języka albo "" = język systemu. */
    fun current(): String {
        val chosen = AppCompatDelegate.getApplicationLocales()
        if (chosen.isEmpty) return ""
        val lang = chosen[0]?.language ?: return ""
        return tags.firstOrNull { Locale.forLanguageTag(it).language == lang } ?: ""
    }

    /** Ustawia język; "" = jak w systemie. Aktywność odtwarza się sama. */
    fun set(tag: String) {
        AppCompatDelegate.setApplicationLocales(
            if (tag.isEmpty()) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(tag),
        )
    }

    /** Nazwa języka w tym języku, np. „Polski”, „English”. */
    fun displayName(tag: String): String {
        val l = Locale.forLanguageTag(tag)
        return l.getDisplayName(l).replaceFirstChar { it.titlecase(l) }
    }
}
