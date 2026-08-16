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

## 🛠 技术栈

Kotlin · LibGDX 1.12 · GLSL ES 3.00 · Jetpack DataStore · WorkManager · Kotlinx Coroutines/Serialization · TFLite（规划）

## 🔒 安全约定

- 打包全部由 GitHub Actions 云端执行，本地不产出 APK；
- 签名密钥仅经 GitHub Secrets 注入，**仓库内严禁提交任何 `.jks` / 密码 / token / `.env`**；
- 已配置 `.gitignore` 强制排除全部敏感文件。

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
