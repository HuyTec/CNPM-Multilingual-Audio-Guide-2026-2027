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
import vn.hvp.travelvoice.R
import vn.hvp.travelvoice.data.remote.*

/** The public product is anonymous on both platforms; no unsupported OTP endpoint is offered. */
@Composable
fun TravelProfile(base: String, onServer: (String) -> Unit = {}, onPackages: () -> Unit = {}) {
    val context = LocalContext.current
    val sync by PlaybackOutbox.state(context).collectAsState()
    var server by rememberSaveable(base) { mutableStateOf(base) }
    var message by remember { mutableStateOf("") }
    var invalid by remember { mutableStateOf(false) }
    val visitor = remember { TouristIdentity(context).installation().take(8) }
    Column(Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(stringResource(R.string.q1_profile), style = MaterialTheme.typography.headlineSmall)
        Text(stringResource(R.string.q1_guest), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.q1_anonymous_hint))
        Text(stringResource(R.string.q1_visitor_code, visitor))
        Text(stringResource(R.string.q1_server), style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(server, { server = it; invalid = false }, Modifier.fillMaxWidth(),
            singleLine = true, isError = invalid, label = { Text(stringResource(R.string.q1_server)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            supportingText = { Text(stringResource(if (invalid) R.string.q1_server_invalid else R.string.q1_server_hint)) })
        Button(onClick = {
            val normalized = runCatching { DemoApi.normalizeBaseUrl(server) }.getOrNull()
            if (normalized == null) { invalid = true; return@Button }
            onServer(normalized)
            message = context.getString(R.string.q1_server_saved)
        }) { Text(stringResource(R.string.q1_save_server)) }
        Text(stringResource(R.string.q1_sync_automatic))
        if (sync.base == base) {
            if (sync.syncing && sync.pending > 0) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (sync.pending > 0) Text(stringResource(R.string.q1_sync_pending, sync.pending), Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            if (sync.error) Text(stringResource(R.string.q1_sync_auto_error), color = MaterialTheme.colorScheme.error,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            if (sync.rejected > 0) Text(stringResource(R.string.q1_sync_rejected, sync.rejected), color = MaterialTheme.colorScheme.error)
        }
        OutlinedButton(onClick = onPackages) { Text(stringResource(R.string.q1_offline)) }
        if (message.isNotBlank()) Text(message, Modifier.semantics { liveRegion = LiveRegionMode.Polite })
    }
}
