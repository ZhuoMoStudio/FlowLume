package com.zhuomo.flowlume.effects

import com.zhuomo.flowlume.config.RenderConfig
import com.zhuomo.flowlume.render.EffectGroup
import com.zhuomo.flowlume.render.EffectRegistry

/** 性能模式策略：降分辨率 + 关闭粒子组（由渲染宿主执行实际降分辨率） */
object PerformancePolicy {

    fun isEnabled(config: RenderConfig): Boolean = config.performanceMode

    fun toggle(config: RenderConfig, performance: Boolean): RenderConfig =
        config.copy(performanceMode = performance)

    /** 性能模式下被强制关闭的动效 id 集合（供 UI 展示禁用态） */
    fun disabledEffectIds(): Set<String> =
        EffectRegistry.byGroup(EffectGroup.PARTICLE_DECOR).map { it.id }.toSet()
}
