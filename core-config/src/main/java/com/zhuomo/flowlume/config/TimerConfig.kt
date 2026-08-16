package com.zhuomo.flowlume.config

import kotlinx.serialization.Serializable

/** 全屏模式专属：计时器配置（桌面壁纸形态不加载） */
@Serializable
data class TimerConfig(
    val mode: TimerModeConfig = TimerModeConfig.COUNT_DOWN,
    val durationMs: Long = 25 * 60_000L,
    val pomodoroWorkMs: Long = 25 * 60_000L,
    val pomodoroBreakMs: Long = 5 * 60_000L,
    val loop: Boolean = false,
    // 提醒开关
    val vibrate: Boolean = true,
    val dialog: Boolean = true,
    val sound: Boolean = true,
    // 时间文字样式
    val textStyle: TimeTextStyleConfig = TimeTextStyleConfig()
)

@Serializable
enum class TimerModeConfig { COUNT_UP, COUNT_DOWN, POMODORO }

@Serializable
enum class TimeAnchorConfig { TOP_LEFT, TOP_CENTER, TOP_RIGHT, MID_LEFT, CENTER, MID_RIGHT, BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT, CUSTOM }

@Serializable
enum class TimeLayerConfig { TOP, BOTTOM }

@Serializable
data class TimeTextStyleConfig(
    val fontSizeSp: Int = 96,
    val color: Long = 0xFFFFFFFF,
    val alpha: Float = 1.0f,
    val strokeWidthDp: Int = 0,
    val strokeColor: Long = 0xFF000000,
    val anchor: TimeAnchorConfig = TimeAnchorConfig.CENTER,
    val customX: Int = 0,
    val customY: Int = 0,
    val layer: TimeLayerConfig = TimeLayerConfig.TOP
)
