package com.walkfree.engine

import com.walkfree.AppConfig
import kotlin.math.*

/// 信号处理器实现 — Cooley-Tukey FFT + 运动特征提取 (纯 Kotlin)
class SignalProcessorImpl : ISignalProcessor {

    override fun rms(signal: DoubleArray): Double {
        var sum = 0.0
        for (v in signal) sum += v * v
        return sqrt(sum / signal.size)
    }

    override fun amplitude(signal: DoubleArray): Double {
        var minVal = Double.MAX_VALUE
        var maxVal = Double.MIN_VALUE
        for (v in signal) { if (v < minVal) minVal = v; if (v > maxVal) maxVal = v }
        return maxVal - minVal
    }

    override fun zeroCrossings(signal: DoubleArray): Double {
        var count = 0
        for (i in 1 until signal.size) if (signal[i - 1].sign != signal[i].sign) count++
        return count.toDouble() / signal.size
    }

    override fun fftMagnitudes(input: DoubleArray, sampleRateHz: Double): List<Pair<Double, Double>> {
        val n = nextPowerOfTwo(input.size)
        val real = DoubleArray(n); val imag = DoubleArray(n)
        for (i in input.indices) real[i] = input[i]

        var j = 0
        for (i in 0 until n) {
            if (i < j) {
                val tr = real[i]; real[i] = real[j]; real[j] = tr
                val ti = imag[i]; imag[i] = imag[j]; imag[j] = ti
            }
            var m = n shr 1
            while (m >= 1 && j >= m) { j -= m; m = m shr 1 }
            j += m
        }

        var len = 2
        while (len <= n) {
            val half = len / 2; val angle = -2.0 * PI / len
            for (i in 0 until n step len) {
                for (k in 0 until half) {
                    val tReal = cos(angle * k) * real[i + k + half] - sin(angle * k) * imag[i + k + half]
                    val tImag = sin(angle * k) * real[i + k + half] + cos(angle * k) * imag[i + k + half]
                    real[i + k + half] = real[i + k] - tReal; imag[i + k + half] = imag[i + k] - tImag
                    real[i + k] += tReal; imag[i + k] += tImag
                }
            }
            len *= 2
        }

        return (0 until n / 2).map { i ->
            val freq = i * sampleRateHz / n
            freq to (real[i] * real[i] + imag[i] * imag[i]) / (n * n)
        }
    }

    override fun bandPower(magnitudes: List<Pair<Double, Double>>, lowHz: Double, highHz: Double): Double =
        magnitudes.sumOf { (freq, power) -> if (freq in lowHz..highHz) power else 0.0 }

    override fun freezeIndex(magnitudes: List<Pair<Double, Double>>): Double {
        val loco = bandPower(magnitudes, AppConfig.FogDetection.locomotorBandLow, AppConfig.FogDetection.locomotorBandHigh)
        val freeze = bandPower(magnitudes, AppConfig.FogDetection.freezeBandLow, AppConfig.FogDetection.freezeBandHigh)
        return if (loco < 1e-10) 0.0 else freeze / loco
    }

    override fun tremorPeakFrequency(magnitudes: List<Pair<Double, Double>>): Double? {
        var maxP = 0.0; var peak: Double? = null
        for ((freq, power) in magnitudes) {
            if (freq in AppConfig.TremorDetection.freqLow..AppConfig.TremorDetection.freqHigh && power > maxP) { maxP = power; peak = freq }
        }
        return peak
    }

    override fun estimateCadence(magnitudes: List<Pair<Double, Double>>): Double {
        var maxP = 0.0; var peak = 0.0
        for ((freq, power) in magnitudes) {
            if (freq in AppConfig.CadenceEstimation.bandLow..AppConfig.CadenceEstimation.bandHigh && power > maxP) { maxP = power; peak = freq }
        }
        return peak * 60.0
    }

    private fun nextPowerOfTwo(n: Int): Int { var p = AppConfig.fftMinSize; while (p < n) p = p shl 1; return p }
}
