package com.walkfree

/// walkfree 全局配置 — 单点修改，全项目同步
///
/// 包含: 应用元数据、BLE 协议、检测阈值、UI 常量、Supabase 配置
object AppConfig {
    // ═══════════════════════════════════════════════════
    // 应用元数据
    // ═══════════════════════════════════════════════════
    const val appName = "WalkFree"
    const val appNameCn = "步态监测"
    const val bundleIdAndroid = "com.walkfree.app"
    const val bundleIdIos = "com.walkfree.app"
    const val versionName = "1.0.0"

    // ═══════════════════════════════════════════════════
    // BLE 协议 (可穿戴设备 12 字节帧格式)
    // ═══════════════════════════════════════════════════

    /// 每帧字节数: accelX(2) + accelY(2) + accelZ(2) + gyroX(2) + gyroY(2) + gyroZ(2)
    const val bleFrameBytes = 12

    /// 原始数据缩放: millig → m/s² 需 ×9.81/1000, millirad/s → rad/s 需 /1000
    const val bleAccelScale = 9.81 / 1000.0
    const val bleGyroScale = 1.0 / 1000.0

    /// BLE 数据解析 — 字段在帧中的字节偏移 (int16 LE)
    object BleOffsets {
        const val accelX = 0
        const val accelY = 2
        const val accelZ = 4
        const val gyroX = 6
        const val gyroY = 8
        const val gyroZ = 10
    }

    // ═══════════════════════════════════════════════════
    // 信号处理引擎
    // ═══════════════════════════════════════════════════

    /// 采样率 (Hz)
    const val sampleRateHz = 50.0
    const val downsampleIntervalMs = 50L      // 20Hz → 50ms

    /// 滑动窗口大小 (6 秒 @ 50Hz = 300 个采样)
    const val windowSize = 300

    /// FFT 填充目标: 2 的幂
    const val fftMinSize = 256

    // ═══════════════════════════════════════════════════
    // 步态冻结 (FoG) 检测
    // ═══════════════════════════════════════════════════
    object FogDetection {
        const val locomotorBandLow = 0.5
        const val locomotorBandHigh = 3.0
        const val freezeBandLow = 3.0
        const val freezeBandHigh = 8.0
        const val defaultThreshold = 1.8          // 冻结指数阈值 (FI > 1.8 → FoG)
        const val minConfidence = 0.0
        const val maxConfidence = 1.0
    }

    // ═══════════════════════════════════════════════════
    // 震颤检测
    // ═══════════════════════════════════════════════════
    object TremorDetection {
        const val freqLow = 3.0                  // 3-8Hz 频段
        const val freqHigh = 8.0
        const val peakLow = 4.0                  // 帕金森震颤峰值 4-6Hz
        const val peakHigh = 6.0
        const val powerThreshold = 0.3            // 震颤功率阈值
        const val rmsThreshold = 0.05             // 最低 RMS
    }

    // ═══════════════════════════════════════════════════
    // 运动迟缓检测
    // ═══════════════════════════════════════════════════
    object BradyDetection {
        const val amplitudeThreshold = 0.15       // 振幅阈值
        const val cadenceThreshold = 40.0          // 步/分钟
        const val armSwingGyroRmsThreshold = 0.3  // 手臂摆动陀螺仪 RMS
        const val minAmplitude = 0.01             // 排除完全静止
    }

    // ═══════════════════════════════════════════════════
    // 检测引擎通用
    // ═══════════════════════════════════════════════════
    const val consecutiveConfirmations = 3         // 连续确认帧数
    const val debounceMs = 3000L                   // 事件冷却 (3 秒)

    // ═══════════════════════════════════════════════════
    // 校准
    // ═══════════════════════════════════════════════════
    object Calibration {
        const val sampleCount = 300                // 采集 300 个采样 (~6 秒)
        const val batchSize = 100                  // 每 100 个采样算一组特征
        const val fogSigma = 2.5                   // FoG 阈值: 均值 + 2.5σ
        const val tremorSigma = 3.0                // 震颤: 均值 + 3σ
        const val bradyAmplitudeFactor = 0.5       // 运动迟缓振幅: 均值 × 0.5
        const val bradyCadenceFactor = 0.7         // 运动迟缓步频: 均值 × 0.7
    }

    // ═══════════════════════════════════════════════════
    // 步频估计 (FFT 分析)
    // ═══════════════════════════════════════════════════
    object CadenceEstimation {
        const val bandLow = 0.5                   // 0.5-3Hz = 30-180 步/分钟
        const val bandHigh = 3.0
    }

    // ═══════════════════════════════════════════════════
    // 反馈 (触觉 + 音频)
    // ═══════════════════════════════════════════════════
    object Feedback {
        const val cooldownMs = 5000L               // 同类事件反馈冷却 (5 秒)
        const val vibrationMs = 200L               // 振动时长
    }

    // ═══════════════════════════════════════════════════
    // Supabase / 网络
    // ═══════════════════════════════════════════════════
    object Network {
        const val defaultBaseUrl = ""              // 空 = 离线模式
        const val connectTimeoutMs = 10_000L
        const val requestTimeoutMs = 15_000L
        const val socketTimeoutMs = 15_000L
        const val maxRetries = 3
        const val healthEndpoint = "/health"
    }

    // ═══════════════════════════════════════════════════
    // UI
    // ═══════════════════════════════════════════════════
    object UI {
        const val themeStyle = "MaterialTheme"
        const val cornerRadiusLarge = 16
        const val cornerRadiusMedium = 12
        const val cornerRadiusSmall = 8
        const val statusBleConnected = "已连接"
        const val statusBleDisconnected = "未连接 — 点击开始扫描"
        const val statusBleScanning = "扫描中..."
        const val statusMonitoring = "监测中"
        const val statusReady = "就绪"
        const val statusStopped = "已停止"
        const val placeholderHistory = "暂无监测记录"
        const val placeholderSettings = "校准 · 阈值 · Supabase"
    }
}
