package vn.hvp.travelvoice

import android.graphics.PointF
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.maps.MapView
import java.util.concurrent.atomic.AtomicReference

/** Exercise native map selection when tiles load, otherwise verify the real network-error fallback. */
@RunWith(AndroidJUnit4::class)
class MapFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun mapLoadOrFailureKeepsLocationSelectionAvailable() {
        compose.onNodeWithText("Bản đồ").performClick()
        compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.ScrollToIndex))
            .performScrollToNode(hasText("Bản đồ trực tuyến · địa điểm mẫu"))
        compose.waitUntil(60_000) {
            compose.onAllNodesWithText("Bản đồ đã sẵn sàng").fetchSemanticsNodes().isNotEmpty() ||
                compose.onAllNodesWithText("Chưa thể tải bản đồ").fetchSemanticsNodes().isNotEmpty()
        }
        if (compose.onAllNodesWithText("Chưa thể tải bản đồ").fetchSemanticsNodes().isNotEmpty()) {
            android.util.Log.i("MapFlowTest", "OSM tiles unavailable: verifying list fallback; native online branch NOT verified")
            compose.onNodeWithText("Về danh sách").performScrollTo().performClick()
            compose.onNodeWithText("Tên địa điểm hoặc tên đường").performScrollTo().performTextInput("Ngọc Sơn")
            compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.ScrollToIndex))
                .performScrollToNode(hasText("Đền Ngọc Sơn"))
            compose.onNodeWithText("Đền Ngọc Sơn").performClick()
            assertDetailWithoutAutoplay()
            return
        }
        val beforePan = cameraValues()
        compose.onNodeWithTag("location-map").performTouchInput { swipeLeft(durationMillis = 500) }
        compose.waitUntil(5_000) { kotlin.math.abs(cameraValues().second - beforePan.second) > 0.00001 }
        val beforeZoom = cameraValues().third
        compose.onNodeWithText("Phóng to").performScrollTo().performClick()
        compose.waitUntil(5_000) { cameraValues().third > beforeZoom + 0.1 }
        compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.ScrollToIndex))
            .performScrollToNode(hasText("Bản đồ trực tuyến · địa điểm mẫu"))
        // Separate nearby fixture pins before touching the actual Ngoc Son marker.
        compose.runOnUiThread {
            requireNotNull(findMapView(compose.activity.window.decorView)).getMapAsync { map ->
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(21.0308, 105.8525), 16.0))
            }
        }
        val markerPosition = AtomicReference<PointF?>()
        compose.runOnUiThread {
            val mapView = requireNotNull(findMapView(compose.activity.window.decorView))
            mapView.getMapAsync { map ->
                val position = map.projection.toScreenLocation(LatLng(21.0308, 105.8525))
                // The SDK marker's anchor is its bottom tip; touch the icon body.
                markerPosition.set(PointF(position.x, position.y - 14 * mapView.resources.displayMetrics.density))
            }
        }
        compose.waitUntil(5_000) { markerPosition.get() != null }
        val point = requireNotNull(markerPosition.get())
        compose.onNodeWithTag("location-map").performTouchInput { click(Offset(point.x, point.y)) }
        compose.onNodeWithText("Chọn địa điểm này").performScrollTo().performClick()
        android.util.Log.i("MapFlowTest", "Native online pan/zoom/marker branch verified")
        assertDetailWithoutAutoplay()
    }

    private fun assertDetailWithoutAutoplay() {
        compose.onNodeWithText("Đền Ngọc Sơn").assertExists()
        compose.onNodeWithText("Thuyết minh địa điểm").assertExists()
        compose.onNodeWithText("Luồng nghe").assertDoesNotExist()
    }

    private fun cameraValues(): Triple<Double, Double, Double> {
        val result = AtomicReference<Triple<Double, Double, Double>>()
        compose.runOnUiThread {
            requireNotNull(findMapView(compose.activity.window.decorView)).getMapAsync { map ->
                val camera = map.cameraPosition
                val target = requireNotNull(camera.target)
                result.set(Triple(target.latitude, target.longitude, camera.zoom))
            }
        }
        return requireNotNull(result.get())
    }

    private fun findMapView(view: View): MapView? {
        if (view is MapView) return view
        if (view is ViewGroup) for (index in 0 until view.childCount) {
            findMapView(view.getChildAt(index))?.let { return it }
        }
        return null
    }
}
