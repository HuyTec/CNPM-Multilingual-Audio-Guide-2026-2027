package vn.hvp.travelvoice

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import vn.hvp.travelvoice.ui.navigation.TravelQ1App
import vn.hvp.travelvoice.ui.theme.TravelVoiceTheme

/** Injects only the native map surface; catalog still comes from the real bundled signed baseline. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], qualifiers = "w390dp-h844dp-xhdpi")
class TravelQ1UxTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Before fun openMap() {
        compose.activity.setContent {
            TravelVoiceTheme { TravelQ1App(mapContent = { places, select, _ ->
                Column(Modifier.padding(top = 220.dp)) { places.forEach { place -> TextButton(onClick = { select(place.id) }, modifier = Modifier.testTag("pin-${place.id}")) { Text(place.name) } } }
            }) }
        }
    }
    @Test fun permissionExplanationIsOnlyOpenedByNearbyAction() {
        compose.onNodeWithText("Tìm những nơi gần bạn").assertDoesNotExist()
        compose.onNodeWithText("Gần tôi").performClick()
        compose.onNodeWithText("Tìm những nơi gần bạn").assertExists()
        compose.onNodeWithText("Để sau").performClick()
        compose.onNodeWithText("Tìm những nơi gần bạn").assertDoesNotExist()
        compose.onNodeWithContentDescription("Trang chủ").assertExists()
        compose.onNodeWithContentDescription("Về vị trí hiện tại của tôi").assertIsDisplayed().performClick()
        compose.onNodeWithText("Tìm những nơi gần bạn").assertExists()
        compose.onNodeWithText("Để sau").performClick()
    }
    @Test fun homeClearsPlaceSelectionAndLeavesTheMapAvailable() {
        val tag = "pin-20000000-0000-0000-0000-000000000101"
        compose.waitUntil(15_000) { compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag(tag).performClick()
        compose.onNodeWithText("Đọc giới thiệu").assertExists()
        compose.onNodeWithContentDescription("Trang chủ").performClick()
        compose.onNodeWithText("Đọc giới thiệu").assertDoesNotExist()
        compose.onNodeWithTag(tag).assertExists()
    }
    @Test fun offlineIsASettingsDestinationRatherThanABottomTab() {
        compose.onNodeWithText("Ngoại tuyến").assertDoesNotExist()
        compose.onNodeWithText("Cài đặt").performClick()
        compose.onNodeWithText("Ngôn ngữ thuyết minh").assertExists()
        compose.onNodeWithText("Ngoại tuyến").performScrollTo().performClick()
        compose.onNodeWithText("Kiểm tra cập nhật").performScrollTo().assertExists()
        compose.onNodeWithText("Kết nối máy chủ").assertDoesNotExist()
    }
}
