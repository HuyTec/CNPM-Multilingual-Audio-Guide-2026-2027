package vn.hvp.travelvoice.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import vn.hvp.travelvoice.R
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TravelSpeeds(rate: Float, onRate: (Float) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(0.75f, 1f, 1.25f, 1.5f).forEach { value -> FilterChip(selected = rate == value, onClick = { onRate(value) },
            label = { Text("${NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN")).format(value)}×") })
        }
    }
}

@Composable
fun TravelSettings(language: String, onLanguage: (String) -> Unit, rate: Float, onRate: (Float) -> Unit,
                   onOffline: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        item { Text(stringResource(R.string.q1_settings), style = MaterialTheme.typography.headlineSmall) }
        item { Text(stringResource(R.string.q1_language), style = MaterialTheme.typography.titleMedium); TravelLanguages(language, onLanguage) }
        item { Text(stringResource(R.string.q1_audio_settings), style = MaterialTheme.typography.titleMedium); Text(stringResource(R.string.q1_speed)); TravelSpeeds(rate, onRate) }
        item { Text(stringResource(R.string.q1_position), style = MaterialTheme.typography.titleMedium); Text(stringResource(R.string.q1_position_hint))
            TextButton(onClick = { context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))) }) { Text(stringResource(R.string.q1_system_settings)) }
        }
        item { HorizontalDivider(); TextButton(onClick = onOffline, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth()) { Text(stringResource(R.string.q1_offline), style = MaterialTheme.typography.titleMedium); Text(stringResource(R.string.q1_offline_hint), style = MaterialTheme.typography.bodyMedium) }
        } }
        item { Text(stringResource(R.string.q1_about), style = MaterialTheme.typography.titleMedium); Text(stringResource(R.string.q1_about_body)) }
    }
}
