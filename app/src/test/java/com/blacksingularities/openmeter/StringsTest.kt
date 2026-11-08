package com.blacksingularities.openmeter

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Pilnuje tłumaczeń w res/values-xx/strings.xml względem domyślnego angielskiego
 * res/values/strings.xml: każdy język ma komplet tekstów, nie ma kluczy spoza wzorca,
 * a znaczniki formatowania (%1$s, %d…) się zgadzają.
 */
class StringsTest {
    private val res = File("src/main/res")
    private val entry = Regex("""<(string|plurals)\s+name="(\w+)"([^>]*)>(.*?)</\1>""", RegexOption.DOT_MATCHES_ALL)
    private val placeholder = Regex("""%(\d+\$)?[sdf]""")

    private fun read(file: File): Map<String, String> =
        entry.findAll(file.readText())
            .filterNot { it.groupValues[3].contains("translatable=\"false\"") }
            .associate { it.groupValues[2] to it.groupValues[4] }

    private fun translations(): List<File> =
        (res.listFiles() ?: emptyArray())
            .filter { it.isDirectory && it.name.startsWith("values-") }
            .map { File(it, "strings.xml") }
            .filter { it.exists() }

    @Test
    fun everyLanguageIsComplete() {
        val base = read(File(res, "values/strings.xml"))
        assertTrue("Brak domyślnych tekstów", base.isNotEmpty())
        val problems = mutableListOf<String>()
        for (file in translations()) {
            val lang = file.parentFile.name
            val tr = read(file)
            (base.keys - tr.keys).sorted().forEach { problems += "$lang: brak „$it”" }
            (tr.keys - base.keys).sorted().forEach { problems += "$lang: nadmiarowy „$it”" }
            for ((key, text) in tr) {
                val en = base[key] ?: continue
                val a = placeholder.findAll(en).map { it.value }.toSortedSet()
                val b = placeholder.findAll(text).map { it.value }.toSortedSet()
                if (a != b) problems += "$lang: znaczniki „$key” $a ≠ $b"
            }
        }
        assertTrue(problems.joinToString("\n"), problems.isEmpty())
    }

    @Test
    fun noUnescapedApostrophes() {
        val bad = (translations() + File(res, "values/strings.xml")).flatMap { file ->
            read(file).filter { (_, v) -> Regex("""(?<!\\)'""").containsMatchIn(v) }.keys.map { "${file.parentFile.name}: $it" }
        }
        assertTrue("Nieescapowany apostrof: $bad", bad.isEmpty())
    }
}
