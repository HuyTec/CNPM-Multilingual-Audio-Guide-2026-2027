package vn.hvp.travelvoice.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import vn.hvp.travelvoice.BuildConfig
import vn.hvp.travelvoice.data.remote.DemoApi
import vn.hvp.travelvoice.data.remote.PlaybackOutbox
import vn.hvp.travelvoice.data.remote.ServerGuide
import vn.hvp.travelvoice.data.remote.ServerLocation
import vn.hvp.travelvoice.platform.audio.DemoNarrator
import vn.hvp.travelvoice.ui.components.SectionTitle
import vn.hvp.travelvoice.ui.components.StateMessage
import java.util.Locale

/** The real database/audio demonstration is isolated from the existing UI-only scenarios. */
@Composable
fun LiveDemoScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current.applicationContext
    val preferences = remember(context) { context.getSharedPreferences("demo-server", Context.MODE_PRIVATE) }
    var base by rememberSaveable { mutableStateOf(preferences.getString("base", BuildConfig.DEMO_API_BASE_URL)!!) }
    var address by rememberSaveable { mutableStateOf(base) }
    var addressError by remember { mutableStateOf("") }
    var revision by remember { mutableIntStateOf(0) }
    var locations by remember(base, revision) { mutableStateOf<List<ServerLocation>?>(null) }
    var catalogError by remember(base, revision) { mutableStateOf("") }
    var selected by rememberSaveable(base) { mutableStateOf<String?>(null) }
    var guide by remember(base, selected) { mutableStateOf<ServerGuide?>(null) }
    var guideError by remember(base, selected) { mutableStateOf("") }
    var synchronization by remember(base) { mutableStateOf("Lượt nghe sẽ được gửi khi bắt đầu phát.") }
    var lastEvent by remember(base) { mutableStateOf<Pair<String, String>?>(null) }
    val scope = rememberCoroutineScope()
    val inspection = LocalInspectionMode.current

    suspend fun synchronize(event: Pair<String, String>? = null) {
        synchronization = "Đang đồng bộ lượt nghe…"
        try {
            if (event == null) PlaybackOutbox.flush(context, base)
            else PlaybackOutbox.enqueueAndFlush(context, base, event.first, event.second)
            synchronization = "Đã đồng bộ lượt nghe với server."
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { synchronization = "Chưa đồng bộ được lượt nghe. Sự kiện đã lưu sẽ được gửi lại an toàn; chọn Thử lại đồng bộ." }
    }

    LaunchedEffect(base, revision) {
        if (inspection) return@LaunchedEffect
        try {
            locations = DemoApi.locations(base)
            if (locations?.none { it.id == selected } == true) selected = locations?.firstOrNull()?.id
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { catalogError = "Không thể tải địa điểm. Kiểm tra địa chỉ server và kết nối cùng mạng, rồi thử lại." }
        synchronize()
    }
    LaunchedEffect(base, selected) {
        val id = selected ?: return@LaunchedEffect
        if (inspection) return@LaunchedEffect
        try { guide = DemoApi.guide(base, id) }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (error: Exception) { guideError = error.message ?: "Chưa tải được thuyết minh." }
    }
    val currentGuide = guide
    val narrator = rememberNarrator(currentGuide) { eventId ->
        val event = eventId to requireNotNull(currentGuide).contentId
        lastEvent = event
        scope.launch { synchronize(event) }
    }

    LazyColumn(modifier, contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { SectionTitle("Nghe demo trực tuyến", "Nội dung từ PostgreSQL · audio tiếng Việt thật · lượt nghe gửi về Web Admin.") }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = address, onValueChange = { address = it; addressError = "" },
                    label = { Text("Địa chỉ server demo") }, placeholder = { Text("http://192.168.1.10:8081/…") },
                    singleLine = true, isError = addressError.isNotEmpty(), supportingText = {
                        Text(addressError.ifBlank { "Emulator: 10.0.2.2:8081 · điện thoại: IP Wi-Fi của máy chạy Docker." })
                    }, modifier = Modifier.fillMaxWidth())
                OutlinedButton(onClick = {
                    try {
                        base = DemoApi.normalizeBaseUrl(address)
                        address = base
                        preferences.edit().putString("base", base).apply()
                        revision++
                    } catch (_: Exception) { addressError = "Nhập URL http:// hoặc https:// hợp lệ, ví dụ http://192.168.1.10:8081/." }
                }) { Text("Kết nối server") }
            }
        }
        when {
            catalogError.isNotEmpty() -> item { StateMessage("Chưa kết nối được server", catalogError, { revision++ }) }
            locations == null -> item { LinearProgressIndicator(Modifier.fillMaxWidth()); Text("Đang tải địa điểm từ server…") }
            locations!!.isEmpty() -> item { StateMessage("Chưa có địa điểm đã duyệt", "Kiểm tra profile demo và dữ liệu trên server.", { revision++ }) }
            else -> {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Chọn bài thuyết minh ngắn", style = MaterialTheme.typography.titleMedium)
                        locations!!.forEach { location ->
                            OutlinedButton(onClick = { selected = location.id }, modifier = Modifier.fillMaxWidth()) {
                                Text((if (selected == location.id) "Đang chọn: " else "Chọn: ") + location.name)
                            }
                        }
                    }
                }
                when {
                    guideError.isNotEmpty() -> item { StateMessage("Chưa có thuyết minh", guideError, { revision++; selected = null }) }
                    selected != null && guide == null -> item { LinearProgressIndicator(Modifier.fillMaxWidth()); Text("Đang tải nội dung…") }
                    guide != null -> item {
                        val currentGuide = guide!!
                        key(base, currentGuide.contentId) {
                            NarrationContent(currentGuide, narrator)
                        }
                    }
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(synchronization, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                OutlinedButton(onClick = { scope.launch { synchronize(lastEvent) } }) { Text("Thử lại đồng bộ") }
                Text("Một lượt nghe được tính khi bắt đầu phát. Tạm dừng/tiếp tục không tăng lượt; nghe lại tạo lượt mới. Ba bài là nội dung minh họa, không phải thông tin du lịch đã xác minh.", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun rememberNarrator(guide: ServerGuide?, onStarted: (String) -> Unit): DemoNarrator? {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val started by rememberUpdatedState(onStarted)
    val narrator = remember(guide?.contentId, guide?.audioUrl, context, lifecycle) {
        guide?.audioUrl?.let { DemoNarrator(context, lifecycle, it) { id -> started(id) } }
    }
    DisposableEffect(narrator) { onDispose { narrator?.close() } }
    LaunchedEffect(narrator) {
        while (true) { delay(300); narrator?.updatePosition() }
    }
    return narrator
}

@Composable
private fun NarrationContent(guide: ServerGuide, narrator: DemoNarrator?) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle(guide.locationName, "${guide.languageCode} · thuyết minh ngắn từ server")
        Text(guide.scriptText, style = MaterialTheme.typography.bodyLarge)
        if (narrator == null) StateMessage("Chưa có audio", "Server chưa có audio cho bài này; lượt nghe chưa được ghi nhận.")
        else {
            Text(narrator.state, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }, style = MaterialTheme.typography.titleMedium)
            Text("${audioTime(narrator.positionMs)} / ${audioTime(narrator.durationMs)}")
            if (narrator.state == "Đang tải audio…") LinearProgressIndicator(Modifier.fillMaxWidth())
            Button(onClick = { if (narrator.state == "Đang phát") narrator.pause() else narrator.play() },
                enabled = narrator.state != "Đang tải audio…", modifier = Modifier.fillMaxWidth()) {
                Text(when (narrator.state) { "Đang phát" -> "Tạm dừng audio"; "Tạm dừng" -> "Tiếp tục audio"; "Đã nghe hết" -> "Nghe lại"; else -> "Phát thuyết minh" })
            }
        }
    }
}

private fun audioTime(milliseconds: Int): String = String.format(Locale.forLanguageTag("vi-VN"), "%d:%02d", milliseconds / 60000, milliseconds / 1000 % 60)
