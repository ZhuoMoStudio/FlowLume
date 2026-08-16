# 技术文档 06 · 组件化动效系统架构

## 1. 设计目标
- 15+ 项动效**完全解耦**：独立开关、独立参数、独立生命周期；
- 动态组合：用户任意开/关，渲染引擎只打包启用项；
- 参数隔离：双形态各自持有效果状态快照；
- 预设系统：JSON 序列化，可保存/命名/删除/导入导出。

## 2. 核心抽象

```kotlin
enum class EffectGroup { BASE_FLUID, AUDIO_SYNC, PARTICLE_DECOR }

interface Effect {
    val id: String                 // "fx.ripple_spread"
    val group: EffectGroup
    val defaultParams: ParamSet

    /** 每帧被 Composer 调用：向 uniform 表写入参数 */
    fun apply(composer: UniformTable, ctx: FrameContext)

    /** 粒子类动效可覆盖：返回 CPU 侧生成的点数据（或走 GPU 粒子） */
    fun update(delta: Float, ctx: FrameContext) {}

    /** 资源生命周期：进入/离开渲染 */
    fun onAttach(render: RenderCore) {}
    fun onDetach(render: RenderCore) {}
}

data class ParamSet(
    val enabled: Boolean,
    val intensity: Float,   // 0..1
    val speed: Float,       // 0..2
    val size: Float         // 0..1（尺寸范围归一化）
) {
    fun toJson() = ... ; companion object { fun fromJson(...) }
}
```

## 3. 效果注册表（编译期注册，运行期查询）

```kotlin
object EffectRegistry {
    private val map = linkedMapOf<String, Effect>()

    fun register(e: Effect) { map[e.id] = e }

    // 静态初始化（可用 ServiceLoader 或直接列举）
    fun bootstrap() {
        register(FluidTurbulence())
        register(BgPan())
        register(MicroRotation())
        register(ColorDrift())
        register(GlobalGlow())
        register(RippleSpread())
        register(WaveSweep())
        register(BeatPulse())
        register(EdgeFlicker())
        register(ArtBreathing())
        register(SpectrumRadial())
        register(FloatingMotes())
        register(BokehBlobs())
        register(GradientRibbons())
        register(RadialHalo())
        register(CornerGlow())
        register(StarDrift())
    }
}
```

## 4. EffectComposer：动态打包

```kotlin
class EffectComposer(private val active: List<EffectState>) {
    private val uniformTable = UniformTable()

    fun render(delta: Float, ctx: FrameContext) {
        uniformTable.reset()
        for (state in active) {
            if (!state.params.enabled) continue
            val fx = EffectRegistry.byId(state.effectId) ?: continue
            fx.update(delta, ctx)
            fx.apply(uniformTable, ctx)     // 写入 u_xxx uniforms / 追加 draw 调用
        }
        // 按 group 顺序执行 draw 指令队列（shader pass 顺序固定）
        executeDrawQueue(uniformTable)
    }
}
```

### 动效 → Shader 映射规则
| 分组 | 动效 | 实现路径 |
|------|------|----------|
| BASE_FLUID | 流体扰动/平移/微旋转/色彩渐变/全局光晕 | 修改主 shader uniforms（`u_turbulence`、旋转矩阵、渐变速度、glow mix 权重） |
| AUDIO_SYNC | 涟漪/波形/脉冲/边缘光/明暗/频谱径向 | 合成 pass 追加 draw（混合模式 `SRC_ALPHA`），读 `FrameData` |
| PARTICLE_DECOR | 光点/散景/光带/径向光晕/四角辉光/星点 | GPU 粒子系统：一个 `ParticleBatch`（动态 VBO，每粒子 4 顶点 instanced 或退化四边形）+ 各自 shader/纹理（圆形梯度） |

### 粒子批次调度（统一批处理，避免 draw call 爆炸）
```kotlin
class ParticleBatch(maxParticles: Int = 4096) {
    // 所有粒子类动效共享同一 VBO；按 effectId 分区段
    fun spawn(effectId: String, pos: Vec2, life: Float, size: Float, color: Color)
    fun flush(shader: ShaderProgram)  // 一次 drawArrays 渲染全部存活粒子
}
```

## 5. 预设系统（Preset）

```jsonc
// preset JSON 结构
{
  "schema": 1,
  "name": "DEEP LUMEN 深海弥光",
  "official": true,
  "effects": [
    { "id": "fx.turbulence",  "enabled": true,  "intensity": 0.8, "speed": 1.0, "size": 0.5 },
    { "id": "fx.ripple_spread","enabled": true, "intensity": 0.6, "speed": 0.8, "size": 0.7 }
    // ... 全部 17 项
  ],
  "render": { "mode": "BLUR", "scale": 1.0, "speed": 1.0, "brightness": 1.0, "saturation": 1.1 }
}
```

- `PresetStore`：官方预设随包内置（assets/presets/*.json，只读）；自定义预设存 `filesDir/presets/`；
- 导入：SAF 文件选择器读 JSON → 校验 schema → 写入自定义目录 → 列表刷新；
- 导出：分享 `application/json` 文件；
- 套用：覆盖当前形态配置 → 广播 ReloadBus。

## 6. 内置官方预设（初版 4 套）
| 预设 | 主打动效组合 | 氛围 |
|------|--------------|------|
| DEEP LUMEN 深海弥光 | 扰动+渐变+光晕+径向光晕+星点 | 静谧蓝紫 |
| NEON BEAT 霓虹节拍 | 节拍脉冲+涟漪+边缘光+频谱径向 | 高能电音 |
| MORNING HAZE 晨雾 | 平移+光晕+散景+渐变光带 | 柔和明亮 |
| GLASS GARDEN 璃园 | FLUTED GLASS+微旋转+漂浮光点 | 玻璃质感 |

## 7. 性能模式联动
```kotlin
class PerformancePolicy {
    fun onModeChanged(performance: Boolean) {
        if (performance) {
            composer.disableGroup(EffectGroup.PARTICLE_DECOR)  // 关闭全部粒子
            renderCore.setResolutionScale(0.75f)               // 720p
            renderCore.setFpsCap(30)
        } else { restoreFromConfig() }
    }
}
```

## 8. 扩展新动效的流程（文档化约定）
1. 实现 `Effect` 接口（或继承 `ParticleEffect` 基类）；
2. 在 `EffectRegistry.bootstrap()` 注册；
3. 定义默认参数 + UI 元数据（`EffectMeta(title, group, hasSize, hasSpeed...)`）→ 页面3 自动渲染该行控件（**零 UI 代码**）；
4. 提供官方预设条目。
> UI 自动生成：`EffectMeta` 驱动 RecyclerView 条目（复选框 + 滑块组），新动效无需改页面代码。
