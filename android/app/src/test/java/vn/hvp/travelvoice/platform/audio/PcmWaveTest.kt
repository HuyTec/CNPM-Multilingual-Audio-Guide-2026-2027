package vn.hvp.travelvoice.platform.audio

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.ByteBuffer
import java.nio.ByteOrder

class PcmWaveTest {
    @get:Rule val directory = TemporaryFolder()
    private fun wave(name: String, samples: ByteArray, rate: Int = 16000): java.io.File {
        val bytes = ByteBuffer.allocate(44 + samples.size).order(ByteOrder.LITTLE_ENDIAN)
            .put("RIFF".toByteArray()).putInt(36 + samples.size).put("WAVEfmt ".toByteArray()).putInt(16)
            .putShort(1).putShort(1).putInt(rate).putInt(rate * 2).putShort(2).putShort(16)
            .put("data".toByteArray()).putInt(samples.size).put(samples).array()
        return directory.newFile(name).apply { writeBytes(bytes) }
    }
    @Test fun joinedAudioRetainsPcmAndMeasuredByteDuration() {
        val a = wave("a.wav",ByteArray(32000) { 1 }); val b = wave("b.wav",ByteArray(64000) { 2 })
        val target = java.io.File(directory.root,"joined.wav"); PcmWave.join(listOf(a,b),target)
        val bytes = target.readBytes(); val header = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        assertEquals(96044,bytes.size); assertEquals(96000,header.getInt(40))
        assertEquals(3,header.getInt(40) / header.getInt(28))
        assertArrayEquals(ByteArray(32000) { 1 } + ByteArray(64000) { 2 },bytes.copyOfRange(44,bytes.size))
    }
    @Test fun differentSampleRatesAreRejectedWithoutPublishingPartialFile() {
        val target = java.io.File(directory.root,"joined.wav")
        assertThrows(IllegalArgumentException::class.java) { PcmWave.join(listOf(wave("a.wav",ByteArray(10)),wave("b.wav",ByteArray(10),22050)),target) }
        assertFalse(target.exists())
    }
    @Test fun truncatedOrNonWaveInputIsRejected() {
        val invalid = directory.newFile("invalid").apply { writeText("not audio") }
        assertThrows(IllegalArgumentException::class.java) { PcmWave.join(listOf(invalid),java.io.File(directory.root,"joined.wav")) }
    }
}
