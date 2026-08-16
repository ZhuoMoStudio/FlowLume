package com.zhuomo.flowlume.wallpaper

import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import com.zhuomo.flowlume.config.ConfigStore
import com.zhuomo.flowlume.config.Mode
import com.zhuomo.flowlume.config.ReloadBus
import com.zhuomo.flowlume.effects.FlowLumeEffects
import com.zhuomo.flowlume.media.ArtBus
import com.zhuomo.flowlume.render.EffectRegistry
import com.zhuomo.flowlume.render.RenderCore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/** 形态一：Android 系统动态壁纸服务（桌面 / 锁屏） */
class FluidWallpaperService : WallpaperService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        FlowLumeEffects.bootstrap()
    }

    override fun onCreateEngine(): Engine = FluidEngine()

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private inner class FluidEngine : Engine() {

        private var wallpaper: GdxWallpaper? = null
        private var renderCore: RenderCore? = null

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            val core = RenderCore(Mode.WALLPAPER, EffectRegistry.all())
            renderCore = core
            val w = holder.surfaceFrame.width().coerceAtLeast(1)
            val h = holder.surfaceFrame.height().coerceAtLeast(1)
            wallpaper = GdxWallpaper(applicationContext, holder.surface, core).apply {
                setSize(w, h)
                start()
            }
            collectBus()
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            wallpaper?.setSize(width, height)
            renderCore?.resize(width, height)
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            wallpaper?.stop()
            wallpaper = null
            renderCore?.dispose()
            renderCore = null
            super.onSurfaceDestroyed(holder)
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            // 桌面/锁屏不可见时暂停渲染（省电）
            if (visible) wallpaper?.resume() else wallpaper?.pause()
        }

        override fun onOffsetsChanged(
            xOffset: Float, yOffset: Float, xOffsetStep: Float, yOffsetStep: Float,
            xPixelOffset: Int, yPixelOffset: Int
        ) {
            super.onOffsetsChanged(xOffset, yOffset, xOffsetStep, yOffsetStep, xPixelOffset, yPixelOffset)
            // 预留：视差偏移接入（后续版本）
        }

        override fun onDestroy() {
            wallpaper?.stop()
            wallpaper = null
            renderCore?.dispose()
            renderCore = null
            super.onDestroy()
        }

        private fun collectBus() {
            scope.launch {
                ArtBus.events.collect { ev ->
                    renderCore?.pendingArt = ev.artwork
                }
            }
            scope.launch {
                ReloadBus.events.collect { ev ->
                    if (ev.mode == Mode.WALLPAPER) {
                        renderCore?.pendingConfig = ConfigStore.current(Mode.WALLPAPER)
                    }
                }
            }
        }
    }
}
