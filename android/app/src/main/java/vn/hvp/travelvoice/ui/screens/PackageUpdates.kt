package vn.hvp.travelvoice.ui.screens

import androidx.compose.runtime.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import vn.hvp.travelvoice.data.remote.SharedTourRepository

/** Shared snapshot for polling, the update banner and manual checks on the Offline page. */
class PackageUpdates(private val repository: SharedTourRepository) {
    var installed by mutableStateOf<List<JSONObject>>(emptyList()); private set
    var available by mutableStateOf<List<JSONObject>>(emptyList()); private set
    var error by mutableStateOf(false); private set
    var checking by mutableStateOf(false); private set
    var busy by mutableStateOf(false); private set
    private val checkLock = Mutex()
    val pending: List<JSONObject> get() = available.filter { remote ->
        installed.none { it.getJSONObject("manifest").let { local ->
            local.getString("scope") == remote.getString("scope") && local.getString("version") == remote.getString("version")
        } }
    }
    suspend fun check() = checkLock.withLock {
        checking = true
        try { installed = repository.installed(); available = repository.available(); error = false }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { error = true }
        finally { checking = false }
    }
    suspend fun download(manifest: JSONObject, progress: (String) -> Unit) {
        check(!busy); busy = true
        try { repository.download(manifest, progress); installed = repository.installed() }
        finally { busy = false }
    }
    suspend fun deleteOptional() {
        check(!busy); busy = true
        try { repository.deleteOptional(); repository.invalidateCache(); installed = repository.installed() }
        finally { busy = false }
    }
}
