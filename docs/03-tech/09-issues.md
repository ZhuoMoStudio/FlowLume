# 技术文档 09 · 故障处理方案汇总

## 1. 故障分类总表

| 编号 | 故障现象 | 根因 | 处理方案 | 优先级 |
|------|----------|------|----------|--------|
| F-01 | 封面不更新/停留旧封面 | NotificationListenerService 被系统回收或权限被撤销 | ① 权限自检 WorkManager 周期任务；②「一键重启通知监听服务」；③ 电池白名单引导 | P0 |
| F-02 | 壁纸黑屏 | EGL 上下文创建失败（个别 ROM/GPU） | ① `EGL_RECORDABLE_ANDROID` 配置兼容回退（先试记录型，失败降级非记录型）；② 着色器编译失败回退内置兜底纯色 shader | P0 |
| F-03 | 首次启动白屏闪退 | GLES3 不可用 / shader 编译错误 | 启动时 `GLES30` 检测；shader 编译日志上报；失败降级 GLES2 简化管线 | P0 |
| F-04 | 切歌后封面延迟 | 通知解析慢 / 大图解码 | 封面解码异步 + 512px 缩放上限 + `LruCache`；解码完成前沿用旧封面 | P1 |
| F-05 | 音频联动无反应 | 投影授权失效 / AudioRecord 初始化失败 | 权限状态机 + 自动重申请（`MediaProjection.Callback`）；降级 Visualizer 路径 | P1 |
| F-06 | 耗电过高 | 高帧率 + 全粒子 + 高分辨率 | 性能模式（720p + 30fps + 粒子关闭）；壁纸不可见自动暂停（已有）；新增「智能帧率」（静止画面降 30fps） | P1 |
| F-07 | 低端机卡顿 | FBO 金字塔带宽超限 | 金字塔 5 级→3 级动态降级；流体缩放上限内建议值；粒子密度自动缩减 | P1 |
| F-08 | 双形态同时运行内存峰值 | 两套 FBO+纹理 | 共享 Bitmap 唯一副本；FBO 按需分配；`onTrimMemory` 释放 mip 缓存 | P2 |
| F-09 | 计时器通知被系统静音 | 通知渠道未配置 | 创建 `MediaPlayback` 渠道 + 高重要性；用户可关 | P2 |
| F-10 | 国产 ROM 自启动被拒 | 厂商后台限制 | 品牌 Intent 映射引导（MIUI/EMUI/OriginOS/ColorOS）；帮助文档说明 | P2 |
| F-11 | 封面方向错误 | 竖屏封面 EXIF 旋转 | 解码时应用 EXIF 旋转（`ExifInterface`） | P2 |
| F-12 | 实验性功能崩溃 | 未成熟代码 | 实验性开关独立 try-catch 边界 + 崩溃后自动回退默认值 + 提示「已恢复默认」 | P3 |

## 2. 关键机制实现

### 2.1 一键重启通知监听服务（F-01）
```kotlin
fun restartNotificationListener(context: Context) {
    val enabled = NotificationManagerCompat.getEnabledListenerPackages(context)
        .contains(context.packageName)
    if (!enabled) {
        // 未授权 → 跳系统设置
        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        return
    }
    // 已授权但服务失活 → 反射触发系统重绑（API 31+ 有 requestRebind，hidden）
    runCatching {
        val nm = context.getSystemService(NotificationManager::class.java)
        val m = NotificationManager::class.java.getMethod("requestRebind", ComponentName::class.java)
        m.invoke(nm, ComponentName(context, MediaNotificationListener::class.java))
    }.onFailure { // 降级：引导用户手动关/开
        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    }
    NotificationCenter.status.emit(ListenerStatus.RESTARTING)
}
```
> 配合 WorkManager 自检在 10s 后确认 `CONNECTED` 状态并 Snackbar 反馈结果。

### 2.2 GL 故障兜底（F-02 / F-03）
- 全局 `Thread.UncaughtExceptionHandler`：捕获 GL 线程异常 → 记录日志 → 尝试重建 EGL 上下文（最多 2 次）→ 仍失败则切换到兜底渲染器（纯色 + 时间文字，保证壁纸不黑屏）；
- 构建期：`EGL_RECORDABLE_ANDROID` 先尝试，异常回退。

### 2.3 崩溃日志
- 本地日志：`filesDir/logs/` 滚动文件（每个 session 一个，容量 1MB）；
- 反馈功能：选择「反馈问题」→ 打包最近日志 + 设备信息（品牌/型号/Android 版本/分辨率）→ 分享；不上传自动，仅用户主动；
- 生产环境可接 Firebase Crashlytics（可选开关，默认关闭以保护隐私）。

## 3. 调试视图（页面6 实验性功能）
调试 HUD 展示：FPS、帧耗时分解（模糊/流体/粒子/合成）、GL 版本、EGL 配置、封面解析状态（`ART_OK / ART_PENDING / ART_NONE`）、监听服务状态（`CONNECTED / REVOKED`）、音频状态（`CAPTURING / IDLE / ERROR:xxx`）、当前 FBO 层级数。
数据源：`RenderCore.debugStats`（`StateFlow<DebugStats>`，每 500ms 刷新）。
