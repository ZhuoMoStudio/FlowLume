# 技术文档 08 · 本地 ML 图像分割分层渲染（高级拓展，预留接口）

## 1. 功能愿景
- 对专辑封面执行**前景/背景分割**（人像/主体 vs 背景）；
- 渲染时：**背景层**应用流体扰动+模糊（"融化"），**前景主体层**保持相对清晰；
- 合成后画面呈现空间纵深：主体浮在流动光影前方。

```
原封面 ──► ML 分割 ──► mask(alpha)
   │                    │
   ▼                    ▼
背景(1-alpha) ──► 流体扰动/模糊 ──┐
                                 ├─► 合成 → 输出
前景(alpha) ──► 原图(微光效)  ──┘
```

## 2. 接口预留（本期不实现推理，仅留接口）

```kotlin
// :core-render 模块 —— 渲染层只依赖抽象
interface SegmentationProvider {
    val available: Boolean
    /** 输入封面 Bitmap → 输出与封面同尺寸的单通道 mask（0=背景, 255=前景） */
    suspend fun segment(art: Bitmap): Bitmap?
}

object Segmentation {
    @Volatile var provider: SegmentationProvider = NoOpSegmentation()
    suspend fun mask(art: Bitmap): Bitmap? = provider.segment(art)
}

class NoOpSegmentation : SegmentationProvider {
    override val available = false
    override suspend fun segment(art: Bitmap): Bitmap? = null   // 返回 null → 走整图流体路径
}
```

- 实验性页面（页面6）中的「封面智能分割（预览）」开关即绑定此接口；
- 渲染管线内：
  ```kotlin
  if (segmentationEnabled && Segmentation.provider.available) {
      maskTex = upload(mask)          // 上传 mask 为 alpha 纹理
      composer.enableLayerSplit(maskTex)  // 双路径合成
  }
  ```

## 3. 推理实现方案（开发期路线）

| 项 | 选型 |
|----|------|
| 模型 | 轻量分割模型：MediaPipe Selfie Segmentation 风格（MobileNetV3-small 骨干，输出 256×256 mask）或 DeepLabV3-MobileNetV3 量化版 |
| 推理框架 | TFLite 2.14+，`ImageSegmenter` API（tensorflow-lite-support） |
| 硬件加速 | NNAPI delegate（优先 NPU/DSP），降级 GPU delegate，再降级 CPU |
| 输入预处理 | 封面缩放到 256×256，`ImageProcessingOptions` 旋转对齐 |
| 输出后处理 | mask 双线性放大至渲染分辨率，softmax 二值化阈值 0.5 |
| 触发策略 | 封面更新时异步推理（`Dispatchers.Default`），结果缓存 `maskCache[artHash]`；渲染沿用上一帧 mask，避免卡顿 |

## 4. 性能预算
- 推理：256×256 NNAPI ≤ 15ms；CPU 端 ~40ms → 封面切换瞬间有 1 帧延迟可接受（缓存）；
- 内存：模型 ~4MB（量化 int8）+ mask 纹理 ≤ 封面尺寸×4B；
- 渲染双路径额外开销：1 次额外全屏采样 pass（≤1.5ms）；
- 低端机：实验性功能默认关闭，仅高级选项开启。

## 5. 后续演进（预留）
- **多主体分割**：升级模型支持人/物多类 mask，主体分级景深；
- **动态分割**：按节拍能量动态调整前景/背景模糊强度比，强化律动纵深；
- **训练方案**：基于开源数据集（PASCAL-VOC / ADE20K 蒸馏）轻量蒸馏 + 量化感知训练，产出 TFLite 产物由 GitHub Actions 自动构建进 APK assets。
