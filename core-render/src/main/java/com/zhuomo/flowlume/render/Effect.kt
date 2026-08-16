package com.zhuomo.flowlume.render

import com.zhuomo.flowlume.config.EffectState

enum class EffectGroup { BASE_FLUID, AUDIO_SYNC, PARTICLE_DECOR }

/** 动效作用阶段：FLUID = 流体基底 pass；COMPOSITE = 合成 pass */
enum class EffectPhase { FLUID, COMPOSITE }

/** UI 元数据：驱动页面3 控件自动生成（零 UI 代码扩展新动效） */
data class EffectMeta(
    val title: String,
    val group: EffectGroup,
    val hasIntensity: Boolean = true,
    val hasSpeed: Boolean = true,
    val hasSize: Boolean = false
)

/**
 * 组件化动效接口：所有动效完全解耦，独立开关 + 独立参数。
 * onFrame 写 uniform 表；粒子类动效在 onParticles 中产出粒子。
 */
interface Effect {
    val id: String
    val meta: EffectMeta
    val phase: EffectPhase

    fun onFrame(table: UniformTable, ctx: FrameContext, state: EffectState)

    fun onParticles(batch: ParticleBatch, ctx: FrameContext, state: EffectState) {}
}
