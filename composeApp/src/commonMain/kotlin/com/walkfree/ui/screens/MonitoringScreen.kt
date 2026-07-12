package com.walkfree.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.walkfree.AppConfig
import com.walkfree.model.*

@Composable
fun MonitoringScreen() {
    var isMonitoring by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf(AppConfig.UI.statusReady) }
    var statusColor by remember { mutableStateOf(Color.Gray) }
    var latestReading by remember { mutableStateOf<SensorReading?>(null) }
    var recentEvents by remember { mutableStateOf(listOf<DetectionEvent>()) }

    val pulseAnim = rememberInfiniteTransition()
    val pulseScale by pulseAnim.animateFloat(1f, 1.08f, infiniteRepeatable(tween(800, easing = EaseInOut), RepeatMode.Reverse))

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(AppConfig.UI.cornerRadiusLarge.dp)) {
            Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                if (isMonitoring) {
                    Box(Modifier.size(16.dp).scale(pulseScale).background(Color(0xFFEF4444), CircleShape))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(status, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = statusColor)
                    latestReading?.let {
                        Text("加速度: ${"%.2f".format(it.accelMagnitude)} m/s²", color = Color.Gray, fontSize = 13.sp)
                    }
                }
                if (!isMonitoring) {
                    Button(onClick = {
                        isMonitoring = true
                        status = AppConfig.UI.statusMonitoring
                        statusColor = Color(0xFF059669)
                    }) { Text("开始") }
                } else {
                    OutlinedButton(onClick = {
                        isMonitoring = false
                        status = AppConfig.UI.statusStopped
                        statusColor = Color.Gray
                    }) { Text("停止") }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("实时指标", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LiveMetricCard("步频", "— 步/分", Modifier.weight(1f))
            LiveMetricCard("冻结指数", "—", Modifier.weight(1f))
            LiveMetricCard("振幅", "— m/s²", Modifier.weight(1f))
        }

        Spacer(Modifier.height(16.dp))
        Text("检测事件", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(Modifier.height(8.dp))
        if (recentEvents.isEmpty()) {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(AppConfig.UI.cornerRadiusMedium.dp)) {
                Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                    Text(if (isMonitoring) "监测中，等待事件..." else "未开始监测", color = Color.Gray, fontSize = 14.sp)
                }
            }
        } else {
            recentEvents.take(5).forEach { event ->
                Card(Modifier.fillMaxWidth().padding(bottom = 6.dp), shape = RoundedCornerShape(AppConfig.UI.cornerRadiusSmall.dp)) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(Modifier.size(8.dp), shape = CircleShape, color = eventColor(event.type)) {}
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(event.type.label, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("${"%.1f".format(event.durationSeconds)}s · 置信度 ${"%.0f".format(event.confidence * 100)}%", color = Color.Gray, fontSize = 12.sp)
                        }
                        Text(event.severity.label, color = severityColor(event.severity), fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun LiveMetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier, shape = RoundedCornerShape(AppConfig.UI.cornerRadiusMedium.dp)) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
            Text(label, fontSize = 12.sp, color = Color.Gray)
        }
    }
}

fun eventColor(type: MovementDisorderType): Color = when (type) {
    MovementDisorderType.FREEZING_OF_GAIT -> Color(0xFFEF4444)
    MovementDisorderType.RESTING_TREMOR -> Color(0xFFF59E0B)
    MovementDisorderType.BRADYKINESIA -> Color(0xFF3B82F6)
    else -> Color.Gray
}

fun severityColor(severity: Severity): Color = when (severity) {
    Severity.MILD -> Color(0xFFF59E0B)
    Severity.MODERATE -> Color(0xFFF97316)
    Severity.SEVERE -> Color(0xFFEF4444)
}
