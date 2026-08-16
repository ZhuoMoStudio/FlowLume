package com.zhuomo.flowlume.media

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

enum class ListenerStatus { CONNECTED, REVOKED, RESTARTING }

/** 监听服务状态（UI 状态点 + 帮助页排查用） */
object NotificationCenter {
    private val _status = MutableSharedFlow<ListenerStatus>(replay = 1, extraBufferCapacity = 4)
    val status: SharedFlow<ListenerStatus> = _status

    fun emit(status: ListenerStatus) {
        _status.tryEmit(status)
    }
}
