package com.zhuomo.flowlume.app.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.zhuomo.flowlume.app.di.AppContainer
import com.zhuomo.flowlume.app.fullscreen.FullscreenActivity
import com.zhuomo.flowlume.app.ui.useTimerConfig
import com.zhuomo.flowlume.config.TimeAnchorConfig
import com.zhuomo.flowlume.config.TimeLayerConfig
import com.zhuomo.flowlume.config.TimerConfig
import com.zhuomo.flowlume.config.TimerModeConfig
import com.zhuomo.flowlume.ui.CheckRow
import com.zhuomo.flowlume.ui.FlowColors
import com.zhuomo.flowlume.ui.FxCard
import com.zhuomo.flowlume.ui.PrimaryButton
import com.zhuomo.flowlume.ui.RadioRow
import com.zhuomo.flowlume.ui.SliderRow
import com.zhuomo.flowlume.ui.SwitchRow

/** 页面4 · 全屏模式专属面板（TIMER）——仅 App 全屏形态显示 */
@Composable
fun TimerScreen(navController: NavHostController) {
    val context = LocalContext.current
    val (config, update) = useTimerConfig()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FlowColors.BgPrimary)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 模式
        FxCard(title = "MODE 模式") {
            RadioRow("COUNT UP 正计时", config.mode == TimerModeConfig.COUNT_UP) {
                update(config.copy(mode = TimerModeConfig.COUNT_UP))
            }
            RadioRow("COUNT DOWN 倒计时", config.mode == TimerModeConfig.COUNT_DOWN) {
                update(config.copy(mode = TimerModeConfig.COUNT_DOWN))
            }
            RadioRow("POMODORO 番茄工作计时器", config.mode == TimerModeConfig.POMODORO) {
                update(config.copy(mode = TimerModeConfig.POMODORO))
            }
        }

        // 时长
        FxCard(title = "DURATION 时长") {
            SliderRow("时长（分钟）", config.durationMs / 60_000f, valueRange = 1f..90f, steps = 88) {
                update(config.copy(durationMs = (it * 60_000L).toLong()))
            }
            if (config.mode == TimerModeConfig.POMODORO) {
                SliderRow("工作（分钟）", config.pomodoroWorkMs / 60_000f, valueRange = 1f..60f, steps = 58) {
                    update(config.copy(pomodoroWorkMs = (it * 60_000L).toLong()))
                }
                SliderRow("休息（分钟）", config.pomodoroBreakMs / 60_000f, valueRange = 1f..30f, steps = 28) {
                    update(config.copy(pomodoroBreakMs = (it * 60_000L).toLong()))
                }
            }
            Spacer(Modifier.height(4.dp))
            SwitchRow("LOOP 循环模式", config.loop) {
                update(config.copy(loop = it))
            }
        }

        // 提醒
        FxCard(title = "ALERT 提醒方式") {
            CheckRow("震动", checked = config.vibrate) { update(config.copy(vibrate = it)) }
            CheckRow("弹窗提示", checked = config.dialog) { update(config.copy(dialog = it)) }
            CheckRow("提示音", checked = config.sound) { update(config.copy(sound = it)) }
        }

        // 时间文字样式
        FxCard(title = "TIME TEXT 时间文字样式") {
            SliderRow("FONT SIZE 字号", config.textStyle.fontSizeSp / 200f, valueRange = 0.12f..1f, steps = 87) {
                update(config.copy(textStyle = config.textStyle.copy(fontSizeSp = (it * 200).toInt())))
            }
            SliderRow("OPACITY 透明度", config.textStyle.alpha, valueRange = 0.1f..1f, steps = 8) {
                update(config.copy(textStyle = config.textStyle.copy(alpha = it)))
            }
            SliderRow("STROKE 描边粗细", config.textStyle.strokeWidthDp / 12f, valueRange = 0f..1f, steps = 11) {
                update(config.copy(textStyle = config.textStyle.copy(strokeWidthDp = (it * 12).toInt())))
            }
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(TimeAnchorConfig.CENTER, TimeAnchorConfig.TOP_LEFT, TimeAnchorConfig.TOP_RIGHT).forEach { a ->
                    RadioRow(
                        label = a.name,
                        selected = config.textStyle.anchor == a
                    ) {
                        update(config.copy(textStyle = config.textStyle.copy(anchor = a)))
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RadioRow(
                    label = "TOP 顶层",
                    selected = config.textStyle.layer == TimeLayerConfig.TOP
                ) { update(config.copy(textStyle = config.textStyle.copy(layer = TimeLayerConfig.TOP))) }
                RadioRow(
                    label = "BOTTOM 底层",
                    selected = config.textStyle.layer == TimeLayerConfig.BOTTOM
                ) { update(config.copy(textStyle = config.textStyle.copy(layer = TimeLayerConfig.BOTTOM))) }
            }
        }

        PrimaryButton(text = "OPEN FULLSCREEN 打开全屏并开始") {
            AppContainer.timerEngine.start(config)
            context.startActivity(Intent(context, FullscreenActivity::class.java))
        }
        Text(
            text = "计时器为全屏窗口模式专属功能；时间文字由渲染引擎绘制（gdx-freetype）",
            style = MaterialTheme.typography.bodySmall,
            color = FlowColors.TextTertiary
        )
    }
}
