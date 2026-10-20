package vn.hvp.travelvoice

import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicReference

/** Real shared tour + Google SDK. A map failure is not counted as a successful tile integration. */
@RunWith(AndroidJUnit4::class)
class MapFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Test fun googleMapRestrictsCameraAndOpensSharedGuide() {
        compose.waitUntil(25_000) { compose.onAllNodesWithText("Dinh Độc Lập").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Bản đồ",useUnmergedTree=true).performScrollTo().performClick()
        compose.onNodeWithText("Google Maps · bản đồ trực tuyến").performScrollTo()
        compose.waitUntil(60_000) { compose.onAllNodesWithText("Bản đồ đã sẵn sàng").fetchSemanticsNodes().isNotEmpty() }
        val camera=AtomicReference<Triple<Double,Double,Float>>()
        compose.runOnUiThread {
            requireNotNull(findMapView(compose.activity.window.decorView)).getMapAsync { map ->
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(21.03,105.85),2f))
                map.setOnCameraIdleListener { val c=map.cameraPosition;camera.set(Triple(c.target.latitude,c.target.longitude,c.zoom)) }
            }
        }
        compose.waitUntil(10_000) { camera.get()!=null }
        val actual=camera.get();assertTrue(actual.first in 10.757-0.0001..10.800+0.0001)
        assertTrue(actual.second in 106.685-0.0001..106.714+0.0001);assertTrue(actual.third>=14f)
        compose.onNodeWithContentDescription("Xem Dinh Độc Lập").performScrollTo().performClick()
        compose.waitUntil(25_000) { compose.onAllNodesWithText("VI · FULL").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Nghe thuyết minh").performScrollTo().assertExists()
        compose.onNodeWithText("Đang phát").assertDoesNotExist()
    }
    private fun findMapView(view:View):MapView? {
        if(view is MapView)return view
        if(view is ViewGroup)for(index in 0 until view.childCount)findMapView(view.getChildAt(index))?.let{return it}
        return null
    }
}
