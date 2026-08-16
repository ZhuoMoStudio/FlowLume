package com.zhuomo.flowlume.app.fullscreen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zhuomo.flowlume.app.di.AppContainer
import com.zhuomo.flowlume.app.ui.useTimerConfig
import com.zhuomo.flowlume.timer.ReminderBus
import com.zhuomo.flowlume.timer.ReminderExecutor
import com.zhuomo.flowlume.timer.TimeUpEvent
import com.zhuomo.flowlume.timer.TimerEngine
import com.zhuomo.flowlume.ui.FlowColors
import com.zhuomo.flowlume.ui.PrimaryButton
import com.zhuomo.flowlume.ui.SecondaryButton
import kotlinx.coroutines.launch

/** 全屏模式 Compose 控制浮层：计时控制条 + 一键隐藏 UI */
@Composable
fun TimerOverlay() {
    val context = LocalContext.current
    val state by AppContainer.timerEngine.state.collectAsState()
    val (timerCfg, _) = useTimerConfig()
    val hidden by AppContainer.uiControlsHidden.collectAsState()
    var timeUp by remember { mutableStateOf<TimeUpEvent?>(null) }

    LaunchedEffect(Unit) {
        launch {
            ReminderBus.events.collect { ev ->
                timeUp = ev
                if (timerCfg.vibrate) ReminderExecutor.vibrate(context)
                if (timerCfg.sound) ReminderExecutor.playSound(context)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (hidden) {
            // 纯沉浸式：点击任意处唤出控制
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { AppContainer.uiControlsHidden.value = false }
            )
        } else {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(FlowColors.BgCard.copy(alpha = 0.85f))
                    .padding(16.dp)
            ) {
                Text(
                    text = TimerEngine.format(state.displayMs),
                    fontSize = 44.sp,
                    fontFamily = FontFamily.Monospace,
                    color = FlowColors.TextPrimary,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Text(
                    text = when {
                        state.running -> "RUNNING"
                        state.elapsedMs > 0 -> "PAUSED"
                        else -> "READY"
                    },
                    fontSize = 12.sp,
                    color = FlowColors.TextSecondary,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PrimaryButton(
                        text = if (state.running) "RESUME" else "START",
                        onClick = { AppContainer.timerEngine.start(timerCfg) },
                        modifier = Modifier.weight(1f)
                    )
                    SecondaryButton(
                        text = "PAUSE",
                        onClick = { AppContainer.timerEngine.pause() },
                        modifier = Modifier.weight(1f)
                    )
                    SecondaryButton(
                        text = "RESET",
                        onClick = { AppContainer.timerEngine.reset() },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(8.dp))
                SecondaryButton(text = "HIDE UI 隐藏全部控件") {
                    AppContainer.uiControlsHidden.value = true
                }
            }
        }
    }

    timeUp?.let { ev ->
        if (timerCfg.dialog) {
            AlertDialog(
                onDismissRequest = { timeUp = null },
                containerColor = FlowColors.BgCard,
                shape = RoundedCornerShape(16.dp),
                title = { Text("TIME'S UP 时间到", color = FlowColors.TextPrimary) },
                text = {
                    Text(
                        text = if (ev.state.phase == com.zhuomo.flowlume.timer.TimerPhase.WORK)
                            "工作结束，休息一下" else "休息结束，继续专注",
                        color = FlowColors.TextSecondary
                    )
                },
                confirmButton = {
                    Text(
                        text = "OK",
                        color = FlowColors.Accent,
                        modifier = Modifier
                            .clickable { timeUp = null }
                            .padding(12.dp)
                    )
                }
            )
        } else {
            timeUp = null
        }
    }
}
