package com.zhuomo.flowlume.media

import android.graphics.Bitmap
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/** 封面事件：通知监听模块 → 渲染引擎（双形态各自在 GL 线程上传纹理） */
data class ArtEvent(
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val artwork: Bitmap? = null,
    val isPlaying: Boolean = false
)

object ArtBus {
    private val _events = MutableSharedFlow<ArtEvent>(replay = 1, extraBufferCapacity = 4)
    val events: SharedFlow<ArtEvent> = _events

    fun emit(event: ArtEvent) {
        _events.tryEmit(event)
    }
}
