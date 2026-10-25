package vn.hvp.travelvoice

import org.junit.Assert.*
import org.junit.Test
import vn.hvp.travelvoice.platform.images.publishedImageUrl

class PublishedImageUrlTest {
    private val image = "/api/content-images/10000000-0000-0000-0000-000000000001.png"
    @Test fun photoUsesTheCurrentlyConfiguredBackendOrigin() {
        assertEquals("https://current.example$image", publishedImageUrl("https://current.example/", image))
    }
    @Test fun privateDraftForeignHostAndNonHttpImagesAreRejected() {
        for (path in listOf("https://other.example$image", "/api/admin/content/images/test.png", "file:///data/secret", "$image?token=x")) {
            assertThrows(IllegalArgumentException::class.java) { publishedImageUrl("https://current.example/", path) }
        }
    }
}
