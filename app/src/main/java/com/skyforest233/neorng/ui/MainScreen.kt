@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
    androidx.compose.animation.ExperimentalAnimationApi::class
)

package com.skyforest233.neorng.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.pager.PagerState
import com.skyforest233.neorng.AnimState
import com.skyforest233.neorng.AppState
import com.skyforest233.neorng.NeoPalette
import com.skyforest233.neorng.RngEngine
import com.skyforest233.neorng.paletteFor
import kotlinx.coroutines.launch

/**
 * 应用主界面：头部（标题 + 主题/深浅/静音/随机源切换）、
 * Tab 药丸栏（可左右滑动切换）、小票侧栏；
 * 宽屏(≥900dp)时双栏布局，与网页版媒体查询一致。
 */
@Composable
fun MainScreen(app: AppState) {
    val palette = paletteFor(app.theme, app.dark)
    val pagerState = rememberPagerState(initialPage = 0) { 3 }
    val scope = rememberCoroutineScope()
    val noiseBrush = rememberNoiseBrush()

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
                    val items = if (wheelActive) app.parseWheelItems(app.wheelInput) else emptyList()
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
            .onPreviewKeyEvent { event ->
                // 网页版空格快捷键：触发当前 Tab 的动作（输入框聚焦时除外）
                if (event.type == KeyEventType.KeyDown &&
                    event.key == androidx.compose.ui.input.key.Key.Spacebar &&
                    !app.anyInputFocused
                ) {
                    when (pagerState.currentPage) {
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

            BoxWithConstraints(Modifier.fillMaxSize()) {
                val wide = maxWidth >= 900.dp
                if (wide) {
                    Row(
                        Modifier
                            .fillMaxSize()
                            .widthIn(max = 1100.dp)
                            .align(Alignment.TopCenter)
                            .padding(horizontal = 20.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        Column(Modifier.weight(1.5f)) {
                            MainContent(app, palette, pagerState)
                        }
                        Column(Modifier.weight(1f)) {
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
                        MainContent(app, palette, pagerState)
                        Spacer(Modifier.height(24.dp))
                        Sidebar(app, palette)
                        Spacer(Modifier.height(40.dp))
                    }
                }
            }
        }

        // 噪点纹理覆盖层（body::after）
        if (noiseBrush != null) {
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .drawWithContent {
                        drawContent()
                        drawRect(
                            brush = noiseBrush,
                            alpha = if (palette.isDark) 0.08f else 0.05f,
                            blendMode = if (palette.isDark) {
                                androidx.compose.ui.graphics.BlendMode.Overlay
                            } else {
                                androidx.compose.ui.graphics.BlendMode.Multiply
                            }
                        )
                    }
            )
        }

        ToastHost(visible = app.toastMsg != null, message = app.toastMsg, palette = palette)
        ConfettiHost(trigger = app.confettiTrigger, palette = palette)
    }
}

// ==================== 头部 ====================

@Composable
private fun Header(app: AppState, palette: NeoPalette) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.foundation.text.BasicText(
            text = "NEO_RNG",
            style = androidx.compose.ui.text.TextStyle(
                color = palette.textMain,
                fontSize = 22.sp,
                fontWeight = FontWeight.W900,
                letterSpacing = (-0.5).sp,
                fontFamily = com.skyforest233.neorng.NeoSans
            )
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconCircleButton(emoji = "🎨", palette = palette) { app.cycleTheme() }
            IconCircleButton(emoji = "🌓", palette = palette) { app.toggleDarkMode() }
            IconCircleButton(emoji = if (app.muted) "🔕" else "🔔", palette = palette) { app.toggleMute() }
            ModeBadge(app, palette)
        }
    }
}

/** 随机源三态切换徽章（#modeBtn + 状态点颜色） */
@Composable
private fun ModeBadge(app: AppState, palette: NeoPalette) {
    val dotColor = when (app.rngMode) {
        "randomorg" -> palette.accent
        "drand" -> palette.accentSub
        else -> palette.grayLight
    }
    NeoSurface(
        palette = palette,
        onClick = { app.toggleMode() },
        radius = 100.dp,
        borderWidth = 2.5.dp,
        shadowDx = 2.dp,
        shadowDy = 2.dp,
        pressedTranslate = true
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                Modifier
                    .size(10.dp)
                    .background(dotColor, androidx.compose.foundation.shape.CircleShape)
                    .border(2.dp, palette.border, androidx.compose.foundation.shape.CircleShape)
            )
            NeoText(
                RngEngine.MODE_LABELS[app.rngMode] ?: "LOCAL RNG",
                color = palette.textMain,
                fontSize = 12.sp,
                fontWeight = FontWeight.W700
            )
        }
    }
}

// ==================== 主体内容 ====================

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MainContent(app: AppState, palette: NeoPalette, pagerState: PagerState) {
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxWidth()) {
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
                TabButton("🪙 Coin", 0, pagerState, palette, Modifier.weight(1f)) { scope.launch { pagerState.animateScrollToPage(0) } }
                TabButton("🔢 Generator", 1, pagerState, palette, Modifier.weight(1f)) { scope.launch { pagerState.animateScrollToPage(1) } }
                TabButton("🎡 Wheel", 2, pagerState, palette, Modifier.weight(1f)) { scope.launch { pagerState.animateScrollToPage(2) } }
            }
        }

        Spacer(Modifier.height(20.dp))

        // 页面容器：左右滑动切换（swipe 手势）+ 保持三页状态
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
            beyondViewportPageCount = 2
        ) { page ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(palette.card)
            ) {
                when (page) {
                    0 -> FadeInCard(palette) { CoinModule(app, palette) }
                    1 -> FadeInCard(palette) { RngModule(app, palette) }
                    else -> FadeInCard(palette) { WheelModule(app, palette) }
                }
            }
        }
    }
}

@Composable
private fun TabButton(
    text: String,
    index: Int,
    pagerState: PagerState,
    palette: NeoPalette,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val active = pagerState.currentPage == index
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
            shadowDy = 2.dp
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

/** 卡片容器（.card）：大圆角 + 硬阴影 + fadeIn 0.3s 入场 */
@Composable
private fun FadeInCard(palette: NeoPalette, content: @Composable () -> Unit) {
    val transition = androidx.compose.animation.core.MutableTransitionState(false).apply { targetState = true }
    androidx.compose.animation.AnimatedVisibility(
        visibleState = transition,
        enter = androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(300)) +
            androidx.compose.animation.slideInVertically(
                animationSpec = androidx.compose.animation.core.tween(300),
                initialOffsetY = { it / 12 }
            )
    ) {
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
}

@Composable
private fun Sidebar(app: AppState, palette: NeoPalette) {
    ReceiptModule(app, palette)
}
