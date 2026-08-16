package com.zhuomo.flowlume.audio

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.sin
import kotlin.math.sqrt

/** 自研迭代 Radix-2 FFT（1024 点 < 0.5ms，低端机可实时） */
object Fft {

    /** in-place 复数 FFT；im 初始全 0 表示纯实数输入 */
    fun forward(re: FloatArray, im: FloatArray, n: Int) {
        require(n > 0 && n and (n - 1) == 0) { "n must be power of two" }

        // bit-reversal permutation
        var j = 0
        for (i in 1 until n) {
            var bit = n shr 1
            while (j and bit != 0) {
                j = j xor bit
                bit = bit shr 1
            }
            j = j xor bit
            if (i < j) {
                val tr = re[i]; re[i] = re[j]; re[j] = tr
                val ti = im[i]; im[i] = im[j]; im[j] = ti
            }
        }

        var len = 2
        while (len <= n) {
            val ang = -2.0 * PI / len
            val wRe = cos(ang).toFloat()
            val wIm = sin(ang).toFloat()
            var i = 0
            while (i < n) {
                var curRe = 1f
                var curIm = 0f
                for (k in 0 until len / 2) {
                    val uRe = re[i + k]
                    val uIm = im[i + k]
                    val vRe = re[i + k + len / 2] * curRe - im[i + k + len / 2] * curIm
                    val vIm = re[i + k + len / 2] * curIm + im[i + k + len / 2] * curRe
                    re[i + k] = uRe + vRe
                    im[i + k] = uIm + vIm
                    re[i + k + len / 2] = uRe - vRe
                    im[i + k + len / 2] = uIm - vIm
                    val nRe = curRe * wRe - curIm * wIm
                    curIm = curRe * wIm + curIm * wRe
                    curRe = nRe
                }
                i += len
            }
            len = len shl 1
        }
    }

    /** 幅值谱 → 对数频段分桶（40Hz–16kHz） */
    fun bandEnergies(mag: FloatArray, sampleRate: Int, bands: Int = FrameData.BAND_COUNT): FloatArray {
        val result = FloatArray(bands)
        val nyquist = sampleRate / 2f
        val fMin = 40f
        val fMax = 16_000f
        val logMin = ln(fMin)
        val logSpan = ln(fMax / fMin)
        for (i in 1 until mag.size / 2) {
            val freq = i * nyquist / (mag.size - 1)
            if (freq < fMin) continue
            val t = (ln(freq) - logMin) / logSpan
            val idx = (t * bands).toInt().coerceIn(0, bands - 1)
            result[idx] += mag[i]
        }
        // 归一化 + 分贝压缩（更接近听感）
        var max = result.maxOrNull() ?: 0f
        if (max <= 0f) max = 1f
        for (b in 0 until bands) {
            result[b] = sqrt(result[b] / max)
        }
        return result
    }
}
