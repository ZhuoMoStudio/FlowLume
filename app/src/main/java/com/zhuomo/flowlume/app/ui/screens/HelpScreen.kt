package com.zhuomo.flowlume.app.ui.screens

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.zhuomo.flowlume.app.ui.SubPageScaffold
import com.zhuomo.flowlume.media.RestartListenerHelper
import com.zhuomo.flowlume.ui.FlowColors
import com.zhuomo.flowlume.ui.FxCard
import com.zhuomo.flowlume.ui.SecondaryButton

/** 页面8 · 内置帮助中心（Accordion 问答；完整文案见 docs/01-product/help-page.md） */
@Composable
fun HelpScreen(navController: NavHostController) {
    val context = LocalContext.current
    SubPageScaffold(title = "HELP 帮助中心", navController = navController) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(FlowColors.BgPrimary)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HelpItem("软件工作原理") {
                Text(
                    "监听系统媒体通知 → 自动提取专辑封面 → 引擎以封面为基底生成无缝流体光影（Kawase Blur + Domain Wrapping）→ 可选音频频谱/节拍联动 → 渲染到桌面或全屏窗口。全部本地计算，无网络上传。",
                    style = MaterialTheme.typography.bodySmall,
                    color = FlowColors.TextSecondary
                )
            }
            HelpItem("「允许阅读通知」权限说明") {
                Text(
                    "程序仅读取媒体播放类通知中公开的歌曲/封面信息，不读取聊天、短信等私人通知；数据不出设备。权限被回收后壁纸将无法更新封面，可在一键重启监听恢复。",
                    style = MaterialTheme.typography.bodySmall,
                    color = FlowColors.TextSecondary
                )
            }
            HelpItem("音频权限答疑") {
                Text(
                    "程序仅捕获设备正在播放的输出音频，绝不访问麦克风，不采集环境音与人声，遵循 Google Android 官方 AudioPlaybackCapture 最佳实践。",
                    style = MaterialTheme.typography.bodySmall,
                    color = FlowColors.TextSecondary
                )
            }
            HelpItem("读取不到专辑封面？排查") {
                Column {
                    Text(
                        "1. 切歌一次触发新通知；2. 检查通知权限是否授权；3. 一键重启监听服务；4. 确认播放器通知未被系统折叠；5. 加入电池优化白名单。",
                        style = MaterialTheme.typography.bodySmall,
                        color = FlowColors.TextSecondary
                    )
                    Spacer(Modifier.height(8.dp))
                    SecondaryButton(text = "RESTART LISTENER 一键重启") {
                        RestartListenerHelper.restart(context)
                    }
                }
            }
            HelpItem("播放器兼容说明") {
                Text(
                    "原生兼容 Apple Music / Spotify / YouTube Music / SoundCloud 等标准媒体通知；QQ音乐等国产播放器使用自定义样式，需在播放器设置中切换为「系统原生通知样式」。",
                    style = MaterialTheme.typography.bodySmall,
                    color = FlowColors.TextSecondary
                )
            }
            HelpItem("电池优化与后台保活") {
                Text(
                    "系统省电策略可能冻结后台监听服务，建议在电池管理中将 FlowLume 设为「不限制」；App 内提供一键重启监听与电池白名单快捷入口。",
                    style = MaterialTheme.typography.bodySmall,
                    color = FlowColors.TextSecondary
                )
            }
        }
    }
}

@Composable
private fun HelpItem(title: String, content: @Composable () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    FxCard(title = title) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = FlowColors.TextPrimary
            )
            Text(if (expanded) "▾" else "▸", color = FlowColors.TextTertiary)
        }
        if (expanded) {
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}
