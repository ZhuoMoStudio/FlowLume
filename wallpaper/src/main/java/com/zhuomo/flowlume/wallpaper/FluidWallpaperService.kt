package com.zhuomo.flowlume.wallpaper

import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import com.badlogic.gdx.backends.android.AndroidLiveWallpaperService
import com.zhuomo.flowlume.effects.FlowLumeEffects

/**
 * 形态一：Android 系统动态壁纸服务（桌面 / 锁屏）。
 * 复用 LibGDX 官方 AndroidLiveWallpaperService：EGL 上下文 / GL 线程 / Gdx 初始化全自动，
 * 仅需提供 ApplicationListener。
 */
class FluidWallpaperService : AndroidLiveWallpaperService() {

    override fun onCreateApplication() {
        super.onCreateApplication()
        FlowLumeEffects.bootstrap()
        val cfg = AndroidApplicationConfiguration().apply {
            r = 8
            g = 8
            b = 8
            a = 0
            disableAudio = true // 音频统一走 core-audio 模块
        }
        initialize(WallpaperGdxApp(), cfg)
    }
}
