# NEO RNG 🎲

> Neo-Brutalism 风格的多功能随机数工具 —— 网页版的原生 Android 移植（Jetpack Compose）。
> 原版网页见 [`neo random.html`](neo%20random.html)。

## ✨ 功能一览（与网页版 1:1）

| 模块 | 功能 |
|---|---|
| 🪙 **Coin** | 自定义正/反面文字 · 3D 硬币翻转动画 · 1% 概率竖立（EDGE）触发红色警报与警笛音 |
| 🔢 **Generator** | MIN/MAX/COUNT · 绝对唯一(Unique)模式 · 结果逐个弹出 · 远程模式单次上限 200 |
| 🎡 **Wheel** | 选项加权（`霸王茶姬 x2` 语法）· 加权转盘 · 指针 tick 音效 · 中奖印章 · 抽中自动剔除 |
| 🧾 **Receipt 小票** | 历史记录（最多 30 条）· 翻面查看统计（正面/反面/EDGE + 转盘 TOP3 条形图）· 撕毁清空动画 · 系统分享 |

**全局特性**

- 🎨 3 套主题色系（⚡赛博酸性 / 📻复古印刷 / 🍵柔和薄荷）× 深浅模式，实时切换
- 🔮 四种随机源可切换：**RANDOM.ORG**（大气噪声）/ **CF DRAND**（联盟随机数 + SHA-256 派生）/ **LOCAL RNG**（本地）/ **HYBRID MIX**（三源并行逐位混合，任一远程源失败自动降级），超时自动降级并提示
- 🔊 WebAudio 合成音效完整移植（tick / ding / swoosh / tear / siren），支持全局静音
- 📳 触感震动 · 🎉 五彩纸屑 · Toast 通知 · 波点背景 + 噪点纹理
- 👆 左右滑动切换 Tab · 空格键快捷触发（外接键盘）
- 💾 所有输入与历史记录本地持久化（格式与网页版 localStorage 完全一致）

## 📱 构建

### 本地构建（Android Studio）

直接用 Android Studio 打开项目，Sync Gradle 后 Run 即可。

命令行：

```bash
./gradlew assembleDebug     # 调试包 app/build/outputs/apk/debug/
./gradlew assembleRelease   # 发布包（配置签名后自动签名）
```

要求：JDK 17+，Android SDK 34。

### GitHub Actions 构建（推荐）

仓库自带工作流 `.github/workflows/android-build.yml`：

- **每次 push / PR**：自动编译 debug APK 并上传为 Artifact（验证代码可编译）
- **推送 `v*` 标签**（如 `v1.0.0`）：构建签名 Release APK，自动创建 GitHub Release 并附上安装包（版本号取自 tag，versionCode 使用 CI 运行序号保证递增）

## 🔑 配置签名（一次性）

Release 构建使用 GitHub Secrets 注入签名。**未配置时自动回退 debug 签名**（也能安装，但每次构建签名不一致，覆盖安装需卸载重装）。

### 第 1 步：生成 keystore

```bash
keytool -genkeypair -v \
  -keystore neo-release.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias neo
```

> Windows 用户在 PowerShell 中执行 `keytool`（随 JDK 安装）。

### 第 2 步：转为 base64

**macOS / Linux：**

```bash
base64 -i neo-release.jks | tr -d '\n' > keystore.b64
```

**Windows PowerShell：**

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("neo-release.jks")) | Set-Content -NoNewline keystore.b64
```

### 第 3 步：添加 4 个 Secrets

仓库页面 → **Settings → Secrets and variables → Actions → New repository secret**：

| Secret 名 | 值 |
|---|---|
| `KEYSTORE_BASE64` | `keystore.b64` 文件的全部内容 |
| `KEYSTORE_PASSWORD` | 生成 keystore 时设置的 keystore 密码 |
| `KEY_ALIAS` | `neo`（或你自定义的别名） |
| `KEY_PASSWORD` | 生成 keystore 时设置的 key 密码 |

### 第 4 步：发布

```bash
git tag v1.0.0
git push origin v1.0.0
```

Actions 会自动构建 `NEO-RNG-v1.0.0.apk` 并发布到 GitHub Releases。版本号取自 tag（`v1.2.3` → `1.2.3`），versionCode 使用 CI 运行序号，保证递增。

## 🗂 项目结构

```
app/src/main/java/com/skyforest233/neorng/
├── MainActivity.kt      # 入口
├── Palette.kt           # 3套主题 × 深浅模式 + 字体
├── AppState.kt          # 全局状态 / 持久化 / 动作逻辑（对应网页版 JS）
├── Anim.kt              # 硬币 & 转盘物理引擎（renderLoop 移植）
├── Rng.kt               # 三种随机源 + 降级容错
├── SoundFx.kt           # WebAudio 合成音效移植
├── Haptics.kt           # 震动
└── ui/
    ├── MainScreen.kt    # 头部 / Tab 滑动 / 响应式双栏布局
    ├── Components.kt    # Neo-Brutalism 组件库
    ├── CoinModule.kt    # 🪙 硬币
    ├── RngModule.kt     # 🔢 数字生成器
    ├── WheelModule.kt   # 🎡 转盘
    ├── ReceiptModule.kt # 🧾 小票 + 统计
    └── Overlays.kt      # Toast / 纸屑 / 背景
```

## 📄 许可

- 字体：Space Grotesk & JetBrains Mono（SIL Open Font License）
- 应用代码：随仓库许可
