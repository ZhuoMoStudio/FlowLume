package com.zhuomo.flowlume.audio

import android.content.Context
import android.media.audiofx.Visualizer
import android.util.Log

/**
 * 音频可视化捕获：基于系统 Visualizer（分析设备输出音频，非录音接口）。
 * 竞品 Diffuse 同款方案 —— 无需「录制或投放」屏幕捕获授权，
 * 仅需 RECORD_AUDIO（麦克风权限名，但 Visualizer 不采集任何麦克风数据）。
 *
 * 实现：Visualizer(sessionId=0) 绑定系统全局输出混合流；
 * Android 10+ 下持有 RECORD_AUDIO 权限即可访问其他应用输出音频的可视化数据。
 */
class VisualizerCapture(private val context: Context) {

    interface Listener {
        fun onData(waveform: ByteArray?, fft: ByteArray?)
    }

    var listener: Listener? = null

    private var visualizer: Visualizer? = null

    val isRunning: Boolean get() = visualizer?.enabled == true

    /** 启动捕获：绑定全局输出（sessionId=0） */
    fun start() {
        stop()
        val v = runCatching {
            Visualizer(0).apply {
                setCaptureSize(Visualizer.getCaptureSizeRange()[1])
                setDataCaptureListener(
                    object : Visualizer.OnDataCaptureListener {
                        override fun onWaveFormDataCapture(
                            visualizer: Visualizer?, waveform: ByteArray?, samplingRate: Int
                        ) {
                            listener?.onData(waveform, null)
                        }

                        override fun onFftDataCapture(
                            visualizer: Visualizer?, fft: ByteArray?, samplingRate: Int
                        ) {
                            listener?.onData(null, fft)
                        }
                    },
                    Visualizer.getMaxCaptureRate() / 2,
                    true,   // waveform
                    true    // fft
                )
                enabled = true
            }
        }.getOrElse { e ->
            Log.w(TAG, "Visualizer start failed: ${e.message}")
            null
        }
        visualizer = v
        if (v != null) Log.i(TAG, "Visualizer running on global output")
    }

    fun stop() {
        runCatching { visualizer?.enabled = false }
        runCatching { visualizer?.release() }
        visualizer = null
    }

    companion object {
        private const val TAG = "FlowLumeVisualizer"
    }
}