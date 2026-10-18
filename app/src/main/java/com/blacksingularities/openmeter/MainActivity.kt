package com.blacksingularities.openmeter

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import com.blacksingularities.openmeter.core.Prefs
import com.blacksingularities.openmeter.meters.Meter
import com.blacksingularities.openmeter.meters.MeterScreen
import com.blacksingularities.openmeter.ui.HomeScreen
import com.blacksingularities.openmeter.ui.OpenMeterTheme
import com.blacksingularities.openmeter.ui.Palette
import com.blacksingularities.openmeter.ui.RecordingsScreen
import com.blacksingularities.openmeter.ui.SettingsScreen
import com.blacksingularities.openmeter.update.UpdateManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false
        Prefs.init(this)
        if (savedInstanceState == null && Prefs.autoUpdate) UpdateManager.check(this)

        setContent {
            OpenMeterTheme {
                var screen by rememberSaveable { mutableStateOf("home") }
                val meter = Meter.byId(screen)
                val keepOn = Prefs.keepScreenOn && meter != null
                LaunchedEffect(keepOn) {
                    if (keepOn) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
                BackHandler(enabled = screen != "home") { screen = "home" }
                Box(Modifier.fillMaxSize().background(Palette.Ink)) {
                    AnimatedContent(
                        targetState = screen,
                        transitionSpec = {
                            (fadeIn(tween(180)) + scaleIn(tween(220), initialScale = 0.96f)) togetherWith fadeOut(tween(120))
                        },
                        label = "nav",
                    ) { s ->
                        val back = { screen = "home" }
                        when (s) {
                            "home" -> HomeScreen { screen = it }
                            "settings" -> SettingsScreen(back)
                            "recordings" -> RecordingsScreen(back)
                            else -> Meter.byId(s)?.let { MeterScreen(it, back) } ?: HomeScreen { screen = it }
                        }
                    }
                }
            }
        }
    }
}
