package com.myvideo.editor.project

import android.content.Context
import com.myvideo.editor.ui.editor.ClipData
import com.myvideo.editor.ui.editor.ClipType
import com.myvideo.editor.ui.editor.EditorViewModel
import com.myvideo.editor.ui.editor.TrackData
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * NexClip 项目保存/加载
 * JSON格式持久化存储
 */
class ProjectManager(private val context: Context) {

    data class ProjectInfo(
        val id: String,
        val name: String,
        val lastModified: Long,
        val clipCount: Int,
        val trackCount: Int
    )

    private val projectsDir: File
        get() = File(context.filesDir, "projects").also { if (!it.exists()) it.mkdirs() }

    /**
     * 保存项目到本地
     */
    fun saveProject(vm: EditorViewModel, name: String = "未命名项目"): String {
        val projectId = "project_${System.currentTimeMillis()}"
        val json = JSONObject().apply {
            put("id", projectId)
            put("name", name)
            put("canvasRatio", vm.canvasRatio)
            put("customWidth", vm.customWidth)
            put("customHeight", vm.customHeight)
            put("pixelsPerSecond", vm.pixelsPerSecond.toDouble())
            put("playheadPosition", vm.playheadPosition.toDouble())
            put("timestamp", System.currentTimeMillis())

            // 轨道
            val tracksArr = JSONArray()
            vm.tracks.forEach { track ->
                tracksArr.put(JSONObject().apply {
                    put("index", track.index)
                    put("name", track.name)
                    put("isVisible", track.isVisible)
                    put("isLocked", track.isLocked)
                    put("isMuted", track.isMuted)
                    put("isSolo", track.isSolo)
                })
            }
            put("tracks", tracksArr)

            // 片段
            val clipsArr = JSONArray()
            vm.clips.forEach { clip ->
                clipsArr.put(JSONObject().apply {
                    put("id", clip.id)
                    put("name", clip.name)
                    put("leftPx", clip.leftPx.toDouble())
                    put("widthPx", clip.widthPx.toDouble())
                    put("trackIndex", clip.trackIndex)
                    put("type", clip.type.name)
                    put("isMuted", clip.isMuted)
                    put("isLocked", clip.isLocked)
                    put("speed", clip.speed.toDouble())
                    put("isReversed", clip.isReversed)
                    val kfArr = JSONArray()
                    clip.keyframes.forEach { kfArr.put(it.toDouble()) }
                    put("keyframes", kfArr)
                })
            }
            put("clips", clipsArr)

            // 视频URI映射
            val urisObj = JSONObject()
            vm.videoUris.forEach { (k, v) -> urisObj.put(k, v) }
            put("videoUris", urisObj)
        }

        val file = File(projectsDir, "$projectId.json")
        file.writeText(json.toString(2))
        return projectId
    }

    /**
     * 加载项目
     */
    fun loadProject(projectId: String, vm: EditorViewModel): Boolean {
        val file = File(projectsDir, "$projectId.json")
        if (!file.exists()) return false

        return try {
            val json = JSONObject(file.readText())

            vm.canvasRatio = json.optString("canvasRatio", "16:9")
            vm.customWidth = json.optString("customWidth", "1920")
            vm.customHeight = json.optString("customHeight", "1080")
            vm.pixelsPerSecond = json.optDouble("pixelsPerSecond", 80.0).toFloat()
            vm.playheadPosition = json.optDouble("playheadPosition", 0.0).toFloat()

            // 轨道
            vm.tracks.clear()
            val tracksArr = json.getJSONArray("tracks")
            for (i in 0 until tracksArr.length()) {
                val t = tracksArr.getJSONObject(i)
                vm.tracks.add(TrackData(
                    index = t.getInt("index"),
                    name = t.getString("name"),
                    isVisible = t.optBoolean("isVisible", true),
                    isLocked = t.optBoolean("isLocked", false),
                    isMuted = t.optBoolean("isMuted", false),
                    isSolo = t.optBoolean("isSolo", false)
                ))
            }

            // 片段
            vm.clips.clear()
            val clipsArr = json.getJSONArray("clips")
            for (i in 0 until clipsArr.length()) {
                val c = clipsArr.getJSONObject(i)
                val kfArr = c.getJSONArray("keyframes")
                val kfs = mutableListOf<Float>()
                for (j in 0 until kfArr.length()) kfs.add(kfArr.getDouble(j).toFloat())
                vm.clips.add(ClipData(
                    id = c.getString("id"),
                    name = c.getString("name"),
                    leftPx = c.getDouble("leftPx").toFloat(),
                    widthPx = c.getDouble("widthPx").toFloat(),
                    trackIndex = c.getInt("trackIndex"),
                    type = try { ClipType.valueOf(c.getString("type")) } catch (e: Exception) { ClipType.Video },
                    isMuted = c.optBoolean("isMuted", false),
                    isLocked = c.optBoolean("isLocked", false),
                    keyframes = kfs,
                    speed = c.optDouble("speed", 1.0).toFloat(),
                    isReversed = c.optBoolean("isReversed", false)
                ))
            }

            // 视频URI
            vm.videoUris.clear()
            val urisObj = json.optJSONObject("videoUris")
            urisObj?.keys()?.forEach { key ->
                vm.videoUris[key] = urisObj.getString(key)
            }

            true
        } catch (e: Exception) { false }
    }

    /**
     * 获取项目列表
     */
    fun getProjectList(): List<ProjectInfo> {
        return try {
            projectsDir.listFiles()?.filter { it.extension == "json" }?.mapNotNull { file ->
                try {
                    val json = JSONObject(file.readText())
                    ProjectInfo(
                        id = json.getString("id"),
                        name = json.optString("name", "未命名"),
                        lastModified = json.optLong("timestamp", 0),
                        clipCount = json.optJSONArray("clips")?.length() ?: 0,
                        trackCount = json.optJSONArray("tracks")?.length() ?: 0
                    )
                } catch (e: Exception) { null }
            }?.sortedByDescending { it.lastModified } ?: emptyList()
        } catch (e: Exception) { emptyList() }
    }

    /**
     * 删除项目
     */
    fun deleteProject(projectId: String): Boolean {
        val file = File(projectsDir, "$projectId.json")
        return if (file.exists()) file.delete() else false
    }

    /**
     * 重命名项目
     */
    fun renameProject(projectId: String, newName: String): Boolean {
        val file = File(projectsDir, "$projectId.json")
        if (!file.exists()) return false
        return try {
            val json = JSONObject(file.readText())
            json.put("name", newName)
            file.writeText(json.toString(2))
            true
        } catch (e: Exception) { false }
    }
}
