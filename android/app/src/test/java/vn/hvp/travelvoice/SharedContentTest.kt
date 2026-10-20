package vn.hvp.travelvoice

import android.content.Context
import android.util.Base64
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import vn.hvp.travelvoice.data.remote.SharedTourRepository

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class SharedContentTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private fun baseline() = JSONObject(context.assets.open("content/base.json").bufferedReader().use { it.readText() })

    @Test fun bundledBaselineIsSignedAndContainsTheSameSevenTimesSixTimesTwo() {
        val payload = SharedTourRepository.verify(baseline())
        assertEquals(7, payload.getJSONArray("locations").length())
        assertEquals(84, payload.getJSONArray("guides").length())
        assertEquals(6, payload.getJSONArray("languages").length())
        assertTrue(payload.getBoolean("isBase"))
        val guides = payload.getJSONArray("guides")
        for (index in 0 until guides.length()) {
            val guide = guides.getJSONObject(index)
            assertTrue(guide.getString("scriptText").isNotBlank())
            assertTrue(guide.getString("languageCode") in SharedTourRepository.languages)
            assertTrue(guide.getString("scriptType") in listOf("FULL", "SHORT"))
        }
    }
    @Test fun editedPayloadAndWrongBaseFlagAreRejected() {
        val modified = baseline()
        val bytes = Base64.decode(modified.getString("raw"), Base64.DEFAULT)
        bytes[bytes.lastIndex - 1] = (bytes[bytes.lastIndex - 1].toInt() xor 1).toByte()
        modified.put("raw", Base64.encodeToString(bytes, Base64.NO_WRAP))
        assertThrows(IllegalArgumentException::class.java) { SharedTourRepository.verify(modified) }
        val wrongFlag = baseline(); wrongFlag.getJSONObject("manifest").put("isBase", false)
        assertThrows(IllegalArgumentException::class.java) { SharedTourRepository.verify(wrongFlag) }
    }
    @Test fun deletingOptionalDoesNotRemoveProtectedBaseline() = runBlocking {
        val repository = SharedTourRepository(context, "http://127.0.0.1:1/")
        repository.deleteOptional()
        assertTrue(repository.installed().any { it.getJSONObject("manifest").getBoolean("isBase") })
    }
}
