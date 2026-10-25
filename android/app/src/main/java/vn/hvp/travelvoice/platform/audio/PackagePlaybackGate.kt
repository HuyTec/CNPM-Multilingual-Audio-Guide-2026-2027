package vn.hvp.travelvoice.platform.audio

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** An active or paused narration retains its content until the listener stops or finishes. */
object PackagePlaybackGate {
    var installing by mutableStateOf(false); private set
    fun busy() = TravelPlayback.narrator?.state in listOf("Đang phát", "Đang chuẩn bị…", "Tạm dừng")
    suspend fun install(waiting: () -> Unit, commit: suspend () -> Unit) = withContext(Dispatchers.Main) {
        while (busy() || installing) { waiting(); delay(250) }
        installing = true
        try { withContext(Dispatchers.IO) { commit() } }
        finally { installing = false }
    }
}
