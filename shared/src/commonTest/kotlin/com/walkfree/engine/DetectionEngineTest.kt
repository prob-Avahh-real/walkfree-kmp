package com.walkfree.engine

import com.walkfree.model.*
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/// Mock ISignalProcessor for controlled test input
class MockSignalProcessor(
    private val freezeIndexValue: Double = 1.0,
    private val tremorPeakValue: Double? = null,
    private val cadenceValue: Double = 80.0,
    private val amplitudeValue: Double = 0.3,
    private val rmsValue: Double = 0.1
) : ISignalProcessor {
    var lastInput: DoubleArray? = null

    override fun rms(signal: DoubleArray) = rmsValue
    override fun amplitude(signal: DoubleArray) = amplitudeValue
    override fun zeroCrossings(signal: DoubleArray) = 0.0
    override fun fftMagnitudes(input: DoubleArray, sampleRateHz: Double): List<Pair<Double, Double>> {
        lastInput = input
        return emptyList()
    }
    override fun bandPower(magnitudes: List<Pair<Double, Double>>, lowHz: Double, highHz: Double) =
        if (lowHz == 3.0 && highHz == 8.0) 0.5 else 1.0
    override fun freezeIndex(magnitudes: List<Pair<Double, Double>>) = freezeIndexValue
    override fun tremorPeakFrequency(magnitudes: List<Pair<Double, Double>>) = tremorPeakValue
    override fun estimateCadence(magnitudes: List<Pair<Double, Double>>) = cadenceValue
}

class DetectionEngineTest {

    private fun nowMs() = Clock.System.now().toEpochMilliseconds()

    @Test
    fun `no events when window not full`() = runTest {
        val engine = DetectionEngineImpl(signalProcessor = MockSignalProcessor())
        val reading = SensorReading(nowMs(), 1.0, 0.0, 0.0)
        val events = engine.processFrame(reading)
        assertEquals(0, events.size, "Should have no events before window is full")
    }

    @Test
    fun `fog detected when freeze index exceeds threshold`() = runTest {
        val sp = MockSignalProcessor(freezeIndexValue = 3.0)
        val calibration = CalibrationConfig(fogThreshold = 1.8, debounceMs = 0)
        val engine = DetectionEngineImpl(signalProcessor = sp, calibration = calibration)

        val reading = SensorReading(nowMs(), 1.0, 0.0, 0.0)
        repeat(300) { engine.processFrame(reading) }
        repeat(3) { engine.processFrame(reading) }

        val events = engine.processFrame(reading)
        val fogEvents = events.filter { it.type == MovementDisorderType.FREEZING_OF_GAIT }
        assertTrue(fogEvents.isNotEmpty(), "Should detect FoG after 3 confirmations, got ${events.size} events")
        fogEvents.first().let {
            assertEquals(MovementDisorderType.FREEZING_OF_GAIT, it.type)
            assertTrue(it.confidence > 0.0)
        }
    }

    @Test
    fun `no fog when freeze index below threshold`() = runTest {
        val sp = MockSignalProcessor(freezeIndexValue = 1.0)
        val calibration = CalibrationConfig(fogThreshold = 1.8, debounceMs = 0)
        val engine = DetectionEngineImpl(signalProcessor = sp, calibration = calibration)

        val reading = SensorReading(nowMs(), 1.0, 0.0, 0.0)
        repeat(310) { engine.processFrame(reading) }

        val events = engine.processFrame(reading)
        assertTrue(events.none { it.type == MovementDisorderType.FREEZING_OF_GAIT }, "No FoG when FI < threshold")
    }

    @Test
    fun `tremor detected when peak is 4-6Hz with sufficient power`() = runTest {
        val sp = MockSignalProcessor(tremorPeakValue = 5.0, rmsValue = 0.1)
        val calibration = CalibrationConfig(tremorPowerThreshold = 0.2, debounceMs = 0)
        val engine = DetectionEngineImpl(signalProcessor = sp, calibration = calibration)

        val reading = SensorReading(nowMs(), 1.0, 0.0, 0.0)
        repeat(310) { engine.processFrame(reading) }

        val events = engine.processFrame(reading)
        val tremorEvents = events.filter { it.type == MovementDisorderType.RESTING_TREMOR }
        assertTrue(tremorEvents.isNotEmpty(), "Should detect tremor, got events: $events")
    }

    @Test
    fun `no tremor when rms too low`() = runTest {
        val sp = MockSignalProcessor(tremorPeakValue = 5.0, rmsValue = 0.01)
        val calibration = CalibrationConfig(tremorPowerThreshold = 0.2, debounceMs = 0)
        val engine = DetectionEngineImpl(signalProcessor = sp, calibration = calibration)

        val reading = SensorReading(nowMs(), 1.0, 0.0, 0.0)
        repeat(310) { engine.processFrame(reading) }

        val events = engine.processFrame(reading)
        assertTrue(events.none { it.type == MovementDisorderType.RESTING_TREMOR }, "No tremor when RMS < threshold")
    }

    @Test
    fun `bradykinesia detected when amplitude and cadence low`() = runTest {
        val sp = MockSignalProcessor(amplitudeValue = 0.05, cadenceValue = 30.0)
        val calibration = CalibrationConfig(
            bradykinesiaAmplitudeThreshold = 0.15,
            bradykinesiaCadenceThreshold = 40.0,
            armSwingGyroThreshold = 0.3,
            debounceMs = 0
        )
        val engine = DetectionEngineImpl(signalProcessor = sp, calibration = calibration)

        val reading = SensorReading(nowMs(), 1.0, 0.0, 0.0)
        repeat(310) { engine.processFrame(reading) }

        val events = engine.processFrame(reading)
        val bradyEvents = events.filter { it.type == MovementDisorderType.BRADYKINESIA }
        assertTrue(bradyEvents.isNotEmpty(), "Should detect bradykinesia, got events: $events")
    }

    @Test
    fun `debounce prevents rapid re-detection`() = runTest {
        val sp = MockSignalProcessor(freezeIndexValue = 3.0)
        val calibration = CalibrationConfig(fogThreshold = 1.8, debounceMs = 3000L)
        val engine = DetectionEngineImpl(signalProcessor = sp, calibration = calibration)

        val reading = SensorReading(nowMs(), 1.0, 0.0, 0.0)
        repeat(310) { engine.processFrame(reading) }

        engine.processFrame(reading)
        val events2 = engine.processFrame(reading)
        val fogCount2 = events2.count { it.type == MovementDisorderType.FREEZING_OF_GAIT }

        assertTrue(fogCount2 == 0, "Debounce should prevent second FoG event immediately after first")
    }

    @Test
    fun `reset clears all state`() = runTest {
        val sp = MockSignalProcessor(freezeIndexValue = 3.0)
        val calibration = CalibrationConfig(fogThreshold = 1.8, debounceMs = 0)
        val engine = DetectionEngineImpl(signalProcessor = sp, calibration = calibration)

        val reading = SensorReading(nowMs(), 1.0, 0.0, 0.0)
        repeat(310) { engine.processFrame(reading) }

        engine.reset()

        val events = engine.processFrame(reading)
        assertEquals(0, events.size, "After reset, window should be empty = no events")
    }

    @Test
    fun `severity from confidence ranges`() = runTest {
        val sp = MockSignalProcessor(freezeIndexValue = 5.0)
        val calibration = CalibrationConfig(fogThreshold = 1.8, debounceMs = 0)
        val engine = DetectionEngineImpl(signalProcessor = sp, calibration = calibration)

        val reading = SensorReading(nowMs(), 1.0, 0.0, 0.0)
        repeat(310) { engine.processFrame(reading) }

        val events = engine.processFrame(reading)
        val fogEvent = events.first { it.type == MovementDisorderType.FREEZING_OF_GAIT }
        assertEquals(Severity.SEVERE, fogEvent.severity, "FI=5.0 → conf ~1.0 → should be SEVERE")
    }
}
