package vn.hvp.travelvoice.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import vn.hvp.travelvoice.ui.components.DemoStatePicker
import vn.hvp.travelvoice.ui.components.SectionTitle
import vn.hvp.travelvoice.ui.components.StateMessage
import vn.hvp.travelvoice.ui.theme.TravelVoiceTheme
import java.text.NumberFormat
import java.util.Locale

/** All versions and transfer states are presentation fixtures, held only in UI state. */
@Composable
fun OfflineScreen(modifier: Modifier = Modifier) {
    var scenario by rememberSaveable { mutableStateOf("Bình thường") }
    var installedVersion by rememberSaveable { mutableStateOf("1.0") }
    var optionalInstalled by rememberSaveable { mutableStateOf(true) }
    var phase by rememberSaveable { mutableStateOf("Chưa kiểm tra") }
    var progress by rememberSaveable { mutableFloatStateOf(0f) }
    var updateDialog by rememberSaveable { mutableStateOf(false) }
    var deleteDialog by rememberSaveable { mutableStateOf(false) }
    var interruptionHandled by rememberSaveable { mutableStateOf(false) }
    val percent = NumberFormat.getPercentInstance(Locale.forLanguageTag("vi-VN")).format(progress.toDouble())
    val downloading = phase == "Đang tải" || phase == "Đang kiểm tra"
    val targetVersion = "1.1"

    LaunchedEffect(phase, scenario) {
        if (phase == "Đang tải") {
            while (progress < 1f) {
                delay(300)
                progress = (progress + 0.05f).coerceAtMost(1f)
                if (scenario == "Mất mạng" && progress >= 0.4f && !interruptionHandled) {
                    phase = "Tạm dừng"
                    break
                }
            }
            if (phase == "Đang tải") phase = "Đang kiểm tra"
        } else if (phase == "Đang kiểm tra") {
            delay(1000)
            if (scenario == "Gói không hợp lệ") {
                phase = "Gói không hợp lệ"
            } else {
                installedVersion = targetVersion
                optionalInstalled = true
                phase = "Sẵn sàng ngoại tuyến"
            }
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { SectionTitle("Nội dung ngoại tuyến", "Mang theo thuyết minh ngay cả khi không có Internet.") }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionTitle("Nội dung cơ bản", "Đi kèm ứng dụng · phiên bản 1.0")
                    Text("Đã sẵn sàng ngoại tuyến", color = MaterialTheme.colorScheme.primary)
                    Text("Luôn được giữ lại. Không cần tải trước và không thể xóa từ màn hình này.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionTitle("Thuyết minh bổ sung", "Kịch bản và audio cùng phiên bản · dữ liệu mẫu")
                    Text(if (optionalInstalled) "Đã cài phiên bản $installedVersion · 24 MB" else "Chưa cài · không ảnh hưởng nội dung cơ bản")
                    Text("Bản cập nhật đã duyệt $targetVersion · 28 MB. Kiểm tra cần 40 MB dung lượng trống (mô phỏng).", style = MaterialTheme.typography.bodySmall)
                    Button(
                        onClick = {
                            phase = if (scenario == "Đã mới nhất" || (optionalInstalled && installedVersion == targetVersion)) "Đã mới nhất" else "Có bản cập nhật"
                        },
                        enabled = !downloading && phase != "Tạm dừng",
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Kiểm tra cập nhật") }
                    if (phase == "Có bản cập nhật" || !optionalInstalled) {
                        OutlinedButton(onClick = { updateDialog = true }, enabled = !downloading, modifier = Modifier.fillMaxWidth()) {
                            Text(if (optionalInstalled) "Cập nhật nội dung" else "Tải nội dung bổ sung")
                        }
                    }
                    if (optionalInstalled) {
                        TextButton(onClick = { deleteDialog = true }, enabled = !downloading && phase != "Tạm dừng") { Text("Xóa nội dung bổ sung") }
                    }
                }
            }
        }
        item {
            Column(Modifier.semantics { liveRegion = LiveRegionMode.Polite }, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when (phase) {
                    "Chưa kiểm tra" -> Text("Nội dung đã cài có thể sử dụng ngay. Chọn Kiểm tra cập nhật để xem phiên bản mới.")
                    "Có bản cập nhật" -> StateMessage("Có bản cập nhật", "Chỉ bắt đầu tải sau khi bạn xác nhận. Phiên bản hiện tại được giữ đến khi gói mới được kiểm tra xong.")
                    "Đã mới nhất" -> StateMessage("Nội dung đã mới nhất", "Không có thay đổi nào đối với nội dung đang sử dụng.")
                    "Thiếu dung lượng" -> StateMessage("Không đủ dung lượng", "Cần 40 MB, hiện còn 12 MB (mô phỏng). Xóa nội dung bổ sung hoặc thử lại với trạng thái Bình thường.", {
                        scenario = "Bình thường"; phase = "Có bản cập nhật"
                    })
                    "Đang tải", "Tạm dừng" -> {
                        Text(if (phase == "Đang tải") "Đang tải · $percent" else "Mất kết nối · đã giữ $percent tiến độ")
                        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                        Text("Phiên bản đã cài $installedVersion vẫn được giữ lại.", style = MaterialTheme.typography.bodySmall)
                        if (phase == "Tạm dừng") {
                            Button(onClick = { interruptionHandled = true; phase = "Đang tải" }, modifier = Modifier.fillMaxWidth()) { Text("Mô phỏng có mạng và tiếp tục") }
                        }
                        TextButton(onClick = { phase = "Đã hủy"; progress = 0f }) { Text("Hủy cập nhật") }
                    }
                    "Đang kiểm tra" -> {
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                        Text("Đang kiểm tra gói nội dung…")
                        Text("Chưa thay thế phiên bản đã cài $installedVersion.", style = MaterialTheme.typography.bodySmall)
                    }
                    "Gói không hợp lệ" -> StateMessage("Gói nội dung không hợp lệ", "Đã bỏ dữ liệu tải lỗi. Phiên bản $installedVersion vẫn nguyên vẹn; kịch bản và audio không được cài một phần.", {
                        scenario = "Bình thường"; phase = "Có bản cập nhật"; progress = 0f
                    })
                    "Sẵn sàng ngoại tuyến" -> StateMessage("Sẵn sàng ngoại tuyến", "Đã mô phỏng cài phiên bản $installedVersion với kịch bản và audio đồng bộ.")
                    "Đã hủy" -> StateMessage("Đã hủy cập nhật", "Không thay đổi phiên bản nội dung đã cài $installedVersion.")
                    "Đã xóa" -> StateMessage("Đã xóa nội dung bổ sung", "Nội dung cơ bản vẫn có thể sử dụng ngoại tuyến.")
                }
            }
        }
        item {
            Text("Mô phỏng giao diện", style = MaterialTheme.typography.labelLarge)
            DemoStatePicker(scenario, listOf("Bình thường", "Đã mới nhất", "Thiếu dung lượng", "Mất mạng", "Gói không hợp lệ"), {
                scenario = it
                phase = "Chưa kiểm tra"
                progress = 0f
                interruptionHandled = false
            })
            Text("Không tải tệp hay ghi dữ liệu. Tiến độ, dung lượng và phiên bản chỉ thay đổi trong phiên demo.", style = MaterialTheme.typography.bodySmall)
        }
    }

    if (updateDialog) {
        AlertDialog(
            onDismissRequest = { updateDialog = false },
            title = { Text("Cập nhật nội dung?") },
            text = { Text("Gói đã duyệt $targetVersion gồm kịch bản và audio, tải 28 MB và cần 40 MB trống. Nội dung đang cài được giữ lại nếu hủy hoặc cập nhật thất bại. Đây là thao tác mô phỏng.") },
            confirmButton = {
                TextButton(onClick = {
                    updateDialog = false
                    if (scenario == "Thiếu dung lượng") {
                        phase = "Thiếu dung lượng"
                    } else {
                        progress = 0f
                        interruptionHandled = false
                        phase = "Đang tải"
                    }
                }) { Text("Xác nhận cập nhật") }
            },
            dismissButton = { TextButton(onClick = { updateDialog = false; phase = "Đã hủy" }) { Text("Hủy") } },
        )
    }
    if (deleteDialog) {
        AlertDialog(
            onDismissRequest = { deleteDialog = false },
            title = { Text("Xóa nội dung bổ sung?") },
            text = { Text("Chỉ xóa gói thuyết minh bổ sung. Nội dung cơ bản đi kèm ứng dụng luôn được giữ lại.") },
            confirmButton = { TextButton(onClick = { deleteDialog = false; optionalInstalled = false; phase = "Đã xóa" }) { Text("Xóa nội dung bổ sung") } },
            dismissButton = { TextButton(onClick = { deleteDialog = false }) { Text("Giữ lại") } },
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun OfflineScreenPreview() { TravelVoiceTheme { OfflineScreen() } }

@Preview(showBackground = true, widthDp = 360, heightDp = 800, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun OfflineScreenDarkPreview() { TravelVoiceTheme(darkTheme = true) { OfflineScreen() } }

@Preview(showBackground = true, widthDp = 360, heightDp = 800, fontScale = 1.5f)
@Composable
private fun OfflineScreenLargeFontPreview() { TravelVoiceTheme { OfflineScreen() } }
