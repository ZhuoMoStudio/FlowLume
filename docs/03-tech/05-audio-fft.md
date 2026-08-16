# 技术文档 05 · 音频捕获 / FFT 频谱 / 节拍检测模块

> 合规红线：**仅捕获设备本地输出音频，绝不访问麦克风**。遵循 Google Android 官方 AudioPlaybackCapture 最佳实践。

## 1. 采集方案选型

| 方案 | 原理 | 权限 | 版本 | 结论 |
|------|------|------|------|------|
| **A. AudioPlaybackCapture**（主方案） | `MediaProjection` 创建虚拟显示 → `AudioRecord` 捕获输出流 | `MediaProjection` 系统弹窗 + `RECORD_AUDIO` | Android 10+ | ✅ 完整混音输出，含所有 App 声音 |
| B. `Visualizer` 绑定媒体 Session | 从播放器 `AudioSessionId` 读取波形/FFT | API 29+ 受限（仅自己/媒体会话） | 全版本 | ⚠️ 备选降级路径 |
| C. `MediaProjection` + 全混音 | 同上 A | — | — | 与 A 合并 |

**决策**：主用方案 A（官方 `AudioPlaybackCaptureConfiguration.Builder(projection).addMatchingUsage(USAGE_MEDIA)`）；若用户拒绝投影授权，降级方案 B 尝试绑定当前媒体会话；两者皆无则音频动效组禁用并提示。

## 2. 音频引擎架构

```
MediaProjection 授权
   → AudioRecord(捕获配置, 采样率 44100, 单声道, PCM16)
   → 环形缓冲 (1024 样本/帧, 50% 重叠)
   → Hann 加窗 → FFT(1024) → 幅值谱
   → 频段分桶 (8~12 个对数频段) → 能量平滑 (一阶低通, α=0.25)
   → 总能量 energy / 频段能量 bands
   → 节拍检测器 (onset) → beat 事件 + pulse 衰减包络
   → FrameData 发布到 EffectBus(SharedFlow, 30~60Hz)
   → 渲染 GL 线程消费 → uniforms 更新
```

## 3. FFT 实现要点（自研 Radix-2，Kotlin）

```kotlin
object Fft {
    // 迭代式 Cooley–Tukey，1024 点 < 0.5ms（低端机亦可实时）
    fun forward(re: FloatArray, im: FloatArray, n: Int) {
        var j = 0
        for (i in 1 until n) {                       // bit-reversal
            var bit = n shr 1
            while (j and bit != 0) { j = j xor bit; bit = bit shr 1 }
            j = j xor bit
            if (i < j) { swap(re, im, i, j) }
        }
        var len = 2
        while (len <= n) {
            val ang = -2.0 * PI / len
            val wRe = cos(ang); val wIm = sin(ang)
            var i = 0
            while (i < n) {
                var curRe = 1f; var curIm = 0f
                for (k in 0 until len / 2) {
                    val uRe = re[i + k]; val uIm = im[i + k]
                    val vRe = re[i + k + len / 2] * curRe - im[i + k + len / 2] * curIm
                    val vIm = re[i + k + len / 2] * curIm + im[i + k + len / 2] * curRe
                    re[i + k] = uRe + vRe; im[i + k] = uIm + vIm
                    re[i + k + len / 2] = uRe - vRe; im[i + k + len / 2] = uIm - vIm
                    val nRe = curRe * wRe - curIm * wIm; curIm = curRe * wIm + curIm * wRe; curRe = nRe
                }
                i += len
            }
            len = len shl 1
        }
    }
    // 幅值谱 → 对数频段分桶（40Hz–16kHz 8 桶）
    fun bandEnergies(mag: FloatArray, sampleRate: Int, bands: Int = 8): FloatArray { /* log-space bins */ }
}
```

## 4. 节拍检测（鼓点/能量突变）

```kotlin
class BeatDetector(
    private val sensitivity: Float = 1.4f,   // 阈值倍率（可调）
    private val minIntervalMs: Long = 180L
) {
    private var history = ArrayDeque<Float>()   // 最近 43 帧能量（≈1s @43fps）
    private var lastBeatAt = 0L

    fun process(energy: Float, nowMs: Long): Boolean {
        history.addLast(energy)
        if (history.size > 43) history.removeFirst()
        val avg = history.average().toFloat()
        val variance = history.map { (it - avg) * (it - avg) }.average().toFloat()
        val std = sqrt(variance)
        val threshold = avg + sensitivity * std          // 自适应阈值
        val isBeat = energy > threshold && energy > avg * 1.5f
        val coolDown = nowMs - lastBeatAt > minIntervalMs
        return if (isBeat && coolDown) { lastBeatAt = nowMs; true } else false
    }
}
```
> 输出：`beat` 布尔事件 + 指数衰减包络 `pulse = max(pulse * 0.92f, if(beat) 1f else pulse)`，供 shader `u_beatPulse` 使用。

## 5. 数据流发布（协程）

```kotlin
class AudioEngine(private val scope: CoroutineScope) {
    private val _frames = MutableSharedFlow<FrameData>(replay = 1, extraBufferCapacity = 2)
    val frames: SharedFlow<FrameData> = _frames

    suspend fun run() = withContext(Dispatchers.Default) {
        // AudioRecord 循环读取 → FFT → BeatDetector → _frames.emit(FrameData(...))
    }
}

data class FrameData(
    val energy: Float,        // 0..1 总能量
    val bands: FloatArray,    // 8 桶归一化
    val beat: Boolean,
    val pulse: Float          // 0..1 衰减包络
)
```

## 6. Android 权限适配坑点清单

| 坑点 | 现象 | 规避 |
|------|------|------|
| Android 14+ `MediaProjection` 每次须用户确认 | 授权弹窗频繁 | 记忆授权状态；仅在用户开启音频联动时申请 |
| `RECORD_AUDIO` 运行时权限必须与投影同时满足 | 静默失败 | 捕获开始前双重检查 |
| 部分 ROM 对 `MediaProjection` 有 3 分钟超时（旧版） | 音频中途失效 | 监听 `MediaProjection.Callback.onStop` → 自动重申请 |
| 采样率不支持 | `AudioRecord` 构造抛异常 | 遍历 `AudioRecord.getMinBufferSize` 支持的采样率 |
| 耳机/外放切换 | 音源中断 | `AudioManager` 回调监听 → 重建 AudioRecord |
| 无音乐播放时能量为 0 | 动效无反应 | 平滑归零，禁止 NaN（`energy.isFinite()` 防护） |
| 首帧频谱毛刺 | 突变脉冲 | 丢弃前 256ms 预热帧 |

## 7. 与渲染联动（GL 线程消费）
```kotlin
// RenderCore 内部
scope.launch {
    audioEngine.frames.collect { f ->
        composer.setUniform("u_audioEnergy", f.energy)
        composer.setUniform("u_beatPulse", f.pulse)
        composer.setUniform("u_ripplePhase", beatPhase(f))
        composer.setBandData(f.bands)   // 频谱径向扩散动效
    }
}
```
> 渲染线程与采集线程解耦：GL 线程仅在帧开始前读取最新 `FrameData` 快照，避免锁竞争。
