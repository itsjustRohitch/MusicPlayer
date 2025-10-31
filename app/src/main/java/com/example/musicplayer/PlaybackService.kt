package com.example.musicplayer

import android.content.Intent
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this).build()
        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player!!
        if (!player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
    enum class RepeatMode(val exoMode: Int) {
        OFF(Player.REPEAT_MODE_OFF),
        ONE(Player.REPEAT_MODE_ONE),
        ALL(Player.REPEAT_MODE_ALL);

        companion object {
            fun fromExoMode(mode: Int): RepeatMode = when (mode) {
                Player.REPEAT_MODE_ONE -> ONE
                Player.REPEAT_MODE_ALL -> ALL
                else -> OFF
            }
        }
    }

}
