package com.zhuomo.flowlume.wallpaper

import com.badlogic.gdx.ApplicationListener
import com.badlogic.gdx.Gdx
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

/** 壁纸形态的 LibGDX 渲染监听器（由 AndroidLiveWallpaperService 驱动） */
class WallpaperGdxApp : ApplicationListener {

    private lateinit var renderCore: RenderCore
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun create() {
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
    }

    override fun resize(width: Int, height: Int) {
        renderCore.resize(width, height)
    }

    override fun render() {
        renderCore.render(Gdx.graphics.deltaTime)
    }

    override fun pause() {
        // AndroidLiveWallpaperService 在不可见时已节流渲染；此处可选择性降低负载
    }

    override fun resume() = Unit

    override fun dispose() {
        scope.cancel()
        renderCore.dispose()
    }
}
