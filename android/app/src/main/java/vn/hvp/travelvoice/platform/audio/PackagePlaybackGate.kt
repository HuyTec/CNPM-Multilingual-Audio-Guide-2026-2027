package vn.hvp.travelvoice.platform.audio
import kotlinx.coroutines.delay
import java.util.concurrent.ConcurrentHashMap
object PackagePlaybackGate {
    val owners = ConcurrentHashMap<Any, () -> Boolean>()
    suspend fun awaitIdle() { while (owners.values.any { it() }) delay(250) }
}
