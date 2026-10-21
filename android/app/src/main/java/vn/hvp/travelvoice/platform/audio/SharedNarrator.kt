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
import java.text.BreakIterator
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
    private var counted = false
    private var offset = 0
    private val timeout = Runnable { fail("Không thể bắt đầu phát. Hãy kiểm tra giọng đọc/kết nối và thử lại.") }
    private val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_STOP) pause() }
    private val chunks = buildList {
        val text = guide.scriptText
        val iterator = BreakIterator.getSentenceInstance(Locale.forLanguageTag(guide.languageCode))
        iterator.setText(text); var start = iterator.first(); var end = iterator.next()
        while (end != BreakIterator.DONE) { add(text.substring(start, end)); start = end; end = iterator.next() }
        if (isEmpty()) add(text)
    }
    init { lifecycle?.addObserver(observer) }
    private fun acquire() = manager.requestAudioFocus(focus) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    private fun begin() { handler.removeCallbacks(timeout); state = "Đang phát"; if (!counted && (ttsSource || position > 0)) { counted = true; started(UUID.randomUUID().toString()) } }
    fun play() {
        if (closed || state in listOf("Đang phát", "Đang chuẩn bị…")) return
        if (state == "Tạm dừng") {
            if (ttsSource) speak(position, offset) else runCatching { if (acquire()) { player?.start(); begin() } }.onFailure { useTts() }
            return
        }
        stop(); counted = false
        val url = guide.audioUrl
        if (url.isNullOrBlank()) { useTts(); return }
        state = "Đang chuẩn bị…"; ttsSource = false
        try {
            val created = MediaPlayer(); player = created
            created.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK)
            created.setAudioAttributes(attributes); created.setDataSource(url)
            created.setOnPreparedListener {
                if (player === it && !closed) {
                    total = it.duration / 1000
                    if (state != "Đang chuẩn bị…") return@setOnPreparedListener
                    if (acquire()) { it.playbackParams = it.playbackParams.setSpeed(rate); it.start(); begin() }
                    else fail("Thiết bị đang ưu tiên âm thanh khác. Hãy thử lại.")
                }
            }
            created.setOnErrorListener { media, _, _ -> if (player === media && !closed) useTts(); true }
            created.setOnCompletionListener { if (!closed && player === it) { state = "Đã nghe hết"; position = total; manager.abandonAudioFocusRequest(focus) } }
            handler.postDelayed(timeout, 12_000); created.prepareAsync()
        } catch (_: Exception) { useTts() }
    }
    private fun useTts() {
        player?.release(); player = null; generation++; engine?.stop(); ttsSource = true
        position = 0; offset = 0; total = chunks.size; state = "Đang chuẩn bị…"
        handler.removeCallbacks(timeout); handler.postDelayed(timeout, 12_000)
        if (engine == null) {
            engine = TextToSpeech(context) { result -> handler.post {
                if (!closed) { ready = result == TextToSpeech.SUCCESS; if (ready && state == "Đang chuẩn bị…") chooseVoice() else if (!ready) fail("Chưa khởi tạo được dịch vụ đọc văn bản.") }
            } }
        } else if (ready) chooseVoice()
    }
    private fun chooseVoice() {
        val locale = Locale.forLanguageTag(guide.languageCode)
        val voices = engine?.voices.orEmpty().filter { it.locale.language == locale.language }
        val voice = voices.firstOrNull { !it.isNetworkConnectionRequired } ?: voices.firstOrNull()
        if (voice == null) { fail("Thiết bị chưa có giọng ${guide.languageCode}. Hãy cài giọng tương ứng; text vẫn dùng được."); return }
        engine?.voice = voice; voiceName = voice.name; speak(0)
    }
    private fun speak(index: Int, from: Int = 0) {
        if (closed || !ready) return
        if (!acquire()) { fail("Thiết bị đang ưu tiên âm thanh khác."); return }
        generation++; engine?.stop(); val token = generation.toString()
        position = index; offset = from; state = "Đang chuẩn bị…"
        engine?.setSpeechRate(rate)
        engine?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) { handler.post { if (!closed && id == generation.toString()) begin() } }
            override fun onDone(id: String?) { handler.post {
                if (!closed && id == generation.toString()) {
                    offset = 0
                    if (index + 1 < chunks.size) speak(index + 1) else { state = "Đã nghe hết"; manager.abandonAudioFocusRequest(focus) }
                }
            } }
            @Deprecated("Platform callback") override fun onError(id: String?) { handler.post { if (!closed && id == generation.toString()) fail("Giọng đọc gặp lỗi. Kiểm tra mạng/giọng đã cài và thử lại.") } }
            override fun onRangeStart(id: String?, start: Int, end: Int, frame: Int) { handler.post { if (id == generation.toString()) offset = from + start } }
        })
        handler.removeCallbacks(timeout); handler.postDelayed(timeout, 12_000)
        val result = engine?.speak(chunks[index].substring(from.coerceIn(0, chunks[index].length)), TextToSpeech.QUEUE_FLUSH, null, token)
        if (result == TextToSpeech.ERROR) fail("Không thể phát bằng giọng đã chọn.")
    }
    fun pause() {
        if (state !in listOf("Đang phát", "Đang chuẩn bị…")) return
        generation++; engine?.stop(); handler.removeCallbacks(timeout)
        runCatching { player?.pause(); position = player?.currentPosition?.div(1000) ?: position }
        state = "Tạm dừng"; manager.abandonAudioFocusRequest(focus)
    }
    fun stop() {
        generation++; engine?.stop(); player?.release(); player = null; handler.removeCallbacks(timeout)
        state = "Sẵn sàng nghe"; position = 0; total = 0; offset = 0; manager.abandonAudioFocusRequest(focus)
    }
    fun seek(value: Int) {
        if (ttsSource) {
            val active = state in listOf("Đang phát", "Đang chuẩn bị…"); generation++; engine?.stop(); offset = 0
            position = value.coerceIn(0, (chunks.size - 1).coerceAtLeast(0))
            if (active) speak(position) else state = "Tạm dừng"
        } else runCatching { player?.seekTo((value * 1000).coerceIn(0, total * 1000)); position = value.coerceIn(0, total) }
    }
    fun speed(value: Float) {
        rate = value
        if (!ttsSource) runCatching { player?.playbackParams = player?.playbackParams?.setSpeed(value) ?: return }
        else if (state in listOf("Đang phát", "Đang chuẩn bị…")) speak(position, offset)
    }
    fun tick() { if (!ttsSource && state == "Đang phát") runCatching { position = (player?.currentPosition ?: 0) / 1000; if (position > 0) begin() } }
    private fun fail(message: String) { stop(); state = message }
    fun close() { if (!closed) { stop(); closed = true; engine?.shutdown(); engine = null; lifecycle?.removeObserver(observer); handler.removeCallbacksAndMessages(null) } }
}
