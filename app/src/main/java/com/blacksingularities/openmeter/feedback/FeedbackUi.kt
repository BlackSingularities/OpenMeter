package com.blacksingularities.openmeter.feedback

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blacksingularities.openmeter.R
import com.blacksingularities.openmeter.ui.AppDialog
import com.blacksingularities.openmeter.ui.Palette
import com.blacksingularities.openmeter.ui.Pill
import com.blacksingularities.openmeter.ui.Segmented
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Wysyłka w tle (korutyny) – nakładki na blokujące funkcje z [Feedback]. */
suspend fun Feedback.send(context: Context, kind: Feedback.Kind, message: String, email: String, includeDiagnostics: Boolean): Result<Unit> =
    withContext(Dispatchers.IO) { sendBlocking(context, kind, message, email, includeDiagnostics) }

suspend fun Feedback.sendCrash(context: Context, crash: Feedback.PendingCrash, comment: String, email: String): Result<Unit> =
    withContext(Dispatchers.IO) { sendCrashBlocking(context, crash, comment, email) }

suspend fun Feedback.sendCrashText(context: Context, stack: String, comment: String, email: String): Result<Unit> =
    withContext(Dispatchers.IO) { sendCrashTextBlocking(context, stack, comment, email) }

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Palette.Fg,
    unfocusedBorderColor = Palette.Line,
    cursorColor = Palette.Fg,
    focusedTextColor = Palette.Fg,
    unfocusedTextColor = Palette.Fg,
    focusedLabelColor = Palette.Fg,
    unfocusedLabelColor = Palette.TextDim,
    focusedPlaceholderColor = Palette.TextFaint,
    unfocusedPlaceholderColor = Palette.TextFaint,
    focusedSupportingTextColor = Palette.TextFaint,
    unfocusedSupportingTextColor = Palette.TextFaint,
)

/** Formularz zgłoszenia: błąd albo sugestia funkcjonalności, z opcjonalnym e-mailem. */
@Composable
fun FeedbackDialog(onDismiss: () -> Unit, initialKind: Feedback.Kind = Feedback.Kind.BUG) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var kind by rememberSaveable { mutableStateOf(initialKind) }
    var message by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var diagnostics by rememberSaveable { mutableStateOf(true) }
    var sending by remember { mutableStateOf(false) }
    var sent by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val tooShort = stringResource(R.string.fb_too_short)
    val failed = stringResource(R.string.fb_failed)

    AppDialog(
        stringResource(R.string.feedback_entry),
        onDismiss = { if (!sending) onDismiss() },
        buttons = {
            if (sent) {
                Pill(stringResource(R.string.close), active = true, onClick = onDismiss)
            } else {
                Pill(stringResource(R.string.cancel)) { if (!sending) onDismiss() }
                Pill(stringResource(if (sending) R.string.fb_sending else R.string.fb_send), active = true) {
                    if (sending) return@Pill
                    if (message.trim().length < 3) {
                        error = tooShort
                        return@Pill
                    }
                    sending = true
                    scope.launch {
                        Feedback.send(context, kind, message, email, diagnostics)
                            .onSuccess { sent = true }
                            .onFailure { error = failed }
                        sending = false
                    }
                }
            }
        },
    ) {
        if (sent) {
            Text(stringResource(R.string.fb_thanks), color = Palette.Fg)
        } else {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.fb_subtitle), color = Palette.TextDim, fontSize = 13.sp)
                Segmented(
                    listOf(stringResource(R.string.fb_bug), stringResource(R.string.fb_feature)),
                    if (kind == Feedback.Kind.BUG) 0 else 1,
                    Modifier.fillMaxWidth(),
                ) { kind = if (it == 0) Feedback.Kind.BUG else Feedback.Kind.FEATURE }
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it.take(8000); error = null },
                    placeholder = { Text(stringResource(if (kind == Feedback.Kind.BUG) R.string.fb_bug_hint else R.string.fb_feature_hint)) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                    enabled = !sending,
                    colors = fieldColors(),
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it.take(200) },
                    label = { Text(stringResource(R.string.fb_email)) },
                    supportingText = { Text(stringResource(R.string.fb_email_hint)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !sending,
                    colors = fieldColors(),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = diagnostics,
                        onCheckedChange = { diagnostics = it },
                        enabled = !sending,
                        colors = CheckboxDefaults.colors(checkedColor = Palette.Fg, checkmarkColor = Palette.Ink),
                    )
                    Text(stringResource(R.string.fb_diagnostics), color = Palette.TextDim, fontSize = 13.sp)
                }
                if (sending) CircularProgressIndicator(Modifier.size(20.dp), color = Palette.Fg, strokeWidth = 2.dp)
                error?.let { Text(it, color = Palette.Record, fontSize = 13.sp) }
            }
        }
    }
}

/**
 * Pytanie o wysłanie raportu po awarii. Umieść raz w korzeniu interfejsu; pokazuje się tylko,
 * gdy poprzednie uruchomienie skończyło się błędem.
 */
@Composable
fun CrashReportPrompt() {
    val context = LocalContext.current
    var crash by remember { mutableStateOf<Feedback.PendingCrash?>(null) }
    LaunchedEffect(Unit) { crash = Feedback.pendingCrash(context) }
    val current = crash ?: return
    CrashReportDialog(
        stack = current.stack,
        onDismiss = {
            Feedback.discardPendingCrash(context)
            crash = null
        },
        send = { comment, email -> Feedback.sendCrash(context, current, comment, email) },
    )
}

/** Okno raportu o błędzie: komentarz i e-mail są opcjonalne, szczegóły można podejrzeć i skopiować. */
@Composable
fun CrashReportDialog(
    stack: String,
    onDismiss: () -> Unit,
    send: (suspend (comment: String, email: String) -> Result<Unit>)? = null,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    var comment by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var details by remember { mutableStateOf(false) }
    var sending by remember { mutableStateOf(false) }
    var sent by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val failed = stringResource(R.string.fb_failed)

    AppDialog(
        stringResource(R.string.fb_crash_title),
        onDismiss = {},
        buttons = {
            if (sent) {
                Pill(stringResource(R.string.close), active = true, onClick = onDismiss)
            } else {
                Pill(stringResource(R.string.fb_dont_send)) { if (!sending) onDismiss() }
                Pill(stringResource(if (sending) R.string.fb_sending else R.string.fb_send_report), active = true) {
                    if (sending) return@Pill
                    sending = true
                    scope.launch {
                        val result = send?.invoke(comment, email) ?: Feedback.sendCrashText(context, stack, comment, email)
                        result.onSuccess { sent = true }.onFailure { error = failed }
                        sending = false
                    }
                }
            }
        },
    ) {
        if (sent) {
            Text(stringResource(R.string.fb_thanks), color = Palette.Fg)
        } else {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.fb_crash_body), color = Palette.TextDim, fontSize = 14.sp, lineHeight = 20.sp)
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it.take(4000) },
                    placeholder = { Text(stringResource(R.string.fb_crash_comment)) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !sending,
                    colors = fieldColors(),
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it.take(200) },
                    label = { Text(stringResource(R.string.fb_email)) },
                    supportingText = { Text(stringResource(R.string.fb_email_hint)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !sending,
                    colors = fieldColors(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pill(stringResource(if (details) R.string.fb_hide_details else R.string.fb_show_details)) { details = !details }
                    Pill(stringResource(R.string.fb_copy)) { clipboard.setText(AnnotatedString(stack)) }
                }
                if (details) {
                    Text(
                        stack.lines().take(40).joinToString("\n"),
                        color = Palette.TextDim,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        lineHeight = 13.sp,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
                error?.let { Text(it, color = Palette.Record, fontSize = 13.sp) }
            }
        }
    }
}
