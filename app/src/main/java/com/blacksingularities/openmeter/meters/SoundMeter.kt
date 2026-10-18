package com.blacksingularities.openmeter.meters

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.blacksingularities.openmeter.core.Fft
import com.blacksingularities.openmeter.core.Prefs
import com.blacksingularities.openmeter.core.Recorder
import com.blacksingularities.openmeter.core.Ring
import com.blacksingularities.openmeter.core.Stats
import com.blacksingularities.openmeter.core.aWeightDb
import com.blacksingularities.openmeter.core.db
import com.blacksingularities.openmeter.core.noteOf
import com.blacksingularities.openmeter.core.peakBin
import com.blacksingularities.openmeter.core.rememberTicker
import com.blacksingularities.openmeter.ui.ArcGauge
import com.blacksingularities.openmeter.ui.Bars
import com.blacksingularities.openmeter.ui.Digits
import com.blacksingularities.openmeter.ui.LineChart
import com.blacksingularities.openmeter.ui.MeterScaffold
import com.blacksingularities.openmeter.ui.Notice
import com.blacksingularities.openmeter.ui.Palette
import com.blacksingularities.openmeter.ui.Panel
import com.blacksingularities.openmeter.ui.Pill
import com.blacksingularities.openmeter.ui.Readout
import com.blacksingularities.openmeter.ui.SectionLabel
import com.blacksingularities.openmeter.ui.Segmented
import com.blacksingularities.openmeter.ui.Series
import com.blacksingularities.openmeter.ui.StatGrid
import com.blacksingularities.openmeter.ui.fmt
import com.blacksingularities.openmeter.ui.levelColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.pow

private const val SAMPLE_RATE = 44100
private const val FFT_N = 4096
private const val BANDS = 40
private const val WATERFALL_ROWS = 72
private const val F_LO = 31.5
private const val F_HI = 16000.0

/** Przybliżone przesunięcie dBFS → dB SPL dla typowego mikrofonu telefonu (korygowane kalibracją). */
private const val SPL_BASE = 100.0

private fun category(db: Float) = when {
    db < 30 -> "Szept, cisza nocna"
    db < 45 -> "Cicho"
    db < 60 -> "Spokojna rozmowa"
    db < 75 -> "Ruch uliczny, głośna rozmowa"
    db < 85 -> "Głośno"
    db < 100 -> "Szkodliwe przy długim narażeniu"
    else -> "Bardzo głośno – chroń słuch"
}

@SuppressLint("MissingPermission")
@Composable
fun SoundMeter(onBack: () -> Unit) {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    LaunchedEffect(Unit) { if (!granted) launcher.launch(Manifest.permission.RECORD_AUDIO) }

    var paused by remember { mutableStateOf(false) }
    var slow by remember { mutableIntStateOf(0) }
    var level by remember { mutableFloatStateOf(Float.NaN) }
    var leq by remember { mutableFloatStateOf(Float.NaN) }
    var peakHz by remember { mutableDoubleStateOf(0.0) }
    val stats = remember { Stats() }
    val energy = remember { doubleArrayOf(0.0, 0.0) }
    val history = remember { Ring(320) }
    val bands = remember { FloatArray(BANDS) }
    val waterfall = remember { Array(WATERFALL_ROWS) { FloatArray(BANDS) } }
    var wfHead by remember { mutableIntStateOf(0) }
    val ticker = rememberTicker()
    val weighting = if (Prefs.aWeighting) "dB(A)" else "dB(Z)"

    LaunchedEffect(granted, paused) {
        if (!granted || paused) return@LaunchedEffect
        withContext(Dispatchers.Default) {
            val fft = Fft(FFT_N)
            val binHz = SAMPLE_RATE.toDouble() / FFT_N
            val aGain = DoubleArray(FFT_N / 2) { 10.0.pow(aWeightDb(it * binHz) / 10) }
            // Granice pasm w skali logarytmicznej.
            val edges = IntArray(BANDS + 1) {
                val f = F_LO * (F_HI / F_LO).pow(it.toDouble() / BANDS)
                (f / binHz).toInt().coerceIn(1, FFT_N / 2 - 1)
            }
            val audio = context.getSystemService(AudioManager::class.java)
            val source = if (audio?.getProperty(AudioManager.PROPERTY_SUPPORT_AUDIO_SOURCE_UNPROCESSED) == "true") {
                MediaRecorder.AudioSource.UNPROCESSED
            } else {
                MediaRecorder.AudioSource.VOICE_RECOGNITION
            }
            val minBuf = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
            val rec = runCatching {
                AudioRecord(source, SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, maxOf(minBuf, FFT_N * 4))
            }.getOrNull() ?: return@withContext
            if (rec.state != AudioRecord.STATE_INITIALIZED) {
                rec.release()
                return@withContext
            }
            val pcm = ShortArray(FFT_N)
            val samples = DoubleArray(FFT_N)
            val mags = DoubleArray(FFT_N / 2)
            var smoothed = Double.NaN
            try {
                rec.startRecording()
                while (isActive) {
                    var off = 0
                    while (off < FFT_N && isActive) {
                        val r = rec.read(pcm, off, FFT_N - off)
                        if (r <= 0) break
                        off += r
                    }
                    if (off < FFT_N) continue
                    var mean = 0.0
                    for (i in 0 until FFT_N) {
                        samples[i] = pcm[i] / 32768.0
                        mean += samples[i]
                    }
                    mean /= FFT_N
                    var sq = 0.0
                    for (i in 0 until FFT_N) {
                        val v = samples[i] - mean
                        sq += v * v
                    }
                    val rmsDb = (sq / FFT_N).let { if (it <= 0) -120.0 else 10 * log10(it) }
                    fft.magnitudes(samples, mags)
                    var p = 0.0
                    var pa = 0.0
                    for (k in 1 until FFT_N / 2) {
                        val e = mags[k] * mags[k]
                        p += e
                        pa += e * aGain[k]
                    }
                    val weightedDb = if (Prefs.aWeighting && p > 0 && pa > 0) rmsDb + 10 * log10(pa / p) else rmsDb
                    val spl = weightedDb + SPL_BASE + Prefs.soundOffset
                    // Stała czasowa: szybka ~125 ms, wolna ~1 s (uśrednianie energii).
                    val alpha = if (slow == 1) 0.09 else 0.55
                    val e = 10.0.pow(spl / 10)
                    smoothed = if (smoothed.isNaN()) e else smoothed + alpha * (e - smoothed)
                    val shown = (10 * log10(smoothed)).toFloat().coerceAtLeast(0f)

                    val newBands = FloatArray(BANDS) { b ->
                        var s = 0.0
                        for (k in edges[b]..maxOf(edges[b], edges[b + 1] - 1)) s += mags[k] * mags[k]
                        ((10 * log10(s.coerceAtLeast(1e-14)) + 100) / 90).toFloat().coerceIn(0f, 1f)
                    }
                    val pk = peakBin(mags, 3, FFT_N / 2 - 2)
                    val pkDb = mags[pk.toInt().coerceIn(0, mags.size - 1)].db()
                    val freq = if (pkDb > -75) pk * binHz else 0.0

                    withContext(Dispatchers.Main) {
                        level = shown
                        stats.add(shown)
                        energy[0] += 10.0.pow(shown / 10.0)
                        energy[1] += 1.0
                        leq = (10 * log10(energy[0] / energy[1])).toFloat()
                        history.add(shown)
                        for (b in 0 until BANDS) bands[b] = bands[b] * 0.35f + newBands[b] * 0.65f
                        newBands.copyInto(waterfall[wfHead])
                        wfHead = (wfHead + 1) % WATERFALL_ROWS
                        peakHz = freq
                        ticker.tick()
                        Recorder.log(Meter.Sound.id, shown, leq, if (freq > 0) freq else null)
                    }
                }
            } finally {
                runCatching { rec.stop() }
                rec.release()
            }
        }
    }

    MeterScaffold(
        Meter.Sound.id,
        Meter.Sound.title,
        onBack,
        recordColumns = listOf("level_db", "leq_db", "peak_hz"),
        recordInterval = 0,
    ) {
        if (!granted) {
            Notice("Decybelomierz potrzebuje dostępu do mikrofonu. Dźwięk jest analizowany na bieżąco i nigdzie nie jest zapisywany.", "Zezwól") {
                launcher.launch(Manifest.permission.RECORD_AUDIO)
            }
            return@MeterScaffold
        }
        Panel {
            val frac = if (level.isNaN()) 0f else ((level - 20f) / 100f)
            ArcGauge(frac, levelColor(frac)) {
                Readout(fmt(level, 1), weighting, if (level.isNaN()) "Nasłuchiwanie…" else category(level))
            }
            StatGrid(
                listOf(
                    "Min" to fmt(stats.min, 1),
                    "Leq" to fmt(leq, 1),
                    "Maks" to fmt(stats.max, 1),
                ),
            )
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Pill(if (paused) "Wznów" else "Pauza", icon = if (paused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause) { paused = !paused }
                Pill("Zeruj", icon = Icons.Rounded.RestartAlt) {
                    stats.reset(); energy[0] = 0.0; energy[1] = 0.0; history.clear(); leq = Float.NaN; ticker.tick()
                }
                Segmented(listOf("Szybko", "Wolno"), slow, Modifier.weight(1f)) { slow = it }
            }
        }
        Panel {
            SectionLabel("Historia · ostatnie 30 s")
            LineChart(listOf(Series(history, Color.White)), ticker, fill = true, minSpan = 20f, yLabel = { fmt(it, 0) })
        }
        Panel {
            SectionLabel("Widmo")
            Bars(bands, ticker, colorOf = { v -> lerp(Color(0xFF3A3A3A), Color.White, v) })
            BandAxis()
        }
        Panel {
            SectionLabel("Spektrogram")
            Canvas(Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(12.dp)).background(Color.Black)) {
                @Suppress("UNUSED_VARIABLE") val t = ticker.state.intValue
                val cw = size.width / BANDS
                val ch = size.height / WATERFALL_ROWS
                for (r in 0 until WATERFALL_ROWS) {
                    // Najnowszy wiersz na górze.
                    val row = waterfall[(wfHead - 1 - r + WATERFALL_ROWS * 2) % WATERFALL_ROWS]
                    for (b in 0 until BANDS) {
                        val v = row[b]
                        if (v < 0.12f) continue
                        drawRect(heat(v), Offset(b * cw, r * ch), Size(cw + 0.5f, ch + 0.5f))
                    }
                }
            }
            BandAxis()
        }
        Panel {
            SectionLabel("Dominująca częstotliwość · stroik")
            val note = noteOf(peakHz)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(if (peakHz > 0) "${fmt(peakHz, 1)} Hz" else "—", color = Color.White, style = Digits, fontSize = 28.sp)
                    Text(
                        if (note != null) "${if (note.second >= 0) "+" else ""}${note.second} centów" else "Brak wyraźnego tonu",
                        color = Palette.TextDim,
                        fontSize = 13.sp,
                    )
                }
                Text(
                    note?.first ?: "",
                    color = if (note != null && abs(note.second) <= 5) Palette.Ok else Color.White,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(10.dp))
            TunerBar(note?.second)
        }
    }
}

@Composable
private fun BandAxis() {
    Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
        listOf("31", "125", "500", "2k", "8k", "16k").forEachIndexed { i, s ->
            Text(s, color = Palette.TextFaint, fontSize = 10.sp, modifier = Modifier.weight(1f))
            if (i == 5) Spacer(Modifier.width(0.dp))
        }
    }
}

@Composable
private fun TunerBar(cents: Int?) {
    Box(Modifier.fillMaxWidth().height(28.dp)) {
        Canvas(Modifier.fillMaxWidth().height(28.dp)) {
            val mid = size.width / 2
            for (i in -5..5) {
                val x = mid + i * size.width / 11
                drawLine(if (i == 0) Palette.Ok else Palette.TextFaint, Offset(x, if (i == 0) 0f else 8f), Offset(x, size.height - if (i == 0) 0f else 8f), if (i == 0) 3f else 1.5f)
            }
            if (cents != null) {
                val x = mid + cents / 50f * (size.width * 5 / 11)
                drawCircle(if (abs(cents) <= 5) Palette.Ok else Color.White, 8.dp.toPx(), Offset(x, size.height / 2))
            }
        }
    }
}

/** Mapa barw spektrogramu: granat → fiolet → czerwień → złoto → biel. */
private val heatStops = listOf(
    Color(0xFF0B0B2A), Color(0xFF4B1D7A), Color(0xFFC2307A), Color(0xFFFF7A3D), Color(0xFFFFE08A), Color.White,
)

private fun heat(v: Float): Color {
    val x = v.coerceIn(0f, 1f) * (heatStops.size - 1)
    val i = x.toInt().coerceAtMost(heatStops.size - 2)
    return lerp(heatStops[i], heatStops[i + 1], x - i)
}
