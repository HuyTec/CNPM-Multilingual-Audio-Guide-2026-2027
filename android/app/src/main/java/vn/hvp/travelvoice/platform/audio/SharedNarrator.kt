package vn.hvp.travelvoice.platform.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import vn.hvp.travelvoice.data.remote.ServerGuide
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel
import java.io.File
import java.util.Locale
import java.util.UUID

/** Native adapter for the same approved guide identity used on Web. */
class SharedNarrator(private val context: Context, private val lifecycle: Lifecycle?,
                     private val guide: ServerGuide, private val started: (String) -> Unit) {
    var state by mutableStateOf("Sẵn sàng nghe"); private set
    var ttsSource by mutableStateOf(false); private set
    var position by mutableIntStateOf(0); private set
    var total by mutableIntStateOf(0); private set
    var rate by mutableFloatStateOf(1f); private set
    var voiceName by mutableStateOf(""); private set
    private val handler = Handler(Looper.getMainLooper())
    private val manager = context.getSystemService(AudioManager::class.java)
    private val attributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()
    private val focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).setAudioAttributes(attributes)
        .setOnAudioFocusChangeListener({ if (it < 0) pause() }, handler).build()
    private var player: MediaPlayer? = null
    private var engine: TextToSpeech? = null
    private var ready = false
    private var generation = 0
    private var closed = false
    private val receipt = PlaybackReceiptTracker(started)
    private var work: java.io.File? = null
    private var renderTarget: java.io.File? = null
    private var parts = listOf<java.io.File>()
    private val io = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO)
    private val timeout = Runnable { fail("Không thể chuẩn bị âm thanh. Hãy kiểm tra giọng đọc/kết nối và thử lại.") }
    private val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_STOP) pause() }
    private val chunks = guide.scriptText.chunked(TextToSpeech.getMaxSpeechInputLength() - 1)
    init { lifecycle?.addObserver(observer) }
    private fun acquire() = manager.requestAudioFocus(focus) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    private fun begin() { handler.removeCallbacks(timeout); state = "Đang phát" }
    fun play() {
        if (closed || state in listOf("Đang phát", "Đang chuẩn bị…")) return
        if (state == "Tạm dừng" && player != null) {
            runCatching { if (acquire()) { player?.start(); begin() } }.onFailure { fail("Không thể tiếp tục âm thanh.") }
            return
        }
        stop(); receipt.newSession()
        val url = guide.audioUrl
        if (url.isNullOrBlank()) useTts() else { ttsSource = false; prepare(url, true) }
    }
    private fun prepare(url: String, fallback: Boolean) {
        state = "Đang chuẩn bị…"
        try {
            val created = MediaPlayer(); player = created
            created.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK)
            created.setAudioAttributes(attributes); created.setDataSource(url)
            created.setOnPreparedListener {
                if (player === it && !closed && state == "Đang chuẩn bị…") {
                    total = (it.duration / 1000).coerceAtLeast(1)
                    if (acquire()) { it.playbackParams = it.playbackParams.setSpeed(rate); it.start(); begin() }
                    else fail("Thiết bị đang ưu tiên âm thanh khác. Hãy thử lại.")
                }
            }
            created.setOnErrorListener { media, _, _ ->
                if (player === media && !closed) {
                    if (fallback) useTts() else { renderTarget?.delete(); fail("Tệp giọng đọc không khả dụng. Hãy thử lại.") }
                }; true
            }
            created.setOnCompletionListener { if (!closed && player === it) {
                receipt.progressed(it.currentPosition, state == "Đang phát")
                state = "Đã nghe hết"; position = total; manager.abandonAudioFocusRequest(focus)
            } }
            handler.removeCallbacks(timeout); handler.postDelayed(timeout, 15_000); created.prepareAsync()
        } catch (_: Exception) { if (fallback) useTts() else fail("Không thể phát tệp giọng đọc.") }
    }
    private fun useTts() {
        player?.release(); player = null; generation++; engine?.stop(); ttsSource = true
        position = 0; total = 0; state = "Đang chuẩn bị…"
        handler.removeCallbacks(timeout); handler.postDelayed(timeout, 45_000)
        if (engine == null) {
            engine = TextToSpeech(context) { result -> handler.post {
                if (!closed) { ready = result == TextToSpeech.SUCCESS; if (ready && state == "Đang chuẩn bị…") chooseVoice() else if (!ready) fail("Chưa khởi tạo được dịch vụ đọc văn bản.") }
            } }
        } else if (ready) chooseVoice()
    }
    /** Render at 1× once; MediaPlayer supplies real seconds and changes speed without resynthesis. */
    private fun chooseVoice() {
        val locale = Locale.forLanguageTag(guide.languageCode)
        val voices = engine?.voices.orEmpty().filter { it.locale.language == locale.language }
        val voice = voices.firstOrNull { !it.isNetworkConnectionRequired } ?: voices.firstOrNull()
        if (voice == null) { fail("Thiết bị chưa có giọng ${guide.languageCode}. Hãy cài giọng tương ứng; text vẫn dùng được."); return }
        engine?.voice = voice; voiceName = voice.name; engine?.setSpeechRate(1f)
        val directory = File(context.cacheDir,"narration-v1").apply { mkdirs() }
        val cacheKey = vn.hvp.travelvoice.data.remote.SharedTourRepository.sha((guide.contentId + guide.languageCode + guide.scriptType + guide.scriptText + voice.name).toByteArray())
        val target = File(directory,"$cacheKey.wav"); renderTarget = target
        if (target.isFile && target.length() > 44) { target.setLastModified(System.currentTimeMillis()); prepare(target.absolutePath,false); return }
        val token = ++generation
        val folder = File(directory,"work-${UUID.randomUUID()}").apply { mkdirs() }; work = folder
        parts = chunks.indices.map { File(folder,"$it.wav") }
        engine?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) = Unit
            override fun onDone(id: String?) { handler.post {
                if (closed || token != generation) return@post
                val index = id?.substringAfter(':')?.toIntOrNull() ?: return@post
                if (index + 1 < chunks.size) render(index + 1,token)
                else {
                    val files = parts
                    io.launch {
                        val result = runCatching { PcmWave.join(files,target) }
                        folder.deleteRecursively()
                        handler.post {
                            if (!closed && token == generation) {
                                work = null
                                if (result.isSuccess) { trimCache(directory,target); prepare(target.absolutePath,false) }
                                else fail("Không thể tạo tệp âm thanh. Hãy kiểm tra dung lượng và thử lại.")
                            }
                        }
                    }
                }
            } }
            @Deprecated("Platform callback") override fun onError(id: String?) { handler.post { if (!closed && token == generation) fail("Giọng đọc gặp lỗi. Kiểm tra giọng đã cài/kết nối và thử lại.") } }
        })
        render(0,token)
    }
    private fun render(index: Int, token: Int) {
        handler.removeCallbacks(timeout); handler.postDelayed(timeout,45_000)
        if (engine?.synthesizeToFile(chunks[index],android.os.Bundle(),parts[index],"$token:$index") == TextToSpeech.ERROR) fail("Không thể tạo âm thanh bằng giọng đã chọn.")
    }
    private fun trimCache(directory: File, keep: File) {
        val files = directory.listFiles().orEmpty().filter { it.isFile && it.extension == "wav" }.sortedBy { it.lastModified() }
        var size = files.sumOf { it.length() }
        for (file in files) { if (size <= 50L * 1024 * 1024) break; if (file != keep) { val bytes = file.length(); if (file.delete()) size -= bytes } }
    }
    fun pause() {
        if (state !in listOf("Đang phát", "Đang chuẩn bị…")) return
        generation++; engine?.stop(); handler.removeCallbacks(timeout)
        runCatching { player?.pause(); position = player?.currentPosition?.div(1000) ?: position }
        if (total <= 0) { player?.release(); player = null }
        state = "Tạm dừng"; manager.abandonAudioFocusRequest(focus)
    }
    fun stop() {
        generation++; engine?.stop(); player?.release(); player = null; handler.removeCallbacks(timeout)
        work?.let { folder -> io.launch { folder.deleteRecursively() } }; work = null
        state = "Sẵn sàng nghe"; position = 0; total = 0; manager.abandonAudioFocusRequest(focus)
    }
    fun seek(value: Int) {
        if (total <= 0) return
        runCatching { player?.seekTo(value.coerceIn(0,total) * 1000); position = value.coerceIn(0,total) }
        if (state == "Đã nghe hết") state = "Tạm dừng"
    }
    fun speed(value: Float) {
        rate = value
        val active = state == "Đang phát"
        runCatching { player?.let { if (total > 0) { it.playbackParams = it.playbackParams.setSpeed(value); if (!active) it.pause() } } }
    }
    fun tick() { if (state == "Đang phát") runCatching {
        val current = player ?: return@runCatching
        val positionMs = current.currentPosition
        position = positionMs / 1000
        receipt.progressed(positionMs, current.isPlaying)
    } }
    private fun fail(message: String) { stop(); state = message }
    fun close() { if (!closed) { stop(); closed = true; engine?.shutdown(); engine = null; lifecycle?.removeObserver(observer); handler.removeCallbacksAndMessages(null) } }
}
