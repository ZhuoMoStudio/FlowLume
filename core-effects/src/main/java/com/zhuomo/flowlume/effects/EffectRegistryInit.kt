package com.zhuomo.flowlume.effects

import com.zhuomo.flowlume.render.EffectRegistry

/** 编译期注册全部 17 项动效（应用启动时调用一次） */
object FlowLumeEffects {
    fun bootstrap() {
        if (EffectRegistry.all().isNotEmpty()) return
        // 基础流体动效组
        EffectRegistry.register(FluidTurbulence())
        EffectRegistry.register(BgPan())
        EffectRegistry.register(MicroRotation())
        EffectRegistry.register(ColorDrift())
        EffectRegistry.register(GlobalGlow())
        // 音频联动动效组
        EffectRegistry.register(RippleSpread())
        EffectRegistry.register(WaveSweep())
        EffectRegistry.register(BeatPulse())
        EffectRegistry.register(EdgeFlicker())
        EffectRegistry.register(ArtBreathing())
        EffectRegistry.register(SpectrumRadial())
        // 装饰粒子动效组
        EffectRegistry.register(FloatingMotes())
        EffectRegistry.register(BokehBlobs())
        EffectRegistry.register(GradientRibbons())
        EffectRegistry.register(RadialHalo())
        EffectRegistry.register(CornerGlow())
        EffectRegistry.register(StarDrift())
    }
}
