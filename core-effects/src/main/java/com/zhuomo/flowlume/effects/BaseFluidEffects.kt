package com.zhuomo.flowlume.effects

import com.zhuomo.flowlume.config.EffectState
import com.zhuomo.flowlume.render.Effect
import com.zhuomo.flowlume.render.EffectGroup
import com.zhuomo.flowlume.render.EffectMeta
import com.zhuomo.flowlume.render.EffectPhase
import com.zhuomo.flowlume.render.FrameContext
import com.zhuomo.flowlume.render.UniformTable
import kotlin.math.sin

/** 基础流体动效组 */

class FluidTurbulence : Effect {
    override val id = "fx.turbulence"
    override val meta = EffectMeta("FLUID TURBULENCE 流体扰动强度", EffectGroup.BASE_FLUID)
    override val phase = EffectPhase.FLUID
    override fun onFrame(table: UniformTable, ctx: FrameContext, state: EffectState) {
        table.set("u_turbulence", 0.3f + state.intensity * 1.7f)
    }
}

class BgPan : Effect {
    override val id = "fx.bg_pan"
    override val meta = EffectMeta("BG PAN 背景缓慢平移", EffectGroup.BASE_FLUID, hasSize = false)
    override val phase = EffectPhase.FLUID
    override fun onFrame(table: UniformTable, ctx: FrameContext, state: EffectState) {
        val speed = state.speed * 0.05f
        table.set("u_pan", floatArrayOf(ctx.time * speed, ctx.time * speed * 0.7f))
    }
}

class MicroRotation : Effect {
    override val id = "fx.micro_rotation"
    override val meta = EffectMeta("MICRO ROTATION 画面微旋转", EffectGroup.BASE_FLUID, hasSize = false)
    override val phase = EffectPhase.FLUID
    override fun onFrame(table: UniformTable, ctx: FrameContext, state: EffectState) {
        table.set("u_rotate", ctx.time * state.speed * 0.08f * state.intensity)
    }
}

class ColorDrift : Effect {
    override val id = "fx.color_drift"
    override val meta = EffectMeta("COLOR DRIFT 色彩缓慢渐变", EffectGroup.BASE_FLUID, hasSize = false)
    override val phase = EffectPhase.COMPOSITE
    override fun onFrame(table: UniformTable, ctx: FrameContext, state: EffectState) {
        val drift = state.intensity * 0.6f * (0.5f + 0.5f * sin(ctx.time * state.speed))
        table.set("u_colorDrift", drift)
    }
}

class GlobalGlow : Effect {
    override val id = "fx.global_glow"
    override val meta = EffectMeta("GLOBAL GLOW 全局光晕弥散", EffectGroup.BASE_FLUID, hasSize = false)
    override val phase = EffectPhase.COMPOSITE
    override fun onFrame(table: UniformTable, ctx: FrameContext, state: EffectState) {
        table.set("u_glowMix", 0.15f + state.intensity * 0.55f)
    }
}
