package com.zhuomo.flowlume.timer

import android.content.Context
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

data class TimeUpEvent(val state: TimerState)

/** 计时结束提醒总线：震动 / 弹窗 / 提示音 三通道（独立开关由 UI 控制） */
object ReminderBus {
    private val _events = MutableSharedFlow<TimeUpEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<TimeUpEvent> = _events

    fun fire(state: TimerState) {
        _events.tryEmit(TimeUpEvent(state))
    }
}

object ReminderExecutor {
    fun vibrate(context: Context) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE)?.let { vm ->
                (vm as VibratorManager).defaultVibrator
            }
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        } ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 400, 200, 400), -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 400, 200, 400), -1)
        }
    }

    fun playSound(context: Context) {
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION) ?: return
        val tone = RingtoneManager.getRingtone(context, uri)
        tone?.play()
    }
}
