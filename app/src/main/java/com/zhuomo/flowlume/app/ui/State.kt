package com.zhuomo.flowlume.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.zhuomo.flowlume.config.ConfigStore
import com.zhuomo.flowlume.config.Mode
import com.zhuomo.flowlume.config.RenderConfig
import com.zhuomo.flowlume.config.TimerConfig
import kotlinx.coroutines.launch

/** 订阅某形态的渲染配置（DataStore 流），返回 (当前配置, 更新函数) */
@Composable
fun useRenderConfig(mode: Mode): Pair<RenderConfig, (RenderConfig) -> Unit> {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var config by remember { mutableStateOf(ConfigStore.current(mode)) }

    LaunchedEffect(mode) {
        ConfigStore.configFlow(context, mode).collect { config = it }
    }
    val update: (RenderConfig) -> Unit = { new ->
        config = new
        scope.launch { ConfigStore.save(context, mode, new) }
    }
    return config to update
}

/** 订阅计时器配置 */
@Composable
fun useTimerConfig(): Pair<TimerConfig, (TimerConfig) -> Unit> {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var config by remember { mutableStateOf(TimerConfig()) }

    LaunchedEffect(Unit) {
        ConfigStore.timerFlow(context).collect { config = it }
    }
    val update: (TimerConfig) -> Unit = { new ->
        config = new
        scope.launch { ConfigStore.saveTimer(context, new) }
    }
    return config to update
}
