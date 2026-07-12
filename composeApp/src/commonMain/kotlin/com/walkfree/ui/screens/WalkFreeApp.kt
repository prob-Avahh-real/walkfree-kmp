package com.walkfree.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.walkfree.AppConfig

enum class WalkFreeTab(val label: String) { HOME, MONITOR, HISTORY, SETTINGS }

@Composable
fun WalkFreeApp() {
    var selectedTab by remember { mutableStateOf(WalkFreeTab.HOME) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == WalkFreeTab.HOME,
                    onClick = { selectedTab = WalkFreeTab.HOME },
                    icon = { Icon(Icons.Filled.Dashboard, "首页") },
                    label = { Text("首页") }
                )
                NavigationBarItem(
                    selected = selectedTab == WalkFreeTab.MONITOR,
                    onClick = { selectedTab = WalkFreeTab.MONITOR },
                    icon = { Icon(Icons.Filled.MonitorHeart, "监测") },
                    label = { Text("监测") }
                )
                NavigationBarItem(
                    selected = selectedTab == WalkFreeTab.HISTORY,
                    onClick = { selectedTab = WalkFreeTab.HISTORY },
                    icon = { Icon(Icons.Outlined.History, "历史") },
                    label = { Text("历史") }
                )
                NavigationBarItem(
                    selected = selectedTab == WalkFreeTab.SETTINGS,
                    onClick = { selectedTab = WalkFreeTab.SETTINGS },
                    icon = { Icon(Icons.Outlined.Settings, "设置") },
                    label = { Text("设置") }
                )
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when (selectedTab) {
                WalkFreeTab.HOME -> HomeScreen()
                WalkFreeTab.MONITOR -> MonitoringScreen()
                WalkFreeTab.HISTORY -> PlaceholderScreen("历史记录", AppConfig.UI.placeholderHistory)
                WalkFreeTab.SETTINGS -> PlaceholderScreen("设置", AppConfig.UI.placeholderSettings)
            }
        }
    }
}
