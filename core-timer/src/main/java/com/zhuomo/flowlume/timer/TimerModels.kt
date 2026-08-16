package com.zhuomo.flowlume.timer

import com.zhuomo.flowlume.config.TimeAnchorConfig
import com.zhuomo.flowlume.config.TimeLayerConfig
import com.zhuomo.flowlume.config.TimerConfig
import com.zhuomo.flowlume.config.TimerModeConfig

enum class TimerPhase { NONE, WORK, BREAK }

sealed interface TimerMode {
    data object CountUp : TimerMode
    data class CountDown(val totalMs: Long) : TimerMode
    data class Pomodoro(
        val workMs: Long = 25 * 60_000L,
        val breakMs: Long = 5 * 60_000L,
        val longBreakMs: Long = 15 * 60_000L,
        val roundsBeforeLongBreak: Int = 4
    ) : TimerMode

    companion object {
        fun fromConfig(c: TimerConfig): TimerMode = when (c.mode) {
            TimerModeConfig.COUNT_UP -> CountUp
            TimerModeConfig.COUNT_DOWN -> CountDown(c.durationMs)
            TimerModeConfig.POMODORO -> Pomodoro(c.pomodoroWorkMs, c.pomodoroBreakMs)
        }
    }
}

data class TimerState(
    val mode: TimerMode = TimerMode.CountDown(25 * 60_000L),
    val phase: TimerPhase = TimerPhase.NONE,
    val elapsedMs: Long = 0L,
    val remainingMs: Long = 25 * 60_000L,
    val running: Boolean = false,
    val loop: Boolean = false,
    val round: Int = 0
) {
    val displayMs: Long
        get() = when (mode) {
            is TimerMode.CountUp -> elapsedMs
            else -> remainingMs
        }
    val finished: Boolean get() = !running && displayMs == 0L && mode !is TimerMode.CountUp
}

/** 时间文字渲染样式（对齐 UI 规范九宫格 + 自定义坐标 + 图层） */
data class TimeTextStyle(
    val fontSizeSp: Int = 96,
    val color: Long = 0xFFFFFFFF,
    val alpha: Float = 1.0f,
    val strokeWidthDp: Int = 0,
    val strokeColor: Long = 0xFF000000,
    val anchor: TimeAnchorConfig = TimeAnchorConfig.CENTER,
    val customX: Int = 0,
    val customY: Int = 0,
    val layer: TimeLayerConfig = TimeLayerConfig.TOP
) {
    companion object {
        fun fromConfig(c: TimerConfig): TimeTextStyle = TimeTextStyle(
            fontSizeSp = c.textStyle.fontSizeSp,
            color = c.textStyle.color,
            alpha = c.textStyle.alpha,
            strokeWidthDp = c.textStyle.strokeWidthDp,
            strokeColor = c.textStyle.strokeColor,
            anchor = c.textStyle.anchor,
            customX = c.textStyle.customX,
            customY = c.textStyle.customY,
            layer = c.textStyle.layer
        )
    }
}
