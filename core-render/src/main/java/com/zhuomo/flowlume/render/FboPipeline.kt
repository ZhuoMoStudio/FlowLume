package com.zhuomo.flowlume.render

import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.utils.Disposable

/**
 * 多级 FBO 管线：流体基底 + Kawase 模糊金字塔 + 合成目标。
 * 性能模式可降低金字塔层数。
 */
class FboPipeline : Disposable {

    private var base: FrameBuffer? = null
    private var comp: FrameBuffer? = null
    private val blur = ArrayList<FrameBuffer>()
    private var levels = 0

    fun resize(w: Int, h: Int, blurLevels: Int) {
        dispose()
        levels = blurLevels.coerceIn(2, 5)
        base = FrameBuffer(Pixmap.Format.RGBA8888, w, h, false)
        repeat(levels) { blur.add(FrameBuffer(Pixmap.Format.RGBA8888, w, h, false)) }
        comp = FrameBuffer(Pixmap.Format.RGBA8888, w, h, false)
    }

    fun beginBase() { base!!.begin() }
    fun endBase() { base!!.end() }
    fun baseTexture(): Texture = base!!.colorBufferTexture

    fun blurCount(): Int = blur.size
    fun beginBlur(i: Int) { blur[i].begin() }
    fun endBlur(i: Int) { blur[i].end() }
    fun blurTexture(i: Int): Texture = blur[i].colorBufferTexture

    /** 光晕源：取金字塔高层级（较模糊的一级） */
    fun glowTexture(): Texture = blur[minOf(3, blur.size - 1)].colorBufferTexture

    fun beginComp() { comp!!.begin() }
    fun endComp() { comp!!.end() }
    fun compTexture(): Texture = comp!!.colorBufferTexture

    override fun dispose() {
        base?.dispose(); base = null
        blur.forEach { it.dispose() }; blur.clear()
        comp?.dispose(); comp = null
    }
}
