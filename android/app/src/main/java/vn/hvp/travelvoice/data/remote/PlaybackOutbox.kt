package vn.hvp.travelvoice.data.remote

import android.content.Context
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject

/** Automatic, durable listening receipts. UI controls never create or submit a listening count. */
object PlaybackOutbox {
    private var queue: AutomaticPlaybackQueue? = null

    private fun queue(context: Context): AutomaticPlaybackQueue = synchronized(this) {
        queue ?: run {
            val app = context.applicationContext
            val identity = TouristIdentity(app)
            val legacy = app.getSharedPreferences("playback-outbox", Context.MODE_PRIVATE)
            AutomaticPlaybackQueue(
                read = { JSONArray(identity.read("events") ?: legacy.getString("events", "[]")) },
                write = { events ->
                    identity.write("events", events.toString())
                    check(legacy.edit().remove("events").commit())
                },
                send = { event -> DemoApi.playback(event.getString("base"), event.getString("eventId"),
                    event.getString("contentId"), event.optJSONObject("identity"),
                    event.optString("occurredAt").takeIf { it.isNotBlank() }, event.optBoolean("offline")) }
            ).also { queue = it }
        }
    }

    fun state(context: Context): StateFlow<PlaybackSyncState> = queue(context).state

    /** Capture the real playback time now; saving/sending continues after the player is stopped. */
    fun recordAutomatically(context: Context, base: String, eventId: String, contentId: String,
                            offline: Boolean = !VisitorApi.isOnline(context)) {
        val event = JSONObject().put("base", base).put("eventId", eventId).put("contentId", contentId)
            .put("occurredAt", java.time.Instant.now().toString()).put("offline", offline)
        runCatching { TouristIdentity(context).snapshot() }.getOrNull()?.let { event.put("identity", it) }
        queue(context).record(event)
    }

    suspend fun retryAutomatically(context: Context, base: String) { queue(context).retry(base) }

    // Compatibility for the older prototype; the current entry point uses automatic callbacks.
    suspend fun enqueueAndFlush(context: Context, base: String, eventId: String, contentId: String,
                                offline: Boolean = !VisitorApi.isOnline(context)): Int {
        recordAutomatically(context, base, eventId, contentId, offline)
        return flush(context, base)
    }
    suspend fun flush(context: Context, base: String): Int {
        retryAutomatically(context, base)
        val snapshot = state(context).value
        check(!snapshot.error) { "Lượt nghe được giữ để tự gửi lại." }
        return snapshot.pending
    }
}
