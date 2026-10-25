package vn.hvp.travelvoice.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import vn.hvp.travelvoice.BuildConfig
import vn.hvp.travelvoice.data.remote.DemoApi
import vn.hvp.travelvoice.data.remote.PlaybackOutbox
import vn.hvp.travelvoice.data.remote.ServerGuide
import vn.hvp.travelvoice.data.remote.ServerLocation
import vn.hvp.travelvoice.data.remote.SharedTourRepository
import vn.hvp.travelvoice.platform.audio.SharedNarrator
import vn.hvp.travelvoice.ui.components.MapPreview
import vn.hvp.travelvoice.ui.mock.DemoLocation
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SharedTourScreen(modifier: Modifier = Modifier, offlinePage: Boolean = false) {
    val context = LocalContext.current.applicationContext
    val preferences = remember { context.getSharedPreferences("demo-server", Context.MODE_PRIVATE) }
    var base by rememberSaveable { mutableStateOf(preferences.getString("base", BuildConfig.DEMO_API_BASE_URL)!!
        .let { if(it.trimEnd('/')=="http://10.0.2.2:8081")BuildConfig.DEMO_API_BASE_URL else it }) }
    var address by rememberSaveable { mutableStateOf(base) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    val repository = remember(base) { SharedTourRepository(context, base) }
    var revision by remember { mutableIntStateOf(0) }
    var language by rememberSaveable { mutableStateOf("VI") }
    var mode by rememberSaveable { mutableStateOf("FULL") }
    var keyword by rememberSaveable { mutableStateOf("") }
    var allLocations by rememberSaveable { mutableStateOf(false) }
    var map by rememberSaveable { mutableStateOf(false) }
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    var locations by remember(base, revision) { mutableStateOf<List<ServerLocation>?>(null) }
    var catalogError by remember(base, revision) { mutableStateOf("") }
    var source by remember(base, revision) { mutableStateOf("") }
    var guide by remember(base, selected, language, mode, revision) { mutableStateOf<ServerGuide?>(null) }
    var guideError by remember(base, selected, language, mode, revision) { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    LaunchedEffect(repository, revision, language) {
        try { val result = repository.locations(); locations = result.first.map { it.copy(name = it.names[language.lowercase()] ?: it.names["vi"] ?: it.name) }; source = if (result.second) "Chưa kết nối máy chủ · dùng gói offline đã xác minh" else "Nội dung từ máy chủ chung với Web Admin" }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (error: Exception) { catalogError = error.message ?: "Không tải được địa điểm" }
    }
    LaunchedEffect(repository, selected, language, mode, revision) {
        val id = selected ?: return@LaunchedEffect
        try { val result = repository.guide(id, language, mode); guide = result.first; source = if (result.second) "Bài từ gói offline đã xác minh" else "Bài từ máy chủ chung với Web" }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (error: Exception) { guideError = error.message ?: "Không tải được thuyết minh" }
    }
    Column(modifier) {
        PackageUpdateNotice(repository)
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Text(if (offlinePage) "Nội dung offline" else "Quận 1 & điểm đến lân cận", style = MaterialTheme.typography.headlineSmall)
            Text("Nội dung demo đang rà soát; tọa độ minh họa, không phải khoảng cách đo từ GPS.", style = MaterialTheme.typography.bodySmall)
            Text(source, Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
        item {
            OutlinedButton(onClick = { revision++ }) { Text("Làm mới nội dung") }
            TextButton(onClick = { showSettings = !showSettings }) { Text("Kết nối máy chủ") }
            if (showSettings) Column {
                OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Địa chỉ máy chủ") }, modifier = Modifier.fillMaxWidth())
                OutlinedButton(onClick = {
                    runCatching { DemoApi.normalizeBaseUrl(address) }.onSuccess { base = it; preferences.edit().putString("base", it).apply(); revision++ }
                        .onFailure { message = "Địa chỉ chưa hợp lệ" }
                }) { Text("Kết nối") }
            }
            if (message.isNotBlank()) Text(message, Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        }
        if (offlinePage) {
            item { SharedOfflineContent(repository) }
        } else if (selected == null) {
            item {
                OutlinedTextField(value = keyword, onValueChange = { keyword = it }, label = { Text("Tìm địa điểm") }, modifier = Modifier.fillMaxWidth())
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SharedTourRepository.languages.forEach { code -> FilterChip(selected = language == code, onClick = { language = code }, label = { Text(code) }) }
                }
                OutlinedButton(onClick = { map = !map }) { Text(if (map) "Danh sách" else "Bản đồ") }
                FilterChip(selected = allLocations, onClick = { allLocations = !allLocations }, label = { Text("Tất cả địa điểm") })
            }
            val visible = locations?.filter { (allLocations || it.categoryId == SharedTourRepository.TOUR_CATEGORY) && it.name.contains(keyword, true) }.orEmpty()
            if (catalogError.isNotBlank()) item { Text(catalogError); OutlinedButton(onClick = { revision++ }) { Text("Thử lại") } }
            else if (locations == null) item { LinearProgressIndicator(Modifier.fillMaxWidth()); Text("Đang tải địa điểm…") }
            else if (visible.isEmpty()) item { Text("Chưa tìm thấy địa điểm. Hãy thử từ khóa khác.") }
            if (map) item {
                val districtIds = setOf("101", "102", "103", "107").map { "20000000-0000-0000-0000-000000000$it" }.toSet()
                val points = visible.filter { it.id in districtIds }.map { DemoLocation(it.id, it.name, "", it.category, 0, "", "", it.latitude, it.longitude) }
                Text("Chỉ ghim các điểm thuộc tuyến Quận 1. Khung giới hạn là vùng demo hình chữ nhật, chưa phải đường biên hành chính.")
                MapPreview(points, onMarker = { selected = it }, onShowList = { map = false }, districtOneOnly = true)
            }
            items(visible, key = { it.id }) { location ->
                OutlinedButton(onClick = { selected = location.id }, modifier = Modifier.fillMaxWidth()) { Text(location.name) }
            }
        } else {
            item { TextButton(onClick = { selected = null }) { Text("← Khám phá địa điểm khác") }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SharedTourRepository.languages.forEach { code -> FilterChip(selected = language == code, onClick = { language = code }, label = { Text(code) }) }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("FULL", "SHORT").forEach { type ->
                    FilterChip(selected = mode == type, onClick = { mode = type }, label = { Text(if (type == "FULL") "Đầy đủ" else "Ngắn gọn") })
                } }
            }
            if (guideError.isNotBlank()) item { Text(guideError, Modifier.semantics { liveRegion = LiveRegionMode.Polite }); OutlinedButton(onClick = { revision++ }) { Text("Thử lại") } }
            else if (guide == null) item { LinearProgressIndicator(Modifier.fillMaxWidth()); Text("Đang tải thuyết minh…") }
            else item { val content = guide!!; key(base, content.contentId) {
                Text(content.locationName, style = MaterialTheme.typography.headlineSmall)
                Text("${content.languageCode} · ${content.scriptType}")
                if (content.languageCode != language) Text("Chưa có bản $language. Đang dùng ${content.languageCode}, giữ nguyên $mode.")
                SharedNarration(content, base)
                Text(content.scriptText)
            } }
        }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SharedNarration(guide: ServerGuide, base: String) {
    val context = LocalContext.current.applicationContext; val lifecycle = LocalLifecycleOwner.current.lifecycle
    val scope = rememberCoroutineScope(); var receipt by remember { mutableStateOf("") }
    val narrator = remember(guide.contentId, base, lifecycle) { SharedNarrator(context, lifecycle, guide) { event ->
        scope.launch { runCatching { PlaybackOutbox.enqueueAndFlush(context, base, event, guide.contentId) }
            .onFailure { receipt = "Lượt nghe đã lưu, sẽ đồng bộ khi có mạng." } }
    } }
    DisposableEffect(narrator) { onDispose { narrator.close() } }
    LaunchedEffect(narrator) { while (true) { delay(300); narrator.tick() } }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(narrator.state + if (narrator.total > 0) if (narrator.ttsSource) " · TTS" else " · Audio thu sẵn" else "",
            Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { if (narrator.state in listOf("Đang phát", "Đang chuẩn bị…")) narrator.pause() else narrator.play() }) {
                Text(if (narrator.state in listOf("Đang phát", "Đang chuẩn bị…")) "Tạm dừng" else if (narrator.state == "Tạm dừng") "Tiếp tục" else "Nghe thuyết minh") }
            OutlinedButton(onClick = narrator::stop) { Text("Dừng") }
            listOf(0.75f, 1f, 1.25f, 1.5f).forEach { rate -> FilterChip(selected = narrator.rate == rate,
                onClick = { narrator.speed(rate) }, label = { Text("${NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN")).format(rate)}×") }) }
        }
        if (narrator.total > 0) {
            val max = if (narrator.ttsSource) (narrator.total - 1).coerceAtLeast(1) else narrator.total.coerceAtLeast(1)
            Text(if (narrator.ttsSource) "Đoạn ${narrator.position + 1}/${narrator.total} · ${narrator.voiceName}" else "${narrator.position}/${narrator.total} giây")
            Slider(value = narrator.position.toFloat().coerceIn(0f, max.toFloat()), valueRange = 0f..max.toFloat(), onValueChange = { narrator.seek(it.toInt()) })
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { narrator.seek(narrator.position - if (narrator.ttsSource) 1 else 10) }) { Text("Lùi") }
                OutlinedButton(onClick = { narrator.seek(narrator.position + if (narrator.ttsSource) 1 else 10) }) { Text("Tiến") }
            }
            if (narrator.ttsSource) Text("Vị trí theo câu; pause/resume có thể lặp lại từ gần từ cuối máy báo.", style = MaterialTheme.typography.bodySmall)
        }
        if (receipt.isNotBlank()) Text(receipt)
    }
}

@Composable
private fun SharedOfflineContent(repository: SharedTourRepository) {
    val scope = rememberCoroutineScope()
    var installed by remember(repository) { mutableStateOf<List<JSONObject>>(emptyList()) }
    var available by remember(repository) { mutableStateOf<List<JSONObject>>(emptyList()) }
    var message by remember { mutableStateOf("Đang xác minh gói đã cài…") }
    var busy by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf<JSONObject?>(null) }
    var deleting by remember { mutableStateOf(false) }
    var job by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    LaunchedEffect(repository) { runCatching { repository.installed() }.onSuccess { installed = it; message = if (it.isEmpty()) "Chưa có gói đã xác minh." else "Gói cơ bản đã xác minh, có thể đọc offline." }.onFailure { message = "Chưa xác minh được gói. Hãy tải lại khi có mạng." } }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(message, Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        OutlinedButton(enabled = !busy, onClick = { scope.launch {
            busy = true
            try { available = repository.available(); message = "Đã kiểm tra phiên bản; chỉ tải khi bạn xác nhận." }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { message = "Chưa kiểm tra được. Nội dung đã cài vẫn được giữ." }
            finally { busy = false }
        } }) { Text("Kiểm tra cập nhật") }
        available.forEach { manifest ->
            val current = installed.firstOrNull { it.getJSONObject("manifest").getString("scope") == manifest.getString("scope") }
            val same = current?.getJSONObject("manifest")?.getString("version") == manifest.getString("version")
            Text(if (manifest.getBoolean("isBase")) "Gói cơ bản" else "Gói bổ sung · bộ cũ có audio")
            Text(manifest.getString("version"), style = MaterialTheme.typography.bodySmall)
            Text(if (same) "Đã mới nhất" else "Có nội dung chưa cài hoặc bản cập nhật")
            if (!same) Button(enabled = !busy, onClick = { confirm = manifest }) { Text("Tải gói") }
        }
        installed.forEach { record ->
            val manifest = record.getJSONObject("manifest")
            Text(if (manifest.getBoolean("isBase")) "Gói cơ bản · được bảo vệ" else "Gói bổ sung đã cài")
            if (!manifest.getBoolean("isBase")) OutlinedButton(enabled = !busy, onClick = { deleting = true }) { Text("Xóa nội dung bổ sung") }
        }
        Text("Bản đồ nền cần mạng. Nghe offline cần audio đã tải hoặc giọng offline cùng ngôn ngữ.")
        if (busy && job != null) OutlinedButton(onClick = { job?.cancel() }) { Text("Hủy tải") }
    }
    confirm?.let { manifest -> AlertDialog(onDismissRequest = { confirm = null }, title = { Text("Tải gói nội dung?") },
        text = { Text("Kích thước text: ${manifest.getLong("sizeBytes")} byte; audio tải kèm theo manifest. Phiên bản cũ chỉ thay thế sau xác minh.") },
        confirmButton = { TextButton(onClick = { confirm = null; busy = true; job = scope.launch {
            try { repository.download(manifest) { message = it }; installed = repository.installed(); message = "Đã tải và xác minh gói." }
            catch (cancelled: CancellationException) { message = "Đã hủy; phiên bản cũ giữ nguyên."; throw cancelled }
            catch (error: Exception) { message = error.message ?: "Không tải được gói" }
            finally { busy = false; job = null }
        } }) { Text("Xác nhận tải") } }, dismissButton = { TextButton(onClick = { confirm = null }) { Text("Hủy") } }) }
    if (deleting) AlertDialog(onDismissRequest = { deleting = false }, title = { Text("Xóa gói bổ sung?") }, text = { Text("Gói cơ bản không bị xóa.") },
        confirmButton = { TextButton(onClick = { deleting = false; scope.launch { repository.deleteOptional(); installed = repository.installed() } }) { Text("Xóa") } },
        dismissButton = { TextButton(onClick = { deleting = false }) { Text("Hủy") } })
}
