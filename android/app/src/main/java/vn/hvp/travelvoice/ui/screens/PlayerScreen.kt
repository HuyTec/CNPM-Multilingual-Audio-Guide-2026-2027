package vn.hvp.travelvoice.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import vn.hvp.travelvoice.ui.components.*
import vn.hvp.travelvoice.ui.mock.DemoData
import vn.hvp.travelvoice.ui.theme.TravelVoiceTheme
import java.util.Locale
import java.text.NumberFormat

/** UC-03 display simulation. No audio, TTS, audio-focus service or file is used. */
@Composable
@OptIn(ExperimentalLayoutApi::class)
fun PlayerScreen(
    locationId: String,
    language: String,
    mode: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val location = DemoData.find(locationId)
    val validContent = location != null && language in DemoData.languages && mode in listOf("FULL", "SHORT")
    val duration = if (mode == "SHORT") 90f else 300f
    var playing by rememberSaveable(locationId, language, mode) { mutableStateOf(validContent) }
    var position by rememberSaveable(locationId, language, mode) { mutableStateOf(0f) }
    var speed by rememberSaveable { mutableStateOf(1f) }
    var scenario by rememberSaveable(locationId, language, mode) { mutableStateOf("Audio có sẵn") }
    var playbackNotice by rememberSaveable { mutableStateOf("") }
    val tts = scenario == "Audio lỗi → TTS" || scenario == "Thiếu audio → TTS"

    LaunchedEffect(locationId, language, mode, playing, speed, validContent) {
        while (playing && validContent) {
            delay(1000)
            position = (position + speed).coerceAtMost(duration)
            if (position >= duration) playing = false
        }
    }

    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        TextButton(onClick = onBack) { Text("‹ Thuyết minh địa điểm") }
        if (!validContent || location == null) {
            StateMessage("Không có nội dung để phát", "Hãy quay lại và chọn thuyết minh đã được duyệt.")
        } else {
            SectionTitle("Luồng nghe", location.name)
            LocationArtwork(location, Modifier.fillMaxWidth().height(160.dp))
            Text("${languageLabel(language)} · ${if (mode == "FULL") "Đầy đủ" else "Tóm tắt"}", style = MaterialTheme.typography.labelLarge)
            Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.small, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(if (tts) "Giọng đọc TTS · mô phỏng" else "Audio từ bộ nhớ đệm · mô phỏng", style = MaterialTheme.typography.titleSmall)
                    Text(if (tts) "Audio không khả dụng. Tiếp tục với đúng văn bản, ngôn ngữ và chế độ đã chọn." else "Ưu tiên audio có sẵn tương ứng với nội dung đang xem.", style = MaterialTheme.typography.bodySmall)
                }
            }
            Text(guideDemoText(location.name, location.summary, location.fullText, language, mode), style = MaterialTheme.typography.bodyLarge)
            HorizontalDivider()
            Text(if (playing) "Đang phát" else if (position >= duration) "Đã nghe hết" else "Đã tạm dừng", style = MaterialTheme.typography.titleSmall, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            Text("Vị trí nghe", style = MaterialTheme.typography.labelLarge)
            Slider(value = position, onValueChange = { position = it; if (position >= duration) playing = false }, valueRange = 0f..duration, modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Vị trí nghe" })
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(playerTime(position), style = MaterialTheme.typography.labelMedium)
                Text(playerTime(duration), style = MaterialTheme.typography.labelMedium)
            }
            FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedButton(onClick = { position = (position - 15f).coerceAtLeast(0f) }) { Text("Lùi 15 giây") }
                Button(onClick = {
                    if (position >= duration) position = 0f
                    playing = !playing
                    playbackNotice = ""
                }) { Text(if (playing) "Tạm dừng" else "Phát tiếp") }
                OutlinedButton(onClick = { position = (position + 15f).coerceAtMost(duration); if (position >= duration) playing = false }) { Text("Tới 15 giây") }
            }
            Text("Tốc độ phát", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0.75f, 1f, 1.25f, 1.5f).forEach { value ->
                    FilterChip(selected = speed == value, onClick = { speed = value }, label = { Text("${NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN")).format(value)}×") })
                }
            }
            if (playbackNotice.isNotEmpty()) {
                Text(playbackNotice, color = MaterialTheme.colorScheme.error, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            }
            HorizontalDivider()
            Text("Trạng thái thử nghiệm", style = MaterialTheme.typography.labelLarge)
            DemoStatePicker(scenario, listOf("Audio có sẵn", "Thiếu audio → TTS", "Audio lỗi → TTS", "Cuộc gọi đến", "Ngắt tai nghe")) { selected ->
                scenario = selected
                if (selected == "Cuộc gọi đến" || selected == "Ngắt tai nghe") {
                    playing = false
                    playbackNotice = if (selected == "Cuộc gọi đến") "Đã tạm dừng do cuộc gọi đến. Vị trí nghe được giữ nguyên; chọn Phát tiếp khi sẵn sàng." else "Tai nghe đã ngắt. Đã tạm dừng và giữ vị trí để tránh phát qua loa ngoài."
                } else playbackNotice = ""
            }
            Text("Điều khiển chỉ thay đổi giao diện. Không phát âm thanh hoặc tạo giọng đọc thật.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun playerTime(seconds: Float): String = String.format(Locale("vi", "VN"), "%d:%02d", seconds.toInt() / 60, seconds.toInt() % 60)

@Preview(showBackground = true, widthDp = 390, heightDp = 840)
@Composable
private fun PlayerPreview() {
    TravelVoiceTheme { PlayerScreen(DemoData.locations.first().id, "VI", "FULL", {}) }
}
