package vn.hvp.travelvoice

import org.junit.Assert.*
import org.junit.Test
import vn.hvp.travelvoice.platform.audio.PlaybackReceiptTracker

class PlaybackReceiptTrackerTest {
    @Test fun shortAudioIsCountedWhileLoadingAndPausedSeekingAreNot() {
        val ids = mutableListOf<String>(); val tracker = PlaybackReceiptTracker { ids.add(it) }
        tracker.newSession()
        tracker.progressed(0, true); tracker.progressed(2500, false)
        assertTrue(ids.isEmpty())
        tracker.progressed(120, true)
        assertEquals(1, ids.size)
    }
    @Test fun pauseResumeAndRepeatedProgressKeepOneReceiptButReplayStartsANewSession() {
        val ids = mutableListOf<String>(); val tracker = PlaybackReceiptTracker { ids.add(it) }
        tracker.newSession(); tracker.progressed(100, true); tracker.progressed(200, false)
        tracker.progressed(300, true); tracker.progressed(5000, true)
        assertEquals(1, ids.size)
        tracker.newSession(); tracker.progressed(200, true)
        assertEquals(2, ids.toSet().size)
    }
}
