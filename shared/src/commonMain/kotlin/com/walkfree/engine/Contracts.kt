package com.walkfree.engine

import com.walkfree.model.DetectionEvent
import com.walkfree.model.SensorReading
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

// ═══════════════════════════════════════════════════════
// 信号处理器接口
// ═══════════════════════════════════════════════════════

/// 数字信号处理器接口 — FFT + 时域/频域特征提取
interface ISignalProcessor {
    fun rms(signal: DoubleArray): Double
    fun amplitude(signal: DoubleArray): Double
    fun zeroCrossings(signal: DoubleArray): Double
    fun fftMagnitudes(input: DoubleArray, sampleRateHz: Double = 50.0): List<Pair<Double, Double>>
    fun bandPower(magnitudes: List<Pair<Double, Double>>, lowHz: Double, highHz: Double): Double
    fun freezeIndex(magnitudes: List<Pair<Double, Double>>): Double
    fun tremorPeakFrequency(magnitudes: List<Pair<Double, Double>>): Double?
    fun estimateCadence(magnitudes: List<Pair<Double, Double>>): Double

    /// 滑动窗口 — 线程安全 (Mutex 保护 BLE 回调 vs 检测引擎竞争)
    class SignalWindow(maxSize: Int = 300) : ISignalWindow {
        private val buffer = mutableListOf<Double>()
        private val mutex = Mutex()
        private val max = maxSize

        override suspend fun add(value: Double) = mutex.withLock {
            buffer.add(value); if (buffer.size > max) buffer.removeAt(0)
        }
        override suspend fun clear() = mutex.withLock { buffer.clear() }
        override suspend fun toArray(): DoubleArray = mutex.withLock { buffer.toDoubleArray() }
        override suspend fun size(): Int = mutex.withLock { buffer.size }
        override suspend fun isFull(): Boolean = mutex.withLock { buffer.size >= max }
    }
}

/// 滑动窗口抽象
interface ISignalWindow {
    suspend fun add(value: Double)
    suspend fun clear()
    suspend fun toArray(): DoubleArray
    suspend fun size(): Int
    suspend fun isFull(): Boolean
}

// ═══════════════════════════════════════════════════════
// 检测引擎接口
// ═══════════════════════════════════════════════════════

interface IDetectionEngine {
    suspend fun processFrame(reading: SensorReading): List<DetectionEvent>
    suspend fun reset()
}

// ═══════════════════════════════════════════════════════
// ML 推理引擎接口 (预留 — ONNX Runtime 接入点)
// ═══════════════════════════════════════════════════════

interface IMlInferenceEngine {
    val isLoaded: Boolean
    suspend fun predict(accelerometerData: FloatArray): Float
}
