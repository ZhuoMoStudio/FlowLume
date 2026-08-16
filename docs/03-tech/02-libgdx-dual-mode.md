# 技术文档 02 · LibGDX 双形态集成方案（LiveWallpaper + Activity 全屏）

## 1. 问题域分析

| 问题 | 说明 |
|------|------|
| 双渲染上下文 | 壁纸服务与 Activity 各自拥有 Surface/EGL context，GL 资源（纹理/FBO/Program）不能跨 context 直接共享 |
| 生命周期冲突 | 壁纸 Engine 随桌面可见性启停；Activity 随前后台启停；两者可同时存在 |
| 资源重复 | 封面纹理、着色器、FBO 金字塔若各自构建将双倍内存 |
| 配置同步 | 双形态参数独立但共享引擎 |

## 2. 总体方案：共享 RenderCore + 双宿主薄壳

```
                    ┌─────────────── RenderCore (core-render, 无 Android 依赖) ───────────────┐
                    │  ShaderLibrary (Program 缓存)   EffectComposer   FboPipeline            │
                    │  RenderContext(抽象)：beginFrame/endFrame/uploadTexture/swap            │
                    └──────▲──────────────────────────────▲──────────────────────────────────┘
                           │ implements                    │ implements
              ┌────────────┴───────────┐        ┌──────────┴────────────┐
              │ WallpaperHost (形态一)  │        │ FullscreenHost (形态二) │
              │ WallpaperService.Engine │        │ MainActivity + GLSurfaceView │
              │ 自建 GL 线程 + context  │        │ LibGDX AndroidApplication     │
              └────────────────────────┘        └───────────────────────────┘
```

- **RenderCore**：纯逻辑渲染编排，通过 `RenderContext` 抽象与具体 EGL context 解耦；
- **两个宿主各自持有**：GL 线程、EGL context、帧缓冲、LibGDX `ApplicationListener` 壳（薄壳仅做 `create/render/resize/pause/resume` 转发）；
- **共享（线程安全）**：`ShaderLibrary`（着色器源码编译结果按 context 缓存）、封面 Bitmap（`ArtBus`）、`EffectRegistry`、`ConfigStore` 快照。

## 3. 形态一：LiveWallpaperService（核心代码骨架）

```kotlin
// :wallpaper 模块
class FluidWallpaperService : WallpaperService() {
    override fun onCreate() {
        super.onCreate()
        RenderCore.ensureInit()   // 初始化共享着色器库（幂等）
    }
    override fun onDestroy() {
        RenderCore.releaseIfIdle() // 双宿主引用计数归零后释放
        super.onDestroy()
    }
    override fun onCreateEngine(): Engine = FluidEngine()
}

private class FluidEngine : WallpaperService.Engine() {
    private var gdxApp: GdxWallpaper? = null          // LibGDX 壁纸壳
    private var surfaceReady = false
    private val config = RenderConfig.fromMode(Mode.WALLPAPER)

    override fun onSurfaceCreated(holder: SurfaceHolder) {
        super.onSurfaceCreated(holder)
        // LibGDX 无内置壁纸后端，需自建: GLSurfaceView 不可用于壁纸，
        // 直接基于 Surface + EGL14 手工创建 GL 上下文（见下）
        gdxApp = GdxWallpaper(holder.surface, config) { renderCore -> ... }
        gdxApp?.start()  // 启动专用 GL 线程
        surfaceReady = true
    }

    override fun onSurfaceDestroyed(holder: SurfaceHolder) {
        gdxApp?.stopAndRelease()
        gdxApp = null
        super.onSurfaceDestroyed(holder)
    }

    override fun onVisibilityChanged(visible: Boolean) {
        if (visible) gdxApp?.onResume() else gdxApp?.onPause()   // 节电：不可见即暂停渲染
    }

    override fun onOffsetsChanged(xOffset, yOffset, ...) { /* 可选：视差偏移传入 RenderCore */ }

    override fun onDestroy() {
        gdxApp?.stopAndRelease(); gdxApp = null
        super.onDestroy()
    }
}
```

**壁纸 EGL 手工上下文要点（LibGDX 壁纸集成的标准做法）**
- 不使用 `GLSurfaceView`（壁纸无 View），改为：
  ```kotlin
  // 伪代码：壁纸 GL 线程
  val display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
  EGL14.eglInitialize(display, ...)
  val config = chooseConfig(display, attribs)          // RGB_8888 + EGL_RECORDABLE_ANDROID
  val context = EGL14.eglCreateContext(display, config, EGL14.EGL_NO_CONTEXT, ...)
  val surface = EGL14.eglCreateWindowSurface(display, config, holder.surface, ...)
  loop { EGL14.eglMakeCurrent(...); Gdx.gl.glClear(...); renderCore.render(delta); eglSwapBuffers(...) }
  ```
- 用 `AndroidApplicationConfiguration` 初始化 `Gdx.gl/graphics` 环境（LibGDX 静态单例），确保 Shader/FBO 全部走标准 Gdx API；
- 壁纸必须支持 `setFixedSize` 前的任意尺寸；`onSurfaceChanged` 时调用 `renderCore.resize(w,h)`。

## 4. 形态二：Activity 全屏渲染

```kotlin
// :app 模块 —— 全屏窗口模式
class FullscreenActivity : AndroidApplication() {   // LibGDX AndroidApplication
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 沉浸式全屏
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior = BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        val cfg = AndroidApplicationConfiguration().apply {
            useImmersiveMode = true; r = 8; g = 8; b = 8; a = 0
            disableAudio = true   // 音频统一走 core-audio 模块
        }
        initialize(FullscreenGame(ConfigStore.load(Mode.FULLSCREEN)), cfg)
        // 保持屏幕常亮（计时器场景）
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}

class FullscreenGame(private val initial: RenderConfig) : Game() {
    override fun create() {
        val core = RenderCore.acquire(Mode.FULLSCREEN, initial)
        setScreen(FlowScreen(core))   // 全屏渲染 + 计时器 HUD + 手势唤出控制条
    }
}
```

## 5. 双上下文资源管理规范

| 资源 | 策略 |
|------|------|
| 封面纹理 | 不跨 context 共享 GL 纹理；`ArtBus` 广播 Bitmap，各宿主在自身 GL 线程 `PixmapTextureData` 上传；Bitmap 内存唯一副本 |
| Shader | `ShaderLibrary` 按 `Program` 源码 + context 指纹缓存；双宿主各自持有编译实例（源码共享） |
| FBO 金字塔 | 每宿主独立（尺寸随 Surface 变化），不共享 |
| 字体（计时器） | `FreeTypeFontGenerator` 每宿主按需生成，缓存于 `FontCache` |
| 粒子/网格数据 | 纯 CPU 数据（顶点数组）可安全共享只读 |

## 6. 生命周期矩阵

| 事件 | 形态一 Engine | 形态二 Activity | RenderCore |
|------|--------------|-----------------|------------|
| 创建 | `onSurfaceCreated` → GL 线程启动 | `onCreate` → GL 线程启动 | 引用计数 +1 |
| 前台 | `onVisibilityChanged(true)` 恢复渲染 | `onResume` 恢复 | — |
| 后台 | `onVisibilityChanged(false)` 暂停 | `onPause` 暂停 | — |
| 销毁 | `onSurfaceDestroyed` 释放 | `onDestroy` 释放 | 引用计数 -1，归零时释放共享库 |

## 7. 配置热重载
- 任一形态修改参数 → `ConfigStore` 写入 → 通过 `ReloadBus`（`SharedFlow<RenderConfig>`）广播；
- 双宿主各自 `collect` 后**在 GL 线程内**应用：更新 uniform、开关动效、重建 FBO 尺寸（如性能模式切换）。

## 8. 性能预算
- 基准设备：1080×2400 @ 60fps；壁纸模式支持 30fps 档（节电）；
- 帧预算：全屏 16.6ms（其中模糊金字塔 ≤ 4ms、流体主 pass ≤ 3ms、粒子 ≤ 2ms、其余为合成与上传）；
- 性能模式：渲染分辨率降至 720p + 粒子组关闭 + 30fps 上限。
