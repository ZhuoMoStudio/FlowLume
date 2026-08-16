package com.zhuomo.flowlume.config

import kotlinx.serialization.Serializable

/** 单条动效的独立配置（启用 + 强度 + 速度 + 尺寸范围） */
@Serializable
data class EffectState(
    val id: String,
    val enabled: Boolean = false,
    val intensity: Float = 0.5f,
    val speed: Float = 1.0f,
    val size: Float = 0.5f
)
