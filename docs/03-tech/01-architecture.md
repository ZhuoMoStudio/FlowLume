# 技术文档 01 · Android 工程架构设计

## 1. 技术选型总表

| 领域 | 选型 | 版本 | 说明 |
|------|------|------|------|
| 语言 | Kotlin | 1.9+ | 协程 + Flow |
| 渲染 | LibGDX | 1.12.x | 跨形态共享渲染引擎 |
| 构建 | Gradle KTS + AGP | 8.x | version catalog |
| 最低/目标 SDK | minSdk 26 / targetSdk 34 | — | 通知渠道、AudioPlaybackCapture 覆盖 |
| 持久化 | Jetpack DataStore (Preferences) | 1.1+ | 双形态配置隔离 |
| 生命周期 | AndroidX Lifecycle + ViewModel | 2.7+ | StateFlow 状态管理 |
| 后台任务 | WorkManager | 2.9+ | 监听服务自检保活 |
| 字体 | gdx-freetype | 随 LibGDX | 计时器位图字体 |
| 音频 FFT | 自研 Radix-2 FFT（Kotlin） | — | 无额外依赖，可控精度 |
| ML（规划） | TFLite + NNAPI | 2.14+ | 封面分割拓展 |

## 2. Gradle 模块划分

```
FlowLume/ (root)
├── :app                      # 入口：MainActivity + 全屏渲染 + 权限引导 UI
├── :wallpaper                # LiveWallpaperService（形态一）
├── :core-render              # LibGDX 引擎核心：Renderer、Shader、FBO 管线
├── :core-effects             # 组件化动效系统（3 大组 × 15 项动效）
├── :core-audio               # 音频捕获 + FFT + 节拍检测（可选模块）
├── :core-media               # NotificationListenerService + 封面提取
├── :core-timer               # 计时器引擎（正/倒/番茄）
├── :core-config              # DataStore 双形态配置 + 预设仓库
└── :core-ui                  # 设计系统组件（SVG 图标、控件、主题）
```

> 模块依赖方向（单向，禁止反向）：`app / wallpaper → core-ui, core-config → core-render → core-effects → core-audio / core-media / core-timer`。`core-render` 不依赖 Android 权限，便于单测与调试视图。

## 3. 分层架构（Clean + 渲染驱动）

```
┌─ Presentation 层 ─────────────────────────────┐
│  MainActivity / WallpaperService.Engine        │
│  ViewModel (StateFlow<UiState>)                │
├─ Domain 层 ────────────────────────────────────┤
│  RenderConfig / EffectState / Preset / Timer   │
│  (纯 Kotlin 数据模型 + UseCase)                 │
├─ Data 层 ──────────────────────────────────────┤
│  ConfigStore (DataStore 双 keyspace)           │
│  PresetStore (JSON 文件)                       │
│  MediaArtProvider (通知→Bitmap)                │
│  AudioEngine (播放捕获→FFT→节拍)               │
├─ Render 层（LibGDX 共享引擎）──────────────────┤
│  RenderCore(共享) → EffectComposer → Shader    │
│  WallpaperEngine(形态一实例) / FullscreenEngine(形态二实例) │
└────────────────────────────────────────────────┘
```

## 4. 双形态配置隔离（核心需求）

- DataStore 使用**两个独立 KeySpace**：
  ```kotlin
  object Keys {
      val WALLPAPER = preferencesKey<String>("wallpaper")   // 整份配置 JSON
      val FULLSCREEN = preferencesKey<String>("fullscreen") // 整份配置 JSON
  }
  ```
- 每份配置为 `RenderConfig`（渲染参数 + 动效清单 + 封面选项 + 全局调节）与 `TimerConfig`（仅全屏形态使用）的 JSON 序列化（kotlinx.serialization）；
- **复制**：`ConfigStore.copy(target: Mode)` → 读取源 JSON → 写入目标 KeySpace → 通知渲染引擎热重载；
- 设置页开关「双模式独立储存」关闭时：写入层合并为单 KeySpace，两形态共享。

## 5. 权限体系表

| 权限 | 类型 | 用途 | 缺失影响 |
|------|------|------|----------|
| `BIND_NOTIFICATION_LISTENER_SERVICE` | 特殊权限 | 读取媒体通知提取封面（核心） | 封面无法自动获取 |
| `RECORD_AUDIO`(AudioPlaybackCapture 场景) / `CAPTURE_AUDIO_OUTPUT`(系统) | 危险/系统 | 捕获设备输出音频 | 音频联动动效不可用 |
| `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | 普通 | Android 14+ 前台服务类型 | 保活失效 |
| `POST_NOTIFICATIONS` | 危险 | 计时结束/后台提示 | 无通知提醒 |

> 合规要点：音频捕获仅针对**播放输出**（`AudioPlaybackCaptureConfiguration` + `MediaProjection` 方案需用户授权），**不采集麦克风**；实现参考 Google `androidx.media3` 的 playback capture 最佳实践。Android 14+ 必须声明前台服务类型并运行时请求。

## 6. 关键依赖清单（version catalog 节选）

```kotlin
libs {
  // render
  gdx = "com.badlogicgames.gdx:gdx:1.12.1"
  gdx-backend-android = "com.badlogicgames.gdx:gdx-backend-android:1.12.1"
  gdx-freetype = "com.badlogicgames.gdx:gdx-freetype:1.12.1"
  // android
  androidx-core-ktx, lifecycle-viewmodel-ktx, datastore-preferences, work-runtime-ktx
  kotlinx-serialization-json, kotlinx-coroutines-android
  // ml (optional feature)
  org.tensorflow:tensorflow-lite + tensorflow-lite-support
}
```

## 7. 构建与签名（云端打包约定）
- 打包全部由 **GitHub Actions** 云端执行（workflow 见仓库 `.github/workflows/`），本地不产出 APK；
- 签名密钥仅以 GitHub Actions Secrets 注入（`KEYSTORE_BASE64` / `KEYSTORE_PASSWORD` / `KEY_ALIAS`），**仓库内禁止提交任何 `.jks`/`.keystore`/密码文件**；
- `.gitignore` 强制排除：`*.jks, *.keystore, keystore/, *.apk, .env, local.properties, KEYSTORE_BACKUP.txt`。

## 8. 渲染线程模型（概览）
- 每个形态持有**独立 GL 线程 + 独立 EGL context**；
- 共享（无状态或线程安全）：`RenderCore` 着色器缓存、`TextureAtlasCache`（封面纹理源）、`EffectRegistry`、`ConfigStore` 快照；
- 封面 Bitmap 更新通过 `ArtBus`（`SharedFlow<Bitmap>`）广播，各形态在自身 GL 线程上传纹理（Bitmap → GL 纹理必须在对应 context 活跃时上传，故不做跨 context 共享纹理，只共享 Bitmap 内存）。
