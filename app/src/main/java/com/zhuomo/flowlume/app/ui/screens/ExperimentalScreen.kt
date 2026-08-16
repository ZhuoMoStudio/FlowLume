package com.zhuomo.flowlume.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.zhuomo.flowlume.app.di.AppContainer
import com.zhuomo.flowlume.app.ui.SubPageScaffold
import com.zhuomo.flowlume.app.ui.useRenderConfig
import com.zhuomo.flowlume.config.Mode
import com.zhuomo.flowlume.ui.CheckRow
import com.zhuomo.flowlume.ui.FlowColors
import com.zhuomo.flowlume.ui.FxCard

/** 页面6 · 实验性功能分区 */
@Composable
fun ExperimentalScreen(navController: NavHostController) {
    val context = LocalContext.current
    val mode by AppContainer.uiMode.collectAsState()
    val (config, update) = useRenderConfig(mode)

    SubPageScaffold(title = "EXPERIMENTAL 实验性功能", navController = navController) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(FlowColors.BgPrimary)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 警示横幅
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(FlowColors.Danger.copy(alpha = 0.08f))
                    .border(1.dp, FlowColors.Danger.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = "⚠ EXPERIMENTAL FEATURES",
                    color = FlowColors.Danger,
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "板块内功能持续开发，稳定性较差，仅供测试体验。可能导致崩溃或异常表现。",
                    color = FlowColors.TextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            FxCard(title = "DEBUG TOOLS 调试") {
                CheckRow(
                    title = "DEBUG VIEW 调试视图",
                    subtitle = "渲染画面叠加 HUD（FPS / 频谱 / 监听状态）",
                    checked = config.debugView
                ) { update(config.copy(debugView = it)) }
            }

            FxCard(title = "COMING SOON 规划中") {
                CheckRow("MASK SEGMENTATION 封面智能分割（预览）", checked = false, enabled = false) {}
                CheckRow("DOF SIMULATION 景深模拟（规划）", checked = false, enabled = false) {}
            }

            Text(
                text = "实验性功能默认关闭，开启后请留意耗电与稳定性。",
                color = FlowColors.TextTertiary,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
