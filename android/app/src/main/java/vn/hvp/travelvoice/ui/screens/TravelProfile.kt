package vn.hvp.travelvoice.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.*
import org.json.JSONObject
import vn.hvp.travelvoice.R
import vn.hvp.travelvoice.data.remote.*

@Composable
fun TravelProfile(base: String) {
    val context = LocalContext.current
    val store = remember { TouristIdentity(context) }
    val scope = rememberCoroutineScope()
    var session by remember { mutableStateOf<JSONObject?>(null) }
    var ready by remember { mutableStateOf(false) }
    var email by rememberSaveable { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var challenge by rememberSaveable { mutableStateOf<String?>(null) }
    var retryAt by rememberSaveable { mutableLongStateOf(0L) }
    var remaining by remember { mutableLongStateOf(0L) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableIntStateOf(0) }
    LaunchedEffect(store) {
        try { session = withContext(Dispatchers.IO) { store.session() }; ready = true }
        catch (_: Exception) { error = R.string.q1_account_error; ready = true }
    }
    LaunchedEffect(retryAt) {
        do { remaining = ((retryAt - System.currentTimeMillis()) / 1000).coerceAtLeast(0); if (remaining > 0) delay(1000) } while (remaining > 0)
    }
    fun run(action: suspend () -> Unit) { if (!busy) scope.launch {
        busy = true; error = 0
        try { action() }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (failure: Exception) { error = when ((failure as? GuideApiException)?.status) {
            429 -> R.string.q1_otp_limit; 400 -> R.string.q1_otp_invalid; 503 -> R.string.q1_mail_unavailable; else -> R.string.q1_account_error
        } }
        finally { busy = false }
    } }
    Column(Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(stringResource(R.string.q1_profile),style = MaterialTheme.typography.headlineSmall)
        if (!ready) { CircularProgressIndicator() }
        else if (session != null) {
            Text(stringResource(R.string.q1_signed_in)); Text(session!!.getString("email"), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.q1_account_hint))
            OutlinedButton(enabled = !busy, onClick = { run {
                // Acknowledge queued events before revoking the credential they were recorded under.
                PlaybackOutbox.flush(context,base)
                TouristApi.post(base,"logout",JSONObject(),withContext(Dispatchers.IO) { store.snapshot() })
                withContext(Dispatchers.IO) { store.signedOut() }; session = null; challenge = null; code = ""
            } }) { Text(stringResource(R.string.q1_sign_out)) }
        } else {
            Text(stringResource(R.string.q1_guest),style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.q1_guest_hint))
            OutlinedTextField(email,{ email = it; challenge = null; code = "" },Modifier.fillMaxWidth(),singleLine = true,enabled = !busy,
                label = { Text(stringResource(R.string.q1_email)) },keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
            Button(enabled = !busy && remaining == 0L,onClick = { run {
                if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) { error = R.string.q1_email_invalid; return@run }
                val result = TouristApi.post(base,"otp/request",JSONObject().put("email",email.trim()),withContext(Dispatchers.IO) { store.snapshot() })
                challenge = result.getString("challengeId"); retryAt = System.currentTimeMillis() + result.getLong("retryAfterSeconds") * 1000; code = ""
            } },modifier = Modifier.fillMaxWidth()) {
                Text(if (remaining > 0) stringResource(R.string.q1_resend_wait,remaining) else stringResource(if (challenge == null) R.string.q1_send_otp else R.string.q1_resend_otp))
            }
            if (challenge != null) {
                Text(stringResource(R.string.q1_otp_sent),Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                OutlinedTextField(code,{ code = it },Modifier.fillMaxWidth(),singleLine = true,enabled = !busy,
                    label = { Text(stringResource(R.string.q1_otp)) },keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword))
                Button(enabled = !busy,onClick = { run {
                    if (!code.matches(Regex("[0-9]{6}"))) { error = R.string.q1_otp_invalid; return@run }
                    val result = TouristApi.post(base,"otp/verify",JSONObject().put("challengeId",challenge).put("code",code),withContext(Dispatchers.IO) { store.snapshot() })
                    withContext(Dispatchers.IO) { store.signedIn(result) }; session = result; challenge = null; code = ""
                } },modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.q1_verify)) }
            }
        }
        if (busy) { LinearProgressIndicator(Modifier.fillMaxWidth()); Text(stringResource(R.string.q1_processing)) }
        if (error != 0) Text(stringResource(error),Modifier.semantics { liveRegion = LiveRegionMode.Polite },color = MaterialTheme.colorScheme.error)
    }
}
