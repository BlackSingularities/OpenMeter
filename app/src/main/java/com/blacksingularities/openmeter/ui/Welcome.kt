package com.blacksingularities.openmeter.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.FiberManualRecord
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blacksingularities.openmeter.R

/** Pierwsze uruchomienie: krótko o aplikacji, wybór języka i motywu. */
@Composable
fun WelcomeScreen(onDone: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Palette.Ink)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Spacer(Modifier.height(12.dp))
        Image(painterResource(R.drawable.openmeter_mark), null, modifier = Modifier.size(88.dp).align(Alignment.CenterHorizontally))
        Text(
            stringResource(R.string.welcome_title),
            color = Palette.Fg,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            stringResource(R.string.about_tagline),
            color = Palette.TextDim,
            fontSize = 15.sp,
            lineHeight = 21.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(4.dp))
        Feature(Icons.Rounded.Sensors, stringResource(R.string.welcome_f1))
        Feature(Icons.Rounded.FiberManualRecord, stringResource(R.string.welcome_f2))
        Feature(Icons.Rounded.Bookmarks, stringResource(R.string.welcome_f3))
        Feature(Icons.Rounded.Lock, stringResource(R.string.welcome_f4))
        Spacer(Modifier.height(4.dp))
        Panel {
            SectionLabel(stringResource(R.string.set_language))
            LanguagePicker()
            Spacer(Modifier.height(14.dp))
            SectionLabel(stringResource(R.string.set_theme))
            ThemePicker()
        }
        Spacer(Modifier.height(4.dp))
        Pill(stringResource(R.string.welcome_start), active = true, modifier = Modifier.fillMaxWidth(), onClick = onDone)
    }
}

@Composable
private fun Feature(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(Palette.CardHigh), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Palette.Fg, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Text(text, color = Palette.Fg, fontSize = 14.sp, lineHeight = 19.sp, modifier = Modifier.weight(1f))
    }
}
