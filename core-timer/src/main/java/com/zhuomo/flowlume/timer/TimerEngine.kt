package com.zhuomo.flowlume.timer

import com.zhuomo.flowlume.config.TimerConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 计时器引擎：正计时 / 倒计时 / 番茄工作计时器（纯逻辑，100ms tick）。
 * 全屏窗口模式专属，壁纸模式不加载。
 */
class TimerEngine(private val scope: CoroutineScope) {

    private val _state = MutableStateFlow(TimerState())
    val state: StateFlow<TimerState> = _state

    private var job: Job? = null

    fun start(config: TimerConfig) {
        job?.cancel()
        val mode = TimerMode.fromConfig(config)
        val initial = TimerState(
            mode = mode,
            remainingMs = (mode as? TimerMode.CountDown)?.totalMs ?: (mode as? TimerMode.Pomodoro)?.workMs ?: 0L,
            loop = config.loop,
            round = 0,
            phase = if (mode is TimerMode.Pomodoro) TimerPhase.WORK else TimerPhase.NONE
        )
        _state.value = initial.copy(running = true)
        job = scope.launch {
            while (isActive) {
                delay(100)
                tick(100)
            }
        }
    }

    fun pause() {
        _state.value = _state.value.copy(running = false)
    }

    fun resume() {
        val s = _state.value
        if (s.running || s.finished) return
        _state.value = s.copy(running = true)
        job?.cancel()
        job = scope.launch {
            while (isActive) {
                delay(100)
                tick(100)
            }
        }
    }

    fun reset() {
        job?.cancel()
        val s = _state.value
        val total = when (s.mode) {
            is TimerMode.CountDown -> s.mode.totalMs
            is TimerMode.Pomodoro -> s.mode.workMs
            TimerMode.CountUp -> 0L
        }
        _state.value = TimerState(mode = s.mode, remainingMs = total)
    }

    private suspend fun tick(ms: Long) {
        val s = _state.value
        if (!s.running) return
        val elapsed = s.elapsedMs + ms
        val remaining = when (s.mode) {
            is TimerMode.CountUp -> s.remainingMs
            else -> (s.remainingMs - ms).coerceAtLeast(0)
        }
        _state.value = s.copy(elapsedMs = elapsed, remainingMs = remaining)

        if (s.mode !is TimerMode.CountUp && remaining <= 0) {
            when (s.mode) {
                is TimerMode.Pomodoro -> handlePomodoroPhaseEnd()
                else -> {
                    if (s.loop) {
                        _state.value = _state.value.copy(remainingMs = (s.mode as TimerMode.CountDown).totalMs)
                    } else {
                        _state.value = _state.value.copy(running = false)
                        ReminderBus.fire(_state.value)
                    }
                }
            }
        }
    }

    private fun handlePomodoroPhaseEnd() {
        val s = _state.value
        val pomo = s.mode as TimerMode.Pomodoro
        val isWork = s.phase == TimerPhase.WORK
        val nextRound = if (isWork) s.round + 1 else s.round
        val useLongBreak = isWork && nextRound >= pomo.roundsBeforeLongBreak
        val nextPhase = if (isWork) TimerPhase.BREAK else TimerPhase.WORK
        val nextRemaining = when {
            isWork && useLongBreak -> pomo.longBreakMs
            isWork -> pomo.breakMs
            else -> pomo.workMs
        }
        _state.value = s.copy(
            phase = nextPhase,
            round = if (!isWork && nextRound >= pomo.roundsBeforeLongBreak) 0 else nextRound,
            remainingMs = nextRemaining,
            running = s.loop
        )
        ReminderBus.fire(_state.value)
    }

    companion object {
        fun format(ms: Long): String {
            val totalSec = (ms / 1000).coerceAtLeast(0)
            val m = totalSec / 60
            val sec = totalSec % 60
            return "%02d:%02d".format(m, sec)
        }
    }
}
