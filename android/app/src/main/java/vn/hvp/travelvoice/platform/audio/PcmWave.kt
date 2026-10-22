package vn.hvp.travelvoice.platform.audio

import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Joins TTS PCM WAV segments without estimating duration or changing the approved text. */
internal object PcmWave {
    private fun int(bytes: ByteArray, offset: Int) = ByteBuffer.wrap(bytes,offset,4).order(ByteOrder.LITTLE_ENDIAN).int
    fun join(parts: List<File>, target: File) {
        var format: ByteArray? = null
        val audio = java.io.ByteArrayOutputStream()
        for (part in parts) {
            val bytes = part.readBytes(); require(bytes.size >= 44 && bytes.copyOfRange(0,4).toString(Charsets.US_ASCII) == "RIFF" && bytes.copyOfRange(8,12).toString(Charsets.US_ASCII) == "WAVE")
            var cursor = 12; var data = false
            while (cursor + 8 <= bytes.size) {
                val size = int(bytes,cursor + 4); require(size >= 0 && size <= bytes.size - cursor - 8)
                when (bytes.copyOfRange(cursor,cursor+4).toString(Charsets.US_ASCII)) {
                    "fmt " -> { val current = bytes.copyOfRange(cursor+8,cursor+8+size); require(current.size >= 16 && current[0] == 1.toByte() && current[1] == 0.toByte()); if (format == null) format = current else require(format.contentEquals(current)) }
                    "data" -> { audio.write(bytes,cursor+8,size); data = true }
                }
                cursor += 8 + size + size % 2
            }
            require(data && audio.size() <= 50 * 1024 * 1024)
        }
        val fmt = requireNotNull(format); val pcm = audio.toByteArray()
        val buffer = ByteBuffer.allocate(28 + fmt.size + pcm.size).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put("RIFF".toByteArray()).putInt(buffer.capacity()-8).put("WAVEfmt ".toByteArray()).putInt(fmt.size).put(fmt).put("data".toByteArray()).putInt(pcm.size).put(pcm)
        val temporary = File(target.parentFile,"${target.name}.${java.util.UUID.randomUUID()}.part")
        try { temporary.writeBytes(buffer.array()); check(temporary.renameTo(target)) } finally { temporary.delete() }
    }
}
