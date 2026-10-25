package vn.hvp.travelvoice

import java.io.IOException
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import vn.hvp.travelvoice.data.remote.AutomaticPlaybackQueue
import vn.hvp.travelvoice.data.remote.GuideApiException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = OfflineTestApplication::class)
class AutomaticPlaybackQueueTest {
    private val base = "https://content.example/"
    private fun event() = JSONObject().put("base", base).put("eventId", UUID.randomUUID().toString())
        .put("contentId", UUID.randomUUID().toString()).put("occurredAt", "2026-01-01T12:00:00Z").put("offline", true)
    private suspend fun await(condition: () -> Boolean) = withTimeout(5_000) { while (!condition()) delay(10) }

    @Test fun playbackCallbackPersistsAndSendsWithoutAnyUserSyncAction() = runBlocking {
        val disk = AtomicReference("[]")
        val received = AtomicReference<JSONObject>()
        val queue = AutomaticPlaybackQueue({ JSONArray(disk.get()) }, { disk.set(it.toString()) }, { received.set(it) })
        try {
            val play = event()
            queue.record(play)
            await { queue.state.value.acknowledged == 1L }
            assertEquals(play.toString(), received.get().toString())
            assertEquals(0, JSONArray(disk.get()).length())
        } finally { queue.close() }
    }

    @Test fun failedResponseRetriesOriginalReceiptAndNeverCreatesAnotherEventId() = runBlocking {
        val disk = AtomicReference("[]")
        val attempts = AtomicInteger()
        val counted = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
        val queue = AutomaticPlaybackQueue({ JSONArray(disk.get()) }, { disk.set(it.toString()) }, {
            counted.add(it.getString("eventId"))
            if (attempts.incrementAndGet() == 1) throw IOException("response lost after server accepted")
        })
        try {
            val play = event(); queue.record(play)
            await { queue.state.value.error }
            assertEquals(play.getString("eventId"), JSONArray(disk.get()).getJSONObject(0).getString("eventId"))
            queue.retry(base) // The same method invoked by reconnect/resume/15-second timers.
            await { queue.state.value.pending == 0 && queue.state.value.acknowledged == 1L }
            assertEquals(2, attempts.get()); assertEquals(1, counted.size)
        } finally { queue.close() }
    }

    @Test fun aSlowNetworkDoesNotPreventTheNextPlaybackFromBeingSaved() = runBlocking {
        val disk = AtomicReference("[]")
        val started = CompletableDeferred<Unit>(); val release = CompletableDeferred<Unit>()
        val first = event(); val second = event()
        val queue = AutomaticPlaybackQueue({ JSONArray(disk.get()) }, { disk.set(it.toString()) }, {
            if (it.getString("eventId") == first.getString("eventId")) { started.complete(Unit); release.await() }
        })
        try {
            queue.record(first); withTimeout(5_000) { started.await() }
            queue.record(second)
            await { JSONArray(disk.get()).length() == 2 }
            release.complete(Unit)
            await { queue.state.value.acknowledged == 2L }
            assertEquals("[]", disk.get())
        } finally { release.complete(Unit); queue.close() }
    }

    @Test fun oneRejectedReceiptDoesNotBlockLaterValidListeningEvents() = runBlocking {
        val disk = AtomicReference("[]"); val rejected = event(); val valid = event()
        val queue = AutomaticPlaybackQueue({ JSONArray(disk.get()) }, { disk.set(it.toString()) }, {
            if (it.getString("eventId") == rejected.getString("eventId")) throw GuideApiException(404, "retired")
        })
        try {
            queue.record(rejected); queue.record(valid)
            await { queue.state.value.rejected == 1 && queue.state.value.acknowledged == 1L }
            assertTrue(JSONArray(disk.get()).getJSONObject(0).getBoolean("rejected"))
            assertEquals(0, queue.state.value.pending)
        } finally { queue.close() }
    }

    @Test fun storageRecoveryAutomaticallySavesTheOriginalInMemoryReceipt() = runBlocking {
        val available = AtomicBoolean(false); val disk = AtomicReference("[]")
        val received = AtomicReference<String>()
        val queue = AutomaticPlaybackQueue({ JSONArray(disk.get()) }, {
            if (!available.get()) throw IOException("storage temporarily unavailable")
            disk.set(it.toString())
        }, { received.set(it.getString("eventId")) })
        try {
            val play = event(); queue.record(play)
            await { queue.state.value.error }
            available.set(true); queue.retry(base)
            await { queue.state.value.acknowledged == 1L }
            assertEquals(play.getString("eventId"), received.get())
        } finally { queue.close() }
    }
}
