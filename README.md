# WalkFree KMP

步态监测可穿戴应用 — Kotlin Multiplatform + Compose Multiplatform

## 功能

- 可穿戴 IMU 传感器 BLE 实时采集 (6 轴加速度+陀螺仪)
- 步态冻结 (FoG) / 静止性震颤 / 运动迟缓 三引擎检测
- Cooley-Tukey FFT 信号处理 (纯 Kotlin, 零依赖)
- 个性化校准 (均值+sigma 自适应阈值)
- 手机传感器备用 (GPS 跟踪 + 加速度计回退)
- ML 推理接口预留 (ONNX Runtime 接入点)

## 架构

```
walkfree-kmp/
├── shared/                          # Kotlin Multiplatform 共享模块
│   └── src/
│       ├── commonMain/              # 跨平台业务逻辑
│       │   └── com/walkfree/
│       │       ├── AppConfig.kt            # 全局配置 (单点修改)
│       │       ├── model/Entities.kt       # 数据模型
│       │       ├── engine/
│       │       │   ├── Contracts.kt        # ISignalProcessor / IDetectionEngine / IMlInferenceEngine
│       │       │   ├── SignalProcessorImpl.kt  # Cooley-Tukey FFT + 特征提取
│       │       │   └── DetectionEngineImpl.kt  # FoG/震颤/迟缓三引擎 + 校准
│       │       └── data/
│       │           ├── Contracts.kt        # IMonitoringRepository / ISessionStore / expect 平台桥接
│       │           ├── MonitoringRepositoryImpl.kt  # 传感器流 + BLE 解析 + 持久化
│       │           └── HttpClientImpl.kt   # Ktor HTTP 客户端
│       ├── androidMain/              # BLE (Kable) + 传感器 (SensorManager) + GPS (FusedLocation)
│       └── iosMain/                  # BLE (Kable) + 传感器 (CoreMotion) + GPS (CoreLocation)
├── composeApp/                       # Compose Multiplatform UI
│   └── src/commonMain/
│       └── com/walkfree/ui/screens/
│           ├── WalkFreeApp.kt        # 主导航 (4 tab)
│           ├── HomeScreen.kt         # 仪表盘
│           └── MonitoringScreen.kt   # 实时监测
├── iosApp/                           # SwiftUI 原生壳
├── .github/workflows/ci.yml          # CI/CD: Android build + iOS framework + test
├── gradle/libs.versions.toml         # 依赖版本目录
└── build.gradle.kts                  # Gradle 多平台构建
```

## 抽象层

| 层 | 接口 | 默认实现 |
|----|------|---------|
| 平台桥接 | `expect BleAdapter / PhoneSensors / LocationProvider` | Kable + SensorManager/CoreMotion + FusedLocation/CoreLocation |
| 信号处理 | `ISignalProcessor` | `SignalProcessorImpl` (Cooley-Tukey FFT) |
| 检测引擎 | `IDetectionEngine` | `DetectionEngineImpl` (策略模式, 可选注入 ML) |
| ML 推理 | `IMlInferenceEngine` | 空实现 (ONNX 预留) |
| 数据仓库 | `IMonitoringRepository` | `MonitoringRepositoryImpl` (构造注入) |
| 会话持久化 | `ISessionStore` | `InMemorySessionStore` (可换 SQLDelight) |
| HTTP 客户端 | — | `HttpClientImpl` (Ktor) |
| 配置 | — | `AppConfig` (单点修改) |

## 构建

```bash
# Android
./gradlew :composeApp:assembleRelease

# iOS framework
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64

# 测试
./gradlew :shared:allTests
```

## 环境要求

- JDK 17
- Kotlin 2.1.20
- Compose Multiplatform 1.7.3
- Gradle 8.10
- Android SDK 35 (minSdk 26)
