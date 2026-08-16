package com.zhuomo.flowlume.render

/**
 * 全部 GLSL（ES 1.00，GLES2/GLES3 兼容）。
 * 文档 04 中给出了等价 ES 3.00 参考实现；此处为最大兼容性选择 ES 1.00。
 * 核心算法：Kawase Blur（多级迭代模糊）+ Inigo Quilez Domain Wrapping（无缝扰动）。
 */

private const val VERT = """
attribute vec4 a_position;
attribute vec2 a_texCoord0;
varying vec2 v_uv;
void main() {
    v_uv = a_texCoord0;
    gl_Position = a_position;
}
"""

/** PASS-1 流体基底：Domain Wrapping 扰动采样封面 */
val FLUID_FRAG = """
precision mediump float;
uniform sampler2D u_art;
uniform float u_time;
uniform float u_scale;
uniform float u_speed;
uniform float u_turbulence;
uniform float u_brightness;
uniform float u_saturation;
uniform float u_audioEnergy;
uniform float u_beatPulse;
uniform vec2  u_pan;
uniform float u_rotate;
varying vec2 v_uv;

float hash21(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}
float pnoise(vec2 p, float N) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float a = hash21(mod(i, vec2(N)));
    float b = hash21(mod(i + vec2(1.0, 0.0), vec2(N)));
    float c = hash21(mod(i + vec2(0.0, 1.0), vec2(N)));
    float d = hash21(mod(i + vec2(1.0, 1.0), vec2(N)));
    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}
float fbm(vec2 p, float N) {
    return pnoise(p, N) * 0.55 + pnoise(p * 2.0, N * 2.0) * 0.30 + pnoise(p * 4.0, N * 4.0) * 0.15;
}
void main() {
    // 背景平移 + 微旋转
    vec2 baseUv = v_uv + u_pan;
    vec2 centered = baseUv - 0.5;
    float ca = cos(u_rotate);
    float sa = sin(u_rotate);
    centered = vec2(centered.x * ca - centered.y * sa, centered.x * sa + centered.y * ca);
    baseUv = centered + 0.5;

    vec2 flowDir = vec2(0.5, 0.35) * u_speed;
    vec2 p1 = baseUv * u_scale + u_time * flowDir;
    vec2 p2 = baseUv * (u_scale * 1.7) - u_time * flowDir * 0.6;
    float N = 3.0;
    vec2 wrap1 = vec2(fbm(p1, N), fbm(p1 + 7.3, N));
    vec2 wrap2 = vec2(fbm(p2, N), fbm(p2 + 13.1, N));
    vec2 uv_warped = baseUv + (wrap1 * 0.5 + wrap2 * 0.3) * u_turbulence + u_beatPulse * 0.04;
    vec3 base = texture2D(u_art, uv_warped).rgb;
    base *= 0.85 + 0.30 * u_audioEnergy;
    float luma = dot(base, vec3(0.299, 0.587, 0.114));
    base = mix(vec3(luma), base, u_saturation);
    gl_FragColor = vec4(base * u_brightness, 1.0);
}
"""

/** PASS-2 Kawase 单级：四偏移迭代模糊（Intel 算法） */
val KAWASE_FRAG = """
precision mediump float;
uniform sampler2D u_tex;
uniform vec2 u_texelSize;
uniform float u_offset;
varying vec2 v_uv;
void main() {
    vec2 o = u_texelSize * u_offset;
    vec4 a = texture2D(u_tex, v_uv + vec2(-o.x, -o.y));
    vec4 b = texture2D(u_tex, v_uv + vec2( o.x, -o.y));
    vec4 c = texture2D(u_tex, v_uv + vec2( o.x,  o.y));
    vec4 d = texture2D(u_tex, v_uv + vec2(-o.x,  o.y));
    gl_FragColor = (a + b + c + d) * 0.25;
}
"""

/** PASS-3 合成：光晕弥散 + 音频涟漪/波形/边缘光 + 玻璃折射 + 粒子叠加 */
val COMPOSITE_FRAG = """
precision mediump float;
uniform sampler2D u_scene;
uniform sampler2D u_glow;
uniform float u_glowMix;
uniform float u_glassMode;
uniform float u_glassStrength;
uniform float u_time;
uniform float u_beatPulse;
uniform float u_edgeEnergy;
uniform float u_rippleStrength;
uniform float u_ripplePhase;
uniform float u_waveStrength;
uniform float u_wavePhase;
uniform float u_colorDrift;
uniform float u_cornerGlow;
uniform float u_radialHalo;
uniform float u_bands[8];
varying vec2 v_uv;

float hash21(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}
float pnoise(vec2 p, float N) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float a = hash21(mod(i, vec2(N)));
    float b = hash21(mod(i + vec2(1.0, 0.0), vec2(N)));
    float c = hash21(mod(i + vec2(0.0, 1.0), vec2(N)));
    float d = hash21(mod(i + vec2(1.0, 1.0), vec2(N)));
    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}
float fbm(vec2 p, float N) {
    return pnoise(p, N) * 0.55 + pnoise(p * 2.0, N * 2.0) * 0.30 + pnoise(p * 4.0, N * 4.0) * 0.15;
}
void main() {
    vec2 uv = v_uv;
    // FLUTED GLASS 折射：法线扰动采样（波纹玻璃）
    vec2 glassUv = uv;
    if (u_glassMode > 0.5) {
        float e = 0.01;
        float n = fbm(uv * 3.0, 3.0);
        float nx = fbm(uv * 3.0 + vec2(e, 0.0), 3.0) - n;
        float ny = fbm(uv * 3.0 + vec2(0.0, e), 3.0) - n;
        glassUv = uv + vec2(nx, ny) * u_glassStrength * 0.15;
    }
    vec3 scene = texture2D(u_scene, glassUv).rgb;
    vec3 glow  = texture2D(u_glow, glassUv).rgb;

    // 全局光晕弥散（BLUR 模式核心）
    vec3 col = mix(scene, glow, u_glowMix);

    // 节拍脉冲震动
    vec2 pulseUv = uv + (uv - 0.5) * u_beatPulse * 0.02;
    if (u_beatPulse > 0.001) {
        col = mix(col, texture2D(u_scene, pulseUv).rgb, 0.6);
    }
    // 音频扩散涟漪
    float d = length(pulseUv - 0.5);
    float ring = exp(-abs(d - fract(u_ripplePhase) * 0.7) * 18.0);
    col += vec3(0.45, 0.65, 1.0) * ring * u_rippleStrength;
    // 横向波形扫动
    float wave = exp(-abs(pulseUv.x - fract(u_wavePhase)) * 20.0);
    col += vec3(1.0, 0.7, 0.4) * wave * u_waveStrength;
    // 画面边缘光震荡
    float edge = smoothstep(0.35, 0.5, max(abs(pulseUv.x - 0.5), abs(pulseUv.y - 0.5)));
    col += vec3(0.35, 0.55, 1.0) * edge * u_edgeEnergy;
    // 色彩缓慢渐变（近似色相偏移）
    col = mix(col, col.gbr, u_colorDrift * 0.12);
    // 频谱径向扩散（u_bands 常量索引展开）
    float ang = atan(pulseUv.y - 0.5, pulseUv.x - 0.5);
    float angN = ang / 6.28318 + 0.5;
    float bandIdx = angN * 8.0;
    float bandE =
        u_bands[0] * step(0.0, bandIdx) * step(bandIdx, 1.0) +
        u_bands[1] * step(1.0, bandIdx) * step(bandIdx, 2.0) +
        u_bands[2] * step(2.0, bandIdx) * step(bandIdx, 3.0) +
        u_bands[3] * step(3.0, bandIdx) * step(bandIdx, 4.0) +
        u_bands[4] * step(4.0, bandIdx) * step(bandIdx, 5.0) +
        u_bands[5] * step(5.0, bandIdx) * step(bandIdx, 6.0) +
        u_bands[6] * step(6.0, bandIdx) * step(bandIdx, 7.0) +
        u_bands[7] * step(7.0, bandIdx) * step(bandIdx, 8.0);
    col += vec3(0.5, 0.8, 1.0) * bandE * 0.15 * (1.0 - d);
    // 径向光晕
    col += vec3(0.6, 0.5, 1.0) * u_radialHalo * exp(-d * 4.0);
    // 四角辉光
    float corner = exp(-length(uv - vec2(0.0, 0.0)) * 6.0)
                 + exp(-length(uv - vec2(1.0, 0.0)) * 6.0)
                 + exp(-length(uv - vec2(0.0, 1.0)) * 6.0)
                 + exp(-length(uv - vec2(1.0, 1.0)) * 6.0);
    col += vec3(0.4, 0.6, 1.0) * corner * u_cornerGlow * 0.25;
    // 玻璃高光扫动
    if (u_glassMode > 0.5) {
        float spec = pow(0.5 + 0.5 * sin(uv.x * 6.0 + u_time * 0.8), 8.0);
        col += spec * 0.06;
    }
    gl_FragColor = vec4(col, 1.0);
}
"""

/** PASS-4 最终合成：亮度 / 饱和度 */
val FINAL_FRAG = """
precision mediump float;
uniform sampler2D u_scene;
uniform float u_brightness;
uniform float u_saturation;
varying vec2 v_uv;
void main() {
    vec3 c = texture2D(u_scene, v_uv).rgb;
    float luma = dot(c, vec3(0.299, 0.587, 0.114));
    c = mix(vec3(luma), c, u_saturation);
    gl_FragColor = vec4(c * u_brightness, 1.0);
}
"""

/** 粒子点精灵（装饰粒子动效组） */
val PARTICLE_VERT = """
attribute vec4 a_position;   // x(0..1) y(0..1) size(px) alpha
attribute vec4 a_color;      // rgba
varying vec4 v_color;
uniform float u_pointScale;
void main() {
    v_color = a_color;
    gl_Position = vec4(a_position.x * 2.0 - 1.0, 1.0 - a_position.y * 2.0, 0.0, 1.0);
    gl_PointSize = a_position.z * u_pointScale;
}
"""

val PARTICLE_FRAG = """
precision mediump float;
varying vec4 v_color;
void main() {
    vec2 c = gl_PointCoord - 0.5;
    float d = length(c) * 2.0;
    float alpha = (1.0 - smoothstep(0.0, 1.0, d)) * v_color.a;
    gl_FragColor = vec4(v_color.rgb, alpha);
}
"""

object ShaderSources {
    const val VERT = VERT
    val FLUID = VERT + FLUID_FRAG
    val KAWASE = VERT + KAWASE_FRAG
    val COMPOSITE = VERT + COMPOSITE_FRAG
    val FINAL = VERT + FINAL_FRAG
    val PARTICLE = PARTICLE_VERT + PARTICLE_FRAG
}
