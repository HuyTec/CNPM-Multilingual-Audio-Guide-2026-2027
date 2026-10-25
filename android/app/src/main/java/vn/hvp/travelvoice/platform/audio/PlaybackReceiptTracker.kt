package vn.hvp.travelvoice.platform.audio

import java.util.UUID

/** One receipt per actual playback session, including clips shorter than one second. */
internal class PlaybackReceiptTracker(private val started: (String) -> Unit) {
    private var counted = false
    fun newSession() { counted = false }
    fun progressed(positionMs: Int, playing: Boolean) {
        if (!counted && playing && positionMs > 0) {
            counted = true
            started(UUID.randomUUID().toString())
        }
    }
}
