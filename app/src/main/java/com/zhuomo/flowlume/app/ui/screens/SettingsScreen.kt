package com.zhuomo.flowlume.app.ui.screens

import android.app.Activity
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
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.zhuomo.flowlume.app.R
import com.zhuomo.flowlume.app.di.AppContainer
import com.zhuomo.flowlume.app.ui.useRenderConfig
import com.zhuomo.flowlume.app.util.LocaleHelper
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
    val activity = context as? Activity
    val mode by AppContainer.uiMode.collectAsState()
    val (config, update) = useRenderConfig(mode)
    val charcoal by AppContainer.charcoalTheme.collectAsState()
    var separate by remember { mutableStateOf(true) }

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
        FxCard(title = stringResource(R.string.storage_card)) {
            SwitchRow(stringResource(R.string.separate_configs), separate) {
                separate = it
                scope.launch { ConfigStore.setSeparateConfigs(context, it) }
            }
        }

        FxCard(title = stringResource(R.string.appearance_card)) {
            RadioRow(stringResource(R.string.theme_dark), !charcoal) { AppContainer.charcoalTheme.value = false }
            RadioRow(stringResource(R.string.theme_charcoal), charcoal) { AppContainer.charcoalTheme.value = true }
        }

        FxCard(title = stringResource(R.string.language)) {
            RadioRow(stringResource(R.string.lang_follow_system), AppContainer.appLang == LocaleHelper.LANG_SYSTEM) {
                changeLanguage(context, activity, LocaleHelper.LANG_SYSTEM, scope)
            }
            RadioRow(stringResource(R.string.lang_zh), AppContainer.appLang == LocaleHelper.LANG_ZH) {
                changeLanguage(context, activity, LocaleHelper.LANG_ZH, scope)
            }
            RadioRow(stringResource(R.string.lang_en), AppContainer.appLang == LocaleHelper.LANG_EN) {
                changeLanguage(context, activity, LocaleHelper.LANG_EN, scope)
            }
        }

        FxCard(title = stringResource(R.string.perf_card)) {
            SwitchRow(stringResource(R.string.perf_mode), config.performanceMode) {
                update(config.copy(performanceMode = it))
            }
        }

        FxCard(title = stringResource(R.string.service_card)) {
            SecondaryButton(text = stringResource(R.string.restart_listener_btn)) {
                RestartListenerHelper.restart(context)
            }
            Spacer(Modifier.height(8.dp))
            SecondaryButton(text = stringResource(R.string.battery_opt)) {
                val pm = context.getSystemService(PowerManager::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !pm.isIgnoringBatteryOptimizations(context.packageName)) {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                    runCatching { context.startActivity(intent) }
                }
            }
        }

        FxCard(title = stringResource(R.string.more_card)) {
            NavRow(stringResource(R.string.experimental), "BETA") { navController.navigate("experimental") }
            NavRow(stringResource(R.string.about)) { navController.navigate("about") }
            NavRow(stringResource(R.string.help)) { navController.navigate("help") }
        }
    }
}

private fun changeLanguage(
    context: android.content.Context,
    activity: android.app.Activity?,
    lang: String,
    scope: kotlinx.coroutines.CoroutineScope
) {
    scope.launch {
        AppContainer.setLang(context, lang)
        activity?.recreate()
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
        Text(title, color = FlowColors.TextPrimary, style = MaterialTheme.typography.bodyMedium)
        if (badge != null) {
            Text(
                text = badge,
                color = FlowColors.Danger,
                style = MaterialTheme.typography.labelMedium,
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
