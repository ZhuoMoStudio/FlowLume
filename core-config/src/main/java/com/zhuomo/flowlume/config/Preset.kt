package com.zhuomo.flowlume.config

import kotlinx.serialization.Serializable

/** 视觉预设：动效清单 + 渲染参数（不含计时器） */
@Serializable
data class Preset(
    val name: String,
    val official: Boolean = false,
    val effects: List<EffectState>,
    val render: PresetRender = PresetRender()
)

@Serializable
data class PresetRender(
    val renderMode: RenderMode = RenderMode.BLUR,
    val fluidScale: Float = 1.0f,
    val flowSpeed: Float = 1.0f,
    val turbulence: Float = 1.0f,
    val brightness: Float = 1.0f,
    val saturation: Float = 1.0f
)
