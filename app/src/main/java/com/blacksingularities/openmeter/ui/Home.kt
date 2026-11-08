package com.blacksingularities.openmeter.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FiberManualRecord
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
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
import kotlin.math.roundToInt

/** Filtr ekranu głównego: wszystkie, ulubione albo jedna kategoria. */
private const val FILTER_ALL = "all"
private const val FILTER_FAV = "fav"

@Composable
fun HomeScreen(onOpen: (String) -> Unit, onFeedback: () -> Unit) {
    val context = LocalContext.current
    val available = remember { Meter.entries.filter { it.isAvailable(context) } }
    var query by rememberSaveable { mutableStateOf("") }
    var searching by rememberSaveable { mutableStateOf(false) }
    var filter by rememberSaveable { mutableStateOf(FILTER_ALL) }
    val favorites = Prefs.favorites
    val list = Prefs.homeList

    val categories = Category.entries.filter { c -> available.any { it.category == c } }
    val filters = buildList {
        add(FILTER_ALL)
        if (favorites.isNotEmpty()) add(FILTER_FAV)
        categories.forEach { add(it.name) }
    }
    if (filter !in filters) filter = FILTER_ALL

    // Wyszukiwanie po nazwie, opisie i kategorii w bieżącym języku (bez znaków diakrytycznych).
    val searchable = available.associateWith { m ->
        "${stringResource(m.title)} ${stringResource(m.subtitle)} ${stringResource(m.category.label)}".normalized()
    }
    val q = query.normalized()
    val shown = available
        .filter { m ->
            when (filter) {
                FILTER_ALL -> true
                FILTER_FAV -> m.id in favorites
                else -> m.category.name == filter
            }
        }
        .filter { m -> q.isEmpty() || searchable[m]?.contains(q) == true }
        .sortedBy { if (it.id in favorites) 0 else 1 }
    val recent = Prefs.recent.mapNotNull { Meter.byId(it) }.filter { it in available }
    val threshold = with(LocalDensity.current) { 72.dp.toPx() }

    BackHandler(enabled = searching) { searching = false; query = "" }

    Column(Modifier.fillMaxSize().background(Palette.Ink).windowInsetsPadding(WindowInsets.statusBars)) {
        HomeHeader(
            count = available.size,
            searching = searching,
            onSearching = { searching = it; if (!it) query = "" },
            query = query,
            onQuery = { query = it },
            onOpen = onOpen,
            onFeedback = onFeedback,
        )
        FilterChips(filters, filter) { filter = it }
        LazyVerticalGrid(
            columns = GridCells.Fixed(if (list) 1 else 2),
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(filters) {
                    var acc = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { acc = 0f },
                        onDragEnd = {
                            val i = filters.indexOf(filter).coerceAtLeast(0)
                            if (acc <= -threshold) filter = filters[(i + 1).mod(filters.size)]
                            else if (acc >= threshold) filter = filters[(i - 1).mod(filters.size)]
                            acc = 0f
                        },
                    ) { _, dx -> acc += dx }
                },
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) { UpdateBanner() }
            val rec = Recorder.active
            if (rec != null) {
                item(span = { GridItemSpan(maxLineSpan) }) { RecordingBanner(rec) { onOpen(rec) } }
            }
            if (recent.isNotEmpty() && q.isEmpty() && filter == FILTER_ALL) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column {
                        SectionLabel(stringResource(R.string.recent), Modifier.padding(start = 4.dp, top = 4.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(recent, key = { it.id }) { m -> RecentChip(m) { onOpen(m.id) } }
                        }
                    }
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                val label = when {
                    q.isNotEmpty() -> stringResource(R.string.results_count, shown.size)
                    filter == FILTER_FAV -> stringResource(R.string.label_count, stringResource(R.string.favorites), shown.size)
                    filter != FILTER_ALL -> stringResource(R.string.label_count, stringResource(Category.valueOf(filter).label), shown.size)
                    else -> stringResource(R.string.meters_count, shown.size)
                }
                SectionLabel(label, Modifier.padding(start = 4.dp, top = 8.dp))
            }
            if (shown.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) { EmptyResults { query = ""; searching = false; filter = FILTER_ALL } }
            }
            items(shown, key = { it.id }) { m ->
                if (list) MeterRow(m, m.id in favorites, { onOpen(m.id) }) { Prefs.toggleFavorite(m.id) }
                else MeterTile(m, m.id in favorites, { onOpen(m.id) }) { Prefs.toggleFavorite(m.id) }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    stringResource(R.string.fav_hint),
                    color = Palette.TextFaint,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp).windowInsetsPadding(WindowInsets.navigationBars),
                )
            }
        }
    }
}

@Composable
private fun HomeHeader(
    count: Int,
    searching: Boolean,
    onSearching: (Boolean) -> Unit,
    query: String,
    onQuery: (String) -> Unit,
    onOpen: (String) -> Unit,
    onFeedback: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (searching) {
            SearchField(query, onQuery, onClose = { onSearching(false) }, modifier = Modifier.weight(1f))
        } else {
            Image(painterResource(R.drawable.openmeter_mark), null, modifier = Modifier.size(40.dp))
            Spacer(Modifier.width(4.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.app_name), color = Palette.Fg, fontSize = 24.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.4).sp)
                Text(pluralStringResource(R.plurals.meters_available, count, count), color = Palette.TextFaint, fontSize = 12.sp)
            }
            RoundIcon(Icons.Rounded.Search, stringResource(R.string.search)) { onSearching(true) }
        }
        val list = Prefs.homeList
        RoundIcon(
            if (list) Icons.Rounded.GridView else Icons.AutoMirrored.Rounded.ViewList,
            stringResource(if (list) R.string.view_grid else R.string.view_list),
        ) { Prefs.homeList = !list }
        MoreMenu(onOpen, onFeedback)
    }
}

/** Pole wyszukiwania w kształcie pigułki ze strzałką wstecz i czyszczeniem. */
@Composable
private fun SearchField(value: String, onChange: (String) -> Unit, onClose: () -> Unit, modifier: Modifier = Modifier) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    Row(
        modifier.height(HeaderButton).clip(PillShape).background(Palette.CardHigh),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(HeaderButton).reactive(stringResource(R.string.search_close), onClick = onClose), contentAlignment = Alignment.Center) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = Palette.Fg, modifier = Modifier.size(20.dp))
        }
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) Text(stringResource(R.string.search_hint), color = Palette.TextFaint, fontSize = 15.sp, maxLines = 1)
            BasicTextField(
                value = value,
                onValueChange = onChange,
                singleLine = true,
                textStyle = TextStyle(color = Palette.Fg, fontSize = 15.sp),
                cursorBrush = SolidColor(Palette.Fg),
                modifier = Modifier.fillMaxWidth().focusRequester(focus),
            )
        }
        if (value.isNotEmpty()) {
            Box(Modifier.size(HeaderButton).reactive(stringResource(R.string.clear)) { onChange("") }, contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Close, null, tint = Palette.Fg, modifier = Modifier.size(20.dp))
            }
        } else {
            Spacer(Modifier.width(14.dp))
        }
    }
}

/** Menu „⋮”: nagrania, dziennik, ustawienia, informacje i zgłoszenia. */
@Composable
private fun MoreMenu(onOpen: (String) -> Unit, onFeedback: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    val update = UpdateManager.state is UpdateState.Available
    Box {
        RoundIcon(Icons.Rounded.MoreVert, stringResource(R.string.more), badge = update) { open = true }
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            shape = RoundedCornerShape(20.dp),
            containerColor = Palette.CardHigh,
        ) {
            @Composable
            fun item(icon: ImageVector, label: String, dot: Boolean = false, action: () -> Unit) = DropdownMenuItem(
                text = { Text(label, color = Palette.Fg, fontSize = 15.sp, fontWeight = FontWeight.Medium) },
                leadingIcon = { Icon(icon, null, tint = Palette.TextDim, modifier = Modifier.size(20.dp)) },
                trailingIcon = if (dot) ({ Box(Modifier.size(8.dp).clip(CircleShape).background(Palette.Record)) }) else null,
                contentPadding = PaddingValues(horizontal = 16.dp),
                modifier = Modifier.height(46.dp),
                onClick = { open = false; action() },
            )
            item(Icons.Rounded.FolderOpen, stringResource(R.string.menu_recordings)) { onOpen("recordings") }
            item(Icons.Rounded.Bookmarks, stringResource(R.string.menu_log)) { onOpen("log") }
            item(Icons.Rounded.Settings, stringResource(R.string.menu_settings), dot = update) { onOpen("settings") }
            item(Icons.Rounded.BugReport, stringResource(R.string.feedback_entry)) { onFeedback() }
            item(Icons.Rounded.Info, stringResource(R.string.menu_about)) { onOpen("about") }
        }
    }
}

/** Pasek filtrów: wszystkie, ulubione, kategorie. Wybrany chip jest zawsze widoczny. */
@Composable
private fun FilterChips(filters: List<String>, selected: String, onSelect: (String) -> Unit) {
    val state = rememberLazyListState()
    val index = filters.indexOf(selected).coerceAtLeast(0)
    LaunchedEffect(index) { runCatching { state.animateScrollToItem((index - 1).coerceAtLeast(0)) } }
    LazyRow(
        state = state,
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(bottom = 6.dp),
    ) {
        items(filters, key = { it }) { f ->
            val label = when (f) {
                FILTER_ALL -> stringResource(R.string.filter_all)
                FILTER_FAV -> stringResource(R.string.favorites)
                else -> stringResource(Category.valueOf(f).label)
            }
            Chip(label, f == selected, if (f == FILTER_FAV) Icons.Rounded.Star else null) { onSelect(f) }
        }
    }
}

@Composable
private fun Chip(label: String, selected: Boolean, icon: ImageVector?, onClick: () -> Unit) {
    val bg by animateColorAsState(if (selected) Palette.Fg else Palette.CardHigh, tween(160), label = "chip")
    val fg = if (selected) Palette.Ink else Palette.TextDim
    Row(
        Modifier
            .reactive(label, pressScale = 0.94f, onClick = onClick)
            .height(34.dp)
            .clip(PillShape)
            .background(bg)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = fg, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(5.dp))
        }
        Text(label, color = fg, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
private fun RecordingBanner(id: String, onClick: () -> Unit) {
    val m = Meter.byId(id)
    val rows = Recorder.rows.toInt()
    Row(
        Modifier
            .fillMaxWidth()
            .reactive(null, pressScale = 0.98f, onClick = onClick)
            .clip(RoundedCornerShape(22.dp))
            .background(Palette.Record)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.FiberManualRecord, null, tint = Color.White, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(R.string.recording_now, if (m != null) stringResource(m.title) else id),
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(pluralStringResource(R.plurals.rows_back, rows, rows), color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
        }
    }
}

@Composable
private fun RecentChip(meter: Meter, onClick: () -> Unit) {
    val title = stringResource(meter.title)
    Row(
        Modifier
            .reactive(title, pressScale = 0.94f, onClick = onClick)
            .clip(PillShape)
            .background(Palette.Card)
            .padding(start = 6.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(26.dp).clip(CircleShape).background(Palette.CardHigh), contentAlignment = Alignment.Center) {
            Icon(meter.icon, null, tint = Palette.Fg, modifier = Modifier.size(15.dp))
        }
        Spacer(Modifier.width(8.dp))
        Text(title, color = Palette.Fg, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

@Composable
private fun MeterTile(meter: Meter, favorite: Boolean, onClick: () -> Unit, onToggleFavorite: () -> Unit) {
    val title = stringResource(meter.title)
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .reactive(title, pressScale = 0.95f, onLongPress = onToggleFavorite, onClick = onClick)
            .clip(CardShape)
            .background(Palette.Card),
    ) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Box(Modifier.size(48.dp).clip(CircleShape).background(if (favorite) Palette.Fg else Palette.CardHigh), contentAlignment = Alignment.Center) {
                Icon(meter.icon, null, tint = if (favorite) Palette.Ink else Palette.Fg, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.weight(1f))
            Text(title, color = Palette.Fg, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Text(stringResource(meter.subtitle), color = Palette.TextFaint, fontSize = 12.sp, lineHeight = 15.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        FavoriteStar(favorite, Modifier.align(Alignment.TopEnd).padding(6.dp), onToggleFavorite)
    }
}

@Composable
private fun MeterRow(meter: Meter, favorite: Boolean, onClick: () -> Unit, onToggleFavorite: () -> Unit) {
    val title = stringResource(meter.title)
    Row(
        Modifier
            .fillMaxWidth()
            .reactive(title, pressScale = 0.97f, onLongPress = onToggleFavorite, onClick = onClick)
            .clip(RoundedCornerShape(22.dp))
            .background(Palette.Card)
            .padding(start = 12.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(44.dp).clip(CircleShape).background(if (favorite) Palette.Fg else Palette.CardHigh), contentAlignment = Alignment.Center) {
            Icon(meter.icon, null, tint = if (favorite) Palette.Ink else Palette.Fg, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Palette.Fg, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(stringResource(meter.subtitle), color = Palette.TextFaint, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        FavoriteStar(favorite, Modifier, onToggleFavorite)
    }
}

@Composable
private fun FavoriteStar(favorite: Boolean, modifier: Modifier, onToggle: () -> Unit) {
    Box(
        modifier.size(36.dp).reactive(stringResource(if (favorite) R.string.fav_remove else R.string.fav_add), onClick = onToggle),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            if (favorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
            null,
            tint = if (favorite) Palette.Fg else Palette.TextFaint.copy(alpha = 0.3f),
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun EmptyResults(onReset: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Rounded.SearchOff, null, tint = Palette.TextFaint, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(10.dp))
        Text(stringResource(R.string.empty_title), color = Palette.Fg, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Text(stringResource(R.string.empty_hint), color = Palette.TextFaint, fontSize = 13.sp)
        Spacer(Modifier.height(14.dp))
        Pill(stringResource(R.string.show_all), active = true, onClick = onReset)
    }
}

/** Małe litery bez znaków diakrytycznych – „swiatlo” znajdzie „Światło”. */
private fun String.normalized(): String =
    Normalizer.normalize(trim().lowercase().replace('ł', 'l'), Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")

/** Baner nowej wersji: jedno dotknięcie pobiera i instaluje aktualizację. */
@Composable
fun UpdateBanner() {
    val context = LocalContext.current
    val u = UpdateManager.state
    val visible = !UpdateManager.bannerHidden && (u is UpdateState.Available || u is UpdateState.Downloading || u is UpdateState.Ready || u is UpdateState.Installing)
    AnimatedVisibility(visible) {
        val title: String
        val action: String?
        when (u) {
            is UpdateState.Available -> { title = stringResource(R.string.update_new, u.info.version); action = stringResource(R.string.update_action) }
            is UpdateState.Downloading -> { title = stringResource(R.string.update_downloading, (u.progress * 100).roundToInt()); action = null }
            is UpdateState.Ready -> { title = stringResource(R.string.update_ready, u.info.version); action = stringResource(R.string.install) }
            is UpdateState.Installing -> { title = stringResource(R.string.update_installing, u.info.version); action = null }
            else -> { title = ""; action = null }
        }
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Palette.Fg)
                .padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.SystemUpdate, null, tint = Palette.Ink, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, color = Palette.Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text(
                        stringResource(if (u is UpdateState.Available) R.string.update_auto else R.string.update_keep_open),
                        color = Palette.Ink.copy(alpha = 0.65f),
                        fontSize = 12.sp,
                    )
                }
                if (action != null) {
                    Text(
                        action,
                        color = Palette.Fg,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .reactive(action, pressScale = 0.94f) { UpdateManager.updateNow(context) }
                            .clip(PillShape)
                            .background(Palette.Ink)
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    )
                }
                if (u is UpdateState.Available) {
                    Box(
                        Modifier.padding(start = 4.dp).size(34.dp).reactive(stringResource(R.string.later)) { UpdateManager.bannerHidden = true },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.Close, null, tint = Palette.Ink.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
                    }
                }
            }
            if (u is UpdateState.Downloading) {
                LinearProgressIndicator(
                    progress = { u.progress },
                    color = Palette.Ink,
                    trackColor = Palette.Ink.copy(alpha = 0.15f),
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                )
            }
        }
    }
}
