package com.zhuomo.flowlume.audio

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sqrt

/**
 * 音频引擎：捕获 → FFT → 频段分桶 → 节拍检测 → FrameData 发布。
 * 渲染 GL 线程仅消费最新快照，线程解耦。
 */
class AudioEngine(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val capture = AudioCaptureManager(context)
    private val _frames = MutableSharedFlow<FrameData>(replay = 1, extraBufferCapacity = 2)
    val frames: SharedFlow<FrameData> = _frames
    private var job: Job? = null

    val isRunning: Boolean get() = capture.isCapturing

    fun start(resultCode: Int, data: Intent) {
        if (job?.isActive == true) return
        if (!capture.start(resultCode, data)) return
        job = scope.launch { captureLoop() }
    }

    fun stop() {
        job?.cancel()
        job = null
        capture.stop()
        _frames.tryEmit(FrameData.EMPTY)
    }

    private suspend fun captureLoop() {
        val n = 1024
        val buf = ShortArray(n)
        val re = FloatArray(n)
        val im = FloatArray(n)
        val window = hannWindow(n)
        val beatDetector = BeatDetector()
        var prevEnergy = 0f
        var pulse = 0f
        var warmup = 8 // 丢弃前几帧预热

        while (scope.coroutineContext.isActive) {
            val read = capture.read(buf)
            if (read > 0) {
                for (i in 0 until n) {
                    re[i] = (buf[i].toFloat() / 32768f) * window[i]
                    im[i] = 0f
                }
                Fft.forward(re, im, n)
                val mag = FloatArray(n) { i -> sqrt(re[i] * re[i] + im[i] * im[i]) }
                val bands = Fft.bandEnergies(mag, 44100)
                val raw = bands.average().toFloat()
                val energy = if (warmup > 0) { warmup--; 0f } else prevEnergy * 0.75f + raw * 0.25f
                val beat = beatDetector.process(energy, System.currentTimeMillis())
                pulse = if (beat) 1f else (pulse * 0.92f).coerceAtLeast(0f)
                _frames.tryEmit(FrameData(energy, bands, beat, pulse))
                prevEnergy = energy
            }
            delay(16)
        }
    }

    private fun hannWindow(n: Int): FloatArray = FloatArray(n) { i ->
        0.5f * (1f - cos(2.0 * PI * i / (n - 1)).toFloat())
    }
}
