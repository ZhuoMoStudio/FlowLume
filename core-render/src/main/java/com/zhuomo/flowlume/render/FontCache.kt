package com.zhuomo.flowlume.render

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.utils.Disposable

/** 运行时位图字体缓存（gdx-freetype），键 = 字号|描边宽 */
class FontCache(fontFile: FileHandle) : Disposable {

    private val generator = FreeTypeFontGenerator(fontFile)
    private val fonts = HashMap<String, BitmapFont>()

    fun get(sizePx: Int, borderPx: Int = 0): BitmapFont {
        val key = "$sizePx|$borderPx"
        return fonts.getOrPut(key) {
            val param = FreeTypeFontGenerator.FreeTypeFontParameter().apply {
                size = sizePx.coerceIn(12, 400)
                if (borderPx > 0) {
                    borderWidth = borderPx.toFloat()
                    borderColor = Color.BLACK
                }
                minFilter = Texture.TextureFilter.Linear
                magFilter = Texture.TextureFilter.Linear
                genMipMaps = true
            }
            generator.generateFont(param)
        }
    }

    override fun dispose() {
        fonts.values.forEach { it.dispose() }
        fonts.clear()
        generator.dispose()
    }
}
