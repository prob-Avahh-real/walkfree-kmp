package com.walkfree.engine

import kotlin.test.Test
import kotlin.test.assertTrue

class CalibrationEngineTest {

    @Test
    fun `calibration produces personalized thresholds`() {
        val sample = CalibrationEngine.CalibrationSample()
        val sp = SignalProcessorImpl()

        // Simulate normal walking data with noise
        val reading = com.walkfree.model.SensorReading(
            System.currentTimeMillis(), 1.0, 0.5, 0.3, 0.1, 0.05, 0.05
        )

        repeat(10) { CalibrationEngine.collectCalibrationData(reading, sample, sp) }

        assertTrue(sample.freezeIndices.isNotEmpty(), "Should have freeze indices")
        assertTrue(sample.tremorPowers.isNotEmpty(), "Should have tremor powers")
        assertTrue(sample.amplitudes.isNotEmpty(), "Should have amplitudes")
        assertTrue(sample.cadences.isNotEmpty(), "Should have cadences")

        val config = CalibrationEngine.computeConfig(sample)
        assertTrue(config.fogThreshold > 0.0, "Fog threshold should be positive")
        assertTrue(config.tremorPowerThreshold > 0.0, "Tremor power threshold should be positive")
        assertTrue(config.bradykinesiaAmplitudeThreshold > 0.0, "Amplitude threshold should be positive")
        assertTrue(config.bradykinesiaCadenceThreshold > 0.0, "Cadence threshold should be positive")
    }
}
