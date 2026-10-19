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
            val pending = JSONArray(preferences.getString("events", "[]"))
            if ((0 until pending.length()).none { pending.getJSONObject(it).getString("eventId") == eventId }) {
                pending.put(JSONObject().put("base", base).put("eventId", eventId).put("contentId", contentId))
                check(preferences.edit().putString("events", pending.toString()).commit()) { "Chưa lưu được lượt nghe trên thiết bị. Hãy thử lại đồng bộ." }
            }
            flushLocked(context, base)
        } }

    suspend fun flush(context: Context, base: String): Int = withContext(Dispatchers.IO) {
        mutex.withLock { flushLocked(context, base) }
    }

    private suspend fun flushLocked(context: Context, base: String): Int {
        val preferences = context.applicationContext.getSharedPreferences("playback-outbox", Context.MODE_PRIVATE)
        var pending = JSONArray(preferences.getString("events", "[]"))
        // Commit each acknowledgment, so a later failure cannot lose unsent events.
        val ids = (0 until pending.length()).map { pending.getJSONObject(it) }
            .filter { it.getString("base") == base }.map { it.getString("eventId") }
        for (id in ids) {
            val event = (0 until pending.length()).map { pending.getJSONObject(it) }.first { it.getString("eventId") == id }
            DemoApi.playback(base, id, event.getString("contentId"))
            val next = JSONArray()
            (0 until pending.length()).map { pending.getJSONObject(it) }.filter { it.getString("eventId") != id }.forEach(next::put)
            check(preferences.edit().putString("events", next.toString()).commit()) { "Đã gửi nhưng chưa lưu được xác nhận. Có thể thử lại an toàn." }
            pending = next
        }
        return pending.length()
    }
}
