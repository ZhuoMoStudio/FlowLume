# UI 全局设计系统规范（Design Tokens）

> 设计语言：**深色极简赛博科技美学**。所有页面共享同一套 Design Tokens，保证设计一致性。

## 1. 色彩体系

### 1.1 主题色板
| Token | 色值 | 用途 |
|-------|------|------|
| `bg/primary` | `#0A0A0F` | 默认纯黑深色主题底色 |
| `bg/secondary` | `#14141C` | 暗色灰黑主题底色（可切换） |
| `bg/card` | `#12121A` | 卡片内衬色 |
| `stroke/card` | `#FFFFFF` @ 40% | 白色细描边（卡片、输入框） |
| `stroke/divider` | `#FFFFFF` @ 12% | 分隔线 |
| `accent/primary` | `#9D7BFF` | 淡紫色主色（主按钮、选中态、进度条） |
| `accent/press` | `#7E5BEF` | 主色按压态 |
| `accent/disabled` | `#9D7BFF` @ 30% | 主色禁用态 |
| `text/primary` | `#F5F5FA` | 主文字 |
| `text/secondary` | `#A6A6B8` | 次级文字 |
| `text/tertiary` | `#6E6E80` | 说明文字/占位符 |
| `text/on-accent` | `#FFFFFF` | 主按钮内文字 |
| `state/success` | `#4ADE80` | 成功状态（权限已开启） |
| `state/warning` | `#FBBF24` | 警告状态（权限缺失引导） |
| `state/danger` | `#F87171` | 危险/删除操作 |
| `overlay/scrim` | `#000000` @ 60% | 弹窗遮罩 |
| `overlay/glass` | `#FFFFFF` @ 6% | 浮层玻璃底 |

### 1.2 全局背景底衬
- 底层铺设**低透明度暗色插画**（如流体线条/粒子剪影），透明度 ≤ 10%，不得遮挡文字与控件；
- 插画随主题色板自动切换暗度；性能模式下降级为纯色。

## 2. 字体规范（Typography）

| Token | 字体 | 字重 | 字号/行高 | 用途 |
|-------|------|------|-----------|------|
| `display` | Inter / 系统无衬线 | Bold 700 | 28/36 | 页面大标题（英文） |
| `title` | Inter / 系统无衬线 | Bold 700 | 20/28 | 卡片标题 |
| `label` | Inter / 系统无衬线 | SemiBold 600 | 12/16 | 配置参数标签（**全大写英文**） |
| `body` | 系统默认（中文：思源黑体/系统） | Regular 400 | 14/22 | 正文中文 |
| `caption` | 系统默认 | Regular 400 | 12/18 | 辅助说明 |
| `mono` | JetBrains Mono / 等宽 | Regular 400 | 13/20 | 计时数字、调试数据 |

## 3. 间距与圆角（Spacing & Radius）

| Token | 值 | 用途 |
|-------|-----|------|
| `space/xxs` | 4dp | 图标与文字间距 |
| `space/xs` | 8dp | 控件内边距 |
| `space/sm` | 12dp | 卡片内小间距 |
| `space/md` | 16dp | 页面左右安全边距 |
| `space/lg` | 24dp | 卡片间留白、分组间距 |
| `space/xl` | 32dp | 区块间距 |
| `radius/sm` | 8dp | 小控件（复选框/开关） |
| `radius/md` | 12dp | 滑块、输入框、标签 |
| `radius/lg` | 16dp | 卡片、弹窗 |
| `radius/xl` | 24dp | 主按钮、底部面板 |
| `radius/full` | 999dp | 圆形单选、圆形按钮 |

## 4. 图标规范（强制 SVG 矢量）
- **全部界面图标使用 SVG 矢量图形（VectorDrawable）**，禁止位图 PNG；保证任意缩放不失真；
- 图标网格：24×24 viewport，描边风格 `stroke-width=1.8`，圆角端点；
- 图标库：Material Symbols Outlined 风格自绘子集（流体、音波、计时器、预设、设置、帮助、关于等），统一封装为 `res/drawable/*.xml`（VectorDrawable）；
- 状态色：默认 `text/secondary`，选中 `accent/primary`，禁用 `text/tertiary` @ 50%。

## 5. 控件规范与状态体系

### 5.1 方形复选框（Checkbox）
- 尺寸 20×20dp，圆角 4dp；未选中：白色 40% 细描边 + 透明底；选中：`accent/primary` 填充 + 白色对勾 SVG；
- 禁用：整体透明度 40%，不可点击。

### 5.2 圆形单选按钮（Radio）
- 直径 20dp；未选中：白色 40% 描边圆环；选中：外环 `accent/primary` + 内圆 8dp 实心 `accent/primary`；
- 禁用：透明度 40%。

### 5.3 淡紫色圆角主按钮（Primary Button）
- 高度 48dp，圆角 24dp（胶囊）；底色 `accent/primary`，文字 `text/on-accent` 大写英文标签；
- 状态：默认 / 按压(`accent/press`) / 禁用(`accent/disabled` 且透明度 40%) / 加载中（内嵌 16dp 白色环形进度）。

### 5.4 次级描边按钮（Secondary Button）
- 高度 40dp，圆角 12dp；白 40% 描边 + 透明底；按压态白 60% 描边 + 白 6% 底。

### 5.5 平滑圆角横向滑动条（Slider）
- 轨道高 4dp 圆角 2dp，底 `white@12%`，进度 `accent/primary`；滑块圆形直径 20dp `#FFFFFF` 带 `accent/primary` 描边；
- 拖动中滑块放大至 24dp；数值实时显示在轨道右端（等宽数字）。

### 5.6 开关（Switch）
- 尺寸 44×26dp，圆角 13dp；关：`white@12%` 底 + 白 90% 圆钮；开：`accent/primary` 底 + 白圆钮。

### 5.7 弹窗/浮窗/提示框（统一规范）
- 圆角 `radius/lg` 16dp；遮罩 `overlay/scrim`；内容区背景 `bg/card` + 白 40% 描边；
- 标题：`title` 英文粗体；正文 `body`；按钮：主操作右置淡紫主按钮，次操作左置次级按钮；
- Toast/Snackbar：圆角 12dp，玻璃底 `overlay/glass` + 白 20% 描边，底部居中。

### 5.8 状态色语义
| 状态 | 表现 |
|------|------|
| 选中/已开启 | 控件填充 `accent/primary`，图标同步着色 |
| 未选中/关闭 | 透明底 + 白 40% 描边 |
| 禁用 | 整体 40% 透明度，`text/tertiary`，点击无响应 |
| 警告 | 状态点 `state/warning` + 说明文案 |
| 成功 | 状态点 `state/success` |

## 6. 页面骨架规范
- 页面统一结构：顶部栏（返回/标题/右侧操作）+ 滚动内容 + 底部 Tab（按需）；
- 顶部栏：高 56dp，标题 `display` 缩小为 `title`，左侧返回箭头 SVG；
- 内容区：左右 `space/md` 16dp 安全边距，卡片间 `space/lg` 24dp 均匀留白；
- 功能分组以**白色细描边圆角卡片**包裹，卡片头部为分组标题（`label` 大写英文）+ 可折叠箭头。
