package com.zhuomo.flowlume.effects

import com.zhuomo.flowlume.config.EffectState
import com.zhuomo.flowlume.render.Effect
import com.zhuomo.flowlume.render.EffectGroup
import com.zhuomo.flowlume.render.EffectMeta
import com.zhuomo.flowlume.render.EffectPhase
import com.zhuomo.flowlume.render.FrameContext
import com.zhuomo.flowlume.render.UniformTable
import kotlin.math.max
import kotlin.math.sin

/** 音频联动动效组（依赖 FrameData；无音频权限时整组禁用） */

class RippleSpread : Effect {
    override val id = "fx.ripple_spread"
    override val meta = EffectMeta("RIPPLE SPREAD 音频扩散涟漪", EffectGroup.AUDIO_SYNC)
    override val phase = EffectPhase.COMPOSITE
    override fun onFrame(table: UniformTable, ctx: FrameContext, state: EffectState) {
        table.set("u_rippleStrength", state.intensity * ctx.frameData.energy * 0.9f)
        table.set("u_ripplePhase", ctx.time * state.speed * 0.5f)
    }
}

class WaveSweep : Effect {
    override val id = "fx.wave_sweep"
    override val meta = EffectMeta("WAVE SWEEP 横向波形扫动", EffectGroup.AUDIO_SYNC)
    override val phase = EffectPhase.COMPOSITE
    override fun onFrame(table: UniformTable, ctx: FrameContext, state: EffectState) {
        table.set("u_waveStrength", state.intensity * ctx.frameData.energy * 0.9f)
        table.set("u_wavePhase", ctx.time * state.speed * 0.3f)
    }
}

class BeatPulse : Effect {
    override val id = "fx.beat_pulse"
    override val meta = EffectMeta("BEAT PULSE 节拍脉冲震动", EffectGroup.AUDIO_SYNC, hasSize = false)
    override val phase = EffectPhase.COMPOSITE
    override fun onFrame(table: UniformTable, ctx: FrameContext, state: EffectState) {
        val pulse = ctx.frameData.pulse * (0.4f + state.intensity * 1.2f)
        table.set("u_beatPulse", max(table.get("u_beatPulse"), pulse))
    }
}

class EdgeFlicker : Effect {
    override val id = "fx.edge_flicker"
    override val meta = EffectMeta("EDGE FLICKER 画面边缘光震荡", EffectGroup.AUDIO_SYNC, hasSize = false)
    override val phase = EffectPhase.COMPOSITE
    override fun onFrame(table: UniformTable, ctx: FrameContext, state: EffectState) {
        val flicker = ctx.frameData.energy * state.intensity * (0.5f + 0.5f * sin(ctx.time * state.speed * 8f))
        table.set("u_edgeEnergy", flicker)
    }
}

class ArtBreathing : Effect {
    override val id = "fx.art_breathing"
    override val meta = EffectMeta("ART BREATHING 封面明暗起伏", EffectGroup.AUDIO_SYNC, hasSize = false)
    override val phase = EffectPhase.FLUID
    override fun onFrame(table: UniformTable, ctx: FrameContext, state: EffectState) {
        table.set("u_audioEnergy", ctx.frameData.energy * (0.3f + state.intensity * 0.9f))
    }
}

class SpectrumRadial : Effect {
    override val id = "fx.spectrum_radial"
    override val meta = EffectMeta("SPECTRUM RADIAL 频谱径向扩散", EffectGroup.AUDIO_SYNC, hasSize = false)
    override val phase = EffectPhase.COMPOSITE
    override fun onFrame(table: UniformTable, ctx: FrameContext, state: EffectState) {
        val scale = 0.4f + state.intensity * 1.2f
        table.set("u_bands", ctx.frameData.bands.map { it * scale }.toFloatArray())
    }
}
