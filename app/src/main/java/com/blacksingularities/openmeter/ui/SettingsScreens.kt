package com.blacksingularities.openmeter.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blacksingularities.openmeter.core.Prefs
import com.blacksingularities.openmeter.core.Recorder
import com.blacksingularities.openmeter.update.UpdateManager
import com.blacksingularities.openmeter.update.UpdateState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    MeterScaffold("settings", "Ustawienia", onBack) {
        Panel {
            SectionLabel("Ogólne")
            Toggle("Nie wygaszaj ekranu podczas pomiaru", Prefs.keepScreenOn, Prefs::setKeepScreenOn)
            Toggle("Wibracje przy dotyku", Prefs.haptics, Prefs::setHaptics)
            Toggle("Dźwięk wykrywacza metalu", Prefs.detectorSound, Prefs::setDetectorSound)
        }
        Panel {
            SectionLabel("Decybelomierz")
            Toggle("Korekcja A – dB(A)", Prefs.aWeighting, Prefs::setAWeighting)
            Text("Kalibracja: ${if (Prefs.soundOffset >= 0) "+" else ""}${fmt(Prefs.soundOffset, 1)} dB", color = Color.White, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp))
            Slider(
                value = Prefs.soundOffset,
                onValueChange = { Prefs.setSoundOffset((it * 2).toInt() / 2f) },
                valueRange = -20f..20f,
                colors = sliderColors(),
            )
            Text(
                "Mikrofony telefonów różnią się czułością. Porównaj odczyt z wzorcowym miernikiem i przesuń suwak o różnicę.",
                color = Palette.TextFaint,
                fontSize = 12.sp,
                lineHeight = 16.sp,
            )
        }
        Panel {
            SectionLabel("Aktualizacje")
            Toggle("Sprawdzaj przy uruchomieniu", Prefs.autoUpdate, Prefs::setAutoUpdate)
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Wersja ${UpdateManager.currentVersion}", color = Color.White, fontSize = 14.sp)
                    Text(
                        when (val s = UpdateManager.state) {
                            UpdateState.Checking -> "Sprawdzanie…"
                            UpdateState.UpToDate -> "Najnowsza"
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
        Text(
            "OpenMeter · open source (MIT)\ngithub.com/BlackSingularities/OpenMeter",
            color = Palette.TextFaint,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            modifier = Modifier.padding(8.dp),
        )
    }
}

@Composable
private fun Toggle(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onChange(!value) }.padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = Color.White, fontSize = 14.sp, modifier = Modifier.weight(1f))
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
fun sliderColors() = SliderDefaults.colors(
    thumbColor = Color.White,
    activeTrackColor = Color.White,
    inactiveTrackColor = Palette.CardHigh,
    activeTickColor = Color.Transparent,
    inactiveTickColor = Color.Transparent,
)

@Composable
fun RecordingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var version by remember { mutableStateOf(0) }
    val files = remember(version) { Recorder.list(context) }
    val df = remember { SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault()) }
    MeterScaffold("recordings", "Nagrania", onBack) {
        if (files.isEmpty()) {
            Notice("Brak nagrań. W każdym mierniku dotknij REC, aby zapisywać pomiary do pliku CSV. Pliki trafiają też do Pobrane/OpenMeter.")
        }
        files.forEach { f ->
            Panel(padding = 14.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(f.nameWithoutExtension.removePrefix("OpenMeter_"), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        val lines = remember(f) { runCatching { f.useLines { it.count() - 1 } }.getOrDefault(0) }
                        Text(
                            "${df.format(Date(f.lastModified()))} · $lines wierszy · ${f.length() / 1024} kB",
                            color = Palette.TextFaint,
                            fontSize = 12.sp,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RoundIcon(Icons.Rounded.Share, "Udostępnij") { Recorder.share(context, f) }
                        RoundIcon(Icons.Rounded.Delete, "Usuń") {
                            f.delete()
                            version++
                        }
                    }
                }
            }
        }
    }
}
