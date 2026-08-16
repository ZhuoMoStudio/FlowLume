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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.zhuomo.flowlume.app.R
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
    val mode by AppContainer.uiMode.collectAsState()
    val (config, update) = useRenderConfig(mode)

    SubPageScaffold(title = stringResource(R.string.experimental), navController = navController) {
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
                    text = stringResource(R.string.experimental_warn_title),
                    color = FlowColors.Danger,
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.experimental_warn_body),
                    color = FlowColors.TextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            FxCard(title = "DEBUG TOOLS") {
                CheckRow(
                    title = stringResource(R.string.debug_view),
                    subtitle = stringResource(R.string.debug_view_note),
                    checked = config.debugView
                ) { update(config.copy(debugView = it)) }
            }

            FxCard(title = stringResource(R.string.coming_soon)) {
                CheckRow(stringResource(R.string.mask_segmentation), checked = false, enabled = false) {}
                CheckRow(stringResource(R.string.dof_simulation), checked = false, enabled = false) {}
            }

            Text(
                text = stringResource(R.string.experimental_note),
                color = FlowColors.TextTertiary,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
