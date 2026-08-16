package com.zhuomo.flowlume.audio

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sqrt

/**
 * 音频引擎（Visualizer 版）：波形 RMS 能量 + FFT 波段 + 节拍检测 → FrameData。
 * 合规：仅分析设备输出音频的可视化数据，绝不采集麦克风。
 */
class AudioEngine(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val capture = VisualizerCapture(context)
    private val _frames = MutableSharedFlow<FrameData>(replay = 1, extraBufferCapacity = 2)
    val frames: SharedFlow<FrameData> = _frames

    private var job: Job? = null
    private val beatDetector = BeatDetector()
    private var pulse = 0f

    @Volatile private var latestWave: ByteArray? = null
    @Volatile private var latestFft: ByteArray? = null

    val isRunning: Boolean get() = capture.isRunning

    fun start() {
        if (job?.isActive == true) return
        capture.listener = object : VisualizerCapture.Listener {
            override fun onData(waveform: ByteArray?, fft: ByteArray?) {
                if (waveform != null) latestWave = waveform
                if (fft != null) latestFft = fft
            }
        }
        capture.start()
        job = scope.launch { emitLoop() }
    }

    fun stop() {
        job?.cancel()
        job = null
        capture.stop()
        latestWave = null
        latestFft = null
        _frames.tryEmit(FrameData.EMPTY)
    }

    private suspend fun emitLoop() {
        var prevEnergy = 0f
        while (scope.coroutineContext.isActive) {
            val wave = latestWave
            val fft = latestFft

            val energy = if (wave != null) {
                val smoothed = prevEnergy * 0.7f + rms(wave) * 0.3f
                smoothed
            } else 0f

            val bands = if (fft != null) fftBands(fft) else FloatArray(FrameData.BAND_COUNT)

            val beat = beatDetector.process(energy, System.currentTimeMillis())
            pulse = if (beat) 1f else (pulse * 0.92f).coerceAtLeast(0f)

            _frames.tryEmit(FrameData(energy, bands, beat, pulse))
            prevEnergy = energy
            delay(33) // ~30Hz
        }
    }

    /** 波形 RMS → 0..1 能量 */
    private fun rms(wave: ByteArray): Float {
        if (wave.isEmpty()) return 0f
        var sum = 0.0
        for (b in wave) {
            val v = (b.toInt() and 0xFF) - 128
            sum += v * v
        }
        val rms = sqrt(sum / wave.size)
        return (rms / 128f).coerceIn(0f, 1f)
    }

    /** Visualizer FFT（偶实奇虚）→ 8 波段能量 */
    private fun fftBands(fft: ByteArray): FloatArray {
        val n = fft.size / 2
        val mag = FloatArray(n)
        for (i in 0 until n) {
            val real = (fft[i * 2].toInt() and 0xFF) - 128
            val imag = (fft[i * 2 + 1].toInt() and 0xFF) - 128
            mag[i] = sqrt((real * real + imag * imag).toFloat())
        }
        // 低频在前，直接按比例分桶
        val bands = FloatArray(FrameData.BAND_COUNT)
        val per = mag.size / bands.size
        for (b in 0 until bands.size) {
            var s = 0f
            val start = b * per
            val end = minOf(start + per, mag.size)
            for (i in start until end) s += mag[i]
            bands[b] = (s / (end - start).coerceAtLeast(1)).coerceIn(0f, 1f) / 64f
        }
        return bands
    }
}
