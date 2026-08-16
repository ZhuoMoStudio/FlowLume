package com.zhuomo.flowlume.app.fullscreen

import android.util.Log
import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.zhuomo.flowlume.app.di.AppContainer
import com.zhuomo.flowlume.config.ConfigStore
import com.zhuomo.flowlume.config.Mode
import com.zhuomo.flowlume.config.ReloadBus
import com.zhuomo.flowlume.effects.FlowLumeEffects
import com.zhuomo.flowlume.media.ArtBus
import com.zhuomo.flowlume.render.EffectRegistry
import com.zhuomo.flowlume.render.FontCache
import com.zhuomo.flowlume.render.RenderCore
import kotlinx.coroutines.launch

/** 全屏窗口模式的 LibGDX 入口（形态二宿主） */
class FullscreenGame : Game() {

    lateinit var renderCore: RenderCore
        private set
    lateinit var fontCache: FontCache
        private set
    lateinit var batch: SpriteBatch
        private set

    /** 初始化失败时置 true：渲染层降级为纯色，避免闪退 */
    @Volatile var initFailed = false
        private set

    override fun create() {
        try {
            FlowLumeEffects.bootstrap()
            renderCore = RenderCore(Mode.FULLSCREEN, EffectRegistry.all())
            renderCore.create()
            fontCache = FontCache(Gdx.files.internal("fonts/Roboto-Medium.ttf"))
            batch = SpriteBatch()

            // 跨线程投递：封面 / 配置热重载 / 音频帧（GL 线程内消费）
            AppContainer.scope.launch {
                ArtBus.events.collect { renderCore.pendingArt = it.artwork }
            }
            AppContainer.scope.launch {
                ReloadBus.events.collect {
                    if (it.mode == Mode.FULLSCREEN) {
                        renderCore.pendingConfig = ConfigStore.current(Mode.FULLSCREEN)
                    }
                }
            }
            AppContainer.scope.launch {
                AppContainer.audioEngine.frames.collect { renderCore.latestFrameData = it }
            }

            setScreen(FlowScreen(this))
        } catch (e: Throwable) {
            Log.e(TAG, "FullscreenGame.create failed, degraded mode", e)
            initFailed = true
        }
    }

    override fun render() {
        if (initFailed) {
            Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
            Gdx.gl.glClear(com.badlogic.gdx.graphics.GL20.GL_COLOR_BUFFER_BIT)
            return
        }
        super.render()
    }

    override fun dispose() {
        super.dispose()
        runCatching {
            if (::batch.isInitialized) batch.dispose()
            if (::fontCache.isInitialized) fontCache.dispose()
            if (::renderCore.isInitialized) renderCore.dispose()
        }
    }

    companion object {
        private const val TAG = "FlowLumeGame"
    }
}
