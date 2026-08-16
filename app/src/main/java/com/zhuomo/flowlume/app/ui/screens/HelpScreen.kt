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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.zhuomo.flowlume.app.R
import com.zhuomo.flowlume.app.ui.SubPageScaffold
import com.zhuomo.flowlume.media.RestartListenerHelper
import com.zhuomo.flowlume.ui.FlowColors
import com.zhuomo.flowlume.ui.FxCard
import com.zhuomo.flowlume.ui.SecondaryButton

/** 页面8 · 内置帮助中心（Accordion 问答） */
@Composable
fun HelpScreen(navController: NavHostController) {
    val context = LocalContext.current
    SubPageScaffold(title = stringResource(R.string.help), navController = navController) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(FlowColors.BgPrimary)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HelpItem(stringResource(R.string.help_how_title)) {
                HelpBody(stringResource(R.string.help_how_body))
            }
            HelpItem(stringResource(R.string.help_notif_title)) {
                HelpBody(stringResource(R.string.help_notif_body))
            }
            HelpItem(stringResource(R.string.help_audio_title)) {
                HelpBody(stringResource(R.string.help_audio_body))
            }
            HelpItem(stringResource(R.string.help_fix_title)) {
                Column {
                    HelpBody(stringResource(R.string.help_fix_body))
                    Spacer(Modifier.height(8.dp))
                    SecondaryButton(text = stringResource(R.string.restart_listener)) {
                        RestartListenerHelper.restart(context)
                    }
                }
            }
            HelpItem(stringResource(R.string.help_player_title)) {
                HelpBody(stringResource(R.string.help_player_body))
            }
            HelpItem(stringResource(R.string.help_battery_title)) {
                HelpBody(stringResource(R.string.help_battery_body))
            }
        }
    }
}

@Composable
private fun HelpBody(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = FlowColors.TextSecondary
    )
}

@Composable
private fun HelpItem(title: String, content: @Composable () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    FxCard(title = title) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            horizontalArrangement = Arrangement.SpaceBetween,
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
