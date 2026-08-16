package com.zhuomo.flowlume.render

import com.zhuomo.flowlume.config.RenderConfig

/** 按阶段收集启用动效：写 uniform 表 + 产出粒子 */
class EffectComposer(private val effects: List<Effect>) {

    fun apply(
        table: UniformTable,
        ctx: FrameContext,
        config: RenderConfig,
        phase: EffectPhase,
        batch: ParticleBatch
    ) {
        for (fx in effects) {
            if (fx.phase != phase) continue
            val state = config.effects.firstOrNull { it.id == fx.id } ?: continue
            if (!state.enabled) continue
            fx.onFrame(table, ctx, state)
            fx.onParticles(batch, ctx, state)
        }
    }
}
