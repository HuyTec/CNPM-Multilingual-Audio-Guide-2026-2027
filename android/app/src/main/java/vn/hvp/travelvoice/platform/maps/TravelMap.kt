package vn.hvp.travelvoice.platform.maps

import android.location.Location
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import vn.hvp.travelvoice.R
import vn.hvp.travelvoice.data.remote.ServerLocation
import vn.hvp.travelvoice.domain.model.MapCamera
import vn.hvp.travelvoice.domain.model.MapPoint
import vn.hvp.travelvoice.domain.model.isValidMapCoordinate

/** A full-canvas native map; the surrounding screen supplies search and the place sheet. */
@Composable
fun TravelMap(locations: List<ServerLocation>, onSelect: (String) -> Unit, onList: () -> Unit, modifier: Modifier = Modifier,
              position: Location? = null, focusRequest: Int = 0) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val select by rememberUpdatedState(onSelect)
    var camera by rememberSaveable(stateSaver = androidx.compose.runtime.saveable.listSaver(
        save = { listOf(it.latitude, it.longitude, it.zoom) }, restore = { MapCamera(it[0], it[1], it[2]) })) { mutableStateOf(MapCamera(10.777, 106.701, 15.0)) }
    var restore by rememberSaveable { mutableStateOf(false) }
    var state by remember { mutableStateOf(MapLoadState.Loading) }
    var attempt by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    val session = remember(context, lifecycle, attempt) { runCatching { MapSession(context, lifecycle, camera, restore,
        { select(it) }, { camera = it; restore = true }, { state = it }) }.getOrNull() }
    DisposableEffect(session) { onDispose { session?.close() } }
    LaunchedEffect(session, locations) {
        session?.setPoints(locations.mapNotNull { location ->
            val lat = location.latitude ?: return@mapNotNull null
            val lng = location.longitude ?: return@mapNotNull null
            if (isValidMapCoordinate(lat, lng)) MapPoint(location.id, location.name, lat, lng) else null
        })
    }
    LaunchedEffect(session, focusRequest) {
        position?.let { session?.showUserLocation(it.latitude, it.longitude, it.accuracy) }
    }
    LaunchedEffect(session) { if (session == null) state = MapLoadState.Error else { state = MapLoadState.Loading; delay(20_000); if (state == MapLoadState.Loading) state = MapLoadState.Error } }
    val description = stringResource(R.string.q1_map_description)
    Box(modifier.semantics { contentDescription = description }) {
        if (session != null) AndroidView(factory = { session.view }, modifier = Modifier.fillMaxSize())
        if (state != MapLoadState.Ready) Column(Modifier.align(Alignment.Center).padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            if (state == MapLoadState.Loading) CircularProgressIndicator()
            Text(stringResource(if (state == MapLoadState.Loading) R.string.q1_map_loading else R.string.q1_map_error), style = MaterialTheme.typography.bodyMedium)
            if (state == MapLoadState.Error) {
                TextButton(onClick = { attempt++ }) { Text(stringResource(R.string.q1_retry)) }
                TextButton(onClick = onList) { Text(stringResource(R.string.q1_list)) }
            }
        }
    }
}
