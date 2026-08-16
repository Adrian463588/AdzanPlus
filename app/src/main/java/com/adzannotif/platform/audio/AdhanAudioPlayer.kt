package com.adzannotif.platform.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.PowerManager
import android.util.Log
import androidx.media3.common.AudioAttributes as Media3AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.adzannotif.domain.model.AdhanVoice
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Audio player managing Adhan and Takbeer playback using AndroidX Media3 (ExoPlayer)
 * with graceful fallback to Android MediaPlayer, automatic partial wake lock handling,
 * and configurable DND auto-silence timer.
 */
@Singleton
class AdhanAudioPlayer @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private var exoPlayer: ExoPlayer? = null
    private var mediaPlayer: MediaPlayer? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var autoSilenceJob: Job? = null

    companion object {
        private const val TAG = "AdhanAudioPlayer"
        private const val WAKELOCK_TIMEOUT_MS = 15 * 60 * 1000L // 15 minutes max
    }

    /**
     * Plays the specified Adhan voice or custom audio file.
     * Automatically handles wake lock acquisition, auto-silence timer, and audio cleanup.
     */
    fun playAdhan(
        voice: AdhanVoice,
        customUriString: String? = null,
        durationMinutes: Int = 15,
        onCompletion: (() -> Unit)? = null,
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            stop()

            // Acquire partial wake lock to keep CPU active during playback on lockscreen / Doze mode
            acquireWakeLock()

            try {
                // Setup Media3 ExoPlayer with ALARM usage attributes
                val audioAttributes = Media3AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_ALARM)
                    .build()

                val player = ExoPlayer.Builder(context).build().apply {
                    setAudioAttributes(audioAttributes, true)
                    setHandleAudioBecomingNoisy(true)
                    setWakeMode(C.WAKE_MODE_LOCAL)

                    val mediaItem = resolveMediaItem(voice, customUriString)
                    setMediaItem(mediaItem)

                    addListener(object : Player.Listener {
                        override fun onPlaybackStateChanged(playbackState: Int) {
                            if (playbackState == Player.STATE_ENDED) {
                                Log.d(TAG, "ExoPlayer playback ended normally")
                                stop()
                                onCompletion?.invoke()
                            }
                        }

                        override fun onPlayerError(error: PlaybackException) {
                            Log.w(TAG, "ExoPlayer encountered error, attempting MediaPlayer fallback", error)
                            stop()
                            fallbackPlayMediaPlayer(onCompletion)
                        }
                    })

                    prepare()
                    play()
                }

                exoPlayer = player

                // Start auto-silence timer (defaults to dndAutoSilenceMinutes)
                autoSilenceJob = launch {
                    val timeoutMillis = (durationMinutes.coerceAtLeast(1)) * 60 * 1000L
                    delay(timeoutMillis)
                    Log.d(TAG, "Auto-silence timer expired after $durationMinutes minutes")
                    stop()
                    onCompletion?.invoke()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize ExoPlayer, falling back to MediaPlayer", e)
                fallbackPlayMediaPlayer(onCompletion)
            }
        }
    }

    private fun resolveMediaItem(voice: AdhanVoice, customUriString: String?): MediaItem {
        if (!customUriString.isNullOrBlank()) {
            return MediaItem.fromUri(Uri.parse(customUriString))
        }

        // Try raw resource from assets
        val rawResId = context.resources.getIdentifier(voice.rawResName, "raw", context.packageName)
        return if (rawResId != 0) {
            MediaItem.fromUri("android.resource://${context.packageName}/$rawResId")
        } else {
            // Fallback to system default alarm tone
            val defaultUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM)
                ?: android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
            MediaItem.fromUri(defaultUri)
        }
    }

    private fun fallbackPlayMediaPlayer(onCompletion: (() -> Unit)?) {
        try {
            val alertUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM)
                ?: android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)

            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, alertUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .build()
                )
                isLooping = false
                setOnCompletionListener {
                    Log.d(TAG, "MediaPlayer fallback finished playing")
                    stop()
                    onCompletion?.invoke()
                }
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaPlayer fallback error what=$what extra=$extra")
                    stop()
                    true
                }
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fallback MediaPlayer also failed", e)
            stop()
        }
    }

    private fun acquireWakeLock() {
        try {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AdzanNotif:AudioWakeLock").apply {
                setReferenceCounted(false)
                acquire(WAKELOCK_TIMEOUT_MS)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to acquire partial wake lock", e)
        }
    }

    /**
     * Immediately stops playback, cancels the auto-silence timer, releases the wake lock,
     * and frees media player resources.
     */
    fun stop() {
        autoSilenceJob?.cancel()
        autoSilenceJob = null

        try {
            exoPlayer?.let { player ->
                player.stop()
                player.clearMediaItems()
                player.release()
            }
            exoPlayer = null
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping ExoPlayer", e)
        }

        try {
            mediaPlayer?.let { mp ->
                if (mp.isPlaying) {
                    mp.stop()
                }
                mp.reset()
                mp.release()
            }
            mediaPlayer = null
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping MediaPlayer", e)
        }

        try {
            wakeLock?.let { wl ->
                if (wl.isHeld) {
                    wl.release()
                }
            }
            wakeLock = null
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing WakeLock", e)
        }
    }
}
