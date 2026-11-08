package com.blacksingularities.openmeter

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import com.blacksingularities.openmeter.core.Prefs
import com.blacksingularities.openmeter.core.Snapshots
import com.blacksingularities.openmeter.feedback.CrashReportPrompt
import com.blacksingularities.openmeter.feedback.Feedback
import com.blacksingularities.openmeter.feedback.FeedbackDialog
import com.blacksingularities.openmeter.meters.Meter
import com.blacksingularities.openmeter.meters.MeterScreen
import com.blacksingularities.openmeter.ui.AboutScreen
import com.blacksingularities.openmeter.ui.AppScope
import com.blacksingularities.openmeter.ui.HomeScreen
import com.blacksingularities.openmeter.ui.LogScreen
import com.blacksingularities.openmeter.ui.OpenMeterTheme
import com.blacksingularities.openmeter.ui.RecordingsScreen
import com.blacksingularities.openmeter.ui.SettingsScreen
import com.blacksingularities.openmeter.ui.ThemeState
import com.blacksingularities.openmeter.ui.WelcomeScreen
import com.blacksingularities.openmeter.update.UpdateManager

/**
 * Jedyna aktywność. AppCompat zapewnia wybór języka aplikacji (per-app language) także na
 * Androidzie starszym niż 13; nawigacja to prosty stos ekranów zapisywany jako tekst.
 */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Feedback.install(this, "openmeter")
        enableEdgeToEdge()
        Prefs.init(this)
        Snapshots.load(this)
        if (savedInstanceState == null && Prefs.autoUpdate) UpdateManager.check(this)

        setContent {
            OpenMeterTheme {
                val dark = ThemeState.dark
                SideEffect {
                    WindowCompat.getInsetsController(window, window.decorView).apply {
                        isAppearanceLightStatusBars = !dark
                        isAppearanceLightNavigationBars = !dark
                    }
                }
                AppScope {
                    CrashReportPrompt()
                    var stackText by rememberSaveable { mutableStateOf("home") }
                    var feedback by rememberSaveable { mutableStateOf(false) }
                    val stack = stackText.split('/')
                    val screen = stack.last()
                    fun push(s: String) {
                        stackText = if (s == "home") "home" else (stack.filter { it != s } + s).joinToString("/")
                    }
                    fun pop() {
                        stackText = if (stack.size <= 1) "home" else stack.dropLast(1).joinToString("/")
                    }

                    val meter = Meter.byId(screen)
                    val keepOn = Prefs.keepScreenOn && meter != null
                    LaunchedEffect(keepOn) {
                        if (keepOn) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                    LaunchedEffect(screen) { if (meter != null) Prefs.pushRecent(meter.id) }
                    BackHandler(enabled = stack.size > 1) { pop() }

                    if (!Prefs.onboarded) {
                        WelcomeScreen { Prefs.onboarded = true }
                    } else {
                        AnimatedContent(
                            targetState = screen,
                            transitionSpec = {
                                if (targetState == "home") {
                                    (fadeIn(tween(220)) + scaleIn(tween(260), initialScale = 0.97f)) togetherWith
                                        (fadeOut(tween(160)) + slideOutHorizontally(tween(220)) { it / 6 })
                                } else {
                                    (fadeIn(tween(220)) + slideInHorizontally(tween(260)) { it / 5 }) togetherWith
                                        (fadeOut(tween(140)) + scaleOut(tween(220), targetScale = 0.97f))
                                }
                            },
                            label = "nav",
                        ) { s ->
                            val back = { pop() }
                            when (s) {
                                "home" -> HomeScreen(onOpen = { push(it) }, onFeedback = { feedback = true })
                                "settings" -> SettingsScreen(back, onAbout = { push("about") })
                                "about" -> AboutScreen(back, onFeedback = { feedback = true })
                                "recordings" -> RecordingsScreen(back)
                                "log" -> LogScreen(back, onOpen = { push(it) })
                                else -> Meter.byId(s)?.let { MeterScreen(it, back) }
                                    ?: HomeScreen(onOpen = { push(it) }, onFeedback = { feedback = true })
                            }
                        }
                    }
                    if (feedback) FeedbackDialog(onDismiss = { feedback = false })
                }
            }
        }
    }
}
