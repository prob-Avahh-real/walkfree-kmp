package com.walkfree.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.math.PI
import kotlin.math.sin

class SignalProcessorTest {
    private val sp = SignalProcessorImpl()

    @Test
    fun `rms of constant signal equals value`() {
        val signal = DoubleArray(100) { 3.0 }
        assertEquals(3.0, sp.rms(signal), 1e-10)
    }

    @Test
    fun `rms of alternating signal`() {
        val signal = DoubleArray(1000) { if (it % 2 == 0) 1.0 else -1.0 }
        assertEquals(1.0, sp.rms(signal), 1e-10)
    }

    @Test
    fun `amplitude of sine wave`() {
        val signal = DoubleArray(1000) { sin(2.0 * PI * it / 100.0) }
        val ampl = sp.amplitude(signal)
        assertTrue(ampl > 1.9, "Expected amplitude ~2.0, got $ampl")
        assertTrue(ampl < 2.1, "Expected amplitude ~2.0, got $ampl")
    }

    @Test
    fun `zero crossings count`() {
        val signal = doubleArrayOf(1.0, -1.0, 1.0, -1.0, 1.0)
        assertEquals(0.8, sp.zeroCrossings(signal), 1e-10)
    }

    @Test
    fun `fft of pure 5Hz sine produces peak at 5Hz`() {
        val sampleRate = 50.0
        val freq = 5.0
        val n = 256
        val signal = DoubleArray(n) { sin(2.0 * PI * freq * it / sampleRate) }
        val magnitudes = sp.fftMagnitudes(signal, sampleRate)

        // 找 5Hz 附近的 bin
        val near5Hz = magnitudes.filter { (f, _) -> f in 4.5..5.5 }
        assertTrue(near5Hz.isNotEmpty(), "No bins near 5Hz")
        val maxNear5 = near5Hz.maxBy { it.second }
        val others = magnitudes.filter { (f, _) -> f !in 4.5..5.5 && f < 25.0 }
        val maxOther = if (others.isEmpty()) 0.0 else others.maxOf { it.second }

        assertTrue(maxNear5.second > maxOther * 5.0, "5Hz peak should dominate: peak=${maxNear5.second}, maxOther=$maxOther")
    }

    @Test
    fun `band power computation`() {
        val magnitudes = listOf(
            1.0 to 10.0, 2.0 to 20.0, 3.0 to 30.0, 5.0 to 50.0, 10.0 to 100.0
        )
        assertEquals(60.0, sp.bandPower(magnitudes, 1.0, 3.0), 1e-10)
        assertEquals(150.0, sp.bandPower(magnitudes, 5.0, 10.0), 1e-10)
    }

    @Test
    fun `freeze index high when 3-8Hz dominates`() {
        // 模拟冻结场景: 高频功率远大于低频
        val magnitudes = (0 until 128).map { i ->
            val freq = i * 50.0 / 256
            when {
                freq < 0.5 -> freq to 0.01
                freq in 0.5..3.0 -> freq to 1.0
                freq in 3.0..8.0 -> freq to 10.0
                else -> freq to 0.01
            }
        }
        val fi = sp.freezeIndex(magnitudes)
        assertTrue(fi > 1.8, "Freeze index should be > 1.8, got $fi")
    }

    @Test
    fun `freeze index low when locomotion dominates`() {
        val magnitudes = (0 until 128).map { i ->
            val freq = i * 50.0 / 256
            when {
                freq in 0.5..3.0 -> freq to 10.0
                freq in 3.0..8.0 -> freq to 1.0
                else -> freq to 0.01
            }
        }
        val fi = sp.freezeIndex(magnitudes)
        assertTrue(fi < 1.8, "Freeze index should be < 1.8, got $fi")
    }

    @Test
    fun `tremor peak at expected frequency`() {
        val magnitudes = (0 until 128).map { i ->
            val freq = i * 50.0 / 256
            if (freq in 4.5..5.5) freq to 100.0 else freq to 1.0
        }
        val peak = sp.tremorPeakFrequency(magnitudes)
        assertTrue(peak != null, "Should find a tremor peak")
        assertTrue(peak!! in 4.0..6.0, "Peak should be in 4-6Hz, got $peak")
    }

    @Test
    fun `cadence estimate from 1_5Hz peak gives 90 spm`() {
        val magnitudes = (0 until 128).map { i ->
            val freq = i * 50.0 / 256
            if (freq in 1.4..1.6) freq to 100.0 else freq to 1.0
        }
        val cadence = sp.estimateCadence(magnitudes)
        assertTrue(cadence in 80.0..100.0, "Expected ~90 spm, got $cadence")
    }

    @Test
    fun `signal window fills correctly`() {
        val window = ISignalProcessor.SignalWindow(100)
        repeat(50) { window.add(1.0) }
        assertEquals(50, window.size)
        assertEquals(false, window.isFull, "Window should not be full at 50/100")

        repeat(60) { window.add(2.0) }
        assertEquals(100, window.size)
        assertEquals(true, window.isFull, "Window should be full at 100")
    }
}
