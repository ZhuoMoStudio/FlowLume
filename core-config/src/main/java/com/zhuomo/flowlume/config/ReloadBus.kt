package com.zhuomo.flowlume.config

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/** 配置热重载事件：任一形态配置变更后广播，渲染引擎在 GL 线程内应用 */
data class ReloadEvent(val mode: Mode)

object ReloadBus {
    private val _events = MutableSharedFlow<ReloadEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<ReloadEvent> = _events

    fun emit(mode: Mode) {
        _events.tryEmit(ReloadEvent(mode))
    }
}
