package vn.hvp.travelvoice

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import java.net.ServerSocket
import java.net.InetAddress
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import vn.hvp.travelvoice.data.remote.SharedTourRepository
import vn.hvp.travelvoice.data.remote.TouristIdentity
import vn.hvp.travelvoice.ui.screens.PackageUpdates

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class PackagePollingTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    @Test fun manualAndPeriodicChecksShareTheLatestPublishedSnapshot() = runBlocking {
        val bundled = JSONObject(context.assets.open("content/base.json").bufferedReader().use { it.readText() }).getJSONObject("manifest")
        bundled.put("status", "APPROVED")
        val response = AtomicReference(JSONArray().put(bundled).toString())
        val server = ServerSocket(0, 10, InetAddress.getByName("127.0.0.1"))
        val serving = Thread {
            while (!server.isClosed) try {
                server.accept().use { socket ->
                    val reader = socket.getInputStream().bufferedReader()
                    while (!reader.readLine().isNullOrEmpty()) { /* Consume request headers. */ }
                    val bytes = response.get().toByteArray()
                    socket.getOutputStream().apply {
                        write("HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: ${bytes.size}\r\nConnection: close\r\n\r\n".toByteArray())
                        write(bytes); flush()
                    }
                }
            } catch (failure: java.io.IOException) { if (!server.isClosed) throw failure }
        }.apply { isDaemon = true; start() }
        try {
            val repository = SharedTourRepository(context, "http://127.0.0.1:${server.localPort}/")
            val state = PackageUpdates(repository)
            state.check(); assertTrue(state.pending.isEmpty())
            val next = JSONObject(bundled.toString()).put("scope", "OPTIONAL").put("version", "optional-new")
            response.set(JSONArray().put(bundled).put(next).toString())
            state.check(); assertEquals("optional-new", state.pending.single().getString("version"))
            response.set(JSONArray().put(JSONObject(next.toString()).put("version", "optional-newer")).toString())
            state.check(); assertEquals("optional-newer", state.pending.single().getString("version"))
            response.set(JSONArray().put(JSONObject(next.toString()).put("status", "PENDING_REVIEW")).toString())
            state.check(); assertTrue(state.error)
            assertEquals("optional-newer", state.pending.single().getString("version"))
            assertTrue(repository.installed().any { it.getJSONObject("manifest").getBoolean("isBase") })
        } finally { server.close(); serving.join(1_000) }
    }
    @Test fun visitorIdSurvivesReopeningAndDoesNotRequireLoginOrHardwareIdentity() {
        val first = TouristIdentity(context).installation()
        assertEquals(first, TouristIdentity(context).installation())
        assertEquals(first, java.util.UUID.fromString(first).toString())
        val snapshot = TouristIdentity(context).snapshot()
        assertFalse(snapshot.has("token")); assertFalse(snapshot.has("email"))
    }
}
