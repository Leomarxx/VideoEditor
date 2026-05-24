package com.myvideo.editor.engine

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore

/**
 * NexClip 视频选择器
 * 从系统相册选择视频
 */
object VideoPicker {

    /**
     * 获取视频选择Intent
     */
    fun getPickIntent(): Intent {
        return Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI).apply {
            type = "video/*"
        }
    }

    /**
     * 从返回的Intent中提取URI
     */
    fun extractUri(data: Intent?): Uri? {
        return data?.data
    }

    /**
     * 获取最近视频列表（首页展示用）
     */
    fun getRecentVideos(context: Context, limit: Int = 20): List<Uri> {
        val videos = mutableListOf<Uri>()
        try {
            val projection = arrayOf(MediaStore.Video.Media._ID)
            val sortOrder = "${MediaStore.Video.Media.DATE_MODIFIED} DESC"

            context.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection, null, null, sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                var count = 0
                while (cursor.moveToNext() && count < limit) {
                    val id = cursor.getLong(idCol)
                    val uri = android.content.ContentUris.withAppendedId(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id
                    )
                    videos.add(uri)
                    count++
                }
            }
        } catch (e: Exception) { }
        return videos
    }
}
