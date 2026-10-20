package vn.hvp.travelvoice

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Real main app + server/bundled signed baseline. No simulated fixture UI route. */
@RunWith(AndroidJUnit4::class)
class SharedTourFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Test fun sixLanguagesTextSelectionAndProtectedOfflineBase() {
        compose.waitUntil(20_000) { compose.onAllNodes(hasText("Dinh Độc Lập")).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Nội dung từ máy chủ chung với Web Admin").assertExists()
        listOf("VI", "EN", "FR", "JA", "ZH", "RU").forEach { compose.onNodeWithText(it).assertExists() }
        compose.onNodeWithText("Tìm địa điểm").performScrollTo().performTextInput("Dinh")
        compose.onNodeWithText("Dinh Độc Lập").performScrollTo().performClick()
        compose.waitUntil(20_000) { compose.onAllNodes(hasText("VI · FULL")).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Bài từ máy chủ chung với Web").assertExists()
        compose.onNodeWithText("Nghe thuyết minh").performScrollTo().assertExists()
        compose.onNodeWithText("FR").performScrollTo().performClick()
        compose.waitUntil(20_000) { compose.onAllNodes(hasText("FR · FULL")).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Ngắn gọn").performScrollTo().performClick()
        compose.waitUntil(20_000) { compose.onAllNodes(hasText("FR · SHORT")).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Ngoại tuyến").performClick()
        compose.waitUntil(10_000) { compose.onAllNodes(hasText("Gói cơ bản · được bảo vệ")).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Xóa nội dung bổ sung").assertDoesNotExist()
    }
}
