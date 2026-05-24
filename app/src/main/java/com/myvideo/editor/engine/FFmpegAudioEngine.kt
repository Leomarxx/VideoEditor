package com.myvideo.editor.engine

class FFmpegAudioEngine(private val engine: FFmpegRenderEngine) {

    fun applyDenoise(input: String, output: String, cb: FFmpegRenderEngine.RenderCallback) {
        engine.run("-i $input -af \"afftdn=nf=-25\" -c:v copy -c:a aac -b:a 128k -y $output", cb)
    }

    fun applyFade(input: String, output: String, fadeInMs: Long, fadeOutMs: Long, cb: FFmpegRenderEngine.RenderCallback) {
        val fi = fadeInMs / 1000f
        engine.run("-i $input -af \"afade=t=in:d=$fi\" -c:v copy -c:a aac -y $output", cb)
    }

    fun applyVolume(input: String, output: String, volume: Float, cb: FFmpegRenderEngine.RenderCallback) {
        engine.run("-i $input -af \"volume=${volume}\" -c:v copy -c:a aac -y $output", cb)
    }

    fun extractAudio(input: String, output: String, cb: FFmpegRenderEngine.RenderCallback) {
        engine.run("-i $input -vn -c:a aac -b:a 192k -y $output", cb)
    }

    fun replaceAudio(video: String, audio: String, output: String, cb: FFmpegRenderEngine.RenderCallback) {
        engine.run("-i $video -i $audio -c:v copy -c:a aac -map 0:v:0 -map 1:a:0 -shortest -y $output", cb)
    }

    fun mixAudio(video: String, audio: String, output: String, videoVol: Float, audioVol: Float, cb: FFmpegRenderEngine.RenderCallback) {
        engine.run("-i $video -i $audio -filter_complex \"[0:a]volume=$videoVol[a0];[1:a]volume=$audioVol[a1];[a0][a1]amix=inputs=2:duration=first[out]\" -map 0:v -map \"[out]\" -c:v copy -c:a aac -y $output", cb)
    }

    fun applySpeed(input: String, output: String, speed: Float, cb: FFmpegRenderEngine.RenderCallback) {
        val vFilter = "setpts=${1.0/speed}*PTS"
        val aFilter = "atempo=${speed.coerceIn(0.5f, 2.0f)}"
        engine.run("-i $input -vf $vFilter -af $aFilter -c:v libx264 -preset fast -y $output", cb)
    }

    fun applyReverse(input: String, output: String, cb: FFmpegRenderEngine.RenderCallback) {
        engine.run("-i $input -vf reverse -af areverse -c:v libx264 -preset fast -y $output", cb)
    }

    fun addBgm(video: String, bgm: String, output: String, bgmVolume: Float, cb: FFmpegRenderEngine.RenderCallback) {
        engine.run("-i $video -i $bgm -filter_complex \"[1:a]volume=$bgmVolume,aloop=loop=-1:size=2e+09[bgm];[0:a][bgm]amix=inputs=2:duration=first[out]\" -map 0:v -map \"[out]\" -c:v copy -c:a aac -shortest -y $output", cb)
    }
}
