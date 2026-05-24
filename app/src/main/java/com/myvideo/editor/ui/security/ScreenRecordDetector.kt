package com.myvideo.editor.ui.security

import android.app.Activity
import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Build
import android.view.Display

object ScreenRecordDetector {
    fun isScreenRecording(context: Context): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                false
            } else {
                val dm = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
                val displays = dm.displays
                displays.size > 1
            }
        } catch (e: Exception) { false }
    }
}
