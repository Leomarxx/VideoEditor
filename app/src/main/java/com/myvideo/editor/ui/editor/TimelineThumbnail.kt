package com.myvideo.editor.ui.editor

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.myvideo.editor.engine.VideoEngine

/**
 * NexClip 时间轴缩略图
 * 显示视频帧缩略图
 */
@Composable
fun TimelineThumbnail(
    uri: Uri,
    durationMs: Long,
    clipWidthDp: Int,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var thumbnails by remember { mutableStateOf<List<Bitmap?>>(emptyList()) }

    LaunchedEffect(uri) {
        val engine = VideoEngine(context)
        val count = (clipWidthDp / 30).coerceIn(3, 20)
        thumbnails = engine.extractThumbnails(uri, count)
    }

    Row(modifier = modifier.height(30.dp)) {
        thumbnails.forEach { bitmap ->
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                        .clip(RoundedCornerShape(1.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(modifier = Modifier.weight(1f).fillMaxHeight()
                    .background(Color(0xFF1A1A1A)))
            }
        }
    }
}
