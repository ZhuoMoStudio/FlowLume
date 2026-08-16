package com.zhuomo.flowlume.app.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.zhuomo.flowlume.app.BuildConfig
import com.zhuomo.flowlume.app.ui.SubPageScaffold
import com.zhuomo.flowlume.ui.FlowColors
import com.zhuomo.flowlume.ui.FxCard
import com.zhuomo.flowlume.ui.PrimaryButton

/** 页面7 · 关于页面 */
@Composable
fun AboutScreen(navController: NavHostController) {
    val context = LocalContext.current
    SubPageScaffold(title = "ABOUT 关于", navController = navController) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(FlowColors.BgPrimary)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))
            Text("FLOWLUME", style = MaterialTheme.typography.titleLarge, fontSize = 30.sp, color = FlowColors.TextPrimary)
            Text(
                text = "Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})\nAndroid ${Build.VERSION.RELEASE}",
                style = MaterialTheme.typography.bodySmall,
                color = FlowColors.TextTertiary
            )

            FxCard(title = "LINKS 链接") {
                LinkRow("FEEDBACK 反馈问题") {
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")).putExtra(Intent.EXTRA_SUBJECT, "FlowLume Feedback"))
                    }
                }
                LinkRow("OTHER WORKS 其他作品") {
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/ZhuoMoStudio")))
                    }
                }
            }

            FxCard(title = "CREDITS 致谢") {
                CreditLine("图形算法", "Intel Kawase Blur · Inigo Quilez Domain Wrapping")
                CreditLine("开源库", "LibGDX (BadlogicGames) · gdx-freetype · Jetpack · Kotlinx")
                CreditLine("音频可视化参考", "AudioViz · Droid-vizu · wave.js")
                CreditLine("图标", "自绘 SVG 矢量图标集")
            }

            Text(
                text = "© ZhuoMoStudio 松庭灼墨",
                style = MaterialTheme.typography.bodySmall,
                color = FlowColors.TextTertiary
            )
        }
    }
}

@Composable
private fun LinkRow(title: String, onClick: () -> Unit) {
    Text(
        text = title,
        color = FlowColors.Accent,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp)
    )
}

@Composable
private fun CreditLine(label: String, value: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelMedium, color = FlowColors.TextSecondary)
        Text(value, style = MaterialTheme.typography.bodySmall, color = FlowColors.TextTertiary)
    }
}
