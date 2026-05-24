package com.myvideo.editor.engine

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.*
import com.myvideo.editor.ui.editor.EditorViewModel

/**
 * NexClip 播放器Compose集成
 * 管理ExoPlayer生命周期，同步EditorViewModel状态
 */
@Composable
fun rememberVideoPlayer(context: Context, vm: EditorViewModel): VideoPlayerManager? {
    val playerManager = remember { VideoPlayerManager(context) }

    DisposableEffect(Unit) {
        playerManager.init()
        playerManager.onPositionUpdate = { ms ->
            vm.playerCurrentMs = ms
        }
        playerManager.onPlaybackStateChange = { state ->
            vm.playerReady = state == android.media.session.PlaybackState.STATE_PLAYING ||
                    state == 3 // ExoPlayer STATE_READY
            vm.playerDurationMs = playerManager.getDuration()
        }

        onDispose {
            playerManager.release()
        }
    }

    // 自动加载第一个视频
    LaunchedEffect(vm.videoUris) {
        val firstUri = vm.videoUris.values.firstOrNull()
        if (firstUri != null) {
            playerManager.loadVideo(Uri.parse(firstUri))
            vm.playerReady = true
            vm.playerDurationMs = playerManager.getDuration()
        }
    }

    return playerManager
}
