package vn.hvp.travelvoice

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import vn.hvp.travelvoice.data.remote.DemoApi
import vn.hvp.travelvoice.data.remote.SharedTourRepository
import vn.hvp.travelvoice.data.remote.PlaybackOutbox
import vn.hvp.travelvoice.platform.audio.TravelPlayback

/** Exercises the current entry point and real public server; does not mutate Admin content. */
@RunWith(AndroidJUnit4::class)
class SharedTourFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Test fun nativePlaybackAdvancesAndPauseResumeDoesNotStartANewReceipt() {
        val context = compose.activity.applicationContext
        val acknowledged = PlaybackOutbox.state(context).value.acknowledged
        val guide = runBlocking { DemoApi.guide(BuildConfig.API_BASE_URL, "20000000-0000-0000-0000-000000000101", "VI", "FULL") }
        assertNotNull(guide.audioUrl)
        try {
            compose.runOnUiThread { TravelPlayback.play(context, guide, BuildConfig.API_BASE_URL, 1f) }
            compose.waitUntil(45_000) { (TravelPlayback.narrator?.position ?: 0) >= 2 }
            compose.runOnUiThread { TravelPlayback.toggle() }
            compose.waitUntil(5_000) { TravelPlayback.narrator?.state == "Tạm dừng" }
            compose.runOnUiThread { TravelPlayback.toggle() }
            compose.waitUntil(10_000) { (TravelPlayback.narrator?.position ?: 0) >= 3 }
            compose.waitUntil(30_000) { PlaybackOutbox.state(context).value.acknowledged == acknowledged + 1 }
            assertEquals(acknowledged + 1, PlaybackOutbox.state(context).value.acknowledged)
        } finally { compose.runOnUiThread { TravelPlayback.stop(context) } }
    }
    @Test fun anonymousProfileAndPublishedPackageDownloadUseTheSharedBackend() {
        val base = BuildConfig.API_BASE_URL
        val target = runBlocking { DemoApi.locations(base).first { it.name == "A" } }
        val expected = runBlocking { DemoApi.guide(base, target.id, "VI", "FULL") }
        compose.onNodeWithContentDescription("Hồ sơ").performClick()
        compose.onNodeWithText("Bạn đang dùng với tư cách khách").assertExists()
        compose.onNodeWithText("Email").assertDoesNotExist()
        compose.onNodeWithText("Đồng bộ lượt nghe ngay").assertDoesNotExist()
        compose.onNode(hasText("Lượt nghe được ghi tự động", substring = true)).assertExists()
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.onAllNodesWithText("Ngoại tuyến").onFirst().performClick()
        compose.onNodeWithText("Kiểm tra cập nhật").performScrollTo().performClick()
        compose.waitUntil(45_000) { compose.onAllNodesWithText("Gói bổ sung").fetchSemanticsNodes().isNotEmpty() }
        val downloads = compose.onAllNodesWithText("Tải gói")
        if (downloads.fetchSemanticsNodes().isNotEmpty()) {
            downloads.onLast().performScrollTo().performClick()
            compose.onNodeWithText("Tải gói nội dung?").assertExists()
            compose.onNodeWithText("Xác nhận tải").performClick()
            compose.waitUntil(120_000) { compose.onAllNodesWithText("Đã tải và xác minh gói.").fetchSemanticsNodes().isNotEmpty() }
        }
        val offline = SharedTourRepository(compose.activity.applicationContext, "http://127.0.0.1:1/")
        runBlocking {
            assertTrue(offline.localLocations().any { it.id == target.id })
            val result = offline.guide(target.id, "VI", "FULL")
            assertTrue(result.second)
            assertEquals(expected.contentId, result.first.contentId)
            assertEquals(expected.scriptText, result.first.scriptText)
        }
        compose.onNodeWithText("A").performScrollTo().performClick()
        compose.waitUntil(30_000) { compose.onAllNodes(hasText(expected.scriptText, substring = true)).fetchSemanticsNodes().isNotEmpty() }
    }
}
