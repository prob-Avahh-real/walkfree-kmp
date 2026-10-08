# walkfree-kmp

WalkFree KMP 步态监测可穿戴应用 — Kotlin Multiplatform + Compose Multiplatform 功能 - 可穿戴 IMU 传感器 BLE 实时采集 (6 轴加速度+陀螺仪) - 步态冻结 (FoG) / 静止性震颤 / 运动迟缓 三引擎检测 - Cooley-Tukey FFT 信号处理 (纯 Kotlin, 零依赖) - 个性化校准 (均值+sigma 自适应阈值) - 手机传感器备用 (GPS 跟踪 + 加速度计回退)

## 用途与 Target 判定

Kotlin Multiplatform / Compose 工程（已归档）

- 移动端：需要
- 智能硬件：需要

## 目标平台

| 平台 | 方式 | 产出 |
|------|------|------|
| 移动端 | KMP | APK / 既有 iOS |
| 手表 / 戒指 / 传感器 | 按原设计 | — |

## 技术栈信号

- 检测到：ios, kmp

## 命令

```bash
./gradlew :composeApp:assembleDebug
```

## 状态

已归档。默认只做必要修复与知识库同步，不新开大范围需求。

## 助手约束

- Loop：先定可验证 Done 与停止条件；每切片过验证环；失败回灌；详见 docs/LOOP_ENGINEERING.md
- Harness：遵循根 AGENTS「Harness Engineering」；Guide=本文件，Sensor=测试/CI；同类失误二次须沉淀规则或测试（棘轮）。详见 docs/HARNESS_ENGINEERING.md
- 遵循仓库根 `/Users/skat/AGENTS.md` 与本文件的用途判定
- 可选 Target 可搁置；不适用形态不要硬加
- 改动聚焦；公共逻辑抽离复用
