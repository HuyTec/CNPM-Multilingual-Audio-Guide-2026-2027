package vn.hvp.travelvoice.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import vn.hvp.travelvoice.ui.components.*
import vn.hvp.travelvoice.ui.mock.DemoData
import vn.hvp.travelvoice.ui.theme.TravelVoiceTheme

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExploreScreen(onLocation: (String) -> Unit, modifier: Modifier = Modifier, language: String = "VI") {
    var keyword by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("Tất cả") }
    var radius by rememberSaveable { mutableFloatStateOf(500f) }
    var mapVisible by rememberSaveable { mutableStateOf(false) }
    var scenario by rememberSaveable { mutableStateOf("Bình thường") }
    var settingsMessage by rememberSaveable { mutableStateOf(false) }
    var selectedMarker by rememberSaveable { mutableStateOf<String?>(null) }
    var notice by rememberSaveable { mutableStateOf("Không có") }
    var bannerVisible by rememberSaveable { mutableStateOf(true) }
    var invalidNotice by rememberSaveable { mutableStateOf(false) }
    val manual = keyword.isNotBlank() || category != "Tất cả" || mapVisible
    val matches = remember(keyword, category, radius, manual) {
        DemoData.locations.filter { location ->
            (keyword.isBlank() || location.name.contains(keyword.trim(), ignoreCase = true) || location.street.contains(keyword.trim(), ignoreCase = true)) &&
                (category == "Tất cả" || location.category == category) &&
                (manual || location.distanceMeters <= radius.toInt())
        }.sortedBy { it.distanceMeters }.take(if (manual) 20 else 5)
    }
    val selected = DemoData.locations.firstOrNull { it.id == selectedMarker }
    val nearest = DemoData.locations.minByOrNull { it.distanceMeters }
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            SectionTitle("Khám phá địa điểm", "Chọn một điểm đến để đọc và nghe thuyết minh.")
        }
        item {
            OutlinedTextField(value = keyword, onValueChange = { keyword = it }, label = { Text("Tên địa điểm hoặc tên đường") }, placeholder = { Text("Ví dụ: Ngọc Sơn…") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        }
        item {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (listOf("Tất cả") + DemoData.locations.map { it.category }.distinct()).forEach { value ->
                    FilterChip(selected = category == value, onClick = { category = value }, label = { Text(value) })
                }
            }
            TabRow(selectedTabIndex = if (mapVisible) 1 else 0) {
                Tab(selected = !mapVisible, onClick = { mapVisible = false }, text = { Text("Danh sách") })
                Tab(selected = mapVisible, onClick = { mapVisible = true }, text = { Text("Bản đồ") })
            }
        }
        item {
            Text("Tìm gần bạn · bán kính ${radius.toInt()} m", style = MaterialTheme.typography.titleSmall)
            Slider(value = radius, onValueChange = { radius = it }, valueRange = 50f..500f, steps = 8,
                enabled = scenario != "GPS chưa cấp quyền", modifier = Modifier.semantics { contentDescription = "Bán kính tìm địa điểm gần bạn" })
            Text("Khoảng cách GPS đang được mô phỏng. Từ khóa và danh mục tìm trong toàn bộ dữ liệu.", style = MaterialTheme.typography.bodySmall)
        }
        if (scenario == "GPS chưa cấp quyền") {
            item {
                StateMessage("Không thể truy cập vị trí", "Cần quyền vị trí để tự động tìm điểm đến gần bạn. Bạn vẫn có thể tìm theo từ khóa, danh mục hoặc bản đồ.")
                TextButton(onClick = { settingsMessage = true }) { Text("Mở cài đặt") }
                if (settingsMessage) Text("Demo không mở cài đặt thiết bị. Chọn trạng thái Bình thường để mô phỏng đã cấp quyền.", style = MaterialTheme.typography.bodySmall)
            }
        }
        if (notice != "Không có") item {
            if (notice == "Cooldown") {
                Text("Địa điểm vẫn trong thời gian chờ. Không gửi thêm thông báo.", modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }, style = MaterialTheme.typography.bodySmall)
            }
            if (scenario != "GPS chưa cấp quyền" && bannerVisible && notice in listOf("Banner", "Không có quyền thông báo", "ID không hợp lệ") && nearest != null) {
                Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.medium) {
                    Column(Modifier.padding(16.dp)) {
                        Text(when (language) {
                            "EN" -> "You are near Ngoc Son Temple"
                            "JA" -> "近くの観光地：玉山祠"
                            else -> "Bạn đang gần ${nearest.name}"
                        }, style = MaterialTheme.typography.titleSmall)
                        Text(when (language) {
                            "EN" -> "Open the location details. Narration will not play automatically."
                            "JA" -> "観光地の詳細を開きます。音声は自動再生されません。"
                            else -> if (notice == "Không có quyền thông báo") "Ứng dụng đang mở: hiển thị banner thay cho thông báo hệ thống." else "Chọn để xem thông tin địa điểm; thuyết minh không tự phát."
                        }, style = MaterialTheme.typography.bodySmall)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { if (notice == "ID không hợp lệ") invalidNotice = true else onLocation(nearest.id) }) { Text("Xem địa điểm") }
                            TextButton(onClick = { bannerVisible = false }) { Text("Để sau") }
                        }
                    }
                }
            }
            if (invalidNotice) Text("Địa điểm này không còn khả dụng. Bạn có thể tiếp tục khám phá.", color = MaterialTheme.colorScheme.error, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        }
        when {
            scenario == "Đang tải" -> item { LinearProgressIndicator(Modifier.fillMaxWidth()); Text("Đang tìm địa điểm…") }
            scenario == "Lỗi" -> item { StateMessage("Chưa thể tải danh sách", "Dữ liệu được giữ lại. Hãy thử lại.", { scenario = "Bình thường" }) }
            scenario == "GPS chưa cấp quyền" && !manual -> item { Text("Nhập từ khóa hoặc chọn danh mục để tìm thủ công.") }
            scenario == "Trống" || matches.isEmpty() -> item {
                StateMessage("Không tìm thấy địa điểm", if (!manual) "Thử tăng bán kính hoặc tìm bằng từ khóa, danh mục." else "Thử từ khóa khác hoặc chọn danh mục Tất cả.")
                TextButton(onClick = { keyword = ""; category = "Tất cả"; radius = 500f; scenario = "Bình thường" }) { Text("Xem gợi ý") }
            }
            mapVisible -> item { MapPreview(matches, { selectedMarker = it }) }
            else -> {
                item { SectionTitle(if (manual) "Kết quả tìm kiếm" else "Địa điểm gần bạn", "${matches.size} địa điểm · sắp xếp theo khoảng cách") }
                items(matches, key = { it.id }) { location -> LocationCard(location, { onLocation(location.id) }) }
            }
        }
        item {
            HorizontalDivider()
            Text("Mô phỏng giao diện", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 16.dp))
            DemoStatePicker(scenario, listOf("Bình thường", "Đang tải", "GPS chưa cấp quyền", "Trống", "Lỗi"), { scenario = it; settingsMessage = false })
            Text("Thông báo vị trí · UC-05", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 12.dp))
            DemoStatePicker(notice, listOf("Không có", "Banner", "Không có quyền thông báo", "Cooldown", "ID không hợp lệ"), { notice = it; bannerVisible = true; invalidNotice = false })
        }
    }
    if (selected != null) {
        ModalBottomSheet(onDismissRequest = { selectedMarker = null }) {
            Column(Modifier.padding(20.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionTitle(selected.name, selected.street)
                Text(selected.summary)
                Button(onClick = { selectedMarker = null; onLocation(selected.id) }, modifier = Modifier.fillMaxWidth()) { Text("Chọn địa điểm này") }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun ExploreScreenPreview() { TravelVoiceTheme { ExploreScreen({}) } }
