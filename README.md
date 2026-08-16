# FlowLume · 弥光

> **Fluid Music Live Wallpaper for Android** — 让手机桌面变成一面会呼吸的流体光影：音乐响起，光流随之律动。

流体时钟壁纸项目 · 完整设计方案仓库。本文档库包含产品层、UI 设计层、技术开发层全部交付物，可直接作为工程实施蓝图。

## ✨ 核心特性

- **双形态独立渲染**：系统 LiveWallpaper 桌面壁纸 + App 全屏沉浸窗口，共享同一 LibGDX 引擎，参数独立保存、一键互拷
- **封面自动提取**：NotificationListenerService 监听系统媒体通知，自动提取专辑封面作为流体基底（兼容 Apple Music / Spotify / YouTube Music 等）
- **双渲染管线**：BLUR 基础模糊 / FLUTED GLASS 波纹玻璃折射（Kawase Blur + Domain Wrapping 无缝扰动）
- **音频可视化（可选）**：捕获设备输出音频（不读麦克风），FFT 频谱 + 鼓点检测驱动流体
- **组件化动效系统**：3 大分组 17 项动效，独立开关 + 参数滑块，预设保存/导入/导出
- **全屏计时器**：正计时 / 倒计时 / 番茄工作法，时间文字样式全自定义
- **性能模式 / 调试视图 / 实验性功能分区**完备

## 📁 文档结构

```
docs/
├── 01-product/            产品层
│   ├── app-names.md       应用名称备选清单（商用/文艺 双分类）
│   ├── intro-copy.md      应用简介文案（中英双语）
│   ├── user-flow.md       用户完整操作流程
│   └── help-page.md       App 内置帮助页面定稿文案
├── 02-ui/                 UI 设计层
│   ├── design-system.md   全局设计系统 Tokens（色彩/字体/间距/控件状态）
│   └── ui-pages.md        8 页面布局与交互规范
└── 03-tech/               技术开发层（工程可落地）
    ├── 01-architecture.md        Android 工程架构
    ├── 02-libgdx-dual-mode.md    LibGDX 双形态集成
    ├── 03-notification-listener.md 通知监听 + 保活
    ├── 04-shaders.md             Kawase Blur + Domain Wrapping GLSL
    ├── 05-audio-fft.md           音频捕获 / FFT / 节拍检测
    ├── 06-effects-system.md      组件化动效系统
    ├── 07-timer.md               计时器 + 全屏沉浸
    ├── 08-ml-extension.md        本地 ML 分割分层渲染（预留）
    └── 09-issues.md              故障处理方案
```

## 🧱 Android 工程结构（9 模块）

| 模块 | 职责 |
|------|------|
| `:app` | MainActivity（8 页面 Compose UI）+ FullscreenActivity（全屏渲染 + 计时器浮层） |
| `:wallpaper` | 形态一：LiveWallpaperService + 手工 EGL 上下文渲染线程 |
| `:core-render` | LibGDX 渲染核心：RenderCore / FBO 管线 / Kawase 金字塔 / ShaderLibrary |
| `:core-effects` | 组件化动效系统：17 项动效 + 注册表 + 性能策略 |
| `:core-audio` | AudioPlaybackCapture（不读麦克风）+ FFT + 节拍检测 |
| `:core-media` | NotificationListenerService 封面提取 + 保活自检 |
| `:core-timer` | 正计时 / 倒计时 / 番茄工作计时器引擎 |
| `:core-config` | DataStore 双形态配置隔离 + 预设系统 |
| `:core-ui` | Compose 设计系统（SVG 图标 / 控件 / 主题 Tokens） |

技术栈：Kotlin 2.0 · AGP 8.7 · LibGDX 1.12 · GLSL ES 1.00 · Jetpack Compose · DataStore · WorkManager · Gradle 8.10

## 🔧 云端打包（GitHub Actions）

- 推送 `main` 自动触发 `build-debug`（debug APK 产物）；`build-release` 在 Secrets 配置齐全后自动签名构建；
- 签名 Secrets 四项：`KEYSTORE_BASE64` / `KEYSTORE_PASSWORD` / `KEY_ALIAS` / `KEYSTORE_KEY_PASSWORD`；
- 本地不打包、不产出 APK，仓库内零密钥文件（详见 `.gitignore`）。

## 🔒 安全约定

- 打包全部由 GitHub Actions 云端执行，本地不产出 APK；
- 签名密钥仅经 GitHub Secrets 注入，**仓库内严禁提交任何 `.jks` / 密码 / token / `.env`**；
- 已配置 `.gitignore` 强制排除全部敏感文件。

## 📄 开源协议

本项目基于 **Apache License 2.0** 开源（详见 [LICENSE](./LICENSE)）。
宽松许可、商业友好，与 LibGDX / AndroidX 生态一致；允许闭源商用，需保留版权声明。

## 🌍 多语言

- 默认跟随系统语言（中文 / English），设置页可手动切换；
- 文案全部位于 `app/src/main/res/values*/strings.xml`，欢迎通过 PR 补充其他语言。

## 📌 Roadmap

- [ ] Android 工程脚手架 + 双形态渲染骨架
- [ ] Kawase Blur + Domain Wrapping 渲染管线
- [ ] 通知监听封面提取
- [ ] 音频 FFT + 节拍模块
- [ ] 动效系统 + 预设
- [ ] 计时器系统
- [ ] ML 分割拓展（实验性）

---

© ZhuoMoStudio · 设计文档库（初始提交）
