package vn.hvp.travelvoice.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import vn.hvp.travelvoice.ui.mock.DemoData
import vn.hvp.travelvoice.ui.mock.DemoLocation
import vn.hvp.travelvoice.ui.theme.TravelVoiceTheme

/** Offline schematic: marker positions illustrate selection, not geographic navigation. */
@Composable
@OptIn(ExperimentalLayoutApi::class)
fun MapPreview(locations: List<DemoLocation>, onMarker: (String) -> Unit, modifier: Modifier = Modifier) {
    var zoom by rememberSaveable { mutableIntStateOf(1) }
    var pan by rememberSaveable { mutableFloatStateOf(0f) }
    val scale = if (zoom == 2) 1.2f else 1f
    val road = MaterialTheme.colorScheme.outlineVariant
    val land = MaterialTheme.colorScheme.surfaceVariant
    val water = MaterialTheme.colorScheme.secondaryContainer
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Bản đồ minh họa · dữ liệu ngoại tuyến", style = MaterialTheme.typography.labelLarge)
        Surface(shape = MaterialTheme.shapes.medium, color = land) {
            BoxWithConstraints(Modifier.fillMaxWidth().height(300.dp).clip(MaterialTheme.shapes.medium)) {
                Canvas(Modifier.fillMaxSize().graphicsLayer { scaleX = scale; scaleY = scale; translationX = pan * size.width }.semantics { contentDescription = "Sơ đồ minh họa; chọn nút đánh dấu địa điểm bên trên" }) {
                    drawOval(water, topLeft = Offset(size.width * .58f, size.height * .12f), size = Size(size.width * .34f, size.height * .76f))
                    for (index in 1..4) {
                        drawLine(road, Offset(0f, size.height * index / 5), Offset(size.width, size.height * index / 5 + 35), strokeWidth = 13f)
                        drawLine(road, Offset(size.width * index / 5, 0f), Offset(size.width * index / 5 - 35, size.height), strokeWidth = 13f)
                    }
                }
                locations.take(6).forEachIndexed { index, location ->
                    val x = ((.08f + (index % 3) * .29f) - .5f) * scale + .5f + pan
                    val y = ((.12f + (index / 3) * .43f) - .5f) * scale + .5f
                    FilledIconButton(onClick = { onMarker(location.id) }, modifier = Modifier.offset(x = (maxWidth * x).coerceIn(0.dp, (maxWidth - 48.dp).coerceAtLeast(0.dp)), y = (maxHeight * y).coerceIn(0.dp, (maxHeight - 48.dp).coerceAtLeast(0.dp))).size(48.dp)) {
                        Icon(Icons.Default.LocationOn, contentDescription = "Xem ${location.name}")
                    }
                }
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            OutlinedButton(onClick = { zoom = 1 }, enabled = zoom > 1) { Text("Thu nhỏ") }
            OutlinedButton(onClick = { zoom = 2 }, enabled = zoom < 2) { Text("Phóng to") }
            Text("Mức $zoom", modifier = Modifier.padding(top = 14.dp), style = MaterialTheme.typography.labelLarge)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            OutlinedButton(onClick = { pan = (pan + .08f).coerceAtMost(.16f) }, enabled = pan < .16f) { Text("Dịch sang trái") }
            OutlinedButton(onClick = { pan = (pan - .08f).coerceAtLeast(-.16f) }, enabled = pan > -.16f) { Text("Dịch sang phải") }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MapPreviewPreview() { TravelVoiceTheme { MapPreview(DemoData.locations, {}) } }
