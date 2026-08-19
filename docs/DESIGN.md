# 设计规范（DESIGN）

> 还原基准：网页版 `neo random.html` 的 Neo-Brutalism 设计语言。1 CSS px ≈ 1 dp。

## 1. 设计令牌（NeoPalette，3 主题 × 深浅）

| 变量 | cyber浅 | cyber深 | retro浅 | retro深 | pastel浅 | pastel深 |
|---|---|---|---|---|---|---|
| bg | F4F5F5 | 121212 | EFE9E1 | 2A2421 | F0F4F8 | 1A202C |
| card | FFFFFF | 1C1C1C | FAFAFA | 352E2B | FFFFFF | 2D3748 |
| textMain | 141414 | E4E4E4 | 2C2A29 | EFE9E1 | 2D3748 | F0F4F8 |
| border | 141414 | 3A3A3A | 2C2A29 | 5A4E49 | 2D3748 | 4A5568 |
| accent | D4FF00 | D4FF00 | FF7B54 | FF7B54 | 9DECF9 | 9DECF9 |
| accentSub | B28DFF | B28DFF | 4ECDC4 | 4ECDC4 | FBB6CE | FBB6CE |
| btnText | 141414 | 141414 | FAFAFA | 2A2421 | 2D3748 | 1A202C |
| grayLight | E0E2E5 | 2A2A2A | DFD8CE | 3D3531 | E2E8F0 | 2A3441 |
| textMuted | 666666 | 888888 | 7A7571 | 9E8F87 | 718096 | A0AEC0 |
| shadow | 141414 | 000000 | 2C2A29 | 000000 | 2D3748 | 000000 |

## 2. 几何与描边

| 令牌 | 值 |
|---|---|
| 边框宽度 | 2.5dp（小元素 2dp，聚焦输入框 3dp，结果药丸内描边 1dp） |
| 圆角 | sm=12dp（输入/小票） · lg=28dp（卡片） · pill=100dp（按钮/Tab/徽章） |
| 硬阴影 | 偏移 (3,3)，颜色=shadow；按钮按下 (3,3) 位移+阴影消失；Tab 高亮阴影 (2,2)；印章 (6,6) |
| 背景 | 波点：gray-light 1.5dp 圆点 × 24dp 网格；噪点：128px 灰噪声平铺，浅色 multiply 5% / 深色 overlay 8% |

## 3. 字体

| 用途 | 字体 | 规格 |
|---|---|---|
| 全局主字体 | Space Grotesk（500/700） | 标题 22sp/900 · 卡片标签 13sp/900 · 正文 14-16sp/700 |
| 等宽（小票/数值） | JetBrains Mono（400/700） | 小票条目 13sp · 记录值 13-14sp/700 · 统计数值 12sp |
| 回弹曲线 | cubic-bezier(0.175, 0.885, 0.32, 1.275) | `NeoOvershootEasing`（印章/Toast/药丸） |

## 4. 交互反馈

- 按下态：位移(3,3) + 阴影消失（:active 还原）
- 输入聚焦：bg→card、边框 3dp、阴影(3,3)、整体上移 2dp
- 触感：tick 12ms@64 振幅 · ding [0,30,50,30] · 警报 [100×5] · 撕毁 [50,50,50] · 起转 15-20ms
- Toast：顶部滑入胶囊（文字=bg 色，底=textMain），3s 自动收回

## 5. ⛔ 动画红线（重要经验，不可违反）

1. **带边框的图层禁止透明度（alpha）动画**——半透明阶段边框抗锯齿不完全，结束帧会"由细变粗"
2. **带边框的图层避免位移动画**——亚像素偏移同样造成边框渲染抖动；需要滑动时让**外壳静止、内容在被裁剪的容器内滑动**（参考 MainScreen Tab 卡内滑动实现）
3. **面板显隐一律 `AnimatedVisibility`（expand/shrinkVertically）**，不用双面板互换（AnimatedContent 换整面板在滚动容器中会跳变）
4. 布局尺寸**内容驱动**：不使用 `fillMaxSize/matchParentSize/纵向 weight` 定高（无限高度滚动容器中会塌缩为 0）——参考 `NeoSurface` 的 drawBehind + padding 预留阴影实现
5. 动画时长基准：入场/展开 300-350ms（FastOutSlowInEasing）；退出 160-250ms（FastOutLinearInEasing）；回弹类 500ms（NeoOvershootEasing）

## 6. 组件清单（Components.kt）

| 组件 | 用途 | 关键参数 |
|---|---|---|
| NeoSurface | 所有卡片/按钮/徽章外壳 | radius/shadow*/pressedState/contentAlignment |
| NeoTextField | 输入框 | numeric/onFocusChanged（聚焦需上报 app.onFieldFocus） |
| ActionButton | 主行动按钮 | enabled 灰化态 |
| IconCircleButton | 圆形图标按钮 | size 默认 36dp（头部）/30dp（面板内） |
| BrutalCheckbox / SmallBadge / DashedDivider | 勾选框/徽章/虚线 | — |
