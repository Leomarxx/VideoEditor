package com.myvideo.editor.engine

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

/**
 * NexClip 视频播放管理器
 * ExoPlayer预览播放+时间轴同步
 */
class VideoPlayerManager(private val context: Context) {

    private var player: ExoPlayer? = null
    var currentPositionMs: Long = 0L
    var durationMs: Long = 0L
    var isPlaying: Boolean = false
        private set

    var onPositionUpdate: ((Long) -> Unit)? = null
    var onPlaybackStateChange: ((Int) -> Unit)? = null

    fun init() {
        player = ExoPlayer.Builder(context).build().apply {
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    onPlaybackStateChange?.invoke(state)
                    if (state == Player.STATE_READY) {
                        durationMs = duration
                    }
                }
                override fun onIsPlayingChanged(playing: Boolean) {
                    isPlaying = playing
                }
            })
        }
    }

    fun loadVideo(uri: Uri) {
        val mediaItem = MediaItem.fromUri(uri)
        player?.setMediaItem(mediaItem)
        player?.prepare()
    }

    fun play() { player?.play() }
    fun pause() { player?.pause() }

    fun togglePlay() {
        if (isPlaying) pause() else play()
    }

    fun seekTo(ms: Long) {
        player?.seekTo(ms)
        currentPositionMs = ms
    }

    fun stepForward() {
        val newPos = (currentPositionMs + 33).coerceAtMost(durationMs)
        seekTo(newPos)
    }

    fun stepBackward() {
        val newPos = (currentPositionMs - 33).coerceAtLeast(0)
        seekTo(newPos)
    }

    fun getCurrentPosition(): Long = player?.currentPosition ?: 0L

    fun getDuration(): Long = player?.duration ?: 0L

    fun release() {
        player?.release()
        player = null
    }

    fun getPlayer(): ExoPlayer? = player
}
