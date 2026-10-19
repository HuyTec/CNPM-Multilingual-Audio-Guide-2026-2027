package vn.hvp.travelvoice.platform.maps

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import androidx.core.view.doOnLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import okhttp3.OkHttpClient
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapLibreMapOptions
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.module.http.HttpRequestUtil
import vn.hvp.travelvoice.domain.model.MapCamera
import vn.hvp.travelvoice.domain.model.MapPoint
import java.io.IOException
import java.util.concurrent.CopyOnWriteArraySet

enum class MapLoadState { Loading, Ready, Error }

private object MapRuntime {
    private var initialized = false
    private val tileFailures = CopyOnWriteArraySet<() -> Unit>()
    private val mainHandler = Handler(Looper.getMainLooper())

    fun observeTileFailures(listener: () -> Unit) { tileFailures.add(listener) }
    fun removeTileFailures(listener: () -> Unit) { tileFailures.remove(listener) }
    private fun reportTileFailure() { mainHandler.post { tileFailures.forEach { it() } } }

    @Synchronized
    fun initialize(context: Context) {
        if (initialized) return
        MapLibre.getInstance(context.applicationContext)
        // The SDK's native resource cache honours HTTP cache headers.
        HttpRequestUtil.setOkHttpClient(OkHttpClient.Builder().addInterceptor { chain ->
            val request = chain.request().newBuilder().header("User-Agent",
                "TravelVoiceDemo/0.1 (+https://github.com/HuyTec/CNPM-Multilingual-Audio-Guide-2026-2027)").build()
            try {
                chain.proceed(request).also { response ->
                    if (request.url.host == "tile.openstreetmap.org" && !response.isSuccessful) reportTileFailure()
                }
            } catch (error: IOException) {
                if (request.url.host == "tile.openstreetmap.org" && !chain.call().isCanceled()) reportTileFailure()
                throw error
            }
        }.build())
        initialized = true
    }
}

/** Own one native map and pair its lifecycle with the hosting composition. Main thread only. */
@Suppress("DEPRECATION") // Small demo catalog; SDK marker annotations preserve location identity.
class MapSession(
    context: Context, private val lifecycle: Lifecycle, initialCamera: MapCamera, restoreCamera: Boolean,
    private val onMarker: (String) -> Unit, private val onCamera: (MapCamera) -> Unit,
    private val onState: (MapLoadState) -> Unit,
) {
    val view: MapView
    private var map: MapLibreMap? = null
    private var points = emptyList<MapPoint>()
    private val markerIds = mutableMapOf<Long, String>()
    private var styleReady = false
    private var closed = false
    private var fitNext = !restoreCamera
    private val observer: LifecycleEventObserver
    private val tileFailure: () -> Unit = { if (!closed) onState(MapLoadState.Error) }

    init {
        MapRuntime.initialize(context)
        MapRuntime.observeTileFailures(tileFailure)
        view = MapView(context, MapLibreMapOptions.createFromAttributes(context).setPrefetchZoomDelta(0))
        view.onCreate(null)
        observer = LifecycleEventObserver { _, event ->
            if (!closed) when (event) {
                Lifecycle.Event.ON_START -> view.onStart()
                Lifecycle.Event.ON_RESUME -> view.onResume()
                Lifecycle.Event.ON_PAUSE -> view.onPause()
                Lifecycle.Event.ON_STOP -> view.onStop()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        allowMapGesturesInsideScrollingPage()
        view.addOnDidFailLoadingMapListener { if (!closed) onState(MapLoadState.Error) }
        view.addOnDidFinishRenderingMapListener { fully ->
            if (!closed && fully && styleReady) onState(MapLoadState.Ready)
        }
        view.getMapAsync { readyMap ->
            if (closed) return@getMapAsync
            map = readyMap
            readyMap.setPrefetchZoomDelta(0)
            readyMap.setMaxZoomPreference(19.0)
            readyMap.uiSettings.isRotateGesturesEnabled = false
            readyMap.uiSettings.isTiltGesturesEnabled = false
            readyMap.cameraPosition = CameraPosition.Builder()
                .target(LatLng(initialCamera.latitude, initialCamera.longitude)).zoom(initialCamera.zoom).build()
            readyMap.setOnMarkerClickListener { marker -> markerIds[marker.id]?.let(onMarker); true }
            readyMap.addOnCameraIdleListener {
                if (!closed) readyMap.cameraPosition.let { camera ->
                    camera.target?.let { onCamera(MapCamera(it.latitude, it.longitude, camera.zoom)) }
                }
            }
            val json = context.assets.open("maps/osm.json").bufferedReader().use { it.readText() }
            readyMap.setStyle(Style.Builder().fromJson(json)) {
                if (!closed) { styleReady = true; renderPoints() }
            }
        }
    }

    fun setPoints(updated: List<MapPoint>) {
        if (points == updated || closed) return
        if (points.isNotEmpty()) fitNext = true
        points = updated
        renderPoints()
    }

    private fun renderPoints() {
        val currentMap = map ?: return
        if (!styleReady || closed) return
        currentMap.clear()
        markerIds.clear()
        val markers = currentMap.addMarkers(points.map { point ->
            MarkerOptions().position(LatLng(point.latitude, point.longitude)).title(point.name)
        })
        markers.zip(points).forEach { (marker, point) -> markerIds[marker.id] = point.id }
        if (fitNext) { fitToPoints(); fitNext = false }
    }

    fun zoomBy(delta: Double) {
        if (!closed) map?.animateCamera(CameraUpdateFactory.zoomBy(delta))
    }

    fun fitToPoints() {
        val currentMap = map ?: return
        if (closed || points.isEmpty()) return
        view.doOnLayout {
            if (closed) return@doOnLayout
            if (points.size == 1) currentMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                LatLng(points.first().latitude, points.first().longitude), 15.0))
            else {
                val bounds = LatLngBounds.Builder().includes(points.map { LatLng(it.latitude, it.longitude) }).build()
                currentMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, (40 * view.resources.displayMetrics.density).toInt()))
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility") // Compose supplies accessible marker selection and zoom controls.
    private fun allowMapGesturesInsideScrollingPage() {
        view.setOnTouchListener { touched, event ->
            touched.parent?.requestDisallowInterceptTouchEvent(
                event.actionMasked != MotionEvent.ACTION_UP && event.actionMasked != MotionEvent.ACTION_CANCEL)
            false
        }
    }

    fun close() {
        if (closed) return
        closed = true
        MapRuntime.removeTileFailures(tileFailure)
        lifecycle.removeObserver(observer)
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) view.onPause()
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) view.onStop()
        view.onDestroy()
        map = null
        markerIds.clear()
    }
}
