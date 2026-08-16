package com.zhuomo.flowlume.audio

/**
 * 音频分析帧：渲染 GL 线程每帧读取最新快照。
 * 合规声明：仅分析设备本地输出音频，绝不采集麦克风。
 */
data class FrameData(
    val energy: Float = 0f,        // 0..1 总能量（平滑后）
    val bands: FloatArray = FloatArray(BAND_COUNT), // 对数频段能量 0..1
    val beat: Boolean = false,
    val pulse: Float = 0f          // 节拍指数衰减包络 0..1
) {
    companion object {
        const val BAND_COUNT = 8
        val EMPTY = FrameData()
    }
}
