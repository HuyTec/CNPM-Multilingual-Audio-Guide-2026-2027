package vn.hvp.travelvoice.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import vn.hvp.travelvoice.ui.components.*
import vn.hvp.travelvoice.ui.mock.DemoData
import vn.hvp.travelvoice.ui.theme.TravelVoiceTheme

/** UC-02 prototype: scenarios represent approved fixture availability only. */
@Composable
@OptIn(ExperimentalLayoutApi::class)
fun DetailScreen(
    locationId: String,
    onPlay: (String, String, String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    initialLanguage: String = "VI",
    onLanguageChange: (String) -> Unit = {},
) {
    val location = DemoData.find(locationId)
    var selectedLanguage by rememberSaveable(locationId) { mutableStateOf(initialLanguage) }
    var mode by rememberSaveable(locationId) { mutableStateOf("FULL") }
    var scenario by rememberSaveable(locationId) { mutableStateOf("Bình thường") }
    var feedbackOpen by rememberSaveable { mutableStateOf(false) }
    var feedback by rememberSaveable { mutableStateOf("") }
    var feedbackSent by rememberSaveable { mutableStateOf(false) }
    val loading = scenario == "Đang tải"
    val unavailable = scenario == "Chưa có nội dung"
    val contentLanguage = when (scenario) {
        "Thiếu bản dịch" -> if (selectedLanguage == "EN") "VI" else "EN"
        "Chỉ còn tiếng Việt" -> "VI"
        else -> selectedLanguage
    }

    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        TextButton(onClick = onBack) { Text("‹ Quay lại") }
        if (location == null) {
            StateMessage("Không tìm thấy địa điểm", "Địa điểm này không nằm trong dữ liệu mẫu.")
        } else {
            LocationArtwork(location, Modifier.fillMaxWidth().height(190.dp))
            SectionTitle(location.name, "${location.category} · ${location.street}")
            Text("Thuyết minh địa điểm", style = MaterialTheme.typography.titleMedium)
            Text("Ngôn ngữ", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DemoData.languages.forEach { code ->
                    FilterChip(selected = selectedLanguage == code, onClick = { selectedLanguage = code; onLanguageChange(code) }, label = { Text(languageLabel(code)) })
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("FULL" to "Đầy đủ", "SHORT" to "Tóm tắt").forEach { (value, label) ->
                    FilterChip(selected = mode == value, onClick = { mode = value }, label = { Text(label) })
                }
            }
            when {
                loading -> {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    StateMessage("Đang tải nội dung", "Giữ nguyên địa điểm, ngôn ngữ và chế độ đang chọn.")
                }
                unavailable -> {
                    StateMessage("Nội dung đang được cập nhật", "Địa điểm chưa có thuyết minh được duyệt. Bạn có thể gửi phản hồi.")
                    OutlinedButton(onClick = { feedbackOpen = true }) { Text("Gửi phản hồi") }
                    if (feedbackSent) Text("Đã ghi nhận phản hồi mẫu trong phiên demo.", color = MaterialTheme.colorScheme.primary)
                }
                else -> {
                    if (contentLanguage != selectedLanguage) {
                        Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.small) {
                            Text("Chưa có bản dịch ${languageLabel(selectedLanguage)}. Đang hiển thị ${languageLabel(contentLanguage)} theo thứ tự EN → VI.", Modifier.padding(12.dp))
                        }
                    }
                    Text("Nội dung mẫu được duyệt · ${languageLabel(contentLanguage)} · ${if (mode == "FULL") "Đầy đủ" else "Tóm tắt"}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        guideDemoText(location.name, location.summary, location.fullText, contentLanguage, mode),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
            Button(onClick = { onPlay(location.id, contentLanguage, mode) }, enabled = !loading && !unavailable, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text("Nghe thuyết minh")
            }
            HorizontalDivider()
            Text("Trạng thái thử nghiệm", style = MaterialTheme.typography.labelLarge)
            DemoStatePicker(scenario, listOf("Bình thường", "Đang tải", "Thiếu bản dịch", "Chỉ còn tiếng Việt", "Chưa có nội dung")) { scenario = it }
        }
    }
    if (feedbackOpen) {
        AlertDialog(
            onDismissRequest = { feedbackOpen = false },
            title = { Text("Phản hồi nội dung") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Chỉ mô phỏng giao diện; phản hồi không được gửi lên máy chủ.")
                    OutlinedTextField(value = feedback, onValueChange = { feedback = it }, label = { Text("Nội dung phản hồi") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                }
            },
            confirmButton = { TextButton(onClick = { feedbackSent = true; feedbackOpen = false }, enabled = feedback.isNotBlank()) { Text("Gửi mẫu") } },
            dismissButton = { TextButton(onClick = { feedbackOpen = false }) { Text("Hủy") } },
        )
    }
}

internal fun languageLabel(code: String): String = when (code) {
    "VI" -> "Tiếng Việt"
    "EN" -> "English"
    "JA" -> "日本語"
    else -> code
}

internal fun guideDemoText(name: String, summary: String, fullText: String, language: String, mode: String): String =
    if (language == "VI") {
        if (mode == "FULL") fullText else summary
    } else {
        if (language == "EN") {
            if (mode == "SHORT") "$name\n\nA short demonstration guide. This sample illustrates language selection and is not an official tourism narration."
            else "$name\n\nThis is an illustrative guide for the selected attraction. Read at your own pace, switch to the short summary, or open the simulated audio controls.\n\nThe text is a static UI fixture rather than an official tourism narration. No audio or translation service is used."
        } else {
            if (mode == "SHORT") "$name\n\nこれは短いデモ用ガイドです。言語選択を示すサンプルであり、正式な観光案内ではありません。"
            else "$name\n\n選択した観光地のデモ用ガイドです。自分のペースで読み、短い要約に切り替えたり、音声操作のデモを開いたりできます。\n\nこの文章は画面確認用のサンプルです。正式な観光案内ではなく、音声や翻訳サービスも使用していません。"
        }
    }

@Preview(showBackground = true, widthDp = 390, heightDp = 840)
@Composable
private fun DetailPreview() {
    TravelVoiceTheme { DetailScreen(DemoData.locations.first().id, { _, _, _ -> }, {}) }
}
