package vn.hvp.travelvoice

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Exercise the actual navigation shell and its visible UI, without services or data stores. */
@RunWith(AndroidJUnit4::class)
class DemoFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun selectedLanguageAndShortModeReachPlayerAndSurviveBack() {
        openNgocSon()
        compose.onNodeWithText("Tóm tắt").performScrollTo().performClick()
        compose.onNodeWithText("English").performScrollTo().performClick()
        compose.onNodeWithText("Nghe thuyết minh").performScrollTo().performClick()
        compose.onNodeWithText("Luồng nghe").assertExists()
        compose.onNodeWithText("English · Tóm tắt").assertExists()
        compose.onNodeWithContentDescription("Quay lại").performClick()
        compose.onNodeWithText("Tóm tắt").assertIsSelected()
        compose.onNodeWithText("English").assertIsSelected()
        compose.onNodeWithText("Luồng nghe").assertDoesNotExist()
    }

    @Test fun locationBannerOpensDetailWithoutAutoplay() {
        exploreScrollTo("Mô phỏng giao diện")
        compose.onNodeWithText("Banner").performScrollTo().performClick()
        exploreScrollTo("Xem địa điểm")
        compose.onNodeWithText("Xem địa điểm").performClick()
        compose.onNodeWithText("Thuyết minh địa điểm").assertExists()
        compose.onNodeWithText("Nghe thuyết minh").assertIsEnabled()
        compose.onNodeWithText("Luồng nghe").assertDoesNotExist()
        compose.onNodeWithText("Đang phát").assertDoesNotExist()
    }

    @Test fun cancelUpdateThenDeleteOptionalPackageKeepsBundledContent() {
        compose.onNodeWithText("Ngoại tuyến").performClick()
        compose.onNodeWithText("Kiểm tra cập nhật").performScrollTo().performClick()
        compose.onNodeWithText("Cập nhật nội dung").performScrollTo().performClick()
        compose.onNodeWithText("Cập nhật nội dung?").assertExists()
        compose.onNodeWithText("Hủy").performClick()
        compose.onNodeWithText("Đã cài phiên bản 1.0 · 24 MB").assertExists()
        compose.onNodeWithText("Xóa nội dung bổ sung").performScrollTo().performClick()
        compose.onNodeWithText("Xóa nội dung bổ sung?").assertExists()
        compose.onAllNodesWithText("Xóa nội dung bổ sung").onLast().performClick()
        compose.onNodeWithText("Chưa cài · không ảnh hưởng nội dung cơ bản").assertExists()
        exploreScrollTo("Nội dung cơ bản")
        compose.onNodeWithText("Đã sẵn sàng ngoại tuyến").assertIsDisplayed()
        compose.onNodeWithText("Nội dung cơ bản").assertIsDisplayed()
    }

    @Test fun deniedGpsStillAllowsManualSearchOutsideNearbyRadius() {
        exploreScrollTo("Mô phỏng giao diện")
        compose.onNodeWithText("GPS chưa cấp quyền").performScrollTo().performClick()
        exploreScrollTo("Tên địa điểm hoặc tên đường")
        compose.onNodeWithText("Tên địa điểm hoặc tên đường").performTextInput("Hoàng thành")
        exploreScrollTo("Hoàng thành Thăng Long")
        compose.onNodeWithText("Hoàng thành Thăng Long").performClick()
        compose.onNodeWithText("Thuyết minh địa điểm").assertExists()
        compose.onNodeWithText("Nghe thuyết minh").assertIsEnabled()
    }

    @Test fun audioFallbackAndInterruptionKeepPlaybackPosition() {
        openNgocSon()
        compose.onNodeWithText("Tóm tắt").performScrollTo().performClick()
        compose.onNodeWithText("Nghe thuyết minh").performScrollTo().performClick()
        compose.onNodeWithText("Tạm dừng").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Vị trí nghe").performSemanticsAction(SemanticsActions.SetProgress) { it(20f) }
        val position = playbackPosition()
        playerScrollToBottom()
        compose.onNodeWithText("Audio lỗi → TTS").performScrollTo().performClick()
        compose.onNodeWithText("Giọng đọc TTS · mô phỏng").assertExists()
        compose.onNodeWithText("Cuộc gọi đến").performScrollTo().performClick()
        compose.onNodeWithText("Đã tạm dừng").assertExists()
        assertEquals(position, playbackPosition(), 0.01f)
        compose.onNodeWithText("Phát tiếp").performScrollTo().performClick()
        playerScrollToBottom()
        compose.onNodeWithText("Ngắt tai nghe").performScrollTo().performClick()
        compose.onNodeWithText("Đã tạm dừng").assertExists()
        val pausedAt = playbackPosition()
        compose.mainClock.advanceTimeBy(3000)
        compose.waitForIdle()
        assertEquals(pausedAt, playbackPosition(), 0.01f)
    }

    private fun openNgocSon() {
        compose.onNodeWithText("Tên địa điểm hoặc tên đường").performTextInput("Ngọc Sơn")
        exploreScrollTo("Đền Ngọc Sơn")
        compose.onNodeWithText("Đền Ngọc Sơn").performClick()
        compose.onNodeWithText("Thuyết minh địa điểm").assertExists()
    }

    private fun exploreScrollTo(text: String) {
        compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.ScrollToIndex))
            .performScrollToNode(hasText(text))
    }

    private fun playerScrollToBottom() {
        compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange))
            .performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, 10000f) }
    }

    private fun playbackPosition(): Float = compose.onNodeWithContentDescription("Vị trí nghe")
        .fetchSemanticsNode().config[SemanticsProperties.ProgressBarRangeInfo].current
}
