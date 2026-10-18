package com.blacksingularities.openmeter.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FiberManualRecord
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blacksingularities.openmeter.R
import com.blacksingularities.openmeter.core.Recorder
import com.blacksingularities.openmeter.meters.Meter
import com.blacksingularities.openmeter.update.UpdateManager
import com.blacksingularities.openmeter.update.UpdateState
import kotlin.math.roundToInt

@Composable
fun HomeScreen(onOpen: (String) -> Unit) {
    val context = LocalContext.current
    val meters = remember { Meter.entries.filter { it.isAvailable(context) } }
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxSize()
            .background(Palette.Ink)
            .windowInsetsPadding(WindowInsets.statusBars),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = { GridItemSpan(2) }) {
            Row(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.openmeter_mark),
                    contentDescription = null,
                    modifier = Modifier.size(52.dp),
                )
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("OpenMeter", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "${meters.size - 1} mierników w tym telefonie · v${UpdateManager.currentVersion}",
                        color = Palette.TextFaint,
                        fontSize = 12.sp,
                    )
                }
                RoundIcon(Icons.Rounded.FolderOpen, "Nagrania") { onOpen("recordings") }
                Spacer(Modifier.width(8.dp))
                RoundIcon(Icons.Rounded.Settings, "Ustawienia") { onOpen("settings") }
            }
        }
        item(span = { GridItemSpan(2) }) { UpdateBanner() }
        val rec = Recorder.active
        if (rec != null) {
            item(span = { GridItemSpan(2) }) {
                val m = Meter.byId(rec)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Palette.Record.copy(alpha = 0.16f))
                        .border(1.dp, Palette.Record.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                        .clickable { onOpen(rec) }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.FiberManualRecord, null, tint = Palette.Record, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Trwa nagrywanie: ${m?.title ?: rec}", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        items(meters, key = { it.id }) { m -> MeterTile(m) { onOpen(m.id) } }
    }
}

@Composable
private fun MeterTile(meter: Meter, onClick: () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(shape)
            .background(Palette.Card)
            .border(1.dp, Palette.Line, shape)
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Box(
            Modifier.size(48.dp).clip(CircleShape).background(Palette.CardHigh),
            contentAlignment = Alignment.Center,
        ) {
            Icon(meter.icon, null, tint = Color.White, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.weight(1f))
        Text(meter.title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(2.dp))
        Text(meter.subtitle, color = Palette.TextFaint, fontSize = 12.sp, lineHeight = 15.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

/** Baner nowej wersji: jedno dotknięcie pobiera APK z GitHuba i instaluje go. */
@Composable
fun UpdateBanner() {
    val context = LocalContext.current
    val u = UpdateManager.state
    val visible = !UpdateManager.bannerHidden && (u is UpdateState.Available || u is UpdateState.Downloading || u is UpdateState.Ready || u is UpdateState.Installing)
    AnimatedVisibility(visible) {
        val (title, action) = when (u) {
            is UpdateState.Available -> "Nowa wersja ${u.info.version}" to "Aktualizuj"
            is UpdateState.Downloading -> "Pobieranie ${(u.progress * 100).roundToInt()}%" to null
            is UpdateState.Ready -> "Wersja ${u.info.version} pobrana" to "Zainstaluj"
            is UpdateState.Installing -> "Instalowanie ${u.info.version}…" to null
            else -> "" to null
        }
        val notes = (u as? UpdateState.Available)?.info?.notes
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Palette.CardHigh)
                .padding(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.SystemUpdate, null, tint = Color.White, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(
                        if (u is UpdateState.Available) "Pobierze się i zainstaluje automatycznie" else "Nie zamykaj aplikacji",
                        color = Palette.TextDim,
                        fontSize = 11.sp,
                    )
                }
                if (action != null) {
                    Text(
                        action,
                        color = Palette.Ink,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .clip(PillShape)
                            .background(Color.White)
                            .clickable { UpdateManager.updateNow(context) }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    )
                }
                if (u is UpdateState.Available) {
                    Icon(
                        Icons.Rounded.Close,
                        "Później",
                        tint = Palette.TextDim,
                        modifier = Modifier.padding(start = 6.dp).size(32.dp).clip(CircleShape)
                            .clickable { UpdateManager.bannerHidden = true }.padding(6.dp),
                    )
                }
            }
            if (u is UpdateState.Downloading) {
                LinearProgressIndicator(
                    progress = { u.progress },
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                )
            }
            if (!notes.isNullOrBlank()) {
                Text(notes, color = Palette.TextDim, fontSize = 12.sp, lineHeight = 16.sp, maxLines = 5, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp, start = 34.dp))
            }
        }
    }
}
