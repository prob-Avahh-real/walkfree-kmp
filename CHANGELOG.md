# WalkFree KMP — Engineering Hardening v2

**日期:** 2026-06-07
**变更:** v2 workflow 加固 + Agent 化

## 安全修复

- 🔴 AndroidManifest 补全 BLE + Location + BodySensors + ActivityRecognition 权限
- 🔴 `ISignalProcessor.SignalWindow` 无锁 → Mutex.withLock 保护 BLE 回调 vs 检测引擎竞争
- 🟡 CalibrationEngine object → class (可模拟)
- 🟡 InMemorySessionStore 无锁 → Mutex 保护 save/loadAll/delete
- 🟡 DetectionEngineImpl processFrame/reset 竞争 → Mutex atomic 化

## 抽象重构

- ✅ BleAdapter expect 独立到 `platform/BleAdapter.kt`
- ✅ ScanResult 移至 platform 包
- ✅ ISignalWindow 方法改为 suspend (支持 Mutex)
- ✅ IDetectionEngine.processFrame() 改为 suspend
- ✅ kotlinx-coroutines-test 依赖补全

## Agent 化

- ✅ WalkFreeAgent — 自主决策引擎
  - 静止 > 5 min → 自动开始监测
  - 连续 3 次严重事件 → ALERT 模式 + 主动告警
  - 30 min 无事件 → PASSIVE 省电模式
  - MonitorMode 状态机 (IDLE/ACTIVE/PASSIVE/ALERT/CALIBRATING)
  - Mutex 保护内部状态

## 测试

- ✅ WalkFreeAgentTest — 7 用例覆盖全状态转换

## CI/CD

- ✅ 串行依赖: test → android-build → ios-build
- ✅ 移除不稳定的 xcodebuild archive 步骤
