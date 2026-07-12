package com.walkfree.engine

import com.walkfree.model.*
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class WalkFreeAgentTest {
    private val engine = DetectionEngineImpl()

    @Test
    fun `starts in IDLE mode`() = runTest {
        val agent = WalkFreeAgent(engine)
        assertEquals(MonitorMode.IDLE, agent.mode.value)
    }

    @Test
    fun `prolonged stillness triggers autoStart`() = runTest {
        val agent = WalkFreeAgent(engine)
        val stillReading = SensorReading(System.currentTimeMillis(), 0.01, 0.0, 0.0)

        // Simulate 6 frames of stillness (each frame increments quietMinutes)
        repeat(6) { agent.evaluate(stillReading, emptyList()) }

        val decision = agent.lastDecision.value
        assertNotNull(decision)
        assertTrue(decision!!.autoStart, "长时间静止应触发自动开始")
        assertEquals(MonitorMode.ACTIVE, decision.mode)
    }

    @Test
    fun `severe streak triggers ALERT mode`() = runTest {
        val agent = WalkFreeAgent(engine)
        val reading = SensorReading(System.currentTimeMillis(), 0.1, 0.0, 0.0)
        val severeEvent = DetectionEvent("E1", reading.timestamp, MovementDisorderType.FREEZING_OF_GAIT, Severity.SEVERE, 0.9)

        // 3 severe events in a row → ALERT
        repeat(3) { agent.evaluate(reading, listOf(severeEvent)) }

        val decision = agent.lastDecision.value
        assertNotNull(decision)
        assertTrue(decision!!.proactiveAlert, "连续严重事件应触发主动告警")
        assertEquals(MonitorMode.ALERT, decision.mode)
        assertTrue(decision.suggestedAction.contains("就医") || decision.suggestedAction.contains("休息"))
    }

    @Test
    fun `movement resets stillness counter`() = runTest {
        val agent = WalkFreeAgent(engine)
        val stillReading = SensorReading(System.currentTimeMillis(), 0.01, 0.0, 0.0)
        val movingReading = SensorReading(System.currentTimeMillis(), 0.5, 0.0, 0.0)

        repeat(3) { agent.evaluate(stillReading, emptyList()) }
        // Now move
        agent.evaluate(movingReading, emptyList())

        assertFalse(agent.lastDecision.value?.autoStart ?: false, "运动后不应触发自动开始")
    }

    @Test
    fun `long quiet period switches to PASSIVE`() = runTest {
        val agent = WalkFreeAgent(engine)
        // Force into ACTIVE first
        val stillReading = SensorReading(System.currentTimeMillis(), 0.01, 0.0, 0.0)
        repeat(7) { agent.evaluate(stillReading, emptyList()) }
        assertEquals(MonitorMode.ACTIVE, agent.mode.value)

        // Now simulate 31 more frames of stillness → should go PASSIVE
        repeat(31) { agent.evaluate(stillReading, emptyList()) }
        assertEquals(MonitorMode.PASSIVE, agent.mode.value)
    }

    @Test
    fun `non-severe events do not trigger alert`() = runTest {
        val agent = WalkFreeAgent(engine)
        val reading = SensorReading(System.currentTimeMillis(), 0.1, 0.0, 0.0)
        val mildEvent = DetectionEvent("E1", reading.timestamp, MovementDisorderType.BRADYKINESIA, Severity.MILD, 0.3)

        repeat(5) { agent.evaluate(reading, listOf(mildEvent)) }
        assertNotEquals(MonitorMode.ALERT, agent.mode.value)
    }

    @Test
    fun `reset brings back to IDLE`() = runTest {
        val agent = WalkFreeAgent(engine)
        val stillReading = SensorReading(System.currentTimeMillis(), 0.01, 0.0, 0.0)
        repeat(10) { agent.evaluate(stillReading, emptyList()) }

        agent.reset()
        assertEquals(MonitorMode.IDLE, agent.mode.value)
    }
}
