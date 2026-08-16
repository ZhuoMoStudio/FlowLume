package com.zhuomo.flowlume.app.di

import android.content.Context
import com.zhuomo.flowlume.audio.AudioEngine
import com.zhuomo.flowlume.config.ConfigStore
import com.zhuomo.flowlume.config.Mode
import com.zhuomo.flowlume.config.ReloadBus
import com.zhuomo.flowlume.config.TimerConfig
import com.zhuomo.flowlume.effects.FlowLumeEffects
import com.zhuomo.flowlume.media.ListenerHealthCheck
import com.zhuomo.flowlume.timer.TimerEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/** 简易依赖容器（骨架阶段，后续可迁移 Hilt） */
object AppContainer {

    lateinit var appContext: Context
        private set

    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    lateinit var audioEngine: AudioEngine
    val timerEngine = TimerEngine(scope)

    /** 当前编辑/运行形态（UI 概念：决定编辑哪套配置、是否显示计时器 Tab） */
    val uiMode = MutableStateFlow(Mode.WALLPAPER)

    /** 计时器配置内存快照（全屏渲染线程读取） */
    @Volatile var timerConfig: TimerConfig = TimerConfig()

    /** 主题：false=纯黑深色 / true=灰黑深色 */
    val charcoalTheme = MutableStateFlow(false)

    /** 全屏模式一键隐藏 UI 控制信号 */
    val uiControlsHidden = MutableStateFlow(false)

    fun init(context: Context) {
        if (::appContext.isInitialized) return
        appContext = context.applicationContext
        FlowLumeEffects.bootstrap()
        audioEngine = AudioEngine(appContext, scope)
        ListenerHealthCheck.schedule(appContext)

        scope.launch {
            ConfigStore.prime(appContext)
            // DataStore → 内存快照 + 热重载广播
            launch {
                ConfigStore.configFlow(appContext, Mode.WALLPAPER).collect {
                    ConfigStore.wallpaperSnapshot = it
                    ReloadBus.emit(Mode.WALLPAPER)
                }
            }
            launch {
                ConfigStore.configFlow(appContext, Mode.FULLSCREEN).collect {
                    ConfigStore.fullscreenSnapshot = it
                    ReloadBus.emit(Mode.FULLSCREEN)
                }
            }
            launch {
                ConfigStore.timerFlow(appContext).collect { timerConfig = it }
            }
        }
    }
}
