package vn.hvp.travelvoice.platform.audio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.CancellationException
import vn.hvp.travelvoice.MainActivity
import vn.hvp.travelvoice.R
import vn.hvp.travelvoice.data.remote.PlaybackOutbox
import vn.hvp.travelvoice.data.remote.ServerGuide

/** Process-local UI bridge. The service, rather than a Composable, owns the active narrator. */
object TravelPlayback {
    var guide by mutableStateOf<ServerGuide?>(null); internal set
    var narrator by mutableStateOf<SharedNarrator?>(null); internal set
    var appVisible = false; private set
    internal var service: TravelPlaybackService? = null

    fun visible(value: Boolean) { appVisible = value; service?.visibilityChanged() }
    fun play(context: Context, guide: ServerGuide, base: String, rate: Float) {
        check(!PackagePlaybackGate.installing) { "Đang cài gói; hãy chờ hoàn tất." }
        val intent = Intent(context, TravelPlaybackService::class.java).setAction("PLAY")
            .putExtra("content", guide.contentId).putExtra("location", guide.locationId)
            .putExtra("name", guide.locationName).putExtra("language", guide.languageCode)
            .putExtra("type", guide.scriptType).putExtra("text", guide.scriptText)
            .putExtra("audio", guide.audioUrl).putExtra("base", base).putExtra("rate", rate).putExtra("offline", guide.offline)
        ContextCompat.startForegroundService(context, intent)
    }
    fun toggle() { if (PackagePlaybackGate.installing) return; narrator?.let { if (it.state in listOf("Đang phát", "Đang chuẩn bị…")) it.pause() else it.play() }; service?.visibilityChanged() }
    fun stop(context: Context) { context.stopService(Intent(context, TravelPlaybackService::class.java)) }
}

/** Promoted only for background listening; returning to the visible Activity removes its notification. */
class TravelPlaybackService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var media: MediaSession
    private var promoted = false
    private var lastState = ""
    private var lastPosition = -1
    private var server = ""
    private val noisy = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) { TravelPlayback.narrator?.pause(); visibilityChanged() }
    }
    private val ticker = object : Runnable {
        override fun run() {
            TravelPlayback.narrator?.tick()
            val state = TravelPlayback.narrator?.state.orEmpty()
            if (state != lastState) { lastState = state; visibilityChanged() }
            else if (TravelPlayback.narrator?.position != lastPosition) updateMediaState()
            lastPosition = TravelPlayback.narrator?.position ?: -1
            handler.postDelayed(this, 300)
        }
    }
    override fun onCreate() {
        super.onCreate()
        TravelPlayback.service = this
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, uiString(R.string.q1_notification_channel), NotificationManager.IMPORTANCE_LOW))
        media = MediaSession(this, "TravelQ1").apply {
            setCallback(object : MediaSession.Callback() {
                override fun onPlay() { TravelPlayback.narrator?.play(); visibilityChanged() }
                override fun onPause() { TravelPlayback.narrator?.pause(); visibilityChanged() }
                override fun onStop() { stopSelf() }
                override fun onSeekTo(pos: Long) { TravelPlayback.narrator?.seek((pos / 1000).toInt()) }
            })
            isActive = true
        }
        ContextCompat.registerReceiver(this, noisy, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY), ContextCompat.RECEIVER_NOT_EXPORTED)
        handler.post(ticker)
        scope.launch {
            while (isActive) {
                if (server.isNotBlank() && TravelPlayback.narrator?.state in listOf("Đang phát", "Đang chuẩn bị…")) {
                    try {
                        vn.hvp.travelvoice.data.remote.VisitorApi.heartbeat(applicationContext, server)
                        PlaybackOutbox.flush(applicationContext, server)
                    } catch (cancelled: CancellationException) { throw cancelled }
                    catch (_: Exception) { /* Keep queued events; retry while background playback continues. */ }
                }
                delay(60_000)
            }
        }
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // A startForegroundService request must be acknowledged even if the UI is still visible.
        startForeground(NOTIFICATION, notification()); promoted = true
        when (intent?.action) {
            "STOP" -> { stopSelf(); return START_NOT_STICKY }
            "TOGGLE" -> TravelPlayback.toggle()
            "PLAY" -> {
                server = intent.getStringExtra("base").orEmpty()
                val id = intent.getStringExtra("content") ?: run { stopSelf(); return START_NOT_STICKY }
                if (TravelPlayback.guide?.contentId != id) {
                    TravelPlayback.narrator?.close()
                    val guide = ServerGuide(id, intent.getStringExtra("name").orEmpty(), intent.getStringExtra("language").orEmpty(),
                        intent.getStringExtra("type").orEmpty(), intent.getStringExtra("text").orEmpty(), intent.getStringExtra("audio"), intent.getStringExtra("location").orEmpty(), intent.getBooleanExtra("offline", false))
                    TravelPlayback.guide = guide
                    val base = intent.getStringExtra("base").orEmpty()
                    TravelPlayback.narrator = SharedNarrator(applicationContext, null, guide) { event ->
                        scope.launch { runCatching { PlaybackOutbox.enqueueAndFlush(applicationContext, base, event, guide.contentId,
                            guide.offline || !vn.hvp.travelvoice.data.remote.VisitorApi.isOnline(applicationContext)) } }
                    }
                }
                TravelPlayback.narrator?.speed(intent.getFloatExtra("rate", 1f))
                TravelPlayback.narrator?.play()
            }
        }
        visibilityChanged()
        return START_NOT_STICKY
    }
    fun visibilityChanged() {
        val narrator = TravelPlayback.narrator
        val active = narrator?.state in listOf("Đang phát", "Đang chuẩn bị…", "Tạm dừng")
        updateMediaState()
        media.setMetadata(MediaMetadata.Builder().putString(MediaMetadata.METADATA_KEY_TITLE, TravelPlayback.guide?.locationName ?: uiString(R.string.app_name))
            .putString(MediaMetadata.METADATA_KEY_ARTIST, uiString(R.string.app_name)).putLong(MediaMetadata.METADATA_KEY_DURATION, (narrator?.total ?: 0) * 1000L).build())
        if (TravelPlayback.appVisible || !active) {
            if (promoted) { stopForeground(STOP_FOREGROUND_REMOVE); promoted = false }
        } else {
            startForeground(NOTIFICATION, notification()); promoted = true
        }
    }
    private fun uiString(id: Int): String {
        val language = getSharedPreferences("travelq1-settings",Context.MODE_PRIVATE).getString("app-language","vi")!!
        val configuration = android.content.res.Configuration(resources.configuration).apply { setLocale(java.util.Locale.forLanguageTag(language)) }
        return createConfigurationContext(configuration).getString(id)
    }
    private fun updateMediaState() {
        val narrator = TravelPlayback.narrator
        val playing = narrator?.state == "Đang phát"
        val actions = PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE or PlaybackState.ACTION_STOP
        media.setPlaybackState(PlaybackState.Builder().setActions(actions or if ((narrator?.total ?: 0) > 0) PlaybackState.ACTION_SEEK_TO else 0L)
            .setState(if (playing) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED,
                if ((narrator?.total ?: 0) > 0) narrator!!.position * 1000L else PlaybackState.PLAYBACK_POSITION_UNKNOWN, narrator?.rate ?: 1f).build())

    }
    private fun notification(): Notification {
        val playing = TravelPlayback.narrator?.state in listOf("Đang phát", "Đang chuẩn bị…")
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        fun action(name: String, code: Int) = PendingIntent.getService(this, code, Intent(this, TravelPlaybackService::class.java).setAction(name), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        return Notification.Builder(this, CHANNEL).setSmallIcon(R.drawable.ic_stat_audio)
            .setContentTitle(TravelPlayback.guide?.locationName ?: uiString(R.string.app_name))
            .setContentText(uiString(if (playing) R.string.q1_listen else R.string.q1_pause))
            .setContentIntent(open).setOnlyAlertOnce(true).setVisibility(Notification.VISIBILITY_PUBLIC)
            .setCategory(Notification.CATEGORY_TRANSPORT).setOngoing(playing)
            .addAction(Notification.Action.Builder(if (playing) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                uiString(if (playing) R.string.q1_pause else R.string.q1_resume), action("TOGGLE", 1)).build())
            .addAction(Notification.Action.Builder(android.R.drawable.ic_menu_close_clear_cancel, uiString(R.string.q1_stop), action("STOP", 2)).build())
            .setStyle(Notification.MediaStyle().setMediaSession(media.sessionToken).setShowActionsInCompactView(0, 1)).build()
    }
    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null); unregisterReceiver(noisy)
        TravelPlayback.narrator?.close(); TravelPlayback.narrator = null; TravelPlayback.guide = null; TravelPlayback.service = null
        media.release(); scope.cancel(); super.onDestroy()
    }
    override fun onBind(intent: Intent?): IBinder? = null
    companion object { private const val CHANNEL = "travelq1-playback"; private const val NOTIFICATION = 41 }
}
