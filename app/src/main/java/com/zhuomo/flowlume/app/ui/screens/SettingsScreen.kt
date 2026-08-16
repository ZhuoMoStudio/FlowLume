package com.zhuomo.flowlume.app.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.zhuomo.flowlume.app.di.AppContainer
import com.zhuomo.flowlume.app.ui.useRenderConfig
import com.zhuomo.flowlume.config.ConfigStore
import com.zhuomo.flowlume.config.Mode
import com.zhuomo.flowlume.media.RestartListenerHelper
import com.zhuomo.flowlume.ui.FlowColors
import com.zhuomo.flowlume.ui.FxCard
import com.zhuomo.flowlume.ui.RadioRow
import com.zhuomo.flowlume.ui.SecondaryButton
import com.zhuomo.flowlume.ui.SwitchRow
import kotlinx.coroutines.launch

/** 页面5 · 杂项设置 MISC SETTINGS */
@Composable
fun SettingsScreen(navController: NavHostController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val mode by AppContainer.uiMode.collectAsState()
    val (config, update) = useRenderConfig(mode)
    val charcoal by AppContainer.charcoalTheme.collectAsState()
    var separate by remember { mutableStateOf(true) }
    var adaptiveIcon by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        separate = ConfigStore.separateConfigs(context)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FlowColors.BgPrimary)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        FxCard(title = "STORAGE 存储行为") {
            SwitchRow("桌面/全屏模式相互独立储存参数", separate) {
                separate = it
                scope.launch { ConfigStore.setSeparateConfigs(context, it) }
            }
        }

        FxCard(title = "APPEARANCE 外观") {
            SwitchRow("启动器自适应着色", adaptiveIcon) { adaptiveIcon = it }
            Spacer(Modifier.height(4.dp))
            RadioRow("DARK 纯黑深色", !charcoal) { AppContainer.charcoalTheme.value = false }
            RadioRow("CHARCOAL 灰黑深色", charcoal) { AppContainer.charcoalTheme.value = true }
        }

        FxCard(title = "PERFORMANCE 性能") {
            SwitchRow("性能模式（降低分辨率/关闭粒子，适配低端机）", config.performanceMode) {
                update(config.copy(performanceMode = it))
            }
        }

        FxCard(title = "SERVICE 服务") {
            SecondaryButton(text = "RESTART NOTIFICATION LISTENER 一键重启监听") {
                RestartListenerHelper.restart(context)
            }
            Spacer(Modifier.height(8.dp))
            SecondaryButton(text = "BATTERY OPTIMIZATION 电池优化白名单") {
                val pm = context.getSystemService(PowerManager::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !pm.isIgnoringBatteryOptimizations(context.packageName)) {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                    runCatching { context.startActivity(intent) }
                }
            }
        }

        FxCard(title = "MORE 更多") {
            NavRow("EXPERIMENTAL 实验性功能", "BETA") { navController.navigate("experimental") }
            NavRow("ABOUT 关于") { navController.navigate("about") }
            NavRow("HELP 帮助中心") { navController.navigate("help") }
        }
    }
}

@Composable
private fun NavRow(title: String, badge: String? = null, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = FlowColors.TextPrimary, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
        if (badge != null) {
            Text(
                text = badge,
                color = FlowColors.Danger,
                style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                modifier = Modifier
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
                    .background(FlowColors.Danger.copy(alpha = 0.15f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        } else {
            Text("›", color = FlowColors.TextTertiary)
        }
    }
}
