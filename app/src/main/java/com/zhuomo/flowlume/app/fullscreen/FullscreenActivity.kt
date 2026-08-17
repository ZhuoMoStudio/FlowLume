package com.zhuomo.flowlume.app.fullscreen

import android.content.Context
import android.os.Bundle
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.Toast
import androidx.compose.ui.platform.ComposeView
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import com.zhuomo.flowlume.app.di.AppContainer
import com.zhuomo.flowlume.app.util.LocaleHelper
import com.zhuomo.flowlume.ui.FlowLumeTheme

/** 形态二：App 全屏窗口模式（LibGDX AndroidApplication + Compose 控制浮层） */
class FullscreenActivity : AndroidApplication() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.apply(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 计时器场景保持常亮
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        AppContainer.uiControlsHidden.value = false

        val cfg = AndroidApplicationConfiguration().apply {
            useImmersiveMode = true          // 沉浸式全屏（隐藏系统栏）
            disableAudio = true              // 音频统一走 core-audio 模块
            r = 8; g = 8; b = 8; a = 0
        }

        // 全屏渲染初始化失败兜底：捕获异常并明确提示（避免无反应）
        runCatching {
            initialize(FullscreenGame(), cfg)
        }.onFailure { e ->
            android.util.Log.e("FlowLumeFullscreen", "LibGDX init failed", e)
            Toast.makeText(this, "Fullscreen renderer init failed: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Compose 控制浮层（透明，未消费的触摸事件穿透到 GLSurfaceView）
        val overlay = ComposeView(this)
        window.addContentView(
            overlay,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )
        overlay.setContent {
            FlowLumeTheme {
                TimerOverlay()
            }
        }
    }

    override fun onDestroy() {
        AppContainer.audioEngine.stop()
        super.onDestroy()
    }
}
