package com.blacksingularities.openmeter.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blacksingularities.openmeter.R
import com.blacksingularities.openmeter.core.Prefs
import com.blacksingularities.openmeter.core.Recorder
import com.blacksingularities.openmeter.meters.Meter
import com.blacksingularities.openmeter.update.UpdateManager
import com.blacksingularities.openmeter.update.UpdateState
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var confirmReset by remember { mutableStateOf(false) }
    MeterScaffold("settings", "Ustawienia", onBack) {
        Panel {
            SettingsHeader(Icons.Rounded.Tune, "Ogólne", Palette.Mint)
            Toggle("Nie wygaszaj ekranu", "Podczas pracy miernika ekran pozostaje włączony", Prefs.keepScreenOn) { Prefs.keepScreenOn = it }
            Toggle("Wibracje przy dotyku", "Delikatne potwierdzenie przycisków", Prefs.haptics) { Prefs.haptics = it }
            Toggle("Dźwięk wykrywacza metalu", "Ton rośnie wraz z siłą pola", Prefs.detectorSound) { Prefs.detectorSound = it }
        }
        Panel {
            SettingsHeader(Icons.Rounded.ColorLens, "Ekran główny", Palette.Violet)
            Text("Układ mierników", color = Color.White, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
            Segmented(listOf("Siatka", "Lista"), if (Prefs.homeList) 1 else 0, Modifier.fillMaxWidth()) { Prefs.homeList = it == 1 }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Ulubione i ostatnie", color = Color.White, fontSize = 14.sp)
                    Text(
                        "${Prefs.favorites.size} ulubionych · ${Prefs.recent.size} w historii",
                        color = Palette.TextFaint,
                        fontSize = 12.sp,
                    )
                }
                Pill("Wyczyść") { confirmReset = true }
            }
        }
        Panel {
            SettingsHeader(Icons.Rounded.GraphicEq, "Decybelomierz", Palette.Amber)
            Toggle("Korekcja A – dB(A)", "Ważenie zbliżone do czułości ludzkiego ucha", Prefs.aWeighting) { Prefs.aWeighting = it }
            Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Kalibracja", color = Color.White, fontSize = 14.sp, modifier = Modifier.weight(1f))
                Text(
                    "${if (Prefs.soundOffset >= 0) "+" else ""}${fmt(Prefs.soundOffset, 1)} dB",
                    color = Palette.Amber,
                    style = Digits,
                    fontSize = 15.sp,
                )
            }
            Slider(
                value = Prefs.soundOffset,
                onValueChange = { Prefs.soundOffset = ((it * 2).toInt() / 2f) },
                valueRange = -20f..20f,
                colors = sliderColors(Palette.Amber),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Porównaj odczyt z wzorcowym miernikiem i przesuń suwak o różnicę.",
                    color = Palette.TextFaint,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.weight(1f),
                )
                if (Prefs.soundOffset != 0f) {
                    Spacer(Modifier.width(8.dp))
                    Pill("Zeruj") { Prefs.soundOffset = 0f }
                }
            }
        }
        Panel {
            SettingsHeader(Icons.Rounded.SystemUpdate, "Aktualizacje", Palette.Sky)
            Toggle("Sprawdzaj przy uruchomieniu", "Nowe wersje z GitHub Releases", Prefs.autoUpdate) { Prefs.autoUpdate = it }
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Wersja ${UpdateManager.currentVersion}", color = Color.White, fontSize = 14.sp)
                    Text(
                        when (val s = UpdateManager.state) {
                            UpdateState.Checking -> "Sprawdzanie…"
                            UpdateState.UpToDate -> "Masz najnowszą wersję"
                            is UpdateState.Available -> "Dostępna ${s.info.version}"
                            is UpdateState.Failed -> "Błąd: ${s.message}"
                            else -> "Wydania z GitHuba"
                        },
                        color = Palette.TextFaint,
                        fontSize = 12.sp,
                    )
                }
                Pill("Sprawdź", active = true) { UpdateManager.check(context, manual = true) }
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .clip(CardShape)
                .clickable {
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/BlackSingularities/OpenMeter")))
                    }
                }
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(R.drawable.openmeter_mark),
                contentDescription = null,
                modifier = Modifier.size(42.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                "OpenMeter · open source (MIT)\ngithub.com/BlackSingularities/OpenMeter",
                color = Palette.TextFaint,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                modifier = Modifier.weight(1f),
            )
            Icon(Icons.AutoMirrored.Rounded.OpenInNew, "Otwórz", tint = Palette.TextFaint, modifier = Modifier.size(18.dp))
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Projekt i wykonanie: redzikowski.com",
                color = Palette.TextFaint,
                fontSize = 12.sp,
                modifier = Modifier
                    .weight(1f)
                    .clickable { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://redzikowski.com"))) } },
            )
            Pill("☕ Postaw kawę", active = true) {
                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://buycoffee.to/adamredzikowski"))) }
            }
        }
    }
    if (confirmReset) {
        ConfirmDialog(
            title = "Wyczyścić ulubione?",
            text = "Lista ulubionych i ostatnio używanych mierników zostanie wyczyszczona.",
            confirm = "Wyczyść",
            onDismiss = { confirmReset = false },
        ) {
            Prefs.clearPersonalization()
            confirmReset = false
        }
    }
}

@Composable
private fun SettingsHeader(icon: ImageVector, title: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 10.dp)) {
        Box(
            Modifier.size(30.dp).clip(RoundedCornerShape(10.dp)).background(color.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(17.dp))
        }
        Spacer(Modifier.width(10.dp))
        Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun Toggle(label: String, hint: String?, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onChange(!value) }.padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 8.dp)) {
            Text(label, color = Color.White, fontSize = 14.sp)
            if (hint != null) Text(hint, color = Palette.TextFaint, fontSize = 12.sp, lineHeight = 15.sp)
        }
        Switch(
            checked = value,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Palette.Ink,
                checkedTrackColor = Color.White,
                uncheckedThumbColor = Palette.TextDim,
                uncheckedTrackColor = Palette.CardHigh,
                uncheckedBorderColor = Palette.Line,
            ),
        )
    }
}

@Composable
fun sliderColors(color: Color = LocalAccent.current) = SliderDefaults.colors(
    thumbColor = color,
    activeTrackColor = color,
    inactiveTrackColor = Palette.CardHigh,
    activeTickColor = Color.Transparent,
    inactiveTickColor = Color.Transparent,
)

@Composable
fun ConfirmDialog(title: String, text: String, confirm: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Palette.CardHigh,
        title = { Text(title, color = Color.White) },
        text = { Text(text, color = Palette.TextDim) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirm, color = Palette.Danger, fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Anuluj", color = Color.White) } },
    )
}

/** Wczytany plik CSV: nazwy kolumn liczbowych i ich wartości. */
private class CsvData(val columns: List<String>, val values: List<FloatArray>, val rows: Int, val duration: Float)

private fun readCsv(file: File): CsvData? = runCatching {
    val lines = file.readLines().filter { it.isNotBlank() }
    if (lines.size < 2) return null
    val header = lines.first().split(',')
    val body = lines.drop(1).map { it.split(',') }
    // Pomija znacznik czasu i czas względny; zostawia kolumny, w których są liczby.
    val cols = (2 until header.size).filter { c -> body.any { it.getOrNull(c)?.toFloatOrNull() != null } }
    val values = cols.map { c -> FloatArray(body.size) { r -> body[r].getOrNull(c)?.toFloatOrNull() ?: Float.NaN } }
    val duration = body.last().getOrNull(1)?.toFloatOrNull() ?: 0f
    CsvData(cols.map { header[it] }, values, body.size, duration)
}.getOrNull()

@Composable
fun RecordingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var version by remember { mutableIntStateOf(0) }
    val files = remember(version) { Recorder.list(context) }
    val df = remember { SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault()) }
    var expanded by remember { mutableStateOf<String?>(null) }
    var toDelete by remember { mutableStateOf<File?>(null) }
    var deleteAll by remember { mutableStateOf(false) }
    MeterScaffold("recordings", "Nagrania", onBack, actions = {
        if (files.isNotEmpty()) RoundIcon(Icons.Rounded.DeleteSweep, "Usuń wszystkie") { deleteAll = true }
    }) {
        if (files.isEmpty()) {
            Notice("Brak nagrań. W każdym mierniku dotknij REC, aby zapisywać pomiary do pliku CSV. Pliki trafiają też do Pobrane/OpenMeter.")
        } else {
            Panel {
                StatGrid(
                    listOf(
                        "Nagrania" to "${files.size}",
                        "Rozmiar" to formatSize(files.sumOf { it.length() }),
                        "Mierniki" to "${files.map { meterOf(it)?.id ?: "?" }.distinct().size}",
                    ),
                )
            }
        }
        files.forEach { f ->
            val meter = meterOf(f)
            val accent = meter?.accent ?: Color.White
            val open = expanded == f.name
            Panel(padding = 14.dp) {
                Row(
                    Modifier.clip(RoundedCornerShape(12.dp)).clickable { expanded = if (open) null else f.name },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.size(40.dp).clip(RoundedCornerShape(13.dp)).background(accent.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (meter != null) Icon(meter.icon, null, tint = accent, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(meter?.title ?: f.nameWithoutExtension.removePrefix("OpenMeter_"), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            "${df.format(Date(f.lastModified()))} · ${formatSize(f.length())}",
                            color = Palette.TextFaint,
                            fontSize = 12.sp,
                        )
                    }
                    Icon(if (open) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null, tint = Palette.TextDim, modifier = Modifier.size(22.dp))
                }
                AnimatedVisibility(open) {
                    Column(Modifier.padding(top = 12.dp)) {
                        RecordingPreview(f, accent)
                        Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Pill("Udostępnij", icon = Icons.Rounded.Share) { Recorder.share(context, f) }
                            Pill("Usuń", icon = Icons.Rounded.Delete) { toDelete = f }
                        }
                    }
                }
            }
        }
    }
    toDelete?.let { f ->
        ConfirmDialog("Usunąć nagranie?", f.name, "Usuń", onDismiss = { toDelete = null }) {
            f.delete()
            toDelete = null
            expanded = null
            version++
        }
    }
    if (deleteAll) {
        ConfirmDialog("Usunąć wszystkie nagrania?", "Pliki ${files.size} nagrań zostaną usunięte z aplikacji. Kopie w Pobrane/OpenMeter pozostaną.", "Usuń wszystkie", onDismiss = { deleteAll = false }) {
            files.forEach { it.delete() }
            deleteAll = false
            version++
            Toast.makeText(context, "Usunięto nagrania", Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
private fun RecordingPreview(file: File, accent: Color) {
    val data = remember(file) { readCsv(file) }
    if (data == null || data.columns.isEmpty()) {
        Text("Nie udało się odczytać danych z pliku.", color = Palette.TextFaint, fontSize = 12.sp)
        return
    }
    var col by remember(file) { mutableIntStateOf(0) }
    val v = data.values[col]
    val finite = v.filter { !it.isNaN() }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (data.columns.size > 1) {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                data.columns.forEachIndexed { i, name ->
                    val selected = i == col
                    Text(
                        name,
                        color = if (selected) Palette.Ink else Palette.TextDim,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(PillShape)
                            .background(if (selected) accent else Palette.CardHigh)
                            .clickable { col = i }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }
        }
        StaticChart(v, accent)
        StatGrid(
            listOf(
                "Min" to (finite.minOrNull()?.let { formatSmart(it) } ?: "—"),
                "Średnia" to (if (finite.isEmpty()) "—" else formatSmart(finite.average().toFloat())),
                "Max" to (finite.maxOrNull()?.let { formatSmart(it) } ?: "—"),
                "Wiersze" to "${data.rows}",
                "Czas" to "${fmt(data.duration, 1)} s",
                "Kolumny" to "${data.columns.size}",
            ),
        )
    }
}

/** Wykres całej serii z pliku (zmniejszonej do szerokości ekranu). */
@Composable
private fun StaticChart(values: FloatArray, color: Color) {
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Palette.Ink)
            .padding(8.dp),
    ) {
        val pts = values.filter { !it.isNaN() }
        if (pts.size < 2) return@Canvas
        val w = size.width
        val h = size.height
        for (i in 0..3) drawLine(Palette.Grid, Offset(0f, h * i / 3f), Offset(w, h * i / 3f), 1f)
        val lo = pts.min()
        val hi = pts.max().let { if (it - lo < 1e-6f) lo + 1f else it }
        val n = minOf(pts.size, (w / 2).toInt().coerceAtLeast(2))
        val path = Path()
        for (i in 0 until n) {
            val a = i * pts.size / n
            val b = ((i + 1) * pts.size / n).coerceAtLeast(a + 1)
            var avg = 0f
            for (j in a until b) avg += pts[j]
            avg /= (b - a)
            val x = w * i / (n - 1)
            val y = h - (avg - lo) / (hi - lo) * h
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        val area = Path().apply {
            addPath(path)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(area, Brush.verticalGradient(listOf(color.copy(alpha = 0.3f), color.copy(alpha = 0f))))
        drawPath(path, color, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
    }
}

private fun meterOf(f: File): Meter? = f.nameWithoutExtension.removePrefix("OpenMeter_").substringBefore('_').let { Meter.byId(it) }

private fun formatSize(bytes: Long): String = when {
    bytes >= 1024 * 1024 -> String.format(Locale.US, "%.1f MB", bytes / 1048576.0)
    bytes >= 1024 -> "${bytes / 1024} kB"
    else -> "$bytes B"
}
