package com.blacksingularities.openmeter.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blacksingularities.openmeter.R
import com.blacksingularities.openmeter.core.AppLanguage
import com.blacksingularities.openmeter.core.Prefs
import com.blacksingularities.openmeter.core.Recorder
import com.blacksingularities.openmeter.core.Snapshot
import com.blacksingularities.openmeter.core.Snapshots
import com.blacksingularities.openmeter.core.ThemeMode
import com.blacksingularities.openmeter.meters.Meter
import com.blacksingularities.openmeter.update.UpdateManager
import com.blacksingularities.openmeter.update.UpdateState
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun Context.openUrl(url: String) {
    runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

/** Wybór języka: „jak w systemie” + języki z zasobów. */
@Composable
fun LanguagePicker() {
    val current = remember { mutableStateOf(AppLanguage.current()) }
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        val options = listOf("") + AppLanguage.tags
        options.forEach { tag ->
            val label = if (tag.isEmpty()) stringResource(R.string.language_system) else AppLanguage.displayName(tag)
            Pill(label, active = current.value == tag) {
                if (current.value != tag) {
                    current.value = tag
                    AppLanguage.set(tag)
                }
            }
        }
    }
}

/** Wybór motywu: systemowy, ciemny, jasny. */
@Composable
fun ThemePicker() {
    Segmented(
        listOf(stringResource(R.string.theme_system), stringResource(R.string.theme_dark), stringResource(R.string.theme_light)),
        Prefs.theme.ordinal,
        Modifier.fillMaxWidth(),
    ) { Prefs.theme = ThemeMode.entries[it] }
}

@Composable
private fun SettingLabel(text: String, hint: String? = null) {
    Text(text, color = Palette.Fg, fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 6.dp))
    if (hint != null) Text(hint, color = Palette.TextFaint, fontSize = 12.sp, lineHeight = 16.sp)
    Spacer(Modifier.height(8.dp))
}

/** Wiersz z tekstem po lewej i akcją po prawej. */
@Composable
private fun ActionRow(title: String, hint: String?, action: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 8.dp)) {
            Text(title, color = Palette.Fg, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            if (hint != null) Text(hint, color = Palette.TextFaint, fontSize = 12.sp, lineHeight = 16.sp)
        }
        Pill(action, onClick = onClick)
    }
}

@Composable
fun SettingsScreen(onBack: () -> Unit, onAbout: () -> Unit) {
    val context = LocalContext.current
    var confirmReset by remember { mutableStateOf(false) }
    var confirmCalibration by remember { mutableStateOf(false) }
    PageScaffold(stringResource(R.string.menu_settings), onBack) {
        Panel {
            SectionLabel(stringResource(R.string.set_appearance))
            SettingLabel(stringResource(R.string.set_theme))
            ThemePicker()
            Spacer(Modifier.height(8.dp))
            SettingLabel(stringResource(R.string.set_language), stringResource(R.string.set_language_hint))
            LanguagePicker()
            Spacer(Modifier.height(8.dp))
            SettingLabel(stringResource(R.string.set_layout))
            Segmented(listOf(stringResource(R.string.view_grid), stringResource(R.string.view_list)), if (Prefs.homeList) 1 else 0, Modifier.fillMaxWidth()) {
                Prefs.homeList = it == 1
            }
        }
        Panel {
            SectionLabel(stringResource(R.string.set_units))
            SettingLabel(stringResource(R.string.set_unit_system), stringResource(R.string.set_unit_system_hint))
            Segmented(listOf(stringResource(R.string.unit_metric), stringResource(R.string.unit_imperial)), if (Prefs.imperial) 1 else 0, Modifier.fillMaxWidth()) {
                Prefs.imperial = it == 1
            }
            Spacer(Modifier.height(8.dp))
            SettingLabel(stringResource(R.string.set_pressure_unit))
            Segmented(listOf("hPa", "mmHg", "inHg"), Prefs.pressureUnit, Modifier.fillMaxWidth()) { Prefs.pressureUnit = it }
        }
        Panel {
            SectionLabel(stringResource(R.string.set_general))
            Toggle(stringResource(R.string.set_keep_on), stringResource(R.string.set_keep_on_hint), Prefs.keepScreenOn) { Prefs.keepScreenOn = it }
            Toggle(stringResource(R.string.set_haptics), stringResource(R.string.set_haptics_hint), Prefs.haptics) { Prefs.haptics = it }
            Toggle(stringResource(R.string.set_detector_sound), stringResource(R.string.set_detector_sound_hint), Prefs.detectorSound) { Prefs.detectorSound = it }
        }
        Panel {
            SectionLabel(stringResource(R.string.m_sound))
            Toggle(stringResource(R.string.set_a_weighting), stringResource(R.string.set_a_weighting_hint), Prefs.aWeighting) { Prefs.aWeighting = it }
            Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.set_calibration), color = Palette.Fg, fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                Text("${if (Prefs.soundOffset >= 0) "+" else ""}${fmt(Prefs.soundOffset, 1)} dB", color = Palette.Fg, style = Digits, fontSize = 15.sp)
            }
            Slider(
                value = Prefs.soundOffset,
                onValueChange = { Prefs.soundOffset = ((it * 2).toInt() / 2f) },
                valueRange = -20f..20f,
                colors = sliderColors(),
            )
            Text(stringResource(R.string.set_calibration_hint), color = Palette.TextFaint, fontSize = 12.sp, lineHeight = 16.sp)
            Row(Modifier.padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.set_noise_alarm), color = Palette.Fg, fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                Text(
                    if (Prefs.noiseAlarm <= 0f) stringResource(R.string.off) else "${fmt(Prefs.noiseAlarm, 0)} dB",
                    color = Palette.Fg,
                    style = Digits,
                    fontSize = 15.sp,
                )
            }
            Slider(
                value = Prefs.noiseAlarm,
                onValueChange = { v -> Prefs.noiseAlarm = if (v < 40f) 0f else (v / 5f).toInt() * 5f },
                valueRange = 0f..120f,
                colors = sliderColors(),
            )
            Text(stringResource(R.string.set_noise_alarm_hint), color = Palette.TextFaint, fontSize = 12.sp, lineHeight = 16.sp)
        }
        Panel {
            SectionLabel(stringResource(R.string.m_steps))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.set_step_length), color = Palette.Fg, fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                Text("${fmt(Prefs.stepLength * 100f, 0)} cm", color = Palette.Fg, style = Digits, fontSize = 15.sp)
            }
            Slider(
                value = Prefs.stepLength,
                onValueChange = { Prefs.stepLength = (it * 100f).toInt() / 100f },
                valueRange = 0.4f..1.2f,
                colors = sliderColors(),
            )
        }
        Panel {
            SectionLabel(stringResource(R.string.set_data))
            ActionRow(
                stringResource(R.string.set_personal),
                stringResource(R.string.set_personal_hint, Prefs.favorites.size, Prefs.recent.size),
                stringResource(R.string.clear),
            ) { confirmReset = true }
            ActionRow(stringResource(R.string.set_reset_calibration), stringResource(R.string.set_reset_calibration_hint), stringResource(R.string.reset)) {
                confirmCalibration = true
            }
            ActionRow(stringResource(R.string.set_pressure_history), stringResource(R.string.set_pressure_history_hint), stringResource(R.string.clear)) {
                Prefs.clearPressureHistory()
                toast(context, context.getString(R.string.done))
            }
        }
        Panel {
            SectionLabel(stringResource(R.string.set_updates))
            Toggle(stringResource(R.string.set_auto_update), stringResource(R.string.set_auto_update_hint), Prefs.autoUpdate) { Prefs.autoUpdate = it }
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.version_x, UpdateManager.currentVersion), color = Palette.Fg, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    Text(
                        when (val s = UpdateManager.state) {
                            UpdateState.Checking -> stringResource(R.string.update_checking)
                            UpdateState.UpToDate -> stringResource(R.string.update_latest)
                            is UpdateState.Available -> stringResource(R.string.update_available, s.info.version)
                            is UpdateState.Failed -> stringResource(R.string.update_error, s.message)
                            else -> stringResource(R.string.update_idle)
                        },
                        color = Palette.TextFaint,
                        fontSize = 12.sp,
                    )
                }
                Pill(stringResource(R.string.update_check), active = true) { UpdateManager.check(context, manual = true) }
            }
        }
        Panel(Modifier.reactive(stringResource(R.string.menu_about), pressScale = 0.98f, onClick = onAbout)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Info, null, tint = Palette.Fg, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.menu_about), color = Palette.Fg, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    Text(stringResource(R.string.about_hint), color = Palette.TextFaint, fontSize = 12.sp)
                }
            }
        }
    }
    if (confirmReset) {
        ConfirmDialog(
            title = stringResource(R.string.set_personal_confirm),
            text = stringResource(R.string.set_personal_confirm_text),
            confirm = stringResource(R.string.clear),
            onDismiss = { confirmReset = false },
        ) {
            Prefs.clearPersonalization()
            confirmReset = false
        }
    }
    if (confirmCalibration) {
        ConfirmDialog(
            title = stringResource(R.string.set_reset_calibration_confirm),
            text = stringResource(R.string.set_reset_calibration_hint),
            confirm = stringResource(R.string.reset),
            onDismiss = { confirmCalibration = false },
        ) {
            Prefs.resetCalibration()
            confirmCalibration = false
        }
    }
}

@Composable
fun AboutScreen(onBack: () -> Unit, onFeedback: () -> Unit) {
    val context = LocalContext.current
    PageScaffold(stringResource(R.string.menu_about), onBack) {
        Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Image(painterResource(R.drawable.openmeter_mark), null, modifier = Modifier.size(96.dp))
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.app_name), color = Palette.Fg, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.version_x, UpdateManager.currentVersion), color = Palette.TextFaint, fontSize = 13.sp)
            Spacer(Modifier.height(10.dp))
            Text(
                stringResource(R.string.about_tagline),
                color = Palette.TextDim,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
        }
        Panel {
            SectionLabel(stringResource(R.string.about_privacy))
            InfoLine(Icons.Rounded.Lock, stringResource(R.string.about_privacy_text))
            InfoLine(Icons.Rounded.Code, stringResource(R.string.about_license))
            InfoLine(Icons.Rounded.Straighten, stringResource(R.string.about_accuracy))
        }
        Panel {
            SectionLabel(stringResource(R.string.about_author))
            Text(stringResource(R.string.about_author_line), color = Palette.Fg, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(10.dp))
            LinkRow(Icons.Rounded.Public, "redzikowski.com") { context.openUrl("https://redzikowski.com") }
            LinkRow(Icons.Rounded.Email, "kontakt@redzikowski.com") { context.openUrl("mailto:kontakt@redzikowski.com") }
            LinkRow(Icons.Rounded.BugReport, stringResource(R.string.feedback_entry), onClick = onFeedback)
        }
        Pill(stringResource(R.string.about_coffee), icon = Icons.Rounded.LocalCafe, active = true, modifier = Modifier.fillMaxWidth()) {
            context.openUrl("https://buycoffee.to/adamredzikowski")
        }
    }
}

@Composable
private fun InfoLine(icon: ImageVector, text: String) {
    Row(Modifier.padding(vertical = 6.dp)) {
        Icon(icon, null, tint = Palette.TextDim, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(12.dp))
        Text(text, color = Palette.TextDim, fontSize = 13.sp, lineHeight = 18.sp)
    }
}

@Composable
private fun LinkRow(icon: ImageVector, text: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .reactive(text, pressScale = 0.97f, onClick = onClick)
            .clip(RoundedCornerShape(16.dp))
            .background(Palette.CardHigh)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = Palette.Fg, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(12.dp))
        Text(text, color = Palette.Fg, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
    Spacer(Modifier.height(6.dp))
}

// ───────────────────────────── Dziennik odczytów ─────────────────────────────

@Composable
fun LogScreen(onBack: () -> Unit, onOpen: (String) -> Unit) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { Snapshots.load(context) }
    val items = Snapshots.items
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    val df = remember(locale) { SimpleDateFormat("d MMM yyyy, HH:mm:ss", locale) }
    var editing by remember { mutableStateOf<Snapshot?>(null) }
    var clearAll by remember { mutableStateOf(false) }
    val names = Meter.entries.associate { it.id to stringResource(it.title) }
    PageScaffold(stringResource(R.string.menu_log), onBack, actions = {
        if (items.isNotEmpty()) {
            RoundIcon(Icons.Rounded.Share, stringResource(R.string.log_share)) { Snapshots.share(context) { names[it] ?: it } }
            RoundIcon(Icons.Rounded.DeleteSweep, stringResource(R.string.log_clear)) { clearAll = true }
        }
    }) {
        if (items.isEmpty()) {
            Notice(stringResource(R.string.log_empty))
        } else {
            Text(pluralStringResource(R.plurals.log_count, items.size, items.size), color = Palette.TextDim, fontSize = 13.sp, modifier = Modifier.padding(start = 4.dp))
        }
        items.forEach { s ->
            val meter = Meter.byId(s.meterId)
            Panel(padding = 14.dp) {
                Row(verticalAlignment = Alignment.Top) {
                    Box(
                        Modifier.size(40.dp).clip(CircleShape).background(Palette.CardHigh)
                            .reactive(names[s.meterId], pressScale = 0.9f) { if (meter != null) onOpen(meter.id) },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (meter != null) Icon(meter.icon, null, tint = Palette.Fg, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(names[s.meterId] ?: s.meterId, color = Palette.Fg, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(df.format(Date(s.time)), color = Palette.TextFaint, fontSize = 12.sp)
                        Spacer(Modifier.height(6.dp))
                        Text(s.text, color = Palette.Fg, style = Digits, fontSize = 14.sp, lineHeight = 19.sp)
                        if (s.note.isNotBlank()) {
                            Spacer(Modifier.height(6.dp))
                            Text(s.note, color = Palette.TextDim, fontSize = 13.sp, lineHeight = 18.sp)
                        }
                    }
                }
                Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pill(stringResource(if (s.note.isBlank()) R.string.log_add_note else R.string.log_edit_note), icon = Icons.Rounded.Edit) { editing = s }
                    Pill(stringResource(R.string.delete), icon = Icons.Rounded.Delete) { Snapshots.delete(context, s.id) }
                }
            }
        }
    }
    editing?.let { s ->
        var note by remember(s.id) { mutableStateOf(s.note) }
        AppDialog(
            stringResource(R.string.log_note),
            onDismiss = { editing = null },
            buttons = {
                Pill(stringResource(R.string.cancel)) { editing = null }
                Pill(stringResource(R.string.save), active = true) {
                    Snapshots.setNote(context, s.id, note)
                    editing = null
                }
            },
        ) {
            Text(s.text, color = Palette.TextDim, fontSize = 13.sp)
            OutlinedTextField(
                value = note,
                onValueChange = { note = it.take(500) },
                placeholder = { Text(stringResource(R.string.log_note_hint)) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Palette.Fg,
                    cursorColor = Palette.Fg,
                    focusedTextColor = Palette.Fg,
                    unfocusedTextColor = Palette.Fg,
                ),
            )
        }
    }
    if (clearAll) {
        ConfirmDialog(
            stringResource(R.string.log_clear_confirm),
            pluralStringResource(R.plurals.log_count, items.size, items.size),
            stringResource(R.string.delete),
            onDismiss = { clearAll = false },
        ) {
            Snapshots.clear(context)
            clearAll = false
        }
    }
}

// ───────────────────────────── Nagrania ─────────────────────────────

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
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    val df = remember(locale) { SimpleDateFormat("d MMM yyyy, HH:mm", locale) }
    var expanded by remember { mutableStateOf<String?>(null) }
    var toDelete by remember { mutableStateOf<File?>(null) }
    var deleteAll by remember { mutableStateOf(false) }
    PageScaffold(stringResource(R.string.menu_recordings), onBack, actions = {
        if (files.isNotEmpty()) RoundIcon(Icons.Rounded.DeleteSweep, stringResource(R.string.rec_delete_all)) { deleteAll = true }
    }) {
        if (files.isEmpty()) {
            Notice(stringResource(R.string.rec_none))
        } else {
            Panel {
                StatGrid(
                    listOf(
                        stringResource(R.string.menu_recordings) to "${files.size}",
                        stringResource(R.string.rec_size) to formatSize(files.sumOf { it.length() }),
                        stringResource(R.string.rec_meters) to "${files.map { meterOf(it)?.id ?: "?" }.distinct().size}",
                    ),
                )
            }
        }
        files.forEach { f ->
            val meter = meterOf(f)
            val open = expanded == f.name
            val title = if (meter != null) stringResource(meter.title) else f.nameWithoutExtension.removePrefix("OpenMeter_")
            Panel(padding = 14.dp) {
                Row(
                    Modifier.reactive(title, pressScale = 0.98f) { expanded = if (open) null else f.name },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(40.dp).clip(CircleShape).background(Palette.CardHigh), contentAlignment = Alignment.Center) {
                        if (meter != null) Icon(meter.icon, null, tint = Palette.Fg, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(title, color = Palette.Fg, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${df.format(Date(f.lastModified()))} · ${formatSize(f.length())}", color = Palette.TextFaint, fontSize = 12.sp)
                    }
                    Icon(if (open) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null, tint = Palette.TextDim, modifier = Modifier.size(22.dp))
                }
                AnimatedVisibility(open) {
                    Column(Modifier.padding(top = 12.dp)) {
                        RecordingPreview(f)
                        Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Pill(stringResource(R.string.share), icon = Icons.Rounded.Share) { Recorder.share(context, f) }
                            Pill(stringResource(R.string.delete), icon = Icons.Rounded.Delete) { toDelete = f }
                        }
                    }
                }
            }
        }
    }
    toDelete?.let { f ->
        ConfirmDialog(stringResource(R.string.rec_delete_confirm), f.name, stringResource(R.string.delete), onDismiss = { toDelete = null }) {
            f.delete()
            toDelete = null
            expanded = null
            version++
        }
    }
    if (deleteAll) {
        ConfirmDialog(
            stringResource(R.string.rec_delete_all_confirm),
            stringResource(R.string.rec_delete_all_text, files.size),
            stringResource(R.string.rec_delete_all),
            onDismiss = { deleteAll = false },
        ) {
            files.forEach { it.delete() }
            deleteAll = false
            version++
            toast(context, context.getString(R.string.rec_deleted))
        }
    }
}

@Composable
private fun RecordingPreview(file: File) {
    val data = remember(file) { readCsv(file) }
    if (data == null || data.columns.isEmpty()) {
        Text(stringResource(R.string.rec_unreadable), color = Palette.TextFaint, fontSize = 12.sp)
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
                            .reactive(name, pressScale = 0.94f) { col = i }
                            .clip(PillShape)
                            .background(if (selected) Palette.Fg else Palette.CardHigh)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }
        }
        StaticChart(v, Palette.Fg)
        StatGrid(
            listOf(
                stringResource(R.string.min) to (finite.minOrNull()?.let { formatSmart(it) } ?: "—"),
                stringResource(R.string.avg) to (if (finite.isEmpty()) "—" else formatSmart(finite.average().toFloat())),
                stringResource(R.string.max) to (finite.maxOrNull()?.let { formatSmart(it) } ?: "—"),
                stringResource(R.string.rec_rows) to "${data.rows}",
                stringResource(R.string.rec_time) to "${fmt(data.duration, 1)} s",
                stringResource(R.string.rec_columns) to "${data.columns.size}",
            ),
        )
    }
}

/** Wykres całej serii z pliku (zmniejszonej do szerokości ekranu). */
@Composable
private fun StaticChart(values: FloatArray, color: Color) {
    val grid = Palette.Grid
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
        for (i in 0..3) drawLine(grid, Offset(0f, h * i / 3f), Offset(w, h * i / 3f), 1f)
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
        drawPath(area, Brush.verticalGradient(listOf(color.copy(alpha = 0.25f), color.copy(alpha = 0f))))
        drawPath(path, color, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
    }
}

private fun meterOf(f: File): Meter? = f.nameWithoutExtension.removePrefix("OpenMeter_").substringBefore('_').let { Meter.byId(it) }

private fun formatSize(bytes: Long): String = when {
    bytes >= 1024 * 1024 -> String.format(Locale.US, "%.1f MB", bytes / 1048576.0)
    bytes >= 1024 -> "${bytes / 1024} kB"
    else -> "$bytes B"
}
