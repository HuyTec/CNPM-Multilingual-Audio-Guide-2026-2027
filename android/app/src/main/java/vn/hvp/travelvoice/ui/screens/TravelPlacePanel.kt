package vn.hvp.travelvoice.ui.screens

import android.location.Location
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import vn.hvp.travelvoice.R
import vn.hvp.travelvoice.BuildConfig
import vn.hvp.travelvoice.ui.components.PublishedLocationPhoto
import vn.hvp.travelvoice.data.remote.ServerGuide
import vn.hvp.travelvoice.data.remote.ServerLocation
import vn.hvp.travelvoice.data.remote.SharedTourRepository
import vn.hvp.travelvoice.ui.navigation.distanceMeters
import java.text.NumberFormat
import java.util.Locale

@Composable
fun TravelPlaceCard(place: ServerLocation, guide: ServerGuide?, failed: Boolean, position: Location?,
                    onRetry: () -> Unit, onClose: () -> Unit, onRead: () -> Unit, onPlay: () -> Unit,
                    base: String = BuildConfig.API_BASE_URL) {
    Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(place.name, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(place.category, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                position?.let { origin -> distanceMeters(origin, place)?.let { distance ->
                    val number = NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN")).format(distance.toInt())
                    Text(stringResource(R.string.q1_distance, "$number m"), style = MaterialTheme.typography.labelMedium)
                } }
            }
            IconButton(onClick = onClose) { Icon(Icons.Default.Close, stringResource(R.string.q1_close)) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            PublishedLocationPhoto(base, place.imageUrl, place.name, Modifier.size(80.dp), compact = true)
            when {
                failed -> Column(Modifier.weight(1f)) { Text(stringResource(R.string.q1_error)); TextButton(onClick = onRetry) { Text(stringResource(R.string.q1_retry)) } }
                guide == null -> Column(Modifier.weight(1f)) { LinearProgressIndicator(Modifier.fillMaxWidth()); Text(stringResource(R.string.q1_description_pending), style = MaterialTheme.typography.bodySmall) }
                else -> Text(guide.scriptText, Modifier.weight(1f), maxLines = 3, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onPlay, enabled = guide != null, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.PlayArrow, null); Text(stringResource(R.string.q1_play), maxLines = 2)
            }
            TextButton(onClick = onRead) { Text(stringResource(R.string.q1_read_more)) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TravelLanguages(language: String, onLanguage: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SharedTourRepository.languages.forEach { code -> FilterChip(selected = language == code, onClick = { onLanguage(code) }, label = { Text(code) }) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TravelPlaceDetails(place: ServerLocation, guide: ServerGuide?, failed: Boolean, language: String, onLanguage: (String) -> Unit,
                       mode: String, onMode: (String) -> Unit, onRetry: () -> Unit, onPlay: () -> Unit, onRate: (Float) -> Unit = {},
                       base: String = BuildConfig.API_BASE_URL) {
    LazyColumn(Modifier.fillMaxHeight(0.9f), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Text(place.name, style = MaterialTheme.typography.headlineSmall); Text(place.category, color = MaterialTheme.colorScheme.primary) }
        item { PublishedLocationPhoto(base, place.imageUrl, place.name, Modifier.fillMaxWidth().height(180.dp)) }
        item { Text(stringResource(R.string.q1_language), style = MaterialTheme.typography.titleSmall); TravelLanguages(language, onLanguage)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("FULL" to R.string.q1_full, "SHORT" to R.string.q1_short).forEach { (type, label) ->
                FilterChip(selected = mode == type, onClick = { onMode(type) }, label = { Text(stringResource(label)) })
            } }
        }
        if (guide != null && vn.hvp.travelvoice.platform.audio.TravelPlayback.guide?.contentId == guide.contentId) {
            item { TravelPlayerPanel(onRate, inline = true) }
        }
        item {
            Text(stringResource(R.string.q1_description), style = MaterialTheme.typography.titleMedium)
            when {
                failed -> { Text(stringResource(R.string.q1_error)); TextButton(onClick = onRetry) { Text(stringResource(R.string.q1_retry)) } }
                guide == null -> LinearProgressIndicator(Modifier.fillMaxWidth())
                else -> {
                    if (guide.languageCode != language) Text(stringResource(R.string.q1_fallback, language, guide.languageCode), Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                    Text(guide.scriptText)
                }
            }
        }
        item { Button(onClick = onPlay, enabled = guide != null, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.PlayArrow, null); Text(stringResource(R.string.q1_play)) } }

    }
}
