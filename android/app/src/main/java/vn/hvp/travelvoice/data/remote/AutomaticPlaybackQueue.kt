package vn.hvp.travelvoice.data.remote

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject

data class PlaybackSyncState(val base: String = "", val pending: Int = 0, val rejected: Int = 0,
                             val syncing: Boolean = false, val error: Boolean = false, val acknowledged: Long = 0)

/** Storage is locked only for journal mutations, never while waiting for the network. */
internal class AutomaticPlaybackQueue(
    private val read: () -> JSONArray,
    private val write: (JSONArray) -> Unit,
    private val send: suspend (JSONObject) -> Unit,
) {
    private val storage = Mutex()
    private val sending = Mutex()
    private val worker = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val unsaved = java.util.concurrent.ConcurrentHashMap<String, JSONObject>()
    private val mutableState = MutableStateFlow(PlaybackSyncState())
    val state = mutableState.asStateFlow()

    /** Outlives a player/service stopping; the receipt is persisted before any HTTP call. */
    fun record(event: JSONObject) {
        unsaved.putIfAbsent(event.getString("eventId"), JSONObject(event.toString()))
        worker.launch { retry(event.getString("base")) }
    }

    suspend fun retry(base: String) = withContext(Dispatchers.IO) {
        try {
            persist(base)
            if (!sending.tryLock()) return@withContext
            try {
                mutableState.update { it.copy(base = base, syncing = true, error = false) }
                while (isActive) {
                    val event = storage.withLock {
                        val journal = read()
                        (0 until journal.length()).map { journal.getJSONObject(it) }
                            .firstOrNull { it.getString("base") == base && !it.optBoolean("rejected") }
                    } ?: break
                    try {
                        send(event)
                        storage.withLock {
                            val next = JSONArray()
                            val journal = read()
                            repeat(journal.length()) { index -> journal.getJSONObject(index).let {
                                if (it.getString("eventId") != event.getString("eventId")) next.put(it)
                            } }
                            write(next)
                            mutableState.update { it.copy(acknowledged = it.acknowledged + 1) }
                            counts(base, next)
                        }
                    } catch (failure: GuideApiException) {
                        if (failure.status !in listOf(400, 404, 409)) throw failure
                        // Keep bad receipts for diagnosis without blocking all later valid plays.
                        storage.withLock {
                            val journal = read()
                            repeat(journal.length()) { index -> journal.getJSONObject(index).let {
                                if (it.getString("eventId") == event.getString("eventId"))
                                    it.put("rejected", true).put("lastStatus", failure.status)
                            } }
                            write(journal); counts(base, journal)
                        }
                    }
                }
            } finally {
                mutableState.update { it.copy(syncing = false) }
                sending.unlock()
            }
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { mutableState.update { it.copy(base = base, error = true) } }
    }

    private suspend fun persist(base: String) = storage.withLock {
        val journal = read()
        val batch = unsaved.entries.map { it.key to it.value }
        val ids = (0 until journal.length()).map { journal.getJSONObject(it).getString("eventId") }.toSet()
        batch.forEach { (id, event) -> if (id !in ids) journal.put(event) }
        if (batch.isNotEmpty()) {
            write(journal)
            batch.forEach { (id, event) -> unsaved.remove(id, event) }
        }
        counts(base, journal)
    }

    private fun counts(base: String, journal: JSONArray) {
        val entries = (0 until journal.length()).map { journal.getJSONObject(it) }.filter { it.getString("base") == base }
        val pending = entries.filterNot { it.optBoolean("rejected") }.map { it.getString("eventId") }.toSet() +
            unsaved.values.filter { it.getString("base") == base }.map { it.getString("eventId") }
        mutableState.update { it.copy(base = base, pending = pending.size, rejected = entries.count { entry -> entry.optBoolean("rejected") }) }
    }

    fun close() { worker.cancel() }
}
