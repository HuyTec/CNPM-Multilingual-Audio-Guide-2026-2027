package vn.hvp.travelvoice.platform.audio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import java.util.UUID

/** Foreground demo audio; count once per actual start, never on prepare, pause, or resume. */
class DemoNarrator(context: Context, private val lifecycle: Lifecycle, private val audioUrl: String,
                   private val onStarted: (String) -> Unit) {
    var state by mutableStateOf("Sẵn sàng"); private set
    var positionMs by mutableIntStateOf(0); private set
    var durationMs by mutableIntStateOf(0); private set
    private val appContext = context.applicationContext
    private val audioManager = appContext.getSystemService(AudioManager::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private val attributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()
    private val focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).setAudioAttributes(attributes)
        .setOnAudioFocusChangeListener({ change -> if (change < 0) pause() }, handler).build()
    private var player: MediaPlayer? = null
    private var counted = false
    private var closed = false
    private val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_STOP) pause() }
    private val noisy = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) { if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) pause() }
    }

    init {
        lifecycle.addObserver(observer)
        ContextCompat.registerReceiver(appContext, noisy, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY), ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    fun play() {
        if (closed || state == "Đang tải audio…" || state == "Đang phát") return
        if (state == "Tạm dừng") { start(); return }
        player?.release(); player = null; counted = false
        positionMs = 0; durationMs = 0
        state = "Đang tải audio…"
        try {
            val created = MediaPlayer()
            player = created
            created.apply {
                setAudioAttributes(attributes)
                setDataSource(audioUrl)
                setOnPreparedListener {
                    if (!closed && player === it) {
                        durationMs = it.duration
                        this@DemoNarrator.start()
                    }
                }
                setOnCompletionListener { if (!closed) { positionMs = durationMs; state = "Đã nghe hết"; audioManager.abandonAudioFocusRequest(focus) } }
                setOnErrorListener { _, _, _ -> if (!closed) fail(); true }
                prepareAsync()
            }
        } catch (_: Exception) { fail() }
    }

    private fun start() {
        if (closed) return
        if (!lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) { state = "Tạm dừng"; return }
        if (audioManager.requestAudioFocus(focus) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            state = "Không thể phát: thiết bị đang ưu tiên âm thanh khác. Hãy thử lại."; return
        }
        try {
            player?.start()
            state = "Đang phát"
        } catch (_: Exception) { fail() }
    }

    fun pause() {
        if (state != "Đang phát" || closed) return
        runCatching { player?.pause(); positionMs = player?.currentPosition ?: 0 }
        state = "Tạm dừng"
        audioManager.abandonAudioFocusRequest(focus)
    }

    fun updatePosition() {
        if (state != "Đang phát") return
        runCatching {
            positionMs = player?.currentPosition ?: 0
            // start() is asynchronous: wait for observed playback, not its immediate isPlaying value.
            if (!counted && positionMs > 0) { counted = true; onStarted(UUID.randomUUID().toString()) }
        }.onFailure { fail() }
    }

    private fun fail() {
        player?.release(); player = null
        audioManager.abandonAudioFocusRequest(focus)
        state = "Không thể phát audio. Kiểm tra kết nối rồi thử lại."
    }

    fun close() {
        if (closed) return
        closed = true
        lifecycle.removeObserver(observer)
        appContext.unregisterReceiver(noisy)
        player?.release(); player = null
        audioManager.abandonAudioFocusRequest(focus)
        handler.removeCallbacksAndMessages(null)
    }
}
