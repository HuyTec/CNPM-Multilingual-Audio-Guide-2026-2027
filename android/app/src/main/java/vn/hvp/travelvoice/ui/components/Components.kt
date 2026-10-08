package vn.hvp.travelvoice.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import vn.hvp.travelvoice.ui.mock.DemoLocation
import java.text.NumberFormat
import java.util.Locale

@Composable
fun SectionTitle(title: String, subtitle: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.headlineSmall)
        if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun StateMessage(title: String, message: String, onRetry: (() -> Unit)? = null) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(message, style = MaterialTheme.typography.bodyMedium)
            if (onRetry != null) TextButton(onClick = onRetry) { Text("Thử lại") }
        }
    }
}

@Composable
fun DemoStatePicker(selected: String, options: List<String>, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Tình huống mô phỏng jhdasdfdfdfdffd", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { label -> FilterChip(selected = selected == label, onClick = { onSelect(label) }, label = { Text(label) }) }
        }
    }
}

@Composable
fun LocationCard(location: DemoLocation, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(onClick = onClick, modifier = modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            LocationArtwork(location, Modifier.size(80.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(location.name, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("${NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN")).format(location.distanceMeters)} m • ${location.category}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Text(location.summary, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, Modifier.size(18.dp))
        }
    }
}

/** Local vector placeholder, deliberately no photo download or map provider. */
@Composable
fun LocationArtwork(location: DemoLocation, modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    val background = MaterialTheme.colorScheme.primaryContainer
    val stone = MaterialTheme.colorScheme.secondaryContainer
    Canvas(modifier.clip(MaterialTheme.shapes.small).semantics { contentDescription = "Hình minh họa ${location.name}" }) {
        drawRect(background)
        drawCircle(stone, radius = size.minDimension * .45f, center = Offset(size.width * .8f, size.height * .25f))
        val width = size.width
        val height = size.height
        drawRect(primary, topLeft = Offset(width * .2f, height * .48f), size = Size(width * .6f, height * .07f))
        for (i in 0..3) {
            drawRect(primary, topLeft = Offset(width * (.25f + .14f * i), height * .55f), size = Size(width * .06f, height * .23f))
        }
        drawRect(primary, topLeft = Offset(width * .18f, height * .78f), size = Size(width * .64f, height * .06f))
    }
}
