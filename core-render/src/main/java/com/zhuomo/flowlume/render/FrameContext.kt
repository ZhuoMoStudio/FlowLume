package com.zhuomo.flowlume.render

import com.zhuomo.flowlume.audio.FrameData

/** 每帧渲染上下文（GL 线程内只读快照） */
class FrameContext(
    val width: Int,
    val height: Int,
    val time: Float,
    val frameData: FrameData
)
