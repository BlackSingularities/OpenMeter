package com.blacksingularities.openmeter.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FiberManualRecord
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blacksingularities.openmeter.R
import com.blacksingularities.openmeter.core.Prefs
import com.blacksingularities.openmeter.core.Recorder
import com.blacksingularities.openmeter.meters.Category
import com.blacksingularities.openmeter.meters.Meter
import com.blacksingularities.openmeter.update.UpdateManager
import com.blacksingularities.openmeter.update.UpdateState
import java.text.Normalizer
import java.util.Calendar
import kotlin.math.roundToInt

/** Filtr ekranu głównego: wszystkie, ulubione albo jedna kategoria. */
private const val FILTER_ALL = "all"
private const val FILTER_FAV = "fav"

@Composable
fun HomeScreen(onOpen: (String) -> Unit) {
    val context = LocalContext.current
    val available = remember { Meter.entries.filter { it.isAvailable(context) } }
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(FILTER_ALL) }
    val favorites = Prefs.favorites
    val list = Prefs.homeList

    val q = query.normalized()
    val shown = available
        .filter { m ->
            when (filter) {
                FILTER_ALL -> true
                FILTER_FAV -> m.id in favorites
                else -> m.category.name == filter
            }
        }
        .filter { m -> q.isEmpty() || "${m.title} ${m.subtitle} ${m.category.label}".normalized().contains(q) }
        .sortedBy { if (it.id in favorites) 0 else 1 }
    val categories = Category.entries.filter { c -> available.any { it.category == c } }
    val recent = Prefs.recent.mapNotNull { Meter.byId(it) }.filter { it in available }
    val columns = if (list) 1 else 2

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = Modifier
            .fillMaxSize()
            .background(Palette.Ink)
            .drawBehind {
                drawRect(
                    Brush.radialGradient(
                        listOf(Palette.Violet.copy(alpha = 0.14f), Color.Transparent),
                        center = Offset(size.width * 0.9f, 0f),
                        radius = size.width,
                    ),
                )
            }
            .windowInsetsPadding(WindowInsets.statusBars),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) { Header(available.size - 1, onOpen) }
        item(span = { GridItemSpan(maxLineSpan) }) { UpdateBanner() }
        val rec = Recorder.active
        if (rec != null) {
            item(span = { GridItemSpan(maxLineSpan) }) { RecordingBanner(rec) { onOpen(rec) } }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            SearchField(query, { query = it })
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { FilterChip("Wszystkie", filter == FILTER_ALL, Color.White) { filter = FILTER_ALL } }
                if (favorites.isNotEmpty()) {
                    item { FilterChip("Ulubione", filter == FILTER_FAV, Palette.Warn, Icons.Rounded.Star) { filter = FILTER_FAV } }
                }
                items(categories) { c -> FilterChip(c.label, filter == c.name, c.color) { filter = c.name } }
            }
        }
        if (recent.isNotEmpty() && q.isEmpty() && filter == FILTER_ALL) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)) {
                        Icon(Icons.Rounded.History, null, tint = Palette.TextFaint, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("OSTATNIO", color = Palette.TextFaint, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp)
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(recent, key = { it.id }) { m -> RecentChip(m) { onOpen(m.id) } }
                    }
                }
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(Modifier.fillMaxWidth().padding(start = 4.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    when {
                        q.isNotEmpty() -> "WYNIKI · ${shown.size}"
                        filter == FILTER_FAV -> "ULUBIONE · ${shown.size}"
                        filter != FILTER_ALL -> "${Category.valueOf(filter).label.uppercase()} · ${shown.size}"
                        else -> "MIERNIKI · ${shown.size}"
                    },
                    color = Palette.TextFaint,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    if (list) Icons.Rounded.GridView else Icons.AutoMirrored.Rounded.ViewList,
                    if (list) "Siatka" else "Lista",
                    tint = Palette.TextDim,
                    modifier = Modifier.size(32.dp).clip(CircleShape).clickable { Prefs.homeList = !list }.padding(6.dp),
                )
            }
        }
        if (shown.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) { EmptyResults { query = ""; filter = FILTER_ALL } }
        }
        items(shown, key = { it.id }) { m ->
            if (list) MeterRow(m, m.id in favorites, { onOpen(m.id) }) { Prefs.toggleFavorite(m.id) }
            else MeterTile(m, m.id in favorites, { onOpen(m.id) }) { Prefs.toggleFavorite(m.id) }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(
                "Przytrzymaj kafelek, aby dodać go do ulubionych",
                color = Palette.TextFaint,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp).windowInsetsPadding(WindowInsets.navigationBars),
            )
        }
    }
}

@Composable
private fun Header(count: Int, onOpen: (String) -> Unit) {
    val greeting = remember {
        when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 5..11 -> "Dzień dobry"
            in 12..17 -> "Miłego popołudnia"
            in 18..22 -> "Dobry wieczór"
            else -> "Nocne pomiary"
        }
    }
    Row(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Image(
            painter = painterResource(R.drawable.openmeter_mark),
            contentDescription = null,
            modifier = Modifier.size(52.dp),
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(greeting.uppercase(), color = Palette.TextFaint, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp)
            Text("OpenMeter", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp)
            Text("$count mierników · v${UpdateManager.currentVersion}", color = Palette.TextFaint, fontSize = 12.sp)
        }
        RoundIcon(Icons.Rounded.FolderOpen, "Nagrania") { onOpen("recordings") }
        Spacer(Modifier.width(8.dp))
        RoundIcon(Icons.Rounded.Settings, "Ustawienia") { onOpen("settings") }
    }
}

@Composable
private fun RecordingBanner(id: String, onClick: () -> Unit) {
    val m = Meter.byId(id)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Palette.Record.copy(alpha = 0.16f))
            .border(1.dp, Palette.Record.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.FiberManualRecord, null, tint = Palette.Record, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text("Trwa nagrywanie: ${m?.title ?: id}", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text("${Recorder.rows} wierszy · dotknij, aby wrócić", color = Palette.TextDim, fontSize = 11.sp)
        }
    }
}

@Composable
private fun SearchField(value: String, onChange: (String) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(PillShape)
            .background(Palette.Card)
            .border(1.dp, Palette.Line, PillShape)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Search, null, tint = Palette.TextFaint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) Text("Szukaj miernika…", color = Palette.TextFaint, fontSize = 15.sp)
            BasicTextField(
                value = value,
                onValueChange = onChange,
                singleLine = true,
                textStyle = TextStyle(color = Color.White, fontSize = 15.sp),
                cursorBrush = SolidColor(Color.White),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (value.isNotEmpty()) {
            Icon(
                Icons.Rounded.Close,
                "Wyczyść",
                tint = Palette.TextDim,
                modifier = Modifier.size(22.dp).clip(CircleShape).clickable { onChange("") },
            )
        }
    }
}

@Composable
private fun FilterChip(label: String, selected: Boolean, color: Color, icon: androidx.compose.ui.graphics.vector.ImageVector? = null, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val bg by animateColorAsState(if (selected) color else Palette.Card, tween(160), label = "chip")
    Row(
        Modifier
            .clip(PillShape)
            .background(bg)
            .border(1.dp, if (selected) Color.Transparent else Palette.Line, PillShape)
            .clickable {
                if (Prefs.haptics) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = if (selected) Palette.Ink else color, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(5.dp))
        } else if (!selected && color != Color.White) {
            Box(Modifier.size(7.dp).clip(CircleShape).background(color))
            Spacer(Modifier.width(7.dp))
        }
        Text(label, color = if (selected) Palette.Ink else Palette.TextDim, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
private fun RecentChip(meter: Meter, onClick: () -> Unit) {
    Row(
        Modifier
            .clip(PillShape)
            .background(meter.accent.copy(alpha = 0.12f))
            .clickable(onClick = onClick)
            .padding(start = 8.dp, end = 14.dp, top = 7.dp, bottom = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(24.dp).clip(CircleShape).background(meter.accent.copy(alpha = 0.22f)), contentAlignment = Alignment.Center) {
            Icon(meter.icon, null, tint = meter.accent, modifier = Modifier.size(14.dp))
        }
        Spacer(Modifier.width(8.dp))
        Text(meter.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.meterClick(onClick: () -> Unit, onLong: () -> Unit, haptic: androidx.compose.ui.hapticfeedback.HapticFeedback) =
    combinedClickable(
        onClick = onClick,
        onLongClick = {
            if (Prefs.haptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onLong()
        },
    )

@Composable
private fun MeterTile(meter: Meter, favorite: Boolean, onClick: () -> Unit, onToggleFavorite: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val shape = RoundedCornerShape(24.dp)
    val accent = meter.accent
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(shape)
            .background(Palette.Card)
            .drawBehind {
                drawRect(
                    Brush.radialGradient(
                        listOf(accent.copy(alpha = 0.20f), Color.Transparent),
                        center = Offset(0f, 0f),
                        radius = size.width * 1.1f,
                    ),
                )
            }
            .border(1.dp, if (favorite) accent.copy(alpha = 0.45f) else Palette.Line, shape)
            .meterClick(onClick, onToggleFavorite, haptic),
    ) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Box(
                Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(meter.icon, null, tint = accent, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.weight(1f))
            Text(meter.title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Text(meter.subtitle, color = Palette.TextFaint, fontSize = 12.sp, lineHeight = 15.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        FavoriteStar(favorite, Modifier.align(Alignment.TopEnd).padding(8.dp), onToggleFavorite)
    }
}

@Composable
private fun MeterRow(meter: Meter, favorite: Boolean, onClick: () -> Unit, onToggleFavorite: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val shape = RoundedCornerShape(20.dp)
    val accent = meter.accent
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Palette.Card)
            .border(1.dp, if (favorite) accent.copy(alpha = 0.45f) else Palette.Line, shape)
            .meterClick(onClick, onToggleFavorite, haptic)
            .padding(start = 12.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(accent.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(meter.icon, null, tint = accent, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(meter.title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(meter.subtitle, color = Palette.TextFaint, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        FavoriteStar(favorite, Modifier, onToggleFavorite)
    }
}

@Composable
private fun FavoriteStar(favorite: Boolean, modifier: Modifier, onToggle: () -> Unit) {
    Icon(
        if (favorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
        if (favorite) "Usuń z ulubionych" else "Dodaj do ulubionych",
        tint = if (favorite) Palette.Warn else Palette.TextFaint.copy(alpha = 0.25f),
        modifier = modifier.size(36.dp).clip(CircleShape).clickable(onClick = onToggle).padding(8.dp),
    )
}

@Composable
private fun EmptyResults(onReset: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Rounded.SearchOff, null, tint = Palette.TextFaint, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(10.dp))
        Text("Nic tu nie ma", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Text("Zmień wyszukiwanie lub filtr", color = Palette.TextFaint, fontSize = 13.sp)
        Spacer(Modifier.height(14.dp))
        Pill("Pokaż wszystkie", active = true, onClick = onReset)
    }
}

/** Małe litery bez polskich znaków – „swiatlo” znajdzie „Światło”. */
private fun String.normalized(): String =
    Normalizer.normalize(trim().lowercase().replace('ł', 'l'), Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")

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
                .background(Brush.linearGradient(listOf(Palette.Violet.copy(alpha = 0.22f), Palette.CardHigh)))
                .border(1.dp, Palette.Violet.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                .padding(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.SystemUpdate, null, tint = Palette.Violet, modifier = Modifier.size(22.dp))
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
                    color = Palette.Violet,
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
