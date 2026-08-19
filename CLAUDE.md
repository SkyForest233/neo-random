# CLAUDE.md — NEO RNG Android 项目工作指引

> 本文件是所有 AI 助手（Claude Code / 其他 Agent）在本仓库工作时的必读入口。

## 项目一句话

网页版 NEO RNG（Neo-Brutalism 随机数工具）的 **100% 原生 Android 移植**：Jetpack Compose、无 WebView、无 Material 依赖。源网页：`neo random.html`（功能与视觉基准）。

## 标准文件路径（开始工作前必读）

| 文件 | 内容 | 何时读 |
|---|---|---|
| `docs/REQUIREMENTS.md` | 开发需求：功能清单（网页版 1:1 + App 增强） | 评估任何功能改动时 |
| `docs/TECHNOLOGY.md` | 技术规范：栈版本、模块结构、关键机制、构建签名 | 改架构/引擎/构建时 |
| `docs/DESIGN.md` | 设计规范：令牌色表、几何、字体、交互反馈、**动画红线** | 改任何 UI/动画时（红线必读） |
| `docs/WORKFLOW.md` | 执行步骤：开发循环、发布流程、排查手段、会话收尾 | **每次会话开始** |
| `devlog/` | 开发日志：按日归档 | 会话开始看最近一篇了解上下文，**结束前写当日日志** |

## 工作说明

1. **分支纪律**：只在 `arena/01a0145a-neo-random` 分支工作，禁止切分支/推 main
2. **编译验证**：本地无 Android SDK——每次 push 后等 GitHub Actions 结果（WORKFLOW.md 有读取命令），CI 绿了才算改完
3. **设计红线**（详见 docs/DESIGN.md §5，违反必产生视觉 bug）：
   - 带边框图层禁止 alpha/位移动画（边框会"由细变粗"）
   - 面板显隐用 AnimatedVisibility 手风琴，不做双面板互换
   - 布局定高内容驱动，禁用 fillMaxSize/matchParentSize/纵向 weight
4. **自动记录**：每次会话结束前，在 `devlog/` 更新（或新建）当日日志：
   - ✅ 完成事项（具体到文件/模块）
   - 📋 待办事项（优先级 P0-P3）
   - 🐛 已知问题/经验教训（防止同类回归）
   并随代码一起 commit
5. **用户验证**：所有改动需给出 Actions 运行链接供用户下载装机测试；视觉/动效问题以用户真机反馈为准
6. **发布**：版本发布走 tag 流程（WORKFLOW.md §2），签名凭据在 GitHub Secrets，永不明文入库

## 常用命令速查

```bash
# 本地构建（有 SDK 环境）
./gradlew assembleDebug && ./gradlew assembleRelease

# CI 状态与错误（沙箱内）
gh run list --repo SkyForest233/neo-random --limit 3
gh run watch <RUN_ID> --repo SkyForest233/neo-random --exit-status
```
