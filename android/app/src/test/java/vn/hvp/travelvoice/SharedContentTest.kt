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
        assertEquals(6, payload.getJSONArray("retiredLocationIds").length())
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
    @Test fun signedRetirementsHidePlacesInPreviouslyDownloadedPackages() = runBlocking {
        val payload = SharedTourRepository.verify(baseline())
        val oldPlace = JSONObject(payload.getJSONArray("locations").getJSONObject(0).toString()).put("locationId","20000000-0000-0000-0000-000000000001")
        payload.put("scope","OPTIONAL").put("isBase",false).put("retiredLocationIds",org.json.JSONArray())
            .put("locations",org.json.JSONArray().put(oldPlace)).put("guides",org.json.JSONArray())
        val bytes = payload.toString().toByteArray()
        val keys = java.security.KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
        val signer = java.security.Signature.getInstance("SHA256withRSA").apply { initSign(keys.private); update(bytes) }
        val sha = SharedTourRepository.sha(bytes)
        val manifest = JSONObject().put("scope","OPTIONAL").put("isBase",false).put("sizeBytes",bytes.size).put("sha256",sha)
            .put("version","optional-$sha").put("signature",Base64.encodeToString(signer.sign(),Base64.NO_WRAP)).put("keyId",SharedTourRepository.sha(keys.public.encoded))
        val record = JSONObject().put("manifest",manifest).put("raw",Base64.encodeToString(bytes,Base64.NO_WRAP)).put("key",Base64.encodeToString(keys.public.encoded,Base64.NO_WRAP))
        SharedTourRepository.verify(record)
        val folder = java.io.File(context.filesDir,"shared-content-v1").apply { mkdirs() }
        val oldPackage = java.io.File(folder,"OPTIONAL.json"); oldPackage.writeText(record.toString())
        try {
            val locations = SharedTourRepository(context,"http://127.0.0.1:1/").localLocations()
            assertEquals(7,locations.size)
            assertTrue(locations.none { it.id == oldPlace.getString("locationId") })
        } finally { oldPackage.delete() }
    }
}
