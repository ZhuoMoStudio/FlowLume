package com.zhuomo.flowlume.app.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.zhuomo.flowlume.app.BuildConfig
import com.zhuomo.flowlume.app.R
import com.zhuomo.flowlume.app.ui.SubPageScaffold
import com.zhuomo.flowlume.ui.FlowColors
import com.zhuomo.flowlume.ui.FxCard

/** 页面7 · 关于页面 */
@Composable
fun AboutScreen(navController: NavHostController) {
    val context = LocalContext.current
    SubPageScaffold(title = stringResource(R.string.about), navController = navController) {
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
                text = "Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                style = MaterialTheme.typography.bodySmall,
                color = FlowColors.TextTertiary
            )

            FxCard(title = stringResource(R.string.links)) {
                LinkRow(stringResource(R.string.feedback)) {
                    runCatching {
                        context.startActivity(
                            Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"))
                                .putExtra(Intent.EXTRA_SUBJECT, "FlowLume Feedback")
                        )
                    }
                }
                LinkRow(stringResource(R.string.other_works)) {
                    runCatching {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/ZhuoMoStudio"))
                        )
                    }
                }
            }

            FxCard(title = stringResource(R.string.credits)) {
                CreditLine(stringResource(R.string.credits_algo), stringResource(R.string.credits_algo_v))
                CreditLine(stringResource(R.string.credits_lib), stringResource(R.string.credits_lib_v))
                CreditLine(stringResource(R.string.credits_ref), stringResource(R.string.credits_ref_v))
                CreditLine(stringResource(R.string.credits_icon), stringResource(R.string.credits_icon_v))
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
