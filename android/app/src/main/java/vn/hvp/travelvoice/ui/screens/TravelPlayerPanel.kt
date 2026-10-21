package vn.hvp.travelvoice.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import vn.hvp.travelvoice.R
import vn.hvp.travelvoice.platform.audio.TravelPlayback
import java.util.Locale

@Composable
fun TravelMiniPlayer(onOpen: () -> Unit) {
    val guide = TravelPlayback.guide ?: return
    val narrator = TravelPlayback.narrator ?: return
    Surface(color = MaterialTheme.colorScheme.primaryContainer) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onOpen, modifier = Modifier.weight(1f)) {
                Column(Modifier.fillMaxWidth()) {
                    Text(guide.locationName, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleSmall)
                    Text("${guide.languageCode} · ${guide.scriptType}", style = MaterialTheme.typography.labelMedium)
                }
            }
            val playing = narrator.state in listOf("Đang phát", "Đang chuẩn bị…")
            IconButton(onClick = TravelPlayback::toggle) { Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, stringResource(if (playing) R.string.q1_pause else R.string.q1_resume)) }
        }
    }
}

@Composable
fun TravelPlayerPanel(onRate: (Float) -> Unit) {
    val narrator = TravelPlayback.narrator
    val guide = TravelPlayback.guide
    val context = LocalContext.current
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(stringResource(R.string.q1_listen), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        if (narrator == null || guide == null) {
            Text(stringResource(R.string.q1_player_empty), style = MaterialTheme.typography.headlineSmall)
            Text(stringResource(R.string.q1_player_empty_hint)); return
        }
        Text(guide.locationName, style = MaterialTheme.typography.headlineSmall)
        Text("${guide.languageCode} · ${guide.scriptType}")
        Text(narrator.state, style = MaterialTheme.typography.bodyMedium)
        val playing = narrator.state in listOf("Đang phát", "Đang chuẩn bị…")
        val max = if (narrator.ttsSource) (narrator.total - 1).coerceAtLeast(1) else narrator.total.coerceAtLeast(1)
        if (narrator.total > 0) {
            val seekLabel = stringResource(R.string.q1_seek)
            Slider(value = narrator.position.toFloat().coerceIn(0f, max.toFloat()), onValueChange = { narrator.seek(it.toInt()) }, valueRange = 0f..max.toFloat(), modifier = Modifier.semantics { contentDescription = seekLabel })
            Text(if (narrator.ttsSource) stringResource(R.string.q1_tts_progress, narrator.position + 1, narrator.total)
                else "${clock(narrator.position)} / ${clock(narrator.total)}")
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { narrator.seek(narrator.position - if (narrator.ttsSource) 1 else 10) }) {
                Icon(Icons.Default.Replay10, stringResource(if (narrator.ttsSource) R.string.q1_previous_sentence else R.string.q1_rewind)) }
            FilledIconButton(onClick = TravelPlayback::toggle, modifier = Modifier.size(64.dp)) {
                Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, stringResource(if (playing) R.string.q1_pause else R.string.q1_resume), Modifier.size(32.dp)) }
            IconButton(onClick = { narrator.seek(narrator.position + if (narrator.ttsSource) 1 else 10) }) {
                Icon(Icons.Default.Forward10, stringResource(if (narrator.ttsSource) R.string.q1_next_sentence else R.string.q1_forward)) }
        }
        Text(stringResource(R.string.q1_speed), style = MaterialTheme.typography.titleSmall)
        TravelSpeeds(narrator.rate, onRate)
        if (narrator.ttsSource) Text(stringResource(R.string.q1_tts_hint), style = MaterialTheme.typography.bodySmall)
        OutlinedButton(onClick = { TravelPlayback.stop(context) }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.q1_stop)) }
    }
}

private fun clock(seconds: Int) = String.format(Locale.forLanguageTag("vi-VN"), "%d:%02d", seconds / 60, seconds % 60)
