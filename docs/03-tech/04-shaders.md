# 技术文档 04 · Kawase Blur + Domain Wrapping 流体扰动 GLSL 方案

> 参考：Intel 开源 Kawase 双 Pass 模糊；Inigo Quilez（iq）Domain Wrapping 无缝平铺思想。
> 工程实现（`core-render/Shaders.kt`）为最大兼容性采用 **GLSL ES 1.00**（GLES2/GLES3 均可用）；本文件给出等价 **GLSL ES 3.00** 参考实现，两者算法一致。

## 1. 渲染管线总览（每帧）

```
封面 Bitmap (GL 纹理 art)
   │
   ▼
[PASS-1 流体基底] domain-wrapped 扰动采样 art → baseColor (uv 被 flow field 扭曲)
   │
   ▼
[PASS-2 Kawase 模糊金字塔] baseColor 连续 5 级降采样+上采样 → blurPyramid
   │
   ▼
[PASS-3 效果合成] EffectComposer 合并：音频涟漪 / 粒子 / 光晕 / 折射 / 时间文字(底层模式)
   │
   ▼
[PASS-4 最终合成] 亮度/饱和度调节 → 顶层 UI 或时间文字(顶层模式) → 屏幕
```

## 2. Kawase Blur（卡瓦塞多级迭代模糊）

### 2.1 原理
- 每级仅需 **2 次纹理采样**（与标准 9 抽头高斯相比大幅省带宽）；
- 核心：双 Pass（水平+垂直合并为一次 4 点采样），偏移量按 `2^i` 迭代扩张；
- 通过 5 级迭代金字塔近似大半径高斯，视觉上柔和均匀、无方块。

### 2.2 单级 Kernel（GLSL ES 伪代码）
```glsl
// 输入：当前 mip 纹理；输出：下一级更模糊
uniform sampler2D u_tex;
uniform vec2  u_texelSize;   // 1.0 / 当前 mip 尺寸
uniform float u_offset;      // 迭代级数 i → offset = 1.0 + 2.0*i

vec4 kawase(in vec2 uv) {
    vec2 o = u_texelSize * u_offset;
    vec4 a = texture(u_tex, uv + vec2(-o.x, -o.y));
    vec4 b = texture(u_tex, uv + vec2( o.x, -o.y));
    vec4 c = texture(u_tex, uv + vec2( o.x,  o.y));
    vec4 d = texture(u_tex, uv + vec2(-o.x,  o.y));
    return (a + b + c + d) * 0.25;
}
```

### 2.3 金字塔调度（CPU 侧伪代码）
```
level0 = baseColor(全分辨率)
for i in 1..5:
    mip[i] = renderToFbo( kawase( mip[i-1], u_offset=1.0+2.0*(i-1), texelSize=1/mip[i-1].size ) )
// 合成时按需混合 mip[1..5]（远处弥散用高层级，近处细节用低层级）
```
> 降采样 mip 尺寸可选 1/2 逐级缩减（进一步省带宽）；上采样时 `GL_LINEAR` 即可。

## 3. Domain Wrapping（领域包裹 / 无缝扰动）

### 3.1 思想（Inigo Quilez）
- 将采样坐标经过一个**周期性域变换**（domain wrap），使扰动场在平铺边界连续，消除接缝：
  ```
  f(p) 在 [0,1]² 平铺时，令 p' = wrap(p) = p + amplitude * noise(p * freq)
  ```
  其中 noise 使用**周期化哈希**（`hash(p)` 对整数格点取模）保证 `wrap(p) == wrap(p + n)`，从而纹理采样无缝循环；
- 好处：无限循环动画、任意分辨率、无 visible tile。

### 3.2 周期化噪声（伪代码）
```glsl
float hash21(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}
// 周期噪声：对取整后的格点做 mod(N)，保证 N 周期无缝
float pnoise(vec2 p, float N) {
    vec2 i = floor(p); vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);                 // smoothstep
    float a = hash21(mod(i, vec2(N)));
    float b = hash21(mod(i + vec2(1,0), vec2(N)));
    float c = hash21(mod(i + vec2(0,1), vec2(N)));
    float d = hash21(mod(i + vec2(1,1), vec2(N)));
    return mix(mix(a,b,f.x), mix(c,d,f.x), f.y);
}
// 两层 FBM 叠加 → 流动感
float fbm(vec2 p, float N) {
    return pnoise(p, N) * 0.55 + pnoise(p * 2.0, N * 2.0) * 0.3 + pnoise(p * 4.0, N * 4.0) * 0.15;
}
```

## 4. 流体扰动主 Shader（BLUR 模式）

```glsl
// uniforms
uniform sampler2D u_art;      // 专辑封面纹理
uniform vec2  u_resolution;   // 视口
uniform float u_time;         // 秒
uniform float u_scale;        // 流体缩放 (0.5~3.0)
uniform float u_speed;        // 流动速度 (0~2)
uniform float u_turbulence;   // 扰动强度 (0~2)
uniform float u_brightness;   // 全局亮度 (0.5~1.5)
uniform float u_saturation;   // 饱和度 (0~2)
uniform float u_audioEnergy;  // 音频能量 (0~1, 无音频时为 0)
uniform float u_beatPulse;    // 节拍脉冲 (0~1, 峰值衰减)

in vec2 v_uv;  out vec4 fragColor;

void main() {
    // 1) 时间驱动的双层流动坐标（方向错开制造"液体"感）
    vec2 flowDir = vec2(0.5, 0.35) * u_speed;
    vec2 p1 = v_uv * u_scale + u_time * flowDir;
    vec2 p2 = v_uv * (u_scale * 1.7) - u_time * flowDir * 0.6;

    // 2) Domain Wrapping：对封面采样坐标做周期性扰动（核心！）
    float N = 3.0;                                    // 包裹周期
    vec2 wrap1 = vec2(fbm(p1, N), fbm(p1 + 7.3, N));
    vec2 wrap2 = vec2(fbm(p2, N), fbm(p2 + 13.1, N));
    vec2 uv_warped = v_uv + (wrap1 * 0.5 + wrap2 * 0.3) * u_turbulence
                   + u_beatPulse * 0.04;              // 节拍脉冲位移

    // 3) 采样封面（边缘 clamp，配合背景纯色）
    vec3 base = texture(u_art, uv_warped).rgb;

    // 4) 音频能量驱动整体明暗起伏（可选动效）
    base *= 0.85 + 0.30 * u_audioEnergy;

    // 5) 色调调节
    float luma = dot(base, vec3(0.299, 0.587, 0.114));
    base = mix(vec3(luma), base, u_saturation);
    fragColor = vec4(base * u_brightness, 1.0);
}
```
> 输出 `baseColor` 进入 Kawase 金字塔；后续合成时可按 `mix(mip0, mip3, glowAmount)` 叠加光晕弥散。

## 5. FLUTED GLASS 波纹玻璃折射模式

思路：以扰动场生成**法线扰动**，对模糊金字塔做折射采样 + 边缘辉光，模拟"波纹玻璃"。

```glsl
// 输入：已模糊的玻璃底纹 u_scene（Kawase 金字塔 mip2）
uniform sampler2D u_scene;
uniform float u_glassStrength;   // 折射强度
uniform float u_edgeGlow;        // 边缘辉光强度

void main() {
    vec2 p = v_uv;
    // 法线扰动：由 fbm 梯度近似（中心差分）
    float e = 0.01;
    float n = fbm(p * 3.0, 3.0);
    float nx = fbm(p * 3.0 + vec2(e, 0.0), 3.0) - n;
    float ny = fbm(p * 3.0 + vec2(0.0, e), 3.0) - n;
    vec2 refractOffset = vec2(nx, ny) * u_glassStrength * 0.15;

    vec3 glass = texture(u_scene, p + refractOffset).rgb;

    // 边缘暗角+辉光（模拟玻璃边缘）
    float vignette = smoothstep(0.0, 0.35, length(p - 0.5) * 2.0);
    vec3 glow = glass * (1.0 + u_edgeGlow * vignette * 0.4);
    // 叠加高光扫动（可选）
    float spec = pow(0.5 + 0.5 * sin(p.x * 6.0 + u_time * u_speed * 0.8), 8.0);
    fragColor = vec4(glow + spec * 0.06, 1.0);
}
```

## 6. 音频涟漪 / 节拍脉冲（合成 Pass 节选）
```glsl
// 由节拍位置(以屏幕中心为原点的距离场)驱动的扩散圆环
float d = length(v_uv - 0.5);
float ring = exp(-abs(d - fract(u_beatPhase) * 0.7) * 18.0);  // 扩散涟漪
fragColor.rgb += u_rippleColor * ring * u_rippleStrength;
// 边缘光震荡：屏幕边缘按音频能量增亮
float edge = smoothstep(0.35, 0.5, max(abs(v_uv.x-0.5), abs(v_uv.y-0.5)));
fragColor.rgb += vec3(0.35, 0.55, 1.0) * edge * u_edgeEnergy;
```

## 7. Uniform 汇总（EffectComposer 统一打包）
| uniform | 来源 |
|---------|------|
| u_time / u_resolution | RenderCore 帧数据 |
| u_scale / u_speed / u_turbulence | 流体设定 |
| u_brightness / u_saturation | 全局调节 |
| u_audioEnergy / u_beatPulse / u_beatPhase / u_edgeEnergy / u_rippleStrength | AudioEngine + 动效参数 |
| u_glassStrength / u_edgeGlow | FLUTED GLASS 模式参数 |
| 粒子/光带等 | 各粒子动效参数（按启用状态动态注入） |

## 8. 性能与兼容性
- GLSL ES 3.00 要求 Android 6+（`GLES30` 可用），minSdk 26 满足；
- 模糊金字塔 5 级 ≈ 10 次全屏 pass（含降采样），可用 `glBlitFramebuffer` 或直接按 mip 尺寸渲染控制带宽；
- 低端机：金字塔降为 3 级 + 粒子组关闭（性能模式），帧率目标 30fps；
- 调试视图可叠加显示各 pass 耗时（GPU timer query，仅 debug 构建）。
