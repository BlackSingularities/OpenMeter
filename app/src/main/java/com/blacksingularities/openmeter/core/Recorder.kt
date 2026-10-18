package com.blacksingularities.openmeter.core

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.os.SystemClock
import android.provider.MediaStore
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Nagrywanie pomiarów do CSV. Jeden aktywny zapis naraz; miernik podaje nazwy kolumn
 * i dopisuje wiersze, a [minIntervalMs] ogranicza częstotliwość zapisu.
 */
object Recorder {
    var active by mutableStateOf<String?>(null)
        private set
    var rows by mutableLongStateOf(0L)
        private set
    var startedAt by mutableLongStateOf(0L)
        private set

    private var columns: List<String> = emptyList()
    private var buffer = StringBuilder()
    private var minIntervalMs = 0L
    private var lastRowAt = 0L
    private var startWall = 0L
    private var title = ""

    fun isRecording(meterId: String) = active == meterId

    fun start(meterId: String, meterTitle: String, cols: List<String>, minInterval: Long = 50L) {
        active = meterId
        title = meterTitle
        columns = cols
        minIntervalMs = minInterval
        buffer = StringBuilder()
        buffer.append("time_iso,elapsed_s,").append(cols.joinToString(",")).append('\n')
        rows = 0
        lastRowAt = 0
        startedAt = SystemClock.elapsedRealtime()
        startWall = System.currentTimeMillis()
    }

    /** Dopisuje wiersz, jeśli zapis dotyczy tego miernika i minął odstęp. */
    fun log(meterId: String, vararg values: Any?) {
        if (active != meterId) return
        val now = SystemClock.elapsedRealtime()
        if (minIntervalMs > 0 && now - lastRowAt < minIntervalMs) return
        lastRowAt = now
        val elapsed = (now - startedAt) / 1000.0
        buffer.append(iso.format(Date(startWall + (now - startedAt)))).append(',')
            .append(String.format(Locale.US, "%.3f", elapsed))
        for (v in values) {
            buffer.append(',')
            when (v) {
                null -> {}
                is Float -> if (!v.isNaN()) buffer.append(String.format(Locale.US, "%.5f", v))
                is Double -> if (!v.isNaN()) buffer.append(String.format(Locale.US, "%.5f", v))
                else -> buffer.append(v.toString().replace(',', ';'))
            }
        }
        buffer.append('\n')
        rows++
    }

    /** Kończy zapis: plik w katalogu aplikacji i kopia w Pobranych/OpenMeter. */
    fun stop(context: Context): File? {
        val id = active ?: return null
        active = null
        if (rows == 0L) return null
        val dir = recordingsDir(context)
        val name = "OpenMeter_${id}_${fileStamp.format(Date(startWall))}.csv"
        val file = File(dir, name)
        file.writeText(buffer.toString())
        buffer = StringBuilder()
        runCatching { copyToDownloads(context, file) }
        return file
    }

    fun cancel() {
        active = null
        buffer = StringBuilder()
    }

    fun recordingsDir(context: Context) = File(context.filesDir, "recordings").apply { mkdirs() }

    fun list(context: Context): List<File> =
        recordingsDir(context).listFiles { f -> f.extension == "csv" }?.sortedByDescending { it.lastModified() } ?: emptyList()

    fun share(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, context.packageName + ".files", file)
        val send = Intent(Intent.ACTION_SEND)
            .setType("text/csv")
            .putExtra(Intent.EXTRA_STREAM, uri)
            .putExtra(Intent.EXTRA_SUBJECT, file.nameWithoutExtension)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(Intent.createChooser(send, file.name).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun copyToDownloads(context: Context, file: File) {
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, file.name)
            put(MediaStore.Downloads.MIME_TYPE, "text/csv")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/OpenMeter")
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return
        resolver.openOutputStream(uri)?.use { out -> file.inputStream().use { it.copyTo(out) } }
    }

    private val iso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US)
    private val fileStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
}
