package com.zhuomo.flowlume.audio

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.util.Log

/**
 * 音频捕获：AudioPlaybackCapture（Android 10+，即 minSdk 范围）。
 * 仅捕获设备输出混合音频（USAGE_MEDIA），绝不访问麦克风数据流。
 */
class AudioCaptureManager(private val context: Context) {

    private var projection: MediaProjection? = null
    private var audioRecord: AudioRecord? = null

    val isCapturing: Boolean get() = audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING

    /** 需要在 Activity 收到 MediaProjection 授权结果后调用 */
    fun start(resultCode: Int, data: Intent): Boolean = runCatching {
        val pm = context.getSystemService(MediaProjectionManager::class.java)
        val proj = pm.getMediaProjection(resultCode, data) ?: return false
        projection = proj

        val config = AudioPlaybackCaptureConfiguration.Builder(proj)
            .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
            .build()

        val sampleRate = 44100
        val format = AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(sampleRate)
            .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
            .build()

        val minBuf = AudioRecord.getMinBufferSize(
            sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        val record = AudioRecord.Builder()
            .setAudioFormat(format)
            .setBufferSizeInBytes(minBuf * 2)
            .setAudioPlaybackCaptureConfig(config)
            .build()

        proj.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() {
                Log.w(TAG, "MediaProjection stopped by system")
                stop()
            }
        }, null)

        record.startRecording()
        audioRecord = record
        true
    }.getOrElse {
        Log.e(TAG, "capture start failed: $it")
        stop()
        false
    }

    /** 读取一帧 PCM 数据，返回读取样本数 */
    fun read(buffer: ShortArray): Int =
        audioRecord?.read(buffer, 0, buffer.size, AudioRecord.READ_BLOCKING) ?: 0

    fun stop() {
        runCatching { audioRecord?.stop() }
        audioRecord?.release()
        audioRecord = null
        runCatching { projection?.stop() }
        projection = null
    }

    companion object {
        private const val TAG = "FlowLumeAudioCap"
        val available: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    }
}
