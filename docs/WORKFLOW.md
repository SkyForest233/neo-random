# 执行步骤（WORKFLOW）

> 本项目的标准开发/发布/排查流程。任何开发会话开始前先通读本文 + docs/DESIGN.md。

## 1. 开发循环（每次改动必走）

```
改代码 → git add/commit → git push origin arena/01a0145a-neo-random
       → GitHub Actions 自动构建（每次 push 必出 debug APK 验证编译）
       → 读 CI 结果 → 修复直到 success → 通知用户下载装机测试
       → 用户反馈 → 下一轮
```

规则：
- **一切工作在 `arena/01a0145a-neo-random` 分支**，禁止动 main
- **沙箱无 Android SDK，本地不可编译**——CI 是唯一编译验证手段，推送后必须等待结果
- 用户装机测试是唯一运行时验证——改动必须让用户可下载（Actions → Artifacts → neo-rng-apk）
- 提交信息用中文，说清"改了什么+为什么"

### CI 结果读取（沙箱网络受限时）

```bash
RUNID=$(gh run list --repo SkyForest233/neo-random --limit 1 --json databaseId --jq '.[0].databaseId')
gh run watch $RUNID --repo SkyForest233/neo-random --exit-status
JOBID=$(gh api repos/SkyForest233/neo-random/actions/runs/$RUNID/jobs --jq '.jobs[0].id')
gh api repos/SkyForest233/neo-random/check-runs/$JOBID/annotations --jq '.[] | .message' | grep "^e: "
# 编译错误已由 gradlew 诊断补丁转为 ::error:: annotations，可直接 API 读取
```

### Git 注意

- 沙箱快照重置会导致本地历史分叉：push 报 non-fast-forward 时执行
  `git fetch origin arena/01a0145a-neo-random && git reset --soft FETCH_HEAD` 后重新 commit/push
- workflow 文件无权限推送（GitHub App 限制）——`.github/workflows/` 变更需用户网页操作

## 2. 发布流程（Release APK）

1. 一次性配置 4 个 Secrets（见 README「🔑 配置签名」）：KEYSTORE_BASE64 / KEYSTORE_PASSWORD / KEY_ALIAS / KEY_PASSWORD
2. 合并 PR #1 到 main
3. 打标签：`git tag v1.2.3 && git push origin v1.2.3`
4. Actions 自动：签名构建 → `NEO-RNG-v1.2.3.apk` → 发布 GitHub Release（版本号取 tag，versionCode=run_number）

> 也可直接在网页「Draft a new release」发布：触发的是 `release` 事件（而非 tag push），工作流同样会构建并上传 APK 到该 Release。

## 3. 排查流程

| 症状 | 工具 |
|---|---|
| App 崩溃 | 崩溃报告页自动显示（CrashReporter），让用户点"复制报告"回传 |
| 布局异常（空白/塌缩） | 代码里已有 onGloballyPositioned 诊断 Toast（RngModule）；参照 docs/DESIGN.md 第 5 节红线自查 |
| 编译失败 | CI annotations（见上） |
| CI 日志看不了 | Actions 日志 CDN 被沙箱拦截——用 annotations API |

## 4. 会话收尾清单（每次必做）

1. ✅ CI 构建结果为 success
2. ✅ 更新 `devlog/YYYY-MM-DD.md`（完成/待办/教训）
3. ✅ commit + push
4. ✅ 给用户下载链接（run 页面）+ 本轮改动摘要
