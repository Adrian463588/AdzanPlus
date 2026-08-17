package com.adzannotif.platform.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
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
    private var autoSilenceJob: Job? = null

    companion object {
        private const val TAG = "AdhanAudioPlayer"
        private const val WAKELOCK_TIMEOUT_MS = 15 * 60 * 1000L
    }

    /**
     * Starts the configured audio source. A custom URI or bundled raw resource is
     * passed through unchanged so the audio that plays is the audio the user chose.
     */
    override fun playAdhan(
        voice: AdhanVoice,
        customUriString: String?,
        durationMinutes: Int,
        onCompletion: (() -> Unit)?,
    ) {
        playbackScope.launch {
            stop()

            val audioUri = resolveAudioUri(voice, customUriString)
            if (audioUri == null) {
                Log.e(TAG, "No playable audio source for voice=${voice.name}")
                return@launch
            }

            acquireWakeLock()
            val duration = durationMinutes.coerceIn(1, (WAKELOCK_TIMEOUT_MS / 60_000L).toInt())

            try {
                val audioAttributes = Media3AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_ALARM)
                    .build()

                val player = ExoPlayer.Builder(context).build().apply {
                    setAudioAttributes(audioAttributes, true)
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
                            stop()
                            fallbackPlayMediaPlayer(audioUri, duration, onCompletion)
                        }
                    })
                }

                exoPlayer = player
                player.prepare()
                player.play()
                startAutoSilenceTimer(duration, onCompletion)
            } catch (error: Exception) {
                Log.e(TAG, "Media3 failed for the configured audio source", error)
                stop()
                fallbackPlayMediaPlayer(audioUri, duration, onCompletion)
            }
        }
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
        durationMinutes: Int,
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
                    true
                }
                prepare()
                start()
            }
            startAutoSilenceTimer(durationMinutes, onCompletion)
        } catch (error: Exception) {
            Log.e(TAG, "MediaPlayer failed for the configured audio source", error)
            stop()
        }
    }

    private fun startAutoSilenceTimer(
        durationMinutes: Int,
        onCompletion: (() -> Unit)?,
    ) {
        autoSilenceJob?.cancel()
        autoSilenceJob = playbackScope.launch {
            delay(durationMinutes.coerceAtLeast(1) * 60_000L)
            autoSilenceJob = null
            Log.d(TAG, "Auto-silence timer expired after $durationMinutes minutes")
            stop()
            onCompletion?.invoke()
        }
    }

    private fun acquireWakeLock() {
        try {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "AdzanNotif:AudioWakeLock",
            ).apply {
                setReferenceCounted(false)
                acquire(WAKELOCK_TIMEOUT_MS)
            }
        } catch (error: Exception) {
            Log.w(TAG, "Could not acquire the audio wake lock", error)
        }
    }

    /** Stops playback, timers, and the wake lock without substituting another source. */
    override fun stop() {
        autoSilenceJob?.cancel()
        autoSilenceJob = null

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
    }
}
