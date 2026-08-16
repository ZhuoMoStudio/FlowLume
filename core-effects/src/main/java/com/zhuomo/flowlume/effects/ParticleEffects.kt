package com.zhuomo.flowlume.effects

import com.zhuomo.flowlume.config.EffectState
import com.zhuomo.flowlume.render.Effect
import com.zhuomo.flowlume.render.EffectGroup
import com.zhuomo.flowlume.render.EffectMeta
import com.zhuomo.flowlume.render.EffectPhase
import com.zhuomo.flowlume.render.FrameContext
import com.zhuomo.flowlume.render.ParticleBatch
import com.zhuomo.flowlume.render.UniformTable
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/** 装饰粒子动效组（ParticleBatch 统一批处理） */

class FloatingMotes : Effect {
    override val id = "fx.floating_motes"
    override val meta = EffectMeta("FLOATING MOTES 漂浮光点粒子", EffectGroup.PARTICLE_DECOR, hasSize = true)
    override val phase = EffectPhase.COMPOSITE
    private val rnd = Random(42)

    override fun onParticles(batch: ParticleBatch, ctx: FrameContext, state: EffectState) {
        val density = (state.intensity * 14f).toInt().coerceIn(1, 20)
        repeat(density) {
            batch.spawn(
                x = rnd.nextFloat(), y = rnd.nextFloat(),
                size = (1.5f + rnd.nextFloat() * 2.5f) * (0.5f + state.size),
                vx = (rnd.nextFloat() - 0.5f) * 0.03f,
                vy = -0.01f - rnd.nextFloat() * 0.02f,
                life = 4f + rnd.nextFloat() * 4f,
                r = 0.8f, g = 0.85f, b = 1f, a = 0.25f + rnd.nextFloat() * 0.3f
            )
        }
    }
}

class BokehBlobs : Effect {
    override val id = "fx.bokeh_blobs"
    override val meta = EffectMeta("BOKEH BLOBS 散景光斑", EffectGroup.PARTICLE_DECOR, hasSize = true)
    override val phase = EffectPhase.COMPOSITE
    private val rnd = Random(7)

    override fun onParticles(batch: ParticleBatch, ctx: FrameContext, state: EffectState) {
        val count = (state.intensity * 4f).toInt().coerceIn(1, 6)
        repeat(count) {
            batch.spawn(
                x = rnd.nextFloat(), y = rnd.nextFloat(),
                size = (18f + rnd.nextFloat() * 30f) * (0.5f + state.size),
                vx = (rnd.nextFloat() - 0.5f) * 0.01f,
                vy = (rnd.nextFloat() - 0.5f) * 0.01f,
                life = 8f + rnd.nextFloat() * 6f,
                r = 0.9f, g = 0.8f, b = 1f, a = 0.06f
            )
        }
    }
}

class GradientRibbons : Effect {
    override val id = "fx.gradient_ribbons"
    override val meta = EffectMeta("GRADIENT RIBBONS 流动渐变光带", EffectGroup.PARTICLE_DECOR, hasSize = true)
    override val phase = EffectPhase.COMPOSITE
    private val rnd = Random(11)

    override fun onParticles(batch: ParticleBatch, ctx: FrameContext, state: EffectState) {
        val count = (state.intensity * 8f).toInt().coerceIn(1, 12)
        repeat(count) {
            val baseY = rnd.nextFloat()
            val x = (ctx.time * state.speed * 0.08f + rnd.nextFloat()) % 1f
            val y = baseY + sin(x * PI * 4.0).toFloat() * 0.06f
            batch.spawn(
                x = x, y = y,
                size = (2f + rnd.nextFloat() * 3f) * (0.5f + state.size),
                vx = state.speed * 0.06f, vy = 0f,
                life = 3f,
                r = 0.5f + 0.4f * sin(x * 8f).toFloat(),
                g = 0.6f, b = 1f, a = 0.18f
            )
        }
    }
}

class RadialHalo : Effect {
    override val id = "fx.radial_halo"
    override val meta = EffectMeta("RADIAL HALO 径向光晕", EffectGroup.PARTICLE_DECOR, hasSize = false)
    override val phase = EffectPhase.COMPOSITE
    override fun onFrame(table: UniformTable, ctx: FrameContext, state: EffectState) {
        table.set("u_radialHalo", state.intensity * (0.5f + ctx.frameData.energy * 0.5f))
    }
}

class CornerGlow : Effect {
    override val id = "fx.corner_glow"
    override val meta = EffectMeta("CORNER GLOW 四角辉光", EffectGroup.PARTICLE_DECOR, hasSize = false)
    override val phase = EffectPhase.COMPOSITE
    override fun onFrame(table: UniformTable, ctx: FrameContext, state: EffectState) {
        val pulse = 0.6f + 0.4f * sin(ctx.time * state.speed)
        table.set("u_cornerGlow", state.intensity * pulse)
    }
}

class StarDrift : Effect {
    override val id = "fx.star_drift"
    override val meta = EffectMeta("STAR DRIFT 缓慢星点浮动", EffectGroup.PARTICLE_DECOR, hasSize = true)
    override val phase = EffectPhase.COMPOSITE
    private val rnd = Random(99)

    override fun onParticles(batch: ParticleBatch, ctx: FrameContext, state: EffectState) {
        val count = (state.intensity * 6f).toInt().coerceIn(1, 8)
        repeat(count) {
            batch.spawn(
                x = rnd.nextFloat(), y = rnd.nextFloat(),
                size = (0.8f + rnd.nextFloat() * 1.2f) * (0.5f + state.size),
                vx = (rnd.nextFloat() - 0.5f) * 0.006f,
                vy = (rnd.nextFloat() - 0.5f) * 0.006f,
                life = 10f + rnd.nextFloat() * 6f,
                r = 0.9f, g = 0.95f, b = 1f, a = 0.5f
            )
        }
    }
}
