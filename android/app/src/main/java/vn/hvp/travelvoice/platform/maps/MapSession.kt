package vn.hvp.travelvoice.platform.maps

import android.content.Context
import android.view.MotionEvent
import androidx.core.view.doOnLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMapOptions
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import vn.hvp.travelvoice.domain.model.MapCamera
import vn.hvp.travelvoice.domain.model.MapPoint

enum class MapLoadState { Loading, Ready, Error }

/** Own the Google MapView lifecycle; marker identity remains the shared backend locationId. */
class MapSession(
    context: Context, private val lifecycle: Lifecycle, initialCamera: MapCamera, restoreCamera: Boolean,
    private val onMarker: (String) -> Unit, private val onCamera: (MapCamera) -> Unit,
    private val onState: (MapLoadState) -> Unit,
    private val districtOneOnly: Boolean = false,
) {
    val view: MapView
    private var map: GoogleMap? = null
    private var points = emptyList<MapPoint>()
    private var closed = false
    private var fitNext = !restoreCamera
    private val observer: LifecycleEventObserver

    init {
        MapsInitializer.initialize(context, MapsInitializer.Renderer.LATEST) { }
        val options = GoogleMapOptions().camera(CameraPosition.builder()
            .target(LatLng(initialCamera.latitude, initialCamera.longitude)).zoom(initialCamera.zoom.toFloat()).build())
            .rotateGesturesEnabled(false).tiltGesturesEnabled(false).mapToolbarEnabled(false)
        view = object : MapView(context, options) {
            override fun dispatchTouchEvent(event: MotionEvent): Boolean {
                parent?.requestDisallowInterceptTouchEvent(event.actionMasked != MotionEvent.ACTION_UP && event.actionMasked != MotionEvent.ACTION_CANCEL)
                return super.dispatchTouchEvent(event)
            }
        }
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
        view.getMapAsync { readyMap ->
            if (closed) return@getMapAsync
            map = readyMap
            readyMap.setMaxZoomPreference(19f)
            readyMap.uiSettings.isMapToolbarEnabled = false
            readyMap.uiSettings.isZoomControlsEnabled = false
            if (districtOneOnly) {
                readyMap.setMinZoomPreference(14f)
                readyMap.setLatLngBoundsForCameraTarget(LatLngBounds(LatLng(10.757, 106.685), LatLng(10.800, 106.714)))
            }
            readyMap.setOnMarkerClickListener { marker -> (marker.tag as? String)?.let(onMarker); true }
            readyMap.setOnCameraIdleListener {
                if (!closed) readyMap.cameraPosition.let { onCamera(MapCamera(it.target.latitude, it.target.longitude, it.zoom.toDouble())) }
            }
            readyMap.setOnMapLoadedCallback { if (!closed) onState(MapLoadState.Ready) }
            renderPoints()
        }
    }

    fun setPoints(updated: List<MapPoint>) {
        if (points == updated || closed) return
        if (points.isNotEmpty()) fitNext = true
        points = updated
        renderPoints()
    }

    private fun renderPoints() {
        val current = map ?: return
        if (closed) return
        current.clear()
        points.forEach { point -> current.addMarker(MarkerOptions().position(LatLng(point.latitude, point.longitude)).title(point.name))?.tag = point.id }
        if (fitNext) { fitToPoints(); fitNext = false }
    }

    fun zoomBy(delta: Double) {
        if (!closed) map?.animateCamera(CameraUpdateFactory.zoomBy(delta.toFloat()))
    }

    fun fitToPoints() {
        val current = map ?: return
        if (closed || points.isEmpty()) return
        view.doOnLayout {
            if (closed) return@doOnLayout
            if (districtOneOnly) current.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(10.777, 106.701), 15f))
            else if (points.size == 1) current.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(points.first().latitude, points.first().longitude), 15f))
            else current.moveCamera(CameraUpdateFactory.newLatLngBounds(LatLngBounds.builder()
                .apply { points.forEach { include(LatLng(it.latitude, it.longitude)) } }.build(), (40 * view.resources.displayMetrics.density).toInt()))
        }
    }

    fun close() {
        if (closed) return
        closed = true
        lifecycle.removeObserver(observer)
        map?.setOnMapLoadedCallback(null); map?.setOnCameraIdleListener(null); map?.setOnMarkerClickListener(null)
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) view.onPause()
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) view.onStop()
        view.onDestroy(); map = null
    }
}
