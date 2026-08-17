package com.adzannotif.platform.audio

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.media3.common.AudioAttributes as Media3AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.adzannotif.R
import com.adzannotif.domain.model.AdhanVoice
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Plays a configured, real audio source and owns its lifecycle.
 *
 * A missing configured resource is reported as unavailable. It is never replaced
 * with an unrelated system alarm tone. The system alarm tone is only used when the
 * user explicitly selects [AdhanVoice.SYSTEM_DEFAULT].
 */
@Singleton
class AdhanAudioPlayer @Inject constructor(
    @ApplicationContext private val context: Context,
) : AudioGateway {
    private val playbackScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var exoPlayer: ExoPlayer? = null
    private var mediaPlayer: MediaPlayer? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                runCatching { exoPlayer?.play() }
                runCatching {
                    if (mediaPlayer?.isPlaying != true) mediaPlayer?.start()
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                runCatching { exoPlayer?.pause() }
                runCatching {
                    if (mediaPlayer?.isPlaying == true) mediaPlayer?.pause()
                }
            }
            AudioManager.AUDIOFOCUS_LOSS -> stop()
        }
    }

    companion object {
        private const val TAG = "AdhanAudioPlayer"
    }

    /**
     * Starts the configured audio source. A custom URI or bundled raw resource is
     * passed through unchanged so the audio that plays is the audio the user chose.
     */
    override fun playAdhan(
        voice: AdhanVoice,
        customUriString: String?,
        onCompletion: (() -> Unit)?,
    ) {
        playbackScope.launch {
            stop()

            val audioUri = resolveAudioUri(voice, customUriString)
            if (audioUri == null) {
                Log.e(TAG, "No playable audio source for voice=${voice.name}")
                onCompletion?.invoke()
                return@launch
            }

            if (!requestAudioFocus()) {
                Log.w(TAG, "Audio focus was not granted for voice=${voice.name}")
                onCompletion?.invoke()
                return@launch
            }
            acquireWakeLock()
            try {
                val audioAttributes = Media3AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_ALARM)
                    .build()

                val player = ExoPlayer.Builder(context).build().apply {
                    // Media3 only supports automatic focus handling for MEDIA/GAME.
                    // Alarm focus is requested explicitly below so USAGE_ALARM remains
                    // correct without throwing before playback starts.
                    setAudioAttributes(audioAttributes, false)
                    setHandleAudioBecomingNoisy(true)
                    setWakeMode(C.WAKE_MODE_LOCAL)
                    setMediaItem(MediaItem.fromUri(audioUri))

                    addListener(object : Player.Listener {
                        override fun onPlaybackStateChanged(playbackState: Int) {
                            if (playbackState == Player.STATE_ENDED) {
                                Log.d(TAG, "Adhan playback ended normally")
                                stop()
                                onCompletion?.invoke()
                            }
                        }

                        override fun onPlayerError(error: PlaybackException) {
                            Log.w(TAG, "Media3 could not play the configured source; trying the same source with MediaPlayer", error)
                            startFallback(audioUri, onCompletion)
                        }
                    })
                }

                exoPlayer = player
                player.prepare()
                player.play()
            } catch (error: Exception) {
                Log.e(TAG, "Media3 failed for the configured audio source", error)
                startFallback(audioUri, onCompletion)
            }
        }
    }

    private fun startFallback(audioUri: Uri, onCompletion: (() -> Unit)?) {
        stop()
        if (!requestAudioFocus()) {
            onCompletion?.invoke()
            return
        }
        acquireWakeLock()
        fallbackPlayMediaPlayer(audioUri, onCompletion)
    }

    private fun resolveAudioUri(voice: AdhanVoice, customUriString: String?): Uri? {
        if (!customUriString.isNullOrBlank()) {
            val customUri = runCatching { Uri.parse(customUriString) }.getOrNull()
            if (customUri?.scheme.isNullOrBlank()) {
                Log.e(TAG, "Custom audio URI has no scheme")
                return null
            }
            return customUri
        }

        if (voice == AdhanVoice.SYSTEM_DEFAULT) {
            return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        }

        val rawResId = when (voice) {
            AdhanVoice.MAKKAH -> R.raw.adhan_makkah
            AdhanVoice.MADINAH -> R.raw.adhan_madinah
            AdhanVoice.AL_AQSA -> R.raw.adhan_alaqsa
            AdhanVoice.EGYPT -> R.raw.adhan_egypt
            AdhanVoice.KUWAIT -> R.raw.adhan_kuwait
            AdhanVoice.FAJR_SPECIAL -> R.raw.adhan_fajr
            AdhanVoice.SYSTEM_DEFAULT -> return null
        }

        return Uri.parse("android.resource://${context.packageName}/$rawResId")
    }

    private fun fallbackPlayMediaPlayer(
        audioUri: Uri,
        onCompletion: (() -> Unit)?,
    ) {
        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, audioUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .build(),
                )
                isLooping = false
                setOnCompletionListener {
                    Log.d(TAG, "MediaPlayer playback ended normally")
                    stop()
                    onCompletion?.invoke()
                }
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaPlayer could not play the configured source: what=$what extra=$extra")
                    stop()
                    onCompletion?.invoke()
                    true
                }
                prepare()
                start()
            }
        } catch (error: Exception) {
            Log.e(TAG, "MediaPlayer failed for the configured audio source", error)
            stop()
            onCompletion?.invoke()
        }
    }

    private fun requestAudioFocus(): Boolean {
        val manager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return false
        audioManager = manager
        val attributes = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .setUsage(AudioAttributes.USAGE_ALARM)
            .build()

        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(attributes)
                .setAcceptsDelayedFocusGain(false)
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
                .build()
            audioFocusRequest = request
            manager.requestAudioFocus(request)
        } else {
            @Suppress("DEPRECATION")
            manager.requestAudioFocus(
                audioFocusChangeListener,
                AudioManager.STREAM_ALARM,
                AudioManager.AUDIOFOCUS_GAIN,
            )
        }

        if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            abandonAudioFocus()
            return false
        }
        return true
    }

    private fun abandonAudioFocus() {
        val manager = audioManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { manager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            manager.abandonAudioFocus(audioFocusChangeListener)
        }
        audioFocusRequest = null
        audioManager = null
    }

    @SuppressLint("WakelockTimeout")
    private fun acquireWakeLock() {
        try {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "AdzanNotif:AudioWakeLock",
            ).apply {
                setReferenceCounted(false)
                // Playback owns the lock. It is released by completion, error,
                // explicit stop, or the foreground service's onDestroy.
                acquire()
            }
        } catch (error: Exception) {
            Log.w(TAG, "Could not acquire the audio wake lock", error)
        }
    }

    /** Stops playback and the wake lock without substituting another source. */
    override fun stop() {
        try {
            exoPlayer?.let { player ->
                player.stop()
                player.clearMediaItems()
                player.release()
            }
            exoPlayer = null
        } catch (error: Exception) {
            Log.w(TAG, "Could not stop Media3 playback", error)
        }

        try {
            mediaPlayer?.let { player ->
                if (player.isPlaying) player.stop()
                player.reset()
                player.release()
            }
            mediaPlayer = null
        } catch (error: Exception) {
            Log.w(TAG, "Could not stop MediaPlayer playback", error)
        }

        try {
            wakeLock?.let { lock ->
                if (lock.isHeld) lock.release()
            }
            wakeLock = null
        } catch (error: Exception) {
            Log.w(TAG, "Could not release the audio wake lock", error)
        }

        abandonAudioFocus()
    }
}
