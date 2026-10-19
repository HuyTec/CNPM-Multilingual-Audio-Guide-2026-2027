package vn.hvp.travelvoice

import android.util.Base64
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.net.HttpURLConnection
import java.net.URI

/** Optional real-server test: provide demoAdminPassword via instrumentation arguments. */
@RunWith(AndroidJUnit4::class)
class LivePlaybackTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun actualAudioCountsOneStartResumeDoesNotDuplicateAndReplayAddsOne() {
        val password = InstrumentationRegistry.getArguments().getString("demoAdminPassword").orEmpty()
        assumeTrue("Requires the local demo backend and demoAdminPassword argument", password.isNotBlank())
        compose.waitUntil(20_000) {
            compose.onAllNodesWithText("Đang chọn: Bảo tàng Demo").fetchSemanticsNodes().isNotEmpty()
        }
        val before = total(password)
        compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.ScrollToIndex))
            .performScrollToNode(hasText("Phát thuyết minh"))
        compose.onNodeWithText("Phát thuyết minh").performClick()
        compose.waitUntil(20_000) { total(password) == before + 1 }
        compose.waitUntil(10_000) {
            compose.onAllNodes(SemanticsMatcher("nonzero playback position") { node ->
                node.config.getOrNull(SemanticsProperties.Text)?.any { text ->
                    val match = Regex("^(\\d+):(\\d+) / \\d+:\\d+$").matchEntire(text.text)
                    match != null && (match.groupValues[1].toInt() * 60 + match.groupValues[2].toInt()) > 0
                } == true
            }).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Tạm dừng audio").performClick()
        compose.onNodeWithText("Tiếp tục audio").performClick()
        assertEquals(before + 1, total(password))
        compose.waitUntil(45_000) { compose.onAllNodesWithText("Nghe lại").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Nghe lại").performClick()
        compose.waitUntil(20_000) { total(password) == before + 2 }
        android.util.Log.i("LivePlaybackTest", "Real MediaPlayer -> PostgreSQL -> admin API verified; count $before -> ${before + 2}; pause/resume did not add a play")
    }

    private fun total(password: String): Long {
        val connection = URI("http://10.0.2.2:8081/api/admin/analytics").toURL().openConnection() as HttpURLConnection
        connection.connectTimeout = 3000
        connection.readTimeout = 3000
        connection.setRequestProperty("Authorization", "Basic " + Base64.encodeToString("admin:$password".toByteArray(), Base64.NO_WRAP))
        return try { connection.inputStream.bufferedReader().use { JSONObject(it.readText()).getLong("totalPlaybacks") } }
        finally { connection.disconnect() }
    }
}
