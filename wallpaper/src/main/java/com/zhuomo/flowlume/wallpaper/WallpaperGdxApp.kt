package com.zhuomo.flowlume.wallpaper

import android.util.Log
import com.badlogic.gdx.ApplicationListener
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.android.AndroidWallpaperListener
import com.zhuomo.flowlume.config.ConfigStore
import com.zhuomo.flowlume.config.Mode
import com.zhuomo.flowlume.config.ReloadBus
import com.zhuomo.flowlume.media.ArtBus
import com.zhuomo.flowlume.render.EffectRegistry
import com.zhuomo.flowlume.render.RenderCore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * 壁纸形态的 LibGDX 渲染监听器（由 AndroidLiveWallpaperService 驱动）。
 * 加固点：渲染/初始化异常捕获，避免壁纸预览或桌面黑屏/崩溃。
 */
class WallpaperGdxApp : ApplicationListener, AndroidWallpaperListener {

    private lateinit var renderCore: RenderCore
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var degraded = false

    override fun create() {
        try {
            renderCore = RenderCore(Mode.WALLPAPER, EffectRegistry.all())
            renderCore.create()

            // 封面 / 配置热重载：任意线程写入，GL 线程在 render() 内消费
            scope.launch {
                ArtBus.events.collect { renderCore.pendingArt = it.artwork }
            }
            scope.launch {
                ReloadBus.events.collect { ev ->
                    if (ev.mode == Mode.WALLPAPER) {
                        renderCore.pendingConfig = ConfigStore.current(Mode.WALLPAPER)
                    }
                }
            }
        } catch (e: Throwable) {
            Log.e(TAG, "wallpaper create failed, degraded", e)
            degraded = true
        }
    }

    override fun resize(width: Int, height: Int) {
        if (degraded) return
        runCatching { renderCore.resize(width, height) }
    }

    override fun render() {
        if (degraded) {
            Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
            Gdx.gl.glClear(com.badlogic.gdx.graphics.GL20.GL_COLOR_BUFFER_BIT)
            return
        }
        try {
            renderCore.render(Gdx.graphics.deltaTime)
        } catch (e: Throwable) {
            // 单帧异常不崩溃；连续异常可交由系统节流
            Log.w(TAG, "render frame failed: ${e.message}")
        }
    }

    override fun pause() = Unit
    override fun resume() = Unit

    override fun dispose() {
        scope.cancel()
        if (::renderCore.isInitialized) {
            runCatching { renderCore.dispose() }
        }
    }

    // AndroidWallpaperListener：预览状态 / 桌面视差（可选）
    override fun offsetChange(
        xOffset: Float, yOffset: Float,
        xOffsetStep: Float, yOffsetStep: Float,
        xPixelOffset: Int, yPixelOffset: Int
    ) = Unit

    override fun previewStateChange(isPreview: Boolean) {
        Log.i(TAG, "previewStateChange isPreview=$isPreview")
    }

    companion object {
        private const val TAG = "FlowLumeWallpaper"
    }
}
