package com.zhuomo.flowlume.audio

import kotlin.math.sqrt

/** 鼓点/节拍检测：能量突变 + 自适应阈值 + 冷却时间 */
class BeatDetector(
    private val sensitivity: Float = 1.4f,
    private val minIntervalMs: Long = 180L
) {
    private val history = ArrayDeque<Float>()
    private var lastBeatAt = 0L

    /** 返回是否命中节拍；同时输出衰减包络值 */
    fun process(energy: Float, nowMs: Long): Boolean {
        history.addLast(energy)
        if (history.size > 43) history.removeFirst()

        val avg = history.average().toFloat()
        val variance = history.map { (it - avg) * (it - avg) }.average().toFloat()
        val std = sqrt(variance)
        val threshold = avg + sensitivity * std
        val isBeat = energy > threshold && energy > avg * 1.5f
        val coolDown = nowMs - lastBeatAt > minIntervalMs
        return if (isBeat && coolDown) {
            lastBeatAt = nowMs
            true
        } else false
    }
}
