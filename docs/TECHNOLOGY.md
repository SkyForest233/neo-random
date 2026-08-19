# 技术规范（TECHNOLOGY）

## 1. 技术栈

| 层 | 选型 | 版本 |
|---|---|---|
| 语言 | Kotlin | 2.0.20 |
| UI | Jetpack Compose（无 Material 依赖，纯 foundation + 自建设计系统） | BOM 2024.06.00 |
| 构建 | AGP / Gradle | 8.9.1 / 8.11.1，JDK 17 |
| SDK | compile/target/min | 36 / 36 / 26 |
| 网络 | OkHttp（4s 超时） | 4.12.0 |
| 序列化 | org.json（内置） | — |
| 持久化 | SharedPreferences | — |
| CI/CD | GitHub Actions 单工作流 | `.github/workflows/android-build.yml` |

## 2. 模块结构

```
app/src/main/java/com/skyforest233/neorng/
├── MainActivity.kt      # 入口；崩溃报告分流；状态栏图标随主题；onDestroy 取消作用域
├── CrashReporter.kt     # 未捕获异常 → 文件 → 纯 View 报告页
├── AppState.kt          # 全局状态中枢（mutableStateOf）+ 持久化 + 模块动作 + Fx 实现
├── Anim.kt              # CoinAnim/WheelAnim 物理引擎（状态字段驱动 UI）
├── Rng.kt               # 四种随机源 + raw 抓取层 + 降级容错
├── SoundFx.kt           # PCM 离线合成 + AudioTrack(MODE_STATIC)
├── Haptics.kt           # Vibrator（普通/低振幅两档）
├── Palette.kt           # NeoPalette 三主题×深浅 + 字体族
└── ui/
    ├── MainScreen.kt    # 头部/Tab(卡内滑动)/物理帧循环/背景层
    ├── Components.kt    # NeoSurface/NeoTextField/ActionButton 等（内容驱动尺寸）
    ├── CoinModule.kt    # Canvas 圆柱投影硬币
    ├── RngModule.kt     # 生成器 + 结果药丸
    ├── WheelModule.kt   # 加权转盘 + 印章
    ├── ReceiptModule.kt # 单面板小票（手风琴折叠 + 翻面统计 + 撕毁）
    └── Overlays.kt      # Toast/纸屑/波点/噪点
```

## 3. 关键机制

### 随机源引擎（Rng.kt）
- `fetch(min,max,count,unique,mode,onNotice): List<Int>?`——null=区间过小错误
- raw 层：`randomOrgRaw` / `drandRaw`（失败抛异常，无降级）；fetchXxx 包装降级逻辑
- DRAND：`SHA-256(randomness + "_" + nonce)` 前 4 字节大端 → UInt32/2³² → 区间映射；同轮 nonce+100 防碰撞
- HYBRID：三源 `async` 并行，逐位求和取模 `(Σ(vᵢ-min)) mod range + min`

### 动画物理引擎（Anim.kt + MainScreen 帧循环）
- 60fps 标准帧累积（`withFrameNanos`，dt/16.667ms，单帧最多补 5 步）
- 硬币：加速(≤40°/帧) → armedStop(必须达峰值) → 停止(min(当前速度, diff×0.04) 只减不增) → 弹簧收尾(阈值 1.5°)
- 转盘同理（≤25°/帧，diff×0.025）；tick 55ms 节流
- 状态字段必须 `mutableStateOf`（UI 才会重绘）；纯物理量(speed 等)可为普通字段

### 音效（SoundFx.kt）
- 加载时离线合成 PCM 44.1kHz 单声道 16bit；AudioTrack MODE_STATIC，stop→reloadStaticData→play 重放
- 公式与 WebAudio 逐参数一致（见代码注释）

### 持久化（AppState.kt）
- 键：`neoTheme/neoDarkMode/neoMute/neoRngMode/neoRngData(+neoReceiptCollapsed)`
- `neoRngData` JSON：coinHead/coinTail/rngMin/rngMax/rngCount/rngUnique/wheelInput/wheelAutoRemove/history[](旧的在前)
- 输入保存走 `scheduleSave()` 300ms 防抖；动作类（历史变更等）直接 `saveData()`

## 4. 构建与签名

- 签名从环境变量注入：`KEYSTORE_FILE/KEYSTORE_PASSWORD/KEY_ALIAS/KEY_PASSWORD`（CI 从 Secrets）
- 未配置时 release 回退 debug 签名（保证总有可装 APK）
- 版本：`-PVERSION_NAME=<tag去v>` / `-PVERSION_CODE=${{ github.run_number }}`
- gradlew 含 CI 诊断补丁：编译错误转 `::error::` annotations（GITHUB_ACTIONS 环境下生效）
