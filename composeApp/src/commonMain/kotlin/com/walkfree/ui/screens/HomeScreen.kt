package com.walkfree.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.walkfree.AppConfig

@Composable
fun HomeScreen() {
    val summary = Summary(
        todayFog = 2, todayTremor = 5, todayBrady = 1,
        lastSessionDuration = "12:34", weeklyTrend = -1
    )

    Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text(AppConfig.appNameCn, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("运动障碍实时追踪", color = Color.Gray, fontSize = 14.sp)
        Spacer(Modifier.height(24.dp))

        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(AppConfig.UI.cornerRadiusLarge.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Bluetooth, "BLE", tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("可穿戴设备", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(AppConfig.UI.statusBleDisconnected, color = Color.Gray, fontSize = 13.sp)
                }
                Button(onClick = {}, modifier = Modifier.height(36.dp)) { Text("扫描", fontSize = 13.sp) }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("今日摘要", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard("步态冻结", "${summary.todayFog} 次", Color(0xFFEF4444), Modifier.weight(1f))
            MetricCard("静止震颤", "${summary.todayTremor} 次", Color(0xFFF59E0B), Modifier.weight(1f))
            MetricCard("运动迟缓", "${summary.todayBrady} 次", Color(0xFF3B82F6), Modifier.weight(1f))
        }

        Spacer(Modifier.height(16.dp))
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(AppConfig.UI.cornerRadiusMedium.dp)) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("本周趋势", fontSize = 14.sp, fontWeight = FontWeight.W500)
                    Text(
                        if (summary.weeklyTrend < 0) "↓ 较上周减少" else "→ 无变化",
                        color = if (summary.weeklyTrend < 0) Color(0xFF059669) else Color.Gray,
                        fontSize = 13.sp
                    )
                }
                Text("最近 ${summary.lastSessionDuration}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }

        Spacer(Modifier.height(20.dp))
        Text("功能", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FeatureCard("开始监测", Icons.Filled.PlayArrow, Color(0xFF059669), Modifier.weight(1f))
            FeatureCard("用药记录", Icons.Outlined.Medication, Color(0xFF6366F1), Modifier.weight(1f))
            FeatureCard("导出报告", Icons.Outlined.PictureAsPdf, Color(0xFFF97316), Modifier.weight(1f))
        }
    }
}

@Composable
fun MetricCard(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(modifier, shape = RoundedCornerShape(AppConfig.UI.cornerRadiusMedium.dp)) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = color)
            Text(label, fontSize = 12.sp, color = Color.Gray)
        }
    }
}

@Composable
fun FeatureCard(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, modifier: Modifier = Modifier) {
    Card(modifier, shape = RoundedCornerShape(AppConfig.UI.cornerRadiusMedium.dp)) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = color, modifier = Modifier.size(32.dp))
            Spacer(Modifier.height(8.dp))
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.W500)
        }
    }
}

data class Summary(
    val todayFog: Int,
    val todayTremor: Int,
    val todayBrady: Int,
    val lastSessionDuration: String,
    val weeklyTrend: Int
)
