package vn.hvp.travelvoice.data.remote

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/** Persist before sending. A lost response can safely be retried with the original event UUID. */
object PlaybackOutbox {
    private val mutex = Mutex()

    suspend fun enqueueAndFlush(context: Context, base: String, eventId: String, contentId: String): Int =
        withContext(Dispatchers.IO) { mutex.withLock {
            val preferences = context.applicationContext.getSharedPreferences("playback-outbox", Context.MODE_PRIVATE)
            val store = TouristIdentity(context)
            val pending = JSONArray(store.read("events") ?: preferences.getString("events", "[]"))
            if ((0 until pending.length()).none { pending.getJSONObject(it).getString("eventId") == eventId }) {
                pending.put(JSONObject().put("base", base).put("eventId", eventId).put("contentId", contentId).put("identity",store.snapshot()))
                store.write("events",pending.toString()); preferences.edit().remove("events").commit()
            }
            flushLocked(context, base)
        } }

    suspend fun flush(context: Context, base: String): Int = withContext(Dispatchers.IO) {
        mutex.withLock { flushLocked(context, base) }
    }

    private suspend fun flushLocked(context: Context, base: String): Int {
        val preferences = context.applicationContext.getSharedPreferences("playback-outbox", Context.MODE_PRIVATE)
        val store = TouristIdentity(context)
        var pending = JSONArray(store.read("events") ?: preferences.getString("events", "[]"))
        // Commit each acknowledgment, so a later failure cannot lose unsent events.
        val ids = (0 until pending.length()).map { pending.getJSONObject(it) }
            .filter { it.getString("base") == base }.map { it.getString("eventId") }
        for (id in ids) {
            val event = (0 until pending.length()).map { pending.getJSONObject(it) }.first { it.getString("eventId") == id }
            val identity = event.optJSONObject("identity")
            val current = store.session()
            // Refresh only for the same authenticated account; never move an old play to a new user.
            if (current != null && identity?.optString("userId") == current.getString("userId")) identity.put("token",current.getString("accessToken"))
            DemoApi.playback(base, id, event.getString("contentId"),identity)
            val next = JSONArray()
            (0 until pending.length()).map { pending.getJSONObject(it) }.filter { it.getString("eventId") != id }.forEach(next::put)
            store.write("events",next.toString()); preferences.edit().remove("events").commit()
            pending = next
        }
        return pending.length()
    }
}
