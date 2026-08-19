@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
    androidx.compose.animation.ExperimentalAnimationApi::class
)

package com.skyforest233.neorng.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.skyforest233.neorng.AnimState
import com.skyforest233.neorng.AppState
import com.skyforest233.neorng.NeoPalette
import com.skyforest233.neorng.NeoSans
import com.skyforest233.neorng.RngEngine
import com.skyforest233.neorng.paletteFor
import kotlin.math.abs

/**
 * 应用主界面：头部（标题 + 主题/深浅/静音/随机源切换）、
 * Tab 药丸栏 + 滑动切换（与网页版一致的 60px 阈值被动手势）、小票侧栏；
 * 宽屏(≥900dp)时双栏布局，与网页版媒体查询一致。
 */
@Composable
fun MainScreen(app: AppState) {
    val palette = paletteFor(app.theme, app.dark)
    val noiseBitmap = rememberNoiseBitmap()
    var currentTab by rememberSaveable { mutableIntStateOf(0) }
    val switchTab: (Int) -> Unit = { target ->
        if (target != currentTab && target in 0..2) {
            currentTab = target
            app.vibrate(longArrayOf(0, 10)) // 网页版 switchTab 的 triggerVibrate(10)
        }
    }

    // 红色警报（EDGE 竖立）：背景红黑交替闪烁 3 秒
    val emergencyBg by animateColorAsState(
        targetValue = if (app.emergency) Color(0xFFFF0000) else Color(0xFF220000),
        animationSpec = if (app.emergency) {
            infiniteRepeatable(tween(800), RepeatMode.Reverse)
        } else {
            tween(0)
        },
        label = "emergency"
    )
    val baseBg = if (app.emergency) emergencyBg else palette.bg

    // 物理引擎帧循环（renderLoop 移植）：60fps 标准帧累积，空闲时零开销
    LaunchedEffect(Unit) {
        var lastNanos = 0L
        var acc = 0f
        while (true) {
            withFrameNanos { now ->
                if (lastNanos != 0L) acc += (now - lastNanos) / 1_000_000f / 16.6667f
                lastNanos = now
            }
            var steps = acc.toInt()
            acc -= steps
            if (steps > 5) steps = 5
            if (steps > 0) {
                val coinActive = app.coin.isActive
                val wheelActive = app.wheel.state != AnimState.IDLE
                if (coinActive || wheelActive) {
                    val items = if (wheelActive) app.wheel.spinningItems else emptyList()
                    repeat(steps) {
                        if (coinActive) app.coin.stepOnce()
                        if (wheelActive) app.wheel.stepOnce(items)
                    }
                }
            }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(baseBg)
            .applyNoiseOverlay(noiseBitmap, palette)
            .onPreviewKeyEvent { event ->
                // 网页版空格快捷键：触发当前 Tab 的动作（输入框聚焦时除外）
                if (event.type == KeyEventType.KeyDown &&
                    event.key == androidx.compose.ui.input.key.Key.Spacebar &&
                    !app.anyInputFocused
                ) {
                    when (currentTab) {
                        0 -> app.executeCoinFlip()
                        1 -> app.executeRng()
                        2 -> app.executeWheel()
                    }
                    true
                } else false
            }
    ) {
        // 波点背景
        Box(
            Modifier
                .fillMaxSize()
                .drawBehind { drawDotGrid(palette) }
        )

        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
        ) {
            Header(app, palette)

            Box(Modifier.fillMaxSize()) {
                val wide = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp >= 900
                if (wide) {
                    Row(
                        Modifier
                            .fillMaxSize()
                            .widthIn(max = 1100.dp)
                            .align(Alignment.TopCenter)
                            .padding(horizontal = 20.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        Box(Modifier.weight(1.5f)) {
                            MainContent(app, palette, currentTab, switchTab)
                        }
                        Box(Modifier.weight(1f)) {
                            Sidebar(app, palette)
                        }
                    }
                } else {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp)
                    ) {
                        MainContent(app, palette, currentTab, switchTab)
                        Spacer(Modifier.height(24.dp))
                        Sidebar(app, palette)
                        Spacer(Modifier.height(40.dp))
                    }
                }
            }
        }

        ToastHost(visible = app.toastMsg != null, message = app.toastMsg, palette = palette)
        ConfettiHost(trigger = app.confettiTrigger, palette = palette)
    }
}

// ==================== 头部 ====================

@Composable
private fun Header(app: AppState, palette: NeoPalette) {
    // 单行完整布局：36dp 图标 + 完整随机源名称；标题弹性收缩（极窄屏字号自适应，徽章永不截断）
    val compact = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp < 400
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = if (compact) 12.dp else 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.foundation.text.BasicText(
            text = "NEO_RNG",
            style = androidx.compose.ui.text.TextStyle(
                color = palette.textMain,
                fontSize = if (compact) 20.sp else 22.sp,
                fontWeight = FontWeight.W900,
                letterSpacing = (-0.5).sp,
                fontFamily = NeoSans
            ),
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconCircleButton(emoji = "🎨", palette = palette, size = 36.dp, contentDescription = "切换主题") { app.cycleTheme() }
            IconCircleButton(emoji = "🌓", palette = palette, size = 36.dp, contentDescription = "切换深浅模式") { app.toggleDarkMode() }
            IconCircleButton(emoji = if (app.muted) "🔕" else "🔔", palette = palette, size = 36.dp, contentDescription = "切换静音") { app.toggleMute() }
            ModeBadge(app, palette)
        }
    }
}

/** 随机源三态切换徽章（#modeBtn + 状态点颜色），支持整宽布局 */
@Composable
private fun ModeBadge(app: AppState, palette: NeoPalette, modifier: Modifier = Modifier) {
    val dotColor = when (app.rngMode) {
        "randomorg" -> palette.accent
        "drand" -> palette.accentSub
        "hybrid" -> palette.textMain
        else -> palette.grayLight
    }
    NeoSurface(
        palette = palette,
        onClick = { app.toggleMode() },
        radius = 100.dp,
        modifier = modifier.semantics { contentDescription = "随机来源：" + (RngEngine.MODE_LABELS[app.rngMode] ?: "") + "，点按切换" },
        borderWidth = 2.5.dp,
        shadowDx = 2.dp,
        shadowDy = 2.dp,
        pressedTranslate = true,
        contentAlignment = Alignment.Center
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Box(
                Modifier
                    .size(10.dp)
                    .background(dotColor, CircleShape)
                    .border(2.dp, palette.border, CircleShape)
            )
            NeoText(
                RngEngine.MODE_LABELS[app.rngMode] ?: "LOCAL RNG",
                color = palette.textMain,
                fontSize = if (androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp < 400) 11.sp else 12.sp,
                fontWeight = FontWeight.W700
            )
        }
    }
}

// ==================== 主体内容 ====================

@Composable
private fun MainContent(
    app: AppState,
    palette: NeoPalette,
    currentTab: Int,
    onSwitchTab: (Int) -> Unit
) {
    val tabState by rememberUpdatedState(currentTab)

    Column(
        Modifier
            .fillMaxWidth()
            .swipeTabDetector { delta -> onSwitchTab(tabState + delta) }
    ) {
        // Tab 药丸栏（.tabs-container）
        NeoSurface(
            palette = palette,
            radius = 100.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                Modifier.padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TabButton("🪙 Coin", 0, currentTab, palette, Modifier.weight(1f)) { onSwitchTab(0) }
                TabButton("🔢 Number", 1, currentTab, palette, Modifier.weight(1f)) { onSwitchTab(1) }
                TabButton("🎡 Wheel", 2, currentTab, palette, Modifier.weight(1f)) { onSwitchTab(2) }
            }
        }

        Spacer(Modifier.height(20.dp))

        // 内容卡片：状态切换（与网页版 switchTab 的 display 切换一致）
        when (currentTab) {
            0 -> FadeInCard(palette) { CoinModule(app, palette) }
            1 -> FadeInCard(palette) { RngModule(app, palette) }
            else -> FadeInCard(palette) { WheelModule(app, palette) }
        }
    }
}

/**
 * 滑动切页手势（网页版 #swipeArea 的 touchstart/touchend 被动监听移植）：
 * |dx| > 60dp 且 |dx| > 1.5·|dy| 时切换 Tab；不消费事件，不影响内部滚动。
 */
private fun Modifier.swipeTabDetector(onSwipe: (deltaTabs: Int) -> Unit): Modifier =
    this.pointerInput(Unit) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            val startX = down.position.x
            val startY = down.position.y
            var endX = startX
            var endY = startY
            var finished = false
            while (!finished) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                for (change in event.changes) {
                    if (change.pressed) {
                        endX = change.position.x
                        endY = change.position.y
                    }
                    if (change.changedToUp()) finished = true
                }
            }
            val dx = endX - startX
            val dy = endY - startY
            val threshold = 60.dp.toPx()
            if (abs(dx) > threshold && abs(dx) > abs(dy) * 1.5f) {
                onSwipe(if (dx < 0f) 1 else -1)
            }
        }
    }

@Composable
private fun TabButton(
    text: String,
    index: Int,
    currentTab: Int,
    palette: NeoPalette,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val active = currentTab == index
    val bg = if (active) palette.accent else Color.Transparent
    val textColor = if (active) palette.btnText else palette.textMuted
    Box(
        modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        NeoSurface(
            palette = palette,
            bg = bg,
            borderColor = if (active) palette.border else Color.Transparent,
            borderWidth = 2.5.dp,
            radius = 100.dp,
            shadowEnabled = active,
            shadowDx = 2.dp,
            shadowDy = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(Modifier.padding(horizontal = 8.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
                NeoText(
                    text,
                    color = textColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.W900,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * 卡片容器（.card）：大圆角 + 硬阴影。
 * 注：不做入场透明度动画 —— 带边框图层做 alpha 动画会让边框先细后粗（光栅化特性），
 * 因此切换 Tab 时卡片直接完整呈现，边框始终清晰。
 */
@Composable
private fun FadeInCard(palette: NeoPalette, content: @Composable () -> Unit) {
    NeoSurface(
        palette = palette,
        radius = 28.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(Modifier.padding(24.dp)) {
            content()
        }
    }
}

@Composable
private fun Sidebar(app: AppState, palette: NeoPalette) {
    ReceiptModule(app, palette)
}

/** 噪点纹理覆盖（body::after）：128px 噪声图平铺，multiply/overlay 混合 */
private fun Modifier.applyNoiseOverlay(
    noise: ImageBitmap?,
    palette: NeoPalette
): Modifier = this.drawWithContent {
    drawContent()
    if (noise != null) {
        val tile = 128.dp.toPx()
        val alpha = if (palette.isDark) 0.08f else 0.05f
        val blend = if (palette.isDark) {
            androidx.compose.ui.graphics.BlendMode.Overlay
        } else {
            androidx.compose.ui.graphics.BlendMode.Multiply
        }
        var y = 0f
        while (y < size.height) {
            var x = 0f
            while (x < size.width) {
                val w = minOf(tile, size.width - x).toInt().coerceAtLeast(1)
                val h = minOf(tile, size.height - y).toInt().coerceAtLeast(1)
                drawImage(
                    noise,
                    dstOffset = androidx.compose.ui.unit.IntOffset(x.toInt(), y.toInt()),
                    dstSize = androidx.compose.ui.unit.IntSize(w, h),
                    alpha = alpha,
                    blendMode = blend
                )
                x += tile
            }
            y += tile
        }
    }
}
