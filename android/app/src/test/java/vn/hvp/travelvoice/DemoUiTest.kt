package vn.hvp.travelvoice

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.semantics.SemanticsActions
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Host-side interaction/render checks; these do not replace device/TalkBack checks. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], qualifiers = "w390dp-h844dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DemoUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun exploreDetailPlayerBack() {
        capture("explore")
        scrollListTo("Đền Ngọc Sơn")
        compose.onNodeWithText("Đền Ngọc Sơn").performScrollTo().performClick()
        compose.onNodeWithText("English").performScrollTo().performClick()
        compose.onNodeWithText("Tóm tắt").performScrollTo().performClick()
        capture("detail")
        compose.onNodeWithText("Nghe thuyết minh").performScrollTo().performClick()
        compose.onNodeWithText("Luồng nghe").assertExists()
        compose.onNodeWithText("English · Tóm tắt").assertExists()
        capture("player")
        compose.onNodeWithContentDescription("Quay lại").performClick()
        compose.onNodeWithText("Tóm tắt").assertIsSelected()
    }

    @Test fun deniedGpsAllowsManualSearch() {
        compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.ScrollToIndex))
            .performScrollToNode(hasText("Mô phỏng giao diện"))
        compose.onNodeWithText("GPS chưa cấp quyền").performScrollTo().performClick()
        compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.ScrollToIndex))
            .performScrollToNode(hasText("Tên địa điểm hoặc tên đường"))
        compose.onNode(hasSetTextAction()).performScrollTo().performTextInput("Ngọc Sơn")
        scrollListTo("Đền Ngọc Sơn")
        compose.onNodeWithText("Đền Ngọc Sơn").performScrollTo().performClick()
        compose.onNodeWithText("Nghe thuyết minh").assertExists()
    }

    @Test fun offlineCancelledUpdateAndProtectedBase() {
        compose.onNodeWithText("Ngoại tuyến").performClick()
        capture("offline")
        compose.onNodeWithText("Kiểm tra cập nhật").performScrollTo().performClick()
        compose.onNodeWithText("Cập nhật nội dung").performScrollTo().performClick()
        compose.onNodeWithText("Hủy", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Đã cài phiên bản 1.0 · 24 MB").assertExists()
        compose.onNodeWithText("Xóa nội dung bổ sung").performScrollTo().performClick()
        compose.onAllNodesWithText("Xóa nội dung bổ sung").onLast().performClick()
        scrollListTo("Nội dung cơ bản")
        compose.onNodeWithText("Nội dung cơ bản").assertIsDisplayed()
        compose.onNodeWithText("Chưa cài · không ảnh hưởng nội dung cơ bản").assertExists()
    }

    @Test @Config(qualifiers = "w320dp-h720dp-xhdpi")
    fun mapMarkerOpensMatchingDetailOnNarrowWindow() {
        compose.onNodeWithText("Bản đồ").performClick()
        scrollListTo("Bản đồ minh họa · dữ liệu ngoại tuyến")
        capture("map-320")
        compose.onNodeWithContentDescription("Xem Đền Ngọc Sơn").performClick()
        compose.onNodeWithText("Chọn địa điểm này").performClick()
        compose.onNodeWithText("Đền Ngọc Sơn").assertExists()
        compose.onNodeWithText("Thuyết minh địa điểm").assertExists()
    }

    @Test fun bannerOpensDetailWithoutAutoplay() {
        scrollListTo("Mô phỏng giao diện")
        compose.onNodeWithText("Banner").performScrollTo().performClick()
        scrollListTo("Xem địa điểm")
        compose.onNodeWithText("Xem địa điểm").performClick()
        compose.onNodeWithText("Nghe thuyết minh").assertIsEnabled()
        compose.onNodeWithText("Luồng nghe").assertDoesNotExist()
    }

    private fun scrollListTo(text: String) {
        compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.ScrollToIndex))
            .performScrollToNode(hasText(text))
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        val file = File(System.getProperty("demo.captureDir", "build/outputs/ui-demo"), "$name.png")
        file.parentFile?.mkdirs()
        // Host view rendering avoids PixelCopy's window-redraw timeout on Windows/API 28.
        compose.runOnUiThread {
            val view = compose.activity.window.decorView
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
    }
}
