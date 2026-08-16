# 全页面 UI 布局与交互规范（8 页）

> 底部导航（两种形态共用）：`HOME 首页 | RENDER 渲染 | FX 动效 | TIMER 计时器* | SETTINGS 设置`；`*` 计时器 Tab 仅 App 全屏形态显示。

---

## 页面1 · 启动权限引导首页（HOME）

**布局（自上而下）**
1. **顶部区**：品牌区——应用名大标题「FLOWLUME」+ 副标语「Fluid Music Wallpaper」+ 形态徽章（当前形态：DESKTOP WALLPAPER / APP FULLSCREEN）；
2. **权限状态卡片**（白描边圆角卡）：
   - 行1：图标(SVG 铃铛) + 「ALLOW NOTIFICATION ACCESS 允许阅读通知」+ 状态点（绿=已授权 / 黄=未授权）+ 右侧 [去授权] 次级按钮；
   - 行2：图标(SVG 波形) + 「AUDIO CAPTURE 音频捕获(可选)」+ 状态点 + 右侧 [去授权]；
3. **现在播放状态面板**（大卡片，内含实时缩略预览 16:9）：
   - 有音乐：「NOW PLAYING」+ 封面缩略图 + 《歌名》- 歌手 + 迷你频谱条动画；
   - 无音乐：「NO MUSIC DETECTED 未检测到播放中的音乐」+ 引导文案「播放音乐后自动提取专辑封面」；
   - 权限缺失：「NOTIFICATION ACCESS REQUIRED」警告文案 + [一键重启通知监听服务] 按钮；
4. **主操作区**：[SET AS WALLPAPER 设置壁纸] 淡紫主按钮（整宽 48dp）；
5. **形态切换入口**：分段控件（Segmented）「DESKTOP WALLPAPER / APP FULLSCREEN」+ 说明小字「两种形态参数独立保存，可一键互拷」；
6. 底部 Tab。

**交互逻辑**
- 点击 [去授权] → 跳转系统设置页（`Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS` / 音频：系统弹窗）；
- 点击 [设置壁纸] → 若未授权通知则先弹确认弹窗「WALLPAPER READY? 壁纸需要通知权限才能显示封面，是否前往授权？」→ 已授权则直接打开系统壁纸选择器；
- 切形态 → 面板参数整体切换为该形态配置，状态面板缩略预览同步切换。

---

## 页面2 · 通用渲染参数面板（RENDER）

**布局**
1. **FLUID 流体设定卡片**
   - `FLUID SCALE 流体缩放` slider（0.5–3.0，步进 0.1，默认 1.0）；
   - `FLOW SPEED 流动速度` slider（0–200%，默认 100%）；
2. **GRAPHICS 图形设定卡片**
   - 单选组（圆形 Radio）：`BLUR` 基础模糊 / `FLUTED GLASS` 波纹玻璃折射；下方动态说明文案描述当前模式；
   - 切换时提示「渲染模式已切换」（Snackbar），实时预览。
3. **ALBUM ART 专辑封面配置卡片**
   - ☑ `RESTORE LAST ART ON REBOOT 重启应用保留上次封面`（默认开）；
   - ☑ `KEEP ART ON PAUSE 音乐暂停保留上次封面`（默认开）；
   - `DEFAULT TONE 选择默认色调`：色板 6×2 色块单选（无音乐时的流体基底色）。
4. **GLOBAL ADJUST 全局画面调节卡片**
   - `BRIGHTNESS 亮度` slider（50–150%）；
   - `SATURATION 色彩饱和度` slider（0–200%）。
5. **配置互拷卡片**
   - [COPY TO FULLSCREEN 复制到全屏模式] / [COPY TO WALLPAPER 复制到桌面壁纸]（按当前形态显示目标按钮）+ 说明「当前编辑：DESKTOP WALLPAPER 配置」。

**交互逻辑**
- 所有调节实时生效：壁纸模式下渲染引擎常驻，滑动即时反馈；
- 点击复制 → 确认弹窗「OVERWRITE TARGET CONFIG? 将用当前配置覆盖目标形态的全部参数，确定？」→ [CANCEL]/[OVERWRITE]。

---

## 页面3 · 动效控制面板（FX）——核心模块

**布局**
1. **预设快捷条**（横向滚动）：官方预设胶囊标签：「DEEP LUMEN 深海弥光」「NEON BEAT 霓虹节拍」「MORNING HAZE 晨雾」「GLASS GARDEN 璃园」+ [+ 预设管理] 入口；
2. **三张分组卡片**（可折叠，默认展开）：

   **A. BASE FLUID 基础流体动效组**
   | 动效 | 启用 | 参数滑块 |
   |------|------|----------|
   | FLUID TURBULENCE 流体扰动强度 | ☑ | 强度 / 速度 |
   | BG PAN 背景缓慢平移 | ☐ | 速度 |
   | MICRO ROTATION 画面微旋转 | ☐ | 速度 / 幅度 |
   | COLOR DRIFT 色彩缓慢渐变 | ☑ | 速度 / 幅度 |
   | GLOBAL GLOW 全局光晕弥散 | ☑ | 强度 / 尺寸 |

   **B. AUDIO SYNC 音频联动动效组**（无音频权限时整组禁用态 + 黄色提示「需开启音频捕获」）
   | 动效 | 启用 | 参数滑块 |
   |------|------|----------|
   | RIPPLE SPREAD 音频扩散涟漪 | ☑ | 强度 / 速度 / 尺寸 |
   | WAVE SWEEP 横向波形扫动 | ☐ | 强度 / 速度 / 尺寸 |
   | BEAT PULSE 节拍脉冲震动 | ☑ | 强度 / 速度 / 尺寸 |
   | EDGE FLICKER 画面边缘光震荡 | ☐ | 强度 / 速度 / 尺寸 |
   | ART BREATHING 封面明暗起伏 | ☑ | 强度 / 速度 / 尺寸 |
   | SPECTRUM RADIAL 频谱径向扩散 | ☐ | 强度 / 速度 / 尺寸 |

   **C. PARTICLE DECOR 装饰粒子动效组**
   | 动效 | 启用 | 参数滑块 |
   |------|------|----------|
   | FLOATING MOTES 漂浮光点粒子 | ☑ | 密度 / 速度 / 尺寸 |
   | BOKEH BLOBS 散景光斑 | ☐ | 密度 / 速度 / 尺寸 |
   | GRADIENT RIBBONS 流动渐变光带 | ☐ | 强度 / 速度 / 尺寸 |
   | RADIAL HALO 径向光晕 | ☑ | 强度 / 尺寸 |
   | CORNER GLOW 四角辉光 | ☐ | 强度 / 尺寸 |
   | STAR DRIFT 缓慢星点浮动 | ☐ | 密度 / 速度 / 尺寸 |

3. **每条动效行结构**：启用复选框(20dp) + 动效名(中英) + 展开箭头 → 展开后显示 2–3 个滑块（强度/速度/尺寸）；
4. **预设管理弹窗**：列表（官方预设锁形图标不可删；自定义预设可长按删除）；[SAVE CURRENT 保存当前]→ 命名输入弹窗；[IMPORT] 文件选择器（JSON）；[EXPORT] 分享当前预设 JSON。

**交互逻辑**
- 任何开关/滑块变更实时预览并即时写入当前形态配置（DataStore）；
- 音频组整体禁用：组内全部控件 40% 透明度 + 顶部黄色状态条「ENABLE AUDIO CAPTURE TO USE AUDIO FX」；
- 预设套用 → Snackbar「PRESET APPLIED: 深海弥光」。

---

## 页面4 · 全屏模式专属面板（TIMER）——仅 App 全屏形态显示

**布局**
1. **MODE 模式卡片**：单选组（圆形 Radio）：`COUNT UP 正计时` / `COUNT DOWN 倒计时` / `POMODORO 番茄工作计时器`；
2. **DURATION 时长卡片**：数字滚轮/步进器（分：秒，如 25:00；范围 00:01–99:59）；☑ `LOOP 循环模式` 开关；
3. **ALERT 提醒方式卡片**：☑ 震动 / ☑ 弹窗提示 / ☑ 提示音（三者独立复选框；弹窗开关可关）；
4. **TIME TEXT 时间文字样式卡片**：
   - `FONT SIZE 字体大小` slider（24–200sp）；
   - `COLOR 文字颜色`：色板单选 + 自定义 HEX 输入；
   - `OPACITY 透明度` slider（10–100%）；
   - `STROKE 描边粗细` slider（0–12dp）+ 描边颜色色板；
   - `POSITION 文字位置`：九宫格定位选择（左上/上中/右上/左中/**居中**/右中/左下/下中/右下）+ `CUSTOM X/Y 自定义坐标` 输入（-1000–1000）；
   - `LAYER 图层`：单选 `TOP 画面顶层` / `BOTTOM 流体底层`；
5. **沉浸模式**：`HIDE UI 隐藏全部控件` 按钮 → 全屏纯流体（点按任意处唤出 3 秒控制浮层）。

**交互逻辑**
- 切模式 → 时长区联动（番茄模式显示「工作/休息」双时长：如 25:00 / 05:00）；
- 计时运行中修改样式实时生效；[开始/暂停/重置] 主按钮位于面板底部（全屏时悬浮于右下角圆形按钮）；
- 计时结束 → 按提醒开关组合触发：震动 + 弹窗「TIME'S UP 时间到」+ 提示音。

---

## 页面5 · 杂项设置（SETTINGS / MISC SETTINGS）

**布局**
1. **STORAGE 存储行为卡片**
   - ☑ `SEPARATE WALLPAPER & FULLSCREEN CONFIGS 桌面/全屏模式相互独立储存参数`（默认开；关闭后两形态共用同一套参数）；
2. **APPEARANCE 外观卡片**
   - ☑ `ADAPTIVE LAUNCHER ICON 启动器自适应着色`（默认开：图标随系统主题生成单色/彩色变体）；
   - 主题单选：`DARK #0A0A0F 纯黑` / `CHARCOAL #14141C 灰黑`；
3. **PERFORMANCE 性能卡片**
   - ☑ `PERFORMANCE MODE 性能模式`：降低渲染分辨率(默认 1080p→720p)、自动关闭装饰粒子组、降低粒子密度 → 功耗下降；开启时卡片出现黄色说明条；
4. **SERVICE 服务卡片**
   - [RESTART NOTIFICATION LISTENER 一键重启通知监听服务] → 执行后 Snackbar「SERVICE RESTARTED」；
   - [OPEN BATTERY OPTIMIZATION 打开电池优化白名单] → 跳系统设置；
5. **系统区**
   - [实验性功能 EXPERIMENTAL] 入口（带「BETA」红色徽章）→ 页面6；
   - [关于 ABOUT] → 页面7；[帮助中心 HELP] → 页面8。

---

## 页面6 · 实验性功能分区（EXPERIMENTAL）

**布局**
1. **顶部警示横幅**（警告色描边卡片）：「⚠ EXPERIMENTAL FEATURES 实验性功能 —— 板块内功能持续开发，稳定性较差，仅供测试体验。可能导致崩溃或异常表现。」
2. **功能列表**（白描边卡片，禁用态样式为"敬请期待"）：
   - `DEBUG VIEW 调试视图`：开启后在渲染画面叠加 HUD（帧率 / 频谱 / 节拍能量 / 封面解析状态 / FBO 层级），数据用 `mono` 等宽字体；
   - `MASK SEGMENTATION 封面智能分割（预览）`：本地 ML 图像分割接口占位，标注「开发中」；
   - `DOF SIMULATION 景深模拟（规划）`：标注「规划中」；
   - 其余预留入口统一灰色 + 「SOON」标签。
3. 底部常驻提示：「实验性功能默认关闭，开启后请留意耗电与稳定性」。

**交互逻辑**
- 调试视图开关：立即生效，渲染画面右上角出现调试 HUD 浮层，可拖动。

---

## 页面7 · 关于页面（ABOUT）

**布局**
1. **品牌区**：应用图标(SVG) + 「FLOWLUME」+ 版本号 `Version 0.1.0 (1)` + 版权行 `© ZhuoMoStudio`;
2. **链接卡片**：[FEEDBACK 反馈问题]（携带日志）、[OTHER WORKS 其他作品]（跳转开发者主页）；
3. **致谢卡片（CREDITS）**：
   - 图形算法：Intel Kawase Blur、Inigo Quilez Domain Wrapping & Shader Art；
   - 开源库：LibGDX (BadlogicGames)、gdx-freetype、Kotlinx Coroutines、AndroidX、AudioViz / Droid-vizu 灵感参考；
   - 图标：自绘 SVG 矢量图标集；
4. **开源许可**：GPL-3.0 / Apache-2.0（以最终选择为准）说明行。

---

## 页面8 · 内置帮助中心（HELP）

**布局**
1. 顶部搜索框（白描边圆角，占位符「SEARCH HELP 搜索帮助」）；
2. 帮助分类卡（Accordion 展开式问答，内容见 `01-product/help-page.md` 定稿）：
   - 软件工作原理 / 通知权限说明 / 音频权限答疑 / 封面读取失败排查 / 播放器兼容 / 电池优化 / 常见 FAQ / 联系开发者；
3. 底部：「无法解决？→ 反馈问题」入口。

---

## 全局交互补充规则
- **页面跳转**：底部 Tab 切换页面，无过渡堆叠；次级页面（实验性/关于/帮助）压栈进入，返回箭头返回；
- **弹窗触发条件**：破坏性操作（覆盖配置/删除预设）必须确认弹窗；权限跳转前弹说明弹窗；计时结束弹提醒弹窗；
- **空状态**：预设列表为空时显示「NO PRESETS 暂无预设，点击保存当前参数」；
- **触觉反馈**：开关/按钮轻震动（VibrationEffect，时长 8ms），可在设置中关闭（预留）。
