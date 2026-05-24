package com.myvideo.editor.engine

class FFmpegFilterEngine(private val engine: FFmpegRenderEngine) {

    fun apply(input: String, output: String, filterName: String, cb: FFmpegRenderEngine.RenderCallback) {
        val vf = when (filterName) {
            "自然" -> "eq=brightness=0.05:contrast=1.1:saturation=1.1"
            "黑白" -> "hue=s=0"
            "复古" -> "colorbalance=rs=0.1:gs=-0.1:bs=-0.1"
            "冷色" -> "colorbalance=bs=0.2:bm=0.1"
            "暖色" -> "colorbalance=rs=0.2:rm=0.1"
            "高对比" -> "eq=contrast=1.5:brightness=-0.1"
            "柔和" -> "eq=contrast=0.8:brightness=0.1"
            "鲜艳" -> "eq=saturation=1.8"
            "怀旧" -> "colorchannelmixer=.393:.769:.189:0:.349:.686:.168:0:.272:.534:.131"
            "胶片" -> "eq=contrast=1.2:brightness=-0.05:saturation=0.9"
            "HDR" -> "eq=contrast=1.3:brightness=0.1:saturation=1.3"
            "电影" -> "eq=contrast=1.15:brightness=-0.05:saturation=0.85"
            "模糊" -> "boxblur=10:10"
            "锐化" -> "unsharp=5:5:2.0"
            "马赛克" -> "scale=iw/10:ih/10,scale=iw*10:ih*10"
            "像素化" -> "scale=iw/8:ih/8,scale=iw*8:ih*8:flags=neighbor"
            "浮雕" -> "convolution=-2 -1 0 -1 1 1 0 1 2"
            "老电影" -> "hue=s=0,noise=alls=40:allf=t+u,vignette=PI/4"
            "故障" -> "rgbashift=rh=5:bv=-5,noise=alls=20"
            "霓虹" -> "edgedetect=low=0.1:high=0.4,negate"
            "油画" -> "bilateral=sigmaS=5:sigmaR=0.1"
            "素描" -> "edgedetect=low=0:high=1,negate"
            "卡通" -> "bilateral=sigmaS=5:sigmaR=0.1,edgedetect=low=0:high=1"
            else -> "eq=brightness=0:contrast=1:saturation=1"
        }
        engine.run("-i $input -vf $vf -c:v libx264 -preset fast -c:a copy -y $output", cb)
    }

    fun applyMultiple(input: String, output: String, filters: List<String>, cb: FFmpegRenderEngine.RenderCallback) {
        val chain = filters.joinToString(",") { name ->
            when (name) {
                "模糊" -> "boxblur=5:5"
                "锐化" -> "unsharp=5:5:1.0"
                "发光" -> "gblur=sigma=10"
                "马赛克" -> "scale=iw/10:ih/10,scale=iw*10:ih*10"
                "黑白" -> "hue=s=0"
                "鲜艳" -> "eq=saturation=1.5"
                else -> "eq=brightness=0:contrast=1"
            }
        }
        engine.run("-i $input -vf $chain -c:v libx264 -preset fast -c:a copy -y $output", cb)
    }

    fun applyVignette(input: String, output: String, strength: Float, cb: FFmpegRenderEngine.RenderCallback) {
        val angle = (Math.PI / 4 * (strength / 100f)).coerceIn(0.1, Math.PI / 2)
        engine.run("-i $input -vf vignette=PI/${(Math.PI/angle).toInt()} -c:v libx264 -preset fast -c:a copy -y $output", cb)
    }

    fun applyFilmGrain(input: String, output: String, grain: Float, cb: FFmpegRenderEngine.RenderCallback) {
        val amount = (grain / 100f * 50).toInt().coerceIn(0, 50)
        engine.run("-i $input -vf noise=alls=$amount:allf=t+u -c:v libx264 -preset fast -c:a copy -y $output", cb)
    }
}
