# CLAUDE.md — WalkFree KMP

步态监测 KMP 项目。BLE 可穿戴 IMU → FFT 信号处理 → FoG/震颤/运动迟缓检测。

## 命令

```bash
./gradlew :composeApp:assembleRelease    # Android 构建
./gradlew :shared:allTests               # 运行所有测试
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64  # iOS framework
```

## 架构原则

1. `shared/` 放所有业务逻辑 — 不要往 `composeApp/` 加 domain 代码
2. 接口在 `Contracts.kt`，实现在 `*Impl.kt`
3. 配置在 `AppConfig.kt` — 改阈值不用搜代码
4. 平台桥接用 `expect/actual` — 不要往 commonMain 引平台类型
5. 检测引擎用策略模式 — 构造注入 `ISignalProcessor` + 可选 `IMlInferenceEngine`

## 文件组织

- `shared/src/commonMain/com/walkfree/model/` — 数据类 (SensorReading, DetectionEvent, MonitoringSession)
- `shared/src/commonMain/com/walkfree/engine/` — 信号处理 + 检测引擎
- `shared/src/commonMain/com/walkfree/data/` — 仓库 + HTTP 客户端 + expect 桥接
- `shared/src/androidMain/.../platform/` — Android actual 实现
- `shared/src/iosMain/.../platform/` — iOS actual 实现
- `composeApp/src/commonMain/.../ui/screens/` — Compose UI

## 关键常量 (AppConfig)

- `sampleRateHz = 50.0` — 采集采样率
- `windowSize = 300` — 滑动窗口 (6 秒)
- `FogDetection.defaultThreshold = 1.8` — 冻结指数阈值
- `TremorDetection.peakLow..peakHigh = 4.0..6.0` — 帕金森震颤频段
- `BradyDetection.amplitudeThreshold = 0.15` — 运动迟缓振幅阈值
- `consecutiveConfirmations = 3` — 连续确认帧数
- `debounceMs = 3000` — 事件冷却
