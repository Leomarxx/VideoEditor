package com.myvideo.editor.engine

class FFmpegTransitionEngine(private val engine: FFmpegRenderEngine) {

    fun apply(input1: String, input2: String, output: String,
              type: String, durationMs: Long, cb: FFmpegRenderEngine.RenderCallback) {
        val dur = durationMs / 1000f
        val xfade = when (type) {
            "淡入淡出" -> "fade"
            "滑动左" -> "slideleft"
            "滑动右" -> "slideright"
            "擦除" -> "wipeleft"
            "缩放" -> "zoomin"
            "旋转" -> "circleopen"
            "百叶窗" -> "wipeup"
            "棋盘格" -> "radial"
            "径向" -> "circlecrop"
            "交叉溶解" -> "dissolve"
            "闪白" -> "fadeblack"
            "抖动" -> "squeezeh"
            else -> "fade"
        }
        engine.run("-i $input1 -i $input2 -filter_complex \"[0:v][1:v]xfade=transition=$xfade:duration=$dur:offset=0\" -c:v libx264 -preset fast -y $output", cb)
    }

    fun applyPiP(main: String, pip: String, output: String,
                 x: Int, y: Int, w: Int, h: Int, cb: FFmpegRenderEngine.RenderCallback) {
        engine.run("-i $main -i $pip -filter_complex \"[1:v]scale=$w:$h[pip];[0:v][pip]overlay=$x:$y\" -c:v libx264 -preset fast -c:a copy -y $output", cb)
    }

    fun applyChromaKey(input: String, output: String, cb: FFmpegRenderEngine.RenderCallback) {
        engine.run("-i $input -vf \"chromakey=0x00FF00:0.15:0.1\" -c:v libx264 -preset fast -c:a copy -y $output", cb)
    }

    fun applyMotionBlur(input: String, output: String, strength: Float, cb: FFmpegRenderEngine.RenderCallback) {
        val frames = (strength / 100f * 5).toInt().coerceIn(1, 5)
        engine.run("-i $input -vf \"tmix=frames=$frames:weights='1'\" -c:v libx264 -preset fast -c:a copy -y $output", cb)
    }

    fun applyStabilize(input: String, output: String, cb: FFmpegRenderEngine.RenderCallback) {
        engine.run("-i $input -vf \"vidstabdetect=shakiness=5:accuracy=15\" -f null -", cb)
    }

    fun addTextOverlay(input: String, output: String, text: String,
                       x: Int, y: Int, fontSize: Int, color: String, cb: FFmpegRenderEngine.RenderCallback) {
        val safe = text.replace("'", "\\'").replace(":", "\\:")
        engine.run("-i $input -vf \"drawtext=text='$safe':x=$x:y=$y:fontsize=$fontSize:fontcolor=$color\" -c:v libx264 -preset fast -c:a copy -y $output", cb)
    }
}
