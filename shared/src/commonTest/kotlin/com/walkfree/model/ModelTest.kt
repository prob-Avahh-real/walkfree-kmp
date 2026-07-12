package com.walkfree.model

import com.walkfree.engine.CalibrationConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ModelTest {

    @Test
    fun `detection event default values`() {
        val event = DetectionEvent(id = "TEST", timestamp = 1000L, type = MovementDisorderType.FREEZING_OF_GAIT)
        assertEquals("TEST", event.id)
        assertEquals(MovementDisorderType.FREEZING_OF_GAIT, event.type)
        assertEquals(com.walkfree.model.Severity.MODERATE, event.severity)
        assertEquals(0.0, event.confidence)
        assertEquals(null, event.freezeIndex)
    }

    @Test
    fun `monitoring session computed properties`() {
        val events = listOf(
            DetectionEvent("1", 1000L, MovementDisorderType.FREEZING_OF_GAIT),
            DetectionEvent("2", 2000L, MovementDisorderType.FREEZING_OF_GAIT),
            DetectionEvent("3", 3000L, MovementDisorderType.RESTING_TREMOR),
            DetectionEvent("4", 4000L, MovementDisorderType.BRADYKINESIA)
        )
        val session = MonitoringSession(id = "SES", startTime = 1000L, events = events)
        assertEquals(2, session.fogCount)
        assertEquals(1, session.tremorCount)
        assertEquals(1, session.bradykinesiaCount)
    }

    @Test
    fun `movement disorder type labels`() {
        assertEquals("步态冻结", MovementDisorderType.FREEZING_OF_GAIT.label)
        assertEquals("静止性震颤", MovementDisorderType.RESTING_TREMOR.label)
        assertEquals("运动迟缓", MovementDisorderType.BRADYKINESIA.label)
        assertEquals("正常", MovementDisorderType.NORMAL.label)
    }

    @Test
    fun `calibration config defaults from AppConfig`() {
        val config = CalibrationConfig()
        assertEquals(1.8, config.fogThreshold)
        assertEquals(0.3, config.tremorPowerThreshold)
        assertEquals(0.15, config.bradykinesiaAmplitudeThreshold)
        assertEquals(40.0, config.bradykinesiaCadenceThreshold)
        assertEquals(3, config.consecutiveConfirmations)
        assertEquals(3000L, config.debounceMs)
    }
}
