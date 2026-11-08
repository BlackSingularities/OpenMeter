package com.blacksingularities.openmeter.meters

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blacksingularities.openmeter.R
import com.blacksingularities.openmeter.ui.Digits
import com.blacksingularities.openmeter.ui.MeterScaffold
import com.blacksingularities.openmeter.ui.Palette
import com.blacksingularities.openmeter.ui.Panel
import com.blacksingularities.openmeter.ui.Pill
import com.blacksingularities.openmeter.ui.Readout
import com.blacksingularities.openmeter.ui.RoundIcon
import com.blacksingularities.openmeter.ui.SectionLabel
import com.blacksingularities.openmeter.ui.Segmented
import com.blacksingularities.openmeter.ui.Tip
import com.blacksingularities.openmeter.ui.fmt
import com.blacksingularities.openmeter.ui.sliderColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sin

// ───────────────────────────── Linijka ─────────────────────────────

/**
 * Linijka na ekranie: podziałka milimetrowa po lewej i calowa po prawej, dwie przesuwane
 * linie pomiarowe. Skala z gęstości ekranu (ydpi) z możliwością korekty.
 */
@Composable
fun RulerTool(onBack: () -> Unit) {
    val context = LocalContext.current
    val ydpi = remember { context.resources.displayMetrics.ydpi.coerceAtLeast(100f) }
    var scale by remember { mutableFloatStateOf(1f) }
    var a by remember { mutableFloatStateOf(0f) }
    var b by remember { mutableFloatStateOf(Float.NaN) }
    val pxPerMm = ydpi / 25.4f * scale
    val mm = if (b.isNaN()) 0f else abs(b - a) / pxPerMm
    val fg = Palette.Fg
    val faint = Palette.TextFaint
    val accent = Palette.Record
    MeterScaffold(Meter.Ruler.id, stringResource(Meter.Ruler.title), onBack) {
        Panel {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Readout(fmt(mm / 10f, 2), "cm", modifier = Modifier.weight(1f), size = 36.sp)
                Readout(fmt(mm / 25.4f, 2), "in", modifier = Modifier.weight(1f), size = 36.sp)
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Pill(stringResource(R.string.reset), icon = Icons.Rounded.RestartAlt) { a = 0f; b = Float.NaN }
                Spacer(Modifier.weight(1f))
                Text("×${fmt(scale, 3)}", color = Palette.TextDim, style = Digits, fontSize = 13.sp)
                RoundIcon(Icons.Rounded.Remove, stringResource(R.string.ruler_scale_minus)) { scale = (scale - 0.005f).coerceAtLeast(0.8f) }
                RoundIcon(Icons.Rounded.Add, stringResource(R.string.ruler_scale_plus)) { scale = (scale + 0.005f).coerceAtMost(1.2f) }
            }
        }
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(560.dp)
                .pointerInput(pxPerMm) {
                    var moving = 0
                    detectDragGestures(
                        onDragStart = { p ->
                            if (b.isNaN()) b = p.y
                            moving = if (abs(p.y - a) < abs(p.y - b)) 0 else 1
                            if (moving == 0) a = p.y else b = p.y
                        },
                    ) { change, _ ->
                        val y = change.position.y.coerceIn(0f, size.height.toFloat())
                        if (moving == 0) a = y else b = y
                    }
                },
        ) {
            val paint = android.graphics.Paint().apply {
                isAntiAlias = true
                textSize = 11.dp.toPx()
                color = fg.toArgb()
            }
            // Milimetry po lewej.
            var i = 0
            while (i * pxPerMm <= size.height) {
                val y = i * pxPerMm
                val len = when {
                    i % 10 == 0 -> 28.dp.toPx()
                    i % 5 == 0 -> 18.dp.toPx()
                    else -> 10.dp.toPx()
                }
                drawLine(if (i % 10 == 0) fg else faint, Offset(0f, y), Offset(len, y), if (i % 10 == 0) 2f else 1.2f)
                if (i % 10 == 0 && i > 0) {
                    drawContext.canvas.nativeCanvas.drawText("${i / 10}", 32.dp.toPx(), y + 4.dp.toPx(), paint)
                }
                i++
            }
            // Cale po prawej (co 1/8 cala).
            val pxPerEighth = pxPerMm * 25.4f / 8f
            var j = 0
            paint.textAlign = android.graphics.Paint.Align.RIGHT
            while (j * pxPerEighth <= size.height) {
                val y = j * pxPerEighth
                val len = when {
                    j % 8 == 0 -> 28.dp.toPx()
                    j % 4 == 0 -> 20.dp.toPx()
                    j % 2 == 0 -> 14.dp.toPx()
                    else -> 8.dp.toPx()
                }
                drawLine(if (j % 8 == 0) fg else faint, Offset(size.width - len, y), Offset(size.width, y), if (j % 8 == 0) 2f else 1.2f)
                if (j % 8 == 0 && j > 0) {
                    drawContext.canvas.nativeCanvas.drawText("${j / 8}\"", size.width - 32.dp.toPx(), y + 4.dp.toPx(), paint)
                }
                j++
            }
            // Linie pomiarowe.
            drawLine(accent, Offset(0f, a), Offset(size.width, a), 2.dp.toPx(), cap = StrokeCap.Round)
            if (!b.isNaN()) {
                drawLine(accent, Offset(0f, b), Offset(size.width, b), 2.dp.toPx(), cap = StrokeCap.Round)
                drawRect(accent.copy(alpha = 0.08f), Offset(0f, minOf(a, b)), androidx.compose.ui.geometry.Size(size.width, abs(b - a)))
            }
        }
        Tip(stringResource(R.string.ruler_tip))
    }
}

// ───────────────────────────── Generator tonu ─────────────────────────────

private const val TONE_SR = 44100
private const val F_MIN = 20f
private const val F_MAX = 20000f

private fun freqOf(p: Float) = F_MIN * (F_MAX / F_MIN).pow(p)
private fun posOf(f: Float) = (ln(f / F_MIN) / ln(F_MAX / F_MIN)).coerceIn(0f, 1f)

@Composable
fun ToneGeneratorTool(onBack: () -> Unit) {
    var playing by remember { mutableStateOf(false) }
    var freq by remember { mutableFloatStateOf(440f) }
    var volume by remember { mutableFloatStateOf(0.5f) }
    var wave by remember { mutableIntStateOf(0) }
    val f by rememberUpdatedState(freq)
    val vol by rememberUpdatedState(volume)
    val w by rememberUpdatedState(wave)

    LaunchedEffect(playing) {
        if (!playing) return@LaunchedEffect
        withContext(Dispatchers.Default) {
            val minBuf = AudioTrack.getMinBufferSize(TONE_SR, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
            val track = runCatching {
                AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build(),
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setSampleRate(TONE_SR)
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build(),
                    )
                    .setBufferSizeInBytes(maxOf(minBuf, 8192))
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()
            }.getOrNull() ?: return@withContext
            val buf = ShortArray(1024)
            val rnd = java.util.Random()
            var phase = 0.0
            var gain = 0.0
            try {
                track.play()
                while (isActive) {
                    val inc = f.toDouble() / TONE_SR
                    val target = vol.toDouble()
                    val shape = w
                    for (i in buf.indices) {
                        // Płynna zmiana głośności – bez trzasków.
                        gain += (target - gain) * 0.002
                        val s = when (shape) {
                            0 -> sin(2 * PI * phase)
                            1 -> if (phase < 0.5) 0.7 else -0.7
                            2 -> 1.0 - 4.0 * abs(phase - 0.5)
                            else -> (rnd.nextGaussian() * 0.3).coerceIn(-1.0, 1.0)
                        }
                        buf[i] = (s * gain * 32767.0 * 0.9).toInt().coerceIn(-32768, 32767).toShort()
                        phase += inc
                        if (phase >= 1.0) phase -= 1.0
                    }
                    track.write(buf, 0, buf.size)
                }
            } finally {
                runCatching { track.stop() }
                track.release()
            }
        }
    }

    MeterScaffold(Meter.Tone.id, stringResource(Meter.Tone.title), onBack) {
        Panel {
            Readout(if (wave == 3) "—" else fmt(freq, if (freq < 100) 1 else 0), if (wave == 3) "" else "Hz", stringResource(if (playing) R.string.tone_playing else R.string.tone_stopped))
            Spacer(Modifier.height(10.dp))
            Slider(value = posOf(freq), onValueChange = { freq = freqOf(it) }, colors = sliderColors())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Pill(stringResource(R.string.tone_octave_down)) { freq = (freq / 2f).coerceAtLeast(F_MIN) }
                Pill(stringResource(R.string.tone_octave_up)) { freq = (freq * 2f).coerceAtMost(F_MAX) }
                Spacer(Modifier.weight(1f))
                RoundIcon(Icons.Rounded.Remove, "−1 Hz") { freq = (freq - 1f).coerceAtLeast(F_MIN) }
                RoundIcon(Icons.Rounded.Add, "+1 Hz") { freq = (freq + 1f).coerceAtMost(F_MAX) }
            }
            Spacer(Modifier.height(14.dp))
            Pill(
                stringResource(if (playing) R.string.tone_stop else R.string.tone_play),
                icon = if (playing) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                active = true,
                modifier = Modifier.fillMaxWidth(),
            ) { playing = !playing }
        }
        Panel {
            SectionLabel(stringResource(R.string.tone_wave))
            Segmented(
                listOf(stringResource(R.string.tone_sine), stringResource(R.string.tone_square), stringResource(R.string.tone_triangle), stringResource(R.string.tone_noise)),
                wave,
                Modifier.fillMaxWidth(),
            ) { wave = it }
            Spacer(Modifier.height(16.dp))
            SectionLabel(stringResource(R.string.tone_presets))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(50f, 100f, 261.63f, 440f, 1000f, 5000f, 10000f, 15000f).forEach { p ->
                    val label = if (p >= 1000f) "${fmt(p / 1000f, 0)} kHz" else "${fmt(p, if (p % 1f == 0f) 0 else 1)} Hz"
                    Pill(label, active = abs(freq - p) < 0.5f) { freq = p }
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.tone_volume), color = Palette.Fg, fontSize = 15.sp, modifier = Modifier.weight(1f))
                Text("${fmt(volume * 100f, 0)} %", color = Palette.Fg, style = Digits, fontSize = 15.sp)
            }
            Slider(value = volume, onValueChange = { volume = it }, colors = sliderColors())
        }
        Tip(stringResource(R.string.tone_tip))
    }
}
