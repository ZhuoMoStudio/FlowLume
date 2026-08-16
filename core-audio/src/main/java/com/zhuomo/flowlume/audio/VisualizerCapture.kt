package com.zhuomo.flowlume.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.audiofx.Visualizer
import android.util.Log

/**
 * 音频可视化捕获：基于系统 Visualizer（分析设备输出音频，非录音接口）。
 * 竞品 Diffuse 同款方案 —— 无需「录制或投放」屏幕捕获授权，
 * 仅需 RECORD_AUDIO（麦克风权限名，但 Visualizer 不采集任何麦克风数据）。
 *
 * 数据来源：AudioManager.activePlaybackConfigurations 找到当前活跃播放会话，
 * Visualizer 绑定该会话获取波形/FFT，仅用于可视化分析。
 */
class VisualizerCapture(private val context: Context) {

    interface Listener {
        fun onData(waveform: ByteArray?, fft: ByteArray?)
    }

    var listener: Listener? = null

    private var visualizer: Visualizer? = null

    val isRunning: Boolean get() = visualizer?.enabled == true

    /** 启动捕获；无活跃播放时绑定全局输出（sessionId=0） */
    fun start() {
        stop()
        val sessionId = findActiveSessionId()
        val v = runCatching {
            Visualizer(sessionId).apply {
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
            Log.w(TAG, "Visualizer start failed (session=$sessionId): ${e.message}")
            null
        }
        visualizer = v
        if (v != null) Log.i(TAG, "Visualizer running on session=$sessionId")
    }

    fun stop() {
        runCatching { visualizer?.enabled = false }
        runCatching { visualizer?.release() }
        visualizer = null
    }

    private fun findActiveSessionId(): Int {
        val am = context.getSystemService(AudioManager::class.java)
        return runCatching {
            am.activePlaybackConfigurations
                .firstOrNull {
                    it.isActive &&
                        it.audioAttributes.usage in listOf(
                            AudioAttributes.USAGE_MEDIA,
                            AudioAttributes.USAGE_GAME
                        )
                }
                ?.audioSessionId
        }.getOrDefault(null) ?: 0 // 0 = 全局输出混合
    }

    companion object {
        private const val TAG = "FlowLumeVisualizer"
    }
}
