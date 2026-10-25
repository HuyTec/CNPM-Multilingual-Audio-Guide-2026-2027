package vn.hvp.travelvoice

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import vn.hvp.travelvoice.data.remote.locationImageReference

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = OfflineTestApplication::class)
class PoiImageReferenceTest {
    @Test fun newImageFieldAndLegacyImageUrlBothReachTheAndroidPoi() {
        assertEquals("/api/content-images/new.png", locationImageReference(JSONObject().put("image", "/api/content-images/new.png")))
        assertEquals("/api/content-images/old.png", locationImageReference(JSONObject().put("imageUrl", "/api/content-images/old.png")))
        assertNull(locationImageReference(JSONObject().put("image", JSONObject.NULL)))
    }
}
