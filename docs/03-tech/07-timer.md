# 技术文档 07 · 计时器系统与全屏沉浸式实现（形态二专属）

## 1. 计时器引擎（core-timer，纯 Kotlin 可单测）

### 1.1 状态机
```
          start            pause             reset
IDLE ──────────► RUNNING ──────────► PAUSED ────────► IDLE
                  │  ▲                 │
                  │  └── resume ───────┘
                  ▼  (时间到)
               FINISHED ──► (提醒) ──► IDLE / 循环重新 RUNNING
```

### 1.2 三种模式

```kotlin
sealed interface TimerMode {
    data object CountUp : TimerMode
    data class CountDown(val totalMs: Long) : TimerMode
    data class Pomodoro(
        val workMs: Long = 25 * 60_000L,
        val breakMs: Long = 5 * 60_000L,
        val longBreakMs: Long = 15 * 60_000L,
        val roundsBeforeLongBreak: Int = 4
    ) : TimerMode
}

data class TimerState(
    val mode: TimerMode,
    val phase: TimerPhase,        // WORK / BREAK / NONE
    val elapsedMs: Long,          // 已流逝
    val remainingMs: Long,        // 剩余（倒计时/番茄）
    val running: Boolean,
    val loop: Boolean,            // 循环模式
    val round: Int                // 番茄轮次
)
```

### 1.3 引擎实现（协程 + StateFlow）

```kotlin
class TimerEngine(private val scope: CoroutineScope) {
    private val _state = MutableStateFlow(TimerState.initial())
    val state: StateFlow<TimerState> = _state
    private var job: Job? = null

    fun start(mode: TimerMode, loop: Boolean) {
        job?.cancel()
        _state.update { it.copy(mode = mode, loop = loop, running = true, remainingMs = mode.initialRemaining()) }
        job = scope.launch(Dispatchers.Default) {
            while (isActive) {
                delay(100)                                   // 100ms tick，UI 平滑
                val s = _state.value
                val newElapsed = s.elapsedMs + 100
                val newRemaining = (s.remainingMs - 100).coerceAtLeast(0)
                val finished = newRemaining <= 0L && s.mode !is TimerMode.CountUp
                _state.update { it.copy(elapsedMs = newElapsed, remainingMs = newRemaining) }
                if (finished) {
                    if (s.loop) { restartSamePhase() } else {
                        _state.update { it.copy(running = false, remainingMs = 0) }
                        ReminderBus.fire(s)                  // 触发提醒
                        break
                    }
                }
            }
        }
    }

    fun pause() = _state.update { it.copy(running = false) }
    fun resume() = /* 重建 job 继续 */
    fun reset()  = _state.update { TimerState.initial() }
}
```

### 1.4 番茄工作法阶段流转
```
第1~3轮: WORK 25min → BREAK 5min → WORK ...
第4轮  : WORK 25min → LONG BREAK 15min → 轮次归零
```
由 `round` 与 `phase` 字段驱动，`ReminderBus.fire` 携带阶段信息供 UI 显示「工作结束，休息一下」。

## 2. 提醒系统（三种通道组合，独立开关）

| 通道 | 实现 | 说明 |
|------|------|------|
| 震动 | `VibratorManager.vibrate(VibrationEffect.createWaveform([0,400,200,400], -1))` | 需 `VIBRATE` 权限 |
| 弹窗 | 全屏 Activity 内 `AlertDialog`（深色主题）+ 倒计时结束页 | 前台可见时使用 |
| 提示音 | `SoundPool` 预加载 `res/raw/timer_done.ogg` + 系统媒体音量 | 需音频焦点策略 |

```kotlin
object ReminderBus {
    fun fire(state: TimerState) {
        if (state.settings.vibrate) Vibrator.playPattern()
        if (state.settings.sound)   SoundPoolPlayer.play()
        if (state.settings.dialog)  eventChannel.send(TimeUpEvent(state))
    }
}
```

## 3. 时间文字渲染（LibGDX）

### 3.1 位图字体
- `gdx-freetype` 在**运行时**按配置字号生成 `BitmapFont`（`FreeTypeFontGenerator`）；
- 生成参数：字体文件（assets/fonts/Inter-SemiBold.ttf）、尺寸 `sizePx`、`borderWidth`（描边）、`borderColor`；
- 按字号缓存：`FontCache`（LRU，键=字号+描边宽），避免频繁重建。

### 3.2 样式参数 → 绘制
```kotlin
data class TimeTextStyle(
    val fontSize: Float = 96f, val color: Color = Color.WHITE,
    val alpha: Float = 1f, val strokeWidth: Float = 0f, val strokeColor: Color = Color.BLACK,
    val anchor: Anchor = Anchor.CENTER,        // 九宫格
    val customPos: Vector2? = null,            // 自定义坐标 (像素)
    val layer: TimeLayer = TimeLayer.TOP       // TOP 顶层 / BOTTOM 流体底层
)

fun drawTimer(sb: SpriteBatch, font: BitmapFont, text: String, style: TimeTextStyle, vp: Viewport) {
    val layout = GlyphLayout(font, text)
    val pos = when {
        style.customPos != null -> style.customPos
        else -> anchor.position(layout.width, layout.height, vp)
    }
    font.color = style.color.applyAlpha(style.alpha)
    font.draw(sb, layout, pos.x, pos.y)
}
```

### 3.3 图层渲染顺序
- `TimeLayer.BOTTOM`：在流体合成 pass 之前绘制 → 文字被流体/粒子覆盖（半透明融入）；
- `TimeLayer.TOP`：全部渲染后绘制 → 始终清晰置顶；
- 渲染顺序由 `FlowScreen.render()` 中调用点决定。

## 4. 全屏沉浸式实现

```kotlin
class FullscreenActivity : AndroidApplication() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // ① 沉浸式：隐藏系统栏 + 边缘手势
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        // ② 常亮（计时场景）
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        // ③ 防止休眠时渲染线程饿死：保持 CPU 唤醒锁（可选）
        // wakeLock = powerManager.newWakeLock(PARTIAL_WAKE_LOCK, "flowlume:timer")
    }
}
```

### 一键隐藏 UI（纯观赏模式）
```kotlin
class ImmersionController {
    fun hideAll(showControlHint: Boolean) {
        uiLayer.visible = false          // 隐藏全部控件
        // 屏幕四角保留 12dp 透明热区，点击任一点唤出 3s 控制浮层
        touchHotspots.enable(4)
    }
    fun reveal() { uiLayer.visible = true; touchHotspots.disable() }
}
```

## 5. 与全屏渲染的关系
- 计时器 UI 是 LibGDX `Stage` 内的独立 `Group`（`uiLayer`），与流体 `Screen` 同帧渲染；
- 计时状态（`TimerEngine.state`）通过 `collectAsState` 驱动 Stage 文本更新（每 100ms tick）；
- 退出全屏模式 → `TimerEngine` 停止并保留状态，下次进入可选择继续/重置。
