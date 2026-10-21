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
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import vn.hvp.travelvoice.R
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
    private var userPoint: LatLng? = null
    private var userAccuracy = 0f
    private var userMarker: Marker? = null
    private var accuracyCircle: Circle? = null
    private var centerUserWhenReady = false
    private val userTitle = context.getString(R.string.q1_current_location)
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
        renderUserLocation()
        if (centerUserWhenReady) centerOnUser()
    }

    /** Display a one-shot fix separately from POIs, and retain it across marker refreshes. */
    fun showUserLocation(latitude: Double, longitude: Double, accuracyMeters: Float) {
        if (closed || !latitude.isFinite() || !longitude.isFinite() || latitude !in -90.0..90.0 || longitude !in -180.0..180.0) return
        userPoint = LatLng(latitude, longitude)
        userAccuracy = accuracyMeters.takeIf { it.isFinite() && it > 0 } ?: 0f
        centerUserWhenReady = true
        renderUserLocation(); centerOnUser()
    }

    private fun renderUserLocation() {
        val current = map ?: return
        val point = userPoint ?: return
        userMarker?.remove(); accuracyCircle?.remove()
        userMarker = current.addMarker(MarkerOptions().position(point).title(userTitle)
            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)).zIndex(2f))
        accuracyCircle = current.addCircle(CircleOptions().center(point).radius(userAccuracy.toDouble())
            .strokeWidth(2f).strokeColor(0xFF1A73E8.toInt()).fillColor(0x201A73E8))
    }

    private fun centerOnUser() {
        val current = map ?: return
        val point = userPoint ?: return
        view.doOnLayout {
            if (!closed) {
                current.animateCamera(CameraUpdateFactory.newLatLngZoom(point, 16f))
                centerUserWhenReady = false
            }
        }
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
