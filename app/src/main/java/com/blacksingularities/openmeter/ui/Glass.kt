package com.blacksingularities.openmeter.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blacksingularities.openmeter.core.Prefs
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/** Dymki z nazwą elementu pokazywane po przytrzymaniu. */
val LocalHints = staticCompositionLocalOf<HintState?> { null }

/** Stan dymków: tekst i prostokąt elementu (w układzie okna), przy którym dymek się pojawia. */
@Stable
class HintState {
    var text by mutableStateOf<String?>(null)
        private set
    var anchor by mutableStateOf(Rect.Zero)
        private set
    var token by mutableStateOf(0)
        private set

    fun show(text: String, anchor: Rect) {
        this.text = text
        this.anchor = anchor
        token++
    }

    fun hide() {
        text = null
    }
}

/** Tło aplikacji, kolor treści i warstwa dymków dla całego interfejsu. */
@Composable
fun AppScope(content: @Composable BoxScope.() -> Unit) {
    val hints = remember { HintState() }
    CompositionLocalProvider(
        LocalHints provides hints,
        LocalContentColor provides Palette.Fg,
        LocalAccent provides Palette.Fg,
    ) {
        Box(Modifier.fillMaxSize().background(Palette.Ink)) {
            content()
            HintLayer(hints)
        }
    }
}

@Composable
private fun HintLayer(hints: HintState) {
    val density = LocalDensity.current
    var bubble by remember { mutableStateOf(IntSize.Zero) }
    var rootOffset by remember { mutableStateOf(Rect.Zero) }
    LaunchedEffect(hints.token) {
        if (hints.text != null) {
            delay(1600)
            hints.hide()
        }
    }
    Box(
        Modifier
            .fillMaxSize()
            .onGloballyPositioned { rootOffset = it.boundsInRoot() },
    ) {
        val text = hints.text
        AnimatedVisibility(
            visible = text != null,
            enter = fadeIn(tween(140)) + scaleIn(initialScale = 0.85f),
            exit = fadeOut(tween(160)) + scaleOut(targetScale = 0.92f),
            modifier = Modifier.offset {
                val a = hints.anchor
                val gap = with(density) { 10.dp.roundToPx() }
                val x = (a.center.x - rootOffset.left - bubble.width / 2f).roundToInt()
                val above = (a.top - rootOffset.top - bubble.height - gap).roundToInt()
                val y = if (above > gap) above else (a.bottom - rootOffset.top + gap).roundToInt()
                val maxX = (rootOffset.width - bubble.width - gap).roundToInt().coerceAtLeast(gap)
                IntOffset(x.coerceIn(gap, maxX), y)
            },
        ) {
            Text(
                text ?: "",
                color = Palette.Fg,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .onSizeChanged { bubble = it }
                    .clip(RoundedCornerShape(50))
                    .background(Palette.CardHigh)
                    .padding(horizontal = 12.dp, vertical = 7.dp),
            )
        }
    }
}

/**
 * Reaktywna powierzchnia dotyku: sprężyste wciśnięcie, haptyka, po przytrzymaniu dymek
 * z nazwą ([label]); [onLongPress] go zastępuje.
 */
@Composable
fun Modifier.reactive(
    label: String?,
    enabled: Boolean = true,
    onLongPress: (() -> Unit)? = null,
    pressScale: Float = 0.92f,
    onClick: () -> Unit,
): Modifier {
    val haptic = LocalHapticFeedback.current
    val haptics = Prefs.haptics
    val hints = LocalHints.current
    var pressed by remember { mutableStateOf(false) }
    var bounds by remember { mutableStateOf(Rect.Zero) }
    val click by rememberUpdatedState(onClick)
    val long by rememberUpdatedState(onLongPress)
    val currentLabel by rememberUpdatedState(label)
    val currentHaptics by rememberUpdatedState(haptics)
    val scale by animateFloatAsState(
        if (pressed) pressScale else 1f,
        spring(dampingRatio = 0.45f, stiffness = 700f),
        label = "press",
    )
    return this
        .onGloballyPositioned { bounds = it.boundsInRoot() }
        .pointerInput(enabled) {
            if (!enabled) return@pointerInput
            awaitEachGesture {
                val down = awaitFirstDown()
                pressed = true
                if (currentHaptics) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                val slop = viewConfiguration.touchSlop
                var moved = false
                var longFired = false
                try {
                    while (true) {
                        val event = if (!moved && !longFired) {
                            withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) { awaitPointerEvent() }
                        } else {
                            awaitPointerEvent()
                        }
                        if (event == null) {
                            longFired = true
                            if (currentHaptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            val l = long
                            val text = currentLabel
                            if (l != null) l() else if (text != null) hints?.show(text, bounds)
                            continue
                        }
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) {
                            if (!moved && !longFired) {
                                click()
                                change.consume()
                            }
                            break
                        }
                        if ((change.position - down.position).getDistance() > slop) {
                            moved = true
                            pressed = false
                        }
                    }
                } finally {
                    pressed = false
                }
            }
        }
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            alpha = if (enabled) 1f else 0.38f
        }
        .semantics(mergeDescendants = true) {
            if (label != null) contentDescription = label
            role = Role.Button
            if (!enabled) disabled()
            onClick(label = null) {
                if (enabled) click()
                enabled
            }
            val l = onLongPress
            if (l != null) {
                onLongClick(label = null) {
                    if (enabled) l()
                    enabled
                }
            }
        }
}
