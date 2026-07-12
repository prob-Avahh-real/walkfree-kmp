package com.walkfree.engine

import com.walkfree.AppConfig
import com.walkfree.model.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlin.math.*

data class CalibrationConfig(
    val fogThreshold: Double = AppConfig.FogDetection.defaultThreshold,
    val tremorPowerThreshold: Double = AppConfig.TremorDetection.powerThreshold,
    val bradykinesiaAmplitudeThreshold: Double = AppConfig.BradyDetection.amplitudeThreshold,
    val bradykinesiaCadenceThreshold: Double = AppConfig.BradyDetection.cadenceThreshold,
    val armSwingGyroThreshold: Double = AppConfig.BradyDetection.armSwingGyroRmsThreshold,
    val consecutiveConfirmations: Int = AppConfig.consecutiveConfirmations,
    val debounceMs: Long = AppConfig.debounceMs
)

/// 检测引擎实现 — 策略模式 + Mutex 线程安全
class DetectionEngineImpl(
    private val signalProcessor: ISignalProcessor = SignalProcessorImpl(),
    private val calibration: CalibrationConfig = CalibrationConfig(),
    private val mlEngine: IMlInferenceEngine? = null
) : IDetectionEngine {

    private val accelWindow = ISignalProcessor.SignalWindow(AppConfig.windowSize)
    private val gyroWindow = ISignalProcessor.SignalWindow(AppConfig.windowSize)
    private val mutex = Mutex()

    private var fogConfirm = 0; private var tremorConfirm = 0; private var bradyConfirm = 0
    private var lastFog = 0L; private var lastTremor = 0L; private var lastBrady = 0L

    override suspend fun processFrame(reading: SensorReading): List<DetectionEvent> = mutex.withLock {
        val events = mutableListOf<DetectionEvent>()
        accelWindow.add(reading.accelMagnitude)
        gyroWindow.add(reading.gyroMagnitude)
        if (!accelWindow.isFull()) return@withLock emptyList()

        val accelSig = accelWindow.toArray()
        val gyroSig = gyroWindow.toArray()
        val magnitudes = signalProcessor.fftMagnitudes(accelSig, AppConfig.sampleRateHz)
        val now = Clock.System.now().toEpochMilliseconds()

        // ── FoG ──
        val fi = signalProcessor.freezeIndex(magnitudes)
        val fogConf = min(fi / 5.0, AppConfig.FogDetection.maxConfidence)
        if (fi > calibration.fogThreshold) {
            if (++fogConfirm >= calibration.consecutiveConfirmations && (now - lastFog) > calibration.debounceMs) {
                events.add(DetectionEvent("FOG$now", now, MovementDisorderType.FREEZING_OF_GAIT, severity(fogConf), fogConf, freezeIndex = fi))
                lastFog = now; fogConfirm = 0
            }
        } else fogConfirm = 0

        // ── Tremor ──
        val tremorPeak = signalProcessor.tremorPeakFrequency(magnitudes)
        val tremorPower = signalProcessor.bandPower(magnitudes, AppConfig.TremorDetection.freqLow, AppConfig.TremorDetection.freqHigh)
        val accelRMS = signalProcessor.rms(accelSig)
        if (tremorPeak != null && tremorPeak in AppConfig.TremorDetection.peakLow..AppConfig.TremorDetection.peakHigh &&
            tremorPower > calibration.tremorPowerThreshold && accelRMS > AppConfig.TremorDetection.rmsThreshold) {
            if (++tremorConfirm >= calibration.consecutiveConfirmations && (now - lastTremor) > calibration.debounceMs) {
                events.add(DetectionEvent("TMR$now", now, MovementDisorderType.RESTING_TREMOR, Severity.MODERATE, min(tremorPower, 1.0), tremorFrequency = tremorPeak))
                lastTremor = now; tremorConfirm = 0
            }
        } else tremorConfirm = 0

        // ── Bradykinesia ──
        val ampl = signalProcessor.amplitude(accelSig)
        val cadence = signalProcessor.estimateCadence(magnitudes)
        val gyroRMS = signalProcessor.rms(gyroSig)
        if (ampl < calibration.bradykinesiaAmplitudeThreshold && cadence < calibration.bradykinesiaCadenceThreshold &&
            gyroRMS < calibration.armSwingGyroThreshold && ampl > AppConfig.BradyDetection.minAmplitude) {
            if (++bradyConfirm >= calibration.consecutiveConfirmations && (now - lastBrady) > calibration.debounceMs) {
                events.add(DetectionEvent("BDY$now", now, MovementDisorderType.BRADYKINESIA, Severity.MILD, min((calibration.bradykinesiaAmplitudeThreshold - ampl).coerceAtLeast(0.0) / calibration.bradykinesiaAmplitudeThreshold, 1.0), movementAmplitude = ampl))
                lastBrady = now; bradyConfirm = 0
            }
        } else bradyConfirm = 0

        events
    }

    override suspend fun reset() = mutex.withLock {
        accelWindow.clear(); gyroWindow.clear()
        fogConfirm = 0; tremorConfirm = 0; bradyConfirm = 0
        lastFog = 0L; lastTremor = 0L; lastBrady = 0L
    }

    private fun severity(c: Double): Severity = when { c > 0.7 -> Severity.SEVERE; c > 0.4 -> Severity.MODERATE; else -> Severity.MILD }
}

/// 校准引擎 — class (可模拟)
class CalibrationEngine(private val sp: ISignalProcessor = SignalProcessorImpl()) {

    class CalibrationSample(
        val freezeIndices: MutableList<Double> = mutableListOf(),
        val tremorPowers: MutableList<Double> = mutableListOf(),
        val amplitudes: MutableList<Double> = mutableListOf(),
        val cadences: MutableList<Double> = mutableListOf()
    )

    fun collect(reading: SensorReading, sample: CalibrationSample): CalibrationSample {
        val signal = DoubleArray(AppConfig.Calibration.sampleCount) { reading.accelMagnitude }
        val magnitudes = sp.fftMagnitudes(signal)
        sample.freezeIndices.add(sp.freezeIndex(magnitudes))
        sample.tremorPowers.add(sp.bandPower(magnitudes, AppConfig.TremorDetection.freqLow, AppConfig.TremorDetection.freqHigh))
        sample.amplitudes.add(sp.amplitude(signal))
        sample.cadences.add(sp.estimateCadence(magnitudes))
        return sample
    }

    fun computeConfig(sample: CalibrationSample): CalibrationConfig {
        fun mean(v: List<Double>) = if (v.isEmpty()) 0.0 else v.sum() / v.size
        fun std(v: List<Double>): Double {
            val m = mean(v); if (v.size <= 1) return 0.0
            return sqrt(v.sumOf { (it - m) * (it - m) } / (v.size - 1))
        }
        return CalibrationConfig(
            fogThreshold = mean(sample.freezeIndices) + AppConfig.Calibration.fogSigma * std(sample.freezeIndices),
            tremorPowerThreshold = mean(sample.tremorPowers) + AppConfig.Calibration.tremorSigma * std(sample.tremorPowers),
            bradykinesiaAmplitudeThreshold = mean(sample.amplitudes) * AppConfig.Calibration.bradyAmplitudeFactor,
            bradykinesiaCadenceThreshold = mean(sample.cadences) * AppConfig.Calibration.bradyCadenceFactor
        )
    }
}
