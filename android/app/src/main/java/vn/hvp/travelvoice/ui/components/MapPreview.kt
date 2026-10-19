package vn.hvp.travelvoice.ui.components

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AssistChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import vn.hvp.travelvoice.domain.model.MapCamera
import vn.hvp.travelvoice.domain.model.MapPoint
import vn.hvp.travelvoice.domain.model.isValidMapCoordinate
import vn.hvp.travelvoice.platform.maps.MapLoadState
import vn.hvp.travelvoice.platform.maps.MapSession
import vn.hvp.travelvoice.ui.mock.DemoData
import vn.hvp.travelvoice.ui.mock.DemoLocation
import vn.hvp.travelvoice.ui.theme.TravelVoiceTheme

typealias LocationMapContent = @Composable (List<DemoLocation>, (String) -> Unit, () -> Unit) -> Unit

/** Online OSM basemap; fixture markers remain separate from approved backend content. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MapPreview(
    locations: List<DemoLocation>, onMarker: (String) -> Unit, modifier: Modifier = Modifier,
    onShowList: () -> Unit = {},
) {
    val points = remember(locations) {
        locations.mapNotNull { location ->
            val latitude = location.latitude ?: return@mapNotNull null
            val longitude = location.longitude ?: return@mapNotNull null
            if (isValidMapCoordinate(latitude, longitude)) MapPoint(location.id, location.name, latitude, longitude) else null
        }
    }
    val inspection = LocalInspectionMode.current
    val online = if (inspection) false else rememberMapOnline()
    var attempt by remember { mutableIntStateOf(0) }
    var loadState by remember(online, attempt) { mutableStateOf(MapLoadState.Loading) }
    var latitude by rememberSaveable { mutableDoubleStateOf(21.03) }
    var longitude by rememberSaveable { mutableDoubleStateOf(105.85) }
    var zoom by rememberSaveable { mutableDoubleStateOf(13.0) }
    var savedCamera by rememberSaveable { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current
    LaunchedEffect(online, attempt, loadState) {
        if (online && loadState == MapLoadState.Loading) {
            delay(20_000)
            loadState = MapLoadState.Error
        }
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Bản đồ trực tuyến · địa điểm mẫu", style = MaterialTheme.typography.labelLarge)
        when {
            points.isEmpty() -> {
                StateMessage("Chưa có tọa độ địa điểm", "Bạn vẫn có thể khám phá qua danh sách.")
                TextButton(onClick = onShowList) { Text("Về danh sách") }
            }
            inspection -> Text("Bản đồ MapLibre hiển thị khi chạy trên thiết bị hoặc emulator.")
            !online -> {
                StateMessage("Bản đồ cần kết nối mạng", "Dữ liệu danh sách vẫn có thể dùng để chọn địa điểm.")
                TextButton(onClick = onShowList) { Text("Về danh sách") }
            }
            loadState == MapLoadState.Error -> {
                StateMessage("Chưa thể tải bản đồ", "Kiểm tra kết nối rồi thử lại hoặc tiếp tục qua danh sách.", { attempt++ })
                TextButton(onClick = onShowList) { Text("Về danh sách") }
            }
            else -> key(attempt) {
                NativeMapSurface(points, MapCamera(latitude, longitude, zoom), savedCamera,
                    onMarker = onMarker, onState = { if (loadState != MapLoadState.Error) loadState = it }, onCamera = {
                        latitude = it.latitude; longitude = it.longitude; zoom = it.zoom; savedCamera = true
                    })
                if (loadState == MapLoadState.Loading) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text("Đang tải bản đồ…", modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                } else Text("Bản đồ đã sẵn sàng", modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            }
        }
        TextButton(onClick = { uriHandler.openUri("https://www.openstreetmap.org/copyright") }) {
            Text("© OpenStreetMap contributors")
        }
        Text("Chọn địa điểm trên bản đồ hoặc bằng các nút bên dưới.", style = MaterialTheme.typography.bodySmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            points.forEach { point ->
                AssistChip(onClick = { onMarker(point.id) }, label = { Text(point.name) },
                    modifier = Modifier.semantics { contentDescription = "Xem ${point.name}" })
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NativeMapSurface(
    points: List<MapPoint>, camera: MapCamera, restoreCamera: Boolean,
    onMarker: (String) -> Unit, onState: (MapLoadState) -> Unit, onCamera: (MapCamera) -> Unit,
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val uriHandler = LocalUriHandler.current
    val markerCallback by rememberUpdatedState(onMarker)
    val stateCallback by rememberUpdatedState(onState)
    val cameraCallback by rememberUpdatedState(onCamera)
    val session = remember(context, lifecycle) {
        MapSession(context, lifecycle, camera, restoreCamera,
            { markerCallback(it) }, { cameraCallback(it) }, { stateCallback(it) })
    }
    DisposableEffect(session) { onDispose { session.close() } }
    LaunchedEffect(session, points) { session.setPoints(points) }
    Box(Modifier.fillMaxWidth().height(320.dp).testTag("location-map")) {
        AndroidView(factory = { session.view }, modifier = Modifier.fillMaxSize())
        TextButton(
            onClick = { uriHandler.openUri("https://www.openstreetmap.org/copyright") },
            modifier = Modifier.align(Alignment.BottomEnd).background(MaterialTheme.colorScheme.surface),
        ) { Text("© OpenStreetMap contributors", style = MaterialTheme.typography.labelSmall) }
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { session.zoomBy(-1.0) }) { Text("Thu nhỏ") }
        OutlinedButton(onClick = { session.zoomBy(1.0) }) { Text("Phóng to") }
        OutlinedButton(onClick = session::fitToPoints) { Text("Xem tất cả") }
    }
}

/** Remove the Internet-capability observer when the map leaves composition. */
@Composable
private fun rememberMapOnline(): Boolean {
    val context = LocalContext.current.applicationContext
    val connectivity = remember(context) { context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager }
    fun connected(): Boolean = connectivity.getNetworkCapabilities(connectivity.activeNetwork)
        ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
    var online by remember(connectivity) { mutableStateOf(connected()) }
    DisposableEffect(connectivity) {
        val handler = Handler(Looper.getMainLooper())
        var active = true
        val callback = object : ConnectivityManager.NetworkCallback() {
            private fun update() { handler.post { if (active) online = connected() } }
            override fun onAvailable(network: Network) = update()
            override fun onLost(network: Network) = update()
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) = update()
        }
        connectivity.registerDefaultNetworkCallback(callback)
        online = connected()
        onDispose { active = false; connectivity.unregisterNetworkCallback(callback); handler.removeCallbacksAndMessages(null) }
    }
    return online
}

@Preview(showBackground = true)
@Composable
private fun MapPreviewPreview() { TravelVoiceTheme { MapPreview(DemoData.mapLocations, {}) } }
