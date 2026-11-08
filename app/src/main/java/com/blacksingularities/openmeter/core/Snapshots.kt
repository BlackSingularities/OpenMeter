package com.blacksingularities.openmeter.core

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.FileProvider
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Pojedynczy zapisany odczyt miernika. */
data class Snapshot(
    val id: Long,
    val meterId: String,
    val time: Long,
    val text: String,
    val note: String,
)

/**
 * Dziennik odczytów: zapis bieżącej wartości miernika jednym dotknięciem (z opcjonalną notatką).
 * Przechowywany w pliku JSON w katalogu aplikacji.
 */
object Snapshots {
    private const val FILE = "snapshots.json"
    private val state = mutableStateOf<List<Snapshot>>(emptyList())
    private var loaded = false

    /** Najnowsze pierwsze. */
    val items: List<Snapshot> get() = state.value

    fun load(context: Context) {
        if (loaded) return
        loaded = true
        state.value = runCatching {
            val f = File(context.filesDir, FILE)
            if (!f.exists()) return@runCatching emptyList()
            val arr = JSONArray(f.readText())
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                Snapshot(o.getLong("id"), o.getString("meter"), o.getLong("time"), o.getString("text"), o.optString("note"))
            }
        }.getOrDefault(emptyList())
    }

    private fun save(context: Context, list: List<Snapshot>) {
        state.value = list
        runCatching {
            val arr = JSONArray()
            list.forEach { s ->
                arr.put(
                    JSONObject().put("id", s.id).put("meter", s.meterId).put("time", s.time)
                        .put("text", s.text).put("note", s.note),
                )
            }
            File(context.filesDir, FILE).writeText(arr.toString())
        }
    }

    fun add(context: Context, meterId: String, text: String): Snapshot {
        load(context)
        val now = System.currentTimeMillis()
        val s = Snapshot(now, meterId, now, text, "")
        save(context, (listOf(s) + items).take(1000))
        return s
    }

    fun setNote(context: Context, id: Long, note: String) {
        save(context, items.map { if (it.id == id) it.copy(note = note.trim()) else it })
    }

    fun delete(context: Context, id: Long) {
        save(context, items.filter { it.id != id })
    }

    fun clear(context: Context) {
        save(context, emptyList())
    }

    /** Udostępnia cały dziennik jako plik CSV. [meterName] zamienia identyfikator na nazwę. */
    fun share(context: Context, meterName: (String) -> String) {
        val iso = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val csv = buildString {
            append("time,meter,reading,note\n")
            items.forEach { s ->
                append(iso.format(Date(s.time))).append(',')
                    .append(quote(meterName(s.meterId))).append(',')
                    .append(quote(s.text)).append(',')
                    .append(quote(s.note)).append('\n')
            }
        }
        val shareDir = File(context.cacheDir, "share").apply { mkdirs() }
        val shared = File(shareDir, "OpenMeter_log.csv")
        shared.writeText(csv)
        val uri = FileProvider.getUriForFile(context, context.packageName + ".files", shared)
        val send = Intent(Intent.ACTION_SEND)
            .setType("text/csv")
            .putExtra(Intent.EXTRA_STREAM, uri)
            .putExtra(Intent.EXTRA_SUBJECT, "OpenMeter")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(Intent.createChooser(send, shared.name).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun quote(s: String) = "\"" + s.replace("\"", "\"\"") + "\""
}
