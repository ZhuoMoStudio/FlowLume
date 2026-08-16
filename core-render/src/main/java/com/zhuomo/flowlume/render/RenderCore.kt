package com.zhuomo.flowlume.render

import android.graphics.Bitmap
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.utils.Disposable
import com.zhuomo.flowlume.audio.FrameData
import com.zhuomo.flowlume.config.ConfigStore
import com.zhuomo.flowlume.config.Mode
import com.zhuomo.flowlume.config.RenderConfig
import com.zhuomo.flowlume.config.RenderMode
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.min

/**
 * 渲染核心（双形态共享）：流体基底 → Kawase 金字塔 → 合成（动效）→ 最终输出。
 * 宿主负责提供 GL 线程与 EGL 上下文；本类仅使用 Gdx.* 静态 API。
 *
 * 线程约定：
 * - create / resize / render / setArt 必须在宿主 GL 线程调用；
 * - pendingArt / pendingConfig / latestFrameData 允许任意线程写入，render() 内消费。
 */
class RenderCore(
    private val mode: Mode,
    private val effects: List<Effect>
) : Disposable {

    private var width = 1
    private var height = 1
    private var time = 0f

    private lateinit var quad: FullscreenQuad
    private lateinit var fbo: FboPipeline
    private lateinit var fluidShader: ShaderProgram
    private lateinit var kawaseShader: ShaderProgram
    private lateinit var compositeShader: ShaderProgram
    private lateinit var finalShader: ShaderProgram
    private lateinit var particleShader: ShaderProgram

    private val particleBatch = ParticleBatch()
    private val composer = EffectComposer(effects)
    private val table = UniformTable()

    private var config: RenderConfig = ConfigStore.current(mode)
    private var artTexture: Texture = fallbackTexture()

    // ── 跨线程投递（宿主写入，GL 线程消费）──
    @Volatile var pendingArt: Bitmap? = null
    @Volatile var pendingConfig: RenderConfig? = null
    @Volatile var latestFrameData: FrameData = FrameData.EMPTY

    fun create() {
        fluidShader = ShaderLibrary.get("fluid", ShaderSources.BASE_VERTEX, Shaders.FLUID_FRAG)
        kawaseShader = ShaderLibrary.get("kawase", ShaderSources.BASE_VERTEX, Shaders.KAWASE_FRAG)
        compositeShader = ShaderLibrary.get("composite", ShaderSources.BASE_VERTEX, Shaders.COMPOSITE_FRAG)
        finalShader = ShaderLibrary.get("final", ShaderSources.BASE_VERTEX, Shaders.FINAL_FRAG)
        particleShader = ShaderLibrary.get("particle", Shaders.PARTICLE_VERT, Shaders.PARTICLE_FRAG)
        quad = FullscreenQuad()
        fbo = FboPipeline()
        fbo.resize(width, height, blurLevels())
    }

    fun resize(w: Int, h: Int) {
        width = w.coerceAtLeast(1)
        height = h.coerceAtLeast(1)
        fbo.resize(width, height, blurLevels())
    }

    /** 渲染一帧（宿主 GL 线程调用） */
    fun render(delta: Float) {
        time += delta

        pendingConfig?.let { cfg ->
            config = cfg
            pendingConfig = null
            fbo.resize(width, height, blurLevels())
        }
        pendingArt?.let { bmp ->
            artTexture = uploadArt(bmp)
            pendingArt = null
        }

        val frameData = latestFrameData
        val cfg = config
        val ctx = FrameContext(width, height, time, frameData)

        // ── PASS 1：流体基底（Domain Wrapping）──
        fbo.beginBase()
        clear()
        table.reset()
        table.set("u_time", time)
        table.set("u_scale", cfg.fluidScale)
        table.set("u_speed", cfg.flowSpeed)
        table.set("u_turbulence", cfg.turbulence)
        table.set("u_brightness", cfg.brightness)
        table.set("u_saturation", cfg.saturation)
        table.set("u_audioEnergy", frameData.energy)
        table.set("u_beatPulse", frameData.pulse)
        table.set("u_art", 0)
        composer.apply(table, ctx, cfg, EffectPhase.FLUID, particleBatch)
        artTexture.bind(0)
        fluidShader.bind()
        table.applyTo(fluidShader)
        quad.draw(fluidShader)
        fbo.endBase()

        // ── PASS 2：Kawase 模糊金字塔 ──
        var input = fbo.baseTexture()
        for (i in 0 until fbo.blurCount()) {
            fbo.beginBlur(i)
            clear()
            kawaseShader.bind()
            kawaseShader.setUniformf("u_offset", 1f + 2f * i)
            kawaseShader.setUniformf("u_texelSize", 1f / width, 1f / height)
            input.bind(0)
            kawaseShader.setUniformi("u_tex", 0)
            quad.draw(kawaseShader)
            fbo.endBlur(i)
            input = fbo.blurTexture(i)
        }

        // ── PASS 3：合成（光晕 / 音频动效 / 玻璃折射 / 粒子）──
        fbo.beginComp()
        clear()
        table.reset()
        table.set("u_time", time)
        table.set("u_glowMix", 0.45f)
        table.set("u_glassMode", if (cfg.renderMode == RenderMode.FLUTED_GLASS) 1f else 0f)
        table.set("u_glassStrength", 1.2f)
        table.set("u_beatPulse", frameData.pulse)
        table.set("u_edgeEnergy", 0f)
        table.set("u_rippleStrength", 0f)
        table.set("u_ripplePhase", 0f)
        table.set("u_waveStrength", 0f)
        table.set("u_wavePhase", 0f)
        table.set("u_colorDrift", 0f)
        table.set("u_cornerGlow", 0f)
        table.set("u_radialHalo", 0f)
        table.set("u_bands", frameData.bands)
        table.set("u_scene", 0)
        table.set("u_glow", 1)
        composer.apply(table, ctx, cfg, EffectPhase.COMPOSITE, particleBatch)
        fbo.baseTexture().bind(0)
        fbo.glowTexture().bind(1)
        compositeShader.bind()
        table.applyTo(compositeShader)
        quad.draw(compositeShader)

        // 粒子叠加（additive）
        particleBatch.update(delta)
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE)
        particleShader.bind()
        particleShader.setUniformf("u_pointScale", height / 1080f * 3f)
        particleBatch.flush(particleShader)
        Gdx.gl.glDisable(GL20.GL_BLEND)
        fbo.endComp()

        // ── PASS 4：最终输出（亮度 / 饱和度）──
        table.reset()
        table.set("u_brightness", cfg.brightness)
        table.set("u_saturation", cfg.saturation)
        table.set("u_scene", 0)
        fbo.compTexture().bind(0)
        finalShader.bind()
        table.applyTo(finalShader)
        quad.draw(finalShader)
    }

    fun setArtDirect(bitmap: Bitmap?) {
        pendingArt = bitmap
    }

    private fun blurLevels(): Int = if (config.performanceMode) 3 else 5

    private fun clear() {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
    }

    private fun uploadArt(bitmap: Bitmap): Texture {
        val old = artTexture
        val pixmap = bitmap.toPixmap()
        val tex = Texture(pixmap, true)
        pixmap.dispose()
        tex.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear)
        tex.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat)
        old.dispose()
        return tex
    }

    private fun fallbackTexture(): Texture {
        val pixmap = Pixmap(1, 1, Pixmap.Format.RGBA8888)
        pixmap.setColor(com.badlogic.gdx.graphics.Color.valueOf("14142C"))
        pixmap.fill()
        val tex = Texture(pixmap)
        pixmap.dispose()
        tex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
        return tex
    }

    override fun dispose() {
        artTexture.dispose()
        particleBatch.dispose()
        quad.dispose()
        fbo.dispose()
    }
}

/** Android Bitmap → LibGDX Pixmap（RGBA 字节序直拷） */
fun Bitmap.toPixmap(): Pixmap {
    val w = width
    val h = height
    val pixmap = Pixmap(w, h, Pixmap.Format.RGBA8888)
    val buffer: ByteBuffer = pixmap.pixels
    buffer.clear()
    buffer.order(ByteOrder.nativeOrder())
    copyPixelsToBuffer(buffer)
    buffer.rewind()
    return pixmap
}
