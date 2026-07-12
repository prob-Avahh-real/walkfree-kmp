package com.walkfree.engine

import com.walkfree.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/// 监测模式 — Agent 决策输出的工作状态
enum class MonitorMode(val label: String) {
    IDLE("待命"),
    CALIBRATING("校准中"),
    ACTIVE("主动监测"),
    PASSIVE("被动采集"),
    ALERT("告警中")
}

/// Agent 决策 — 检测引擎对外暴露的自主决策结果
data class AgentDecision(
    val mode: MonitorMode,
    val autoStart: Boolean = false,
    val thresholdAdapted: Boolean = false,
    val proactiveAlert: Boolean = false,
    val severityEscalation: Boolean = false,
    val suggestedAction: String = ""
)

/// WalkFree Agent — 自主决策引擎
///
/// 决策规则:
/// - 连续静止 > 5 min → 建议自动开始监测
/// - 连续 3 次严重事件 → 升级 ALERT + 主动告警
/// - 30 min 无事件 → 降级为被动采集
class WalkFreeAgent(
    private val detectionEngine: IDetectionEngine
) {
    private val _mode = MutableStateFlow(MonitorMode.IDLE)
    val mode: StateFlow<MonitorMode> = _mode

    private val _lastDecision = MutableStateFlow<AgentDecision?>(null)
    val lastDecision: StateFlow<AgentDecision?> = _lastDecision

    private val eventHistory = mutableListOf<DetectionEvent>()
    private var quietMinutes = 0
    private var severeStreak = 0
    private val mutex = Mutex()

    suspend fun evaluate(reading: SensorReading, events: List<DetectionEvent>): AgentDecision =
        mutex.withLock {
            if (reading.accelMagnitude < 0.05) quietMinutes++ else quietMinutes = 0

            eventHistory.addAll(events)
            val recent = reading.timestamp - 300_000L
            eventHistory.removeAll { it.timestamp <= recent }

            val autoStart = quietMinutes > 5 && _mode.value == MonitorMode.IDLE
            severeStreak = if (events.any { it.severity == Severity.SEVERE }) severeStreak + 1
            else maxOf(0, severeStreak - 1)

            val alertNow = severeStreak >= 3
            val shouldPassive = eventHistory.isEmpty() && quietMinutes > 30

            val newMode = when {
                autoStart -> MonitorMode.ACTIVE
                alertNow -> MonitorMode.ALERT
                shouldPassive && _mode.value == MonitorMode.ACTIVE -> MonitorMode.PASSIVE
                else -> _mode.value
            }

            val decision = AgentDecision(
                mode = newMode,
                autoStart = autoStart,
                proactiveAlert = alertNow,
                severityEscalation = alertNow,
                suggestedAction = when {
                    alertNow -> "⚠️ 连续严重事件！建议立即休息或就医"
                    autoStart -> "检测到静止，建议开始监测"
                    newMode == MonitorMode.PASSIVE -> "长时间无事件，切换为省电被动采集"
                    else -> ""
                }
            )
            _mode.value = newMode
            _lastDecision.value = decision
            decision
        }

    fun reset() = kotlinx.coroutines.runBlocking {
        mutex.withLock { eventHistory.clear(); severeStreak = 0; quietMinutes = 0; _mode.value = MonitorMode.IDLE }
    }
}
