package com.zhuomo.flowlume.config

import kotlinx.serialization.Serializable

/** 两种渲染模式（共享同一底层引擎，仅 shader 管线不同） */
@Serializable
enum class RenderMode { BLUR, FLUTED_GLASS }

/**
 * 一份形态的完整渲染配置。
 * 桌面壁纸与 App 全屏各自持有一份，可一键互拷（[ConfigStore.copy]）。
 */
@Serializable
data class RenderConfig(
    // 渲染模式
    val renderMode: RenderMode = RenderMode.BLUR,

    // 流体设定
    val fluidScale: Float = 1.0f,
    val flowSpeed: Float = 1.0f,
    val turbulence: Float = 1.0f,

    // 全局画面调节
    val brightness: Float = 1.0f,
    val saturation: Float = 1.0f,

    // 默认色调（无音乐时基底色，ARGB Int）
    val defaultTone: Long = 0xFF14142C,

    // 专辑封面配置
    val restoreArtOnReboot: Boolean = true,
    val keepArtOnPause: Boolean = true,

    // 动效清单（三大分组，每项独立开关/参数）
    val effects: List<EffectState> = defaultEffects(),

    // 性能模式（降分辨率 + 关闭粒子组）
    val performanceMode: Boolean = false,

    // 调试视图（实验性）
    val debugView: Boolean = false
) {
    companion object {
        fun defaultEffects(): List<EffectState> = listOf(
            // 基础流体动效组
            EffectState("fx.turbulence", enabled = true, intensity = 0.8f, speed = 1.0f, size = 0.5f),
            EffectState("fx.bg_pan", enabled = false, intensity = 0.5f, speed = 0.6f, size = 0.5f),
            EffectState("fx.micro_rotation", enabled = false, intensity = 0.4f, speed = 0.5f, size = 0.5f),
            EffectState("fx.color_drift", enabled = true, intensity = 0.5f, speed = 0.8f, size = 0.5f),
            EffectState("fx.global_glow", enabled = true, intensity = 0.6f, speed = 1.0f, size = 0.5f),
            // 音频联动动效组
            EffectState("fx.ripple_spread", enabled = true, intensity = 0.6f, speed = 1.0f, size = 0.7f),
            EffectState("fx.wave_sweep", enabled = false, intensity = 0.5f, speed = 1.0f, size = 0.5f),
            EffectState("fx.beat_pulse", enabled = true, intensity = 0.7f, speed = 1.0f, size = 0.5f),
            EffectState("fx.edge_flicker", enabled = false, intensity = 0.5f, speed = 1.0f, size = 0.5f),
            EffectState("fx.art_breathing", enabled = true, intensity = 0.5f, speed = 1.0f, size = 0.5f),
            EffectState("fx.spectrum_radial", enabled = false, intensity = 0.5f, speed = 1.0f, size = 0.5f),
            // 装饰粒子动效组
            EffectState("fx.floating_motes", enabled = true, intensity = 0.6f, speed = 1.0f, size = 0.4f),
            EffectState("fx.bokeh_blobs", enabled = false, intensity = 0.5f, speed = 1.0f, size = 0.5f),
            EffectState("fx.gradient_ribbons", enabled = false, intensity = 0.5f, speed = 1.0f, size = 0.5f),
            EffectState("fx.radial_halo", enabled = true, intensity = 0.5f, speed = 1.0f, size = 0.6f),
            EffectState("fx.corner_glow", enabled = false, intensity = 0.5f, speed = 1.0f, size = 0.5f),
            EffectState("fx.star_drift", enabled = false, intensity = 0.4f, speed = 0.5f, size = 0.4f)
        )
    }
}
