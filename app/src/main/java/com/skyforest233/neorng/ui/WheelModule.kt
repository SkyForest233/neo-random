package com.skyforest233.neorng.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skyforest233.neorng.AppState
import com.skyforest233.neorng.NeoPalette
import com.skyforest233.neorng.NeoSans
import com.skyforest233.neorng.WheelItem

/**
 * 🎡 转盘模块：加权选项（支持 "选项 x3" 语法）、Canvas 绘制转盘、
 * 中心 SPIN 按钮、右侧指针、中奖结果印章（旋转 -6° 弹出 4 秒）。
 */
@Composable
fun WheelModule(app: AppState, palette: NeoPalette) {
    val items = remember(app.wheelInput) { app.parseWheelItems(app.wheelInput) }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(30.dp)) {
        WheelRender(app, palette, items)
        WheelSetup(app, palette)
    }
}

@Composable
private fun WheelRender(app: AppState, palette: NeoPalette, items: List<WheelItem>) {
    val textMeasurer = rememberTextMeasurer()

    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .widthIn(max = 320.dp)
                .aspectRatio(1f)
        ) {
            // 转盘画布（随物理引擎角度旋转）
            Box(
                Modifier
                    .matchParentSize()
                    .graphicsLayer { rotationZ = app.wheel.angle }
            ) {
                Canvas(Modifier.matchParentSize()) {
                    drawWheel(items, palette, textMeasurer)
                }
            }

            // 右侧指针（.wheel-pointer：指向盘内的三角 + 白色投影描边）
            Canvas(
                Modifier
                    .align(Alignment.CenterEnd)
                    .size(width = 26.dp, height = 30.dp)
            ) {
                drawPointer(palette.card, Offset(4f, 4f))
                drawPointer(palette.border, Offset.Zero)
            }

            // 中心按钮（.wheel-center-btn）
            CenterSpinButton(app, palette, Modifier.align(Alignment.Center))

            // 结果印章（.wheel-result-stamp）
            WheelResultStamp(app, palette)
        }
    }
}

private fun DrawScope.drawPointer(color: Color, offset: Offset) {
    val w = size.width
    val h = size.height
    val path = Path().apply {
        moveTo(offset.x + w, offset.y)
        lineTo(offset.x + w, offset.y + h)
        lineTo(offset.x, offset.y + h / 2f)
        close()
    }
    drawPath(path, color)
}

@Composable
private fun CenterSpinButton(app: AppState, palette: NeoPalette, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Box(
        modifier
            .size(74.dp)
            .graphicsLayer {
                if (pressed) {
                    translationX = 2.dp.toPx()
                    translationY = 2.dp.toPx()
                }
            }
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = app.wheel.buttonEnabled
            ) { app.executeWheel() }
            .drawBehind {
                val c = Offset(size.width / 2f, size.height / 2f)
                val r = (size.minDimension - if (pressed) 0f else 6.dp.toPx()) / 2f
                if (!pressed) {
                    // 硬阴影 = 同尺寸圆向右下偏移 3dp（此前圆心算错画在了左上角）
                    drawCircle(
                        color = palette.shadow,
                        radius = r,
                        center = c + Offset(3.dp.toPx(), 3.dp.toPx())
                    )
                }
                drawCircle(color = palette.card, radius = r, center = c)
                drawCircle(color = palette.border, radius = r, center = c, style = Stroke(2.5.dp.toPx()))
            },
        contentAlignment = Alignment.Center
    ) {
        NeoText("SPIN", color = palette.textMain, fontSize = 16.sp, fontWeight = FontWeight.W900)
    }
}

/**
 * 转盘绘制：与网页版 drawWheel() 逐行对应 ——
 * 颜色循环 [card, accent, bg, accentSub]、2.5px 扇区描边、外圈 5px 描边、
 * 文字沿扇区中线右对齐 radius-20、最大宽度 radius-60、权重 >1 显示 "x权重"。
 */
private fun DrawScope.drawWheel(
    items: List<WheelItem>,
    palette: NeoPalette,
    textMeasurer: TextMeasurer
) {
    val scale = size.width / 320.dp.toPx()
    val cx = size.width / 2f
    val cy = size.height / 2f
    val radius = cx - 3.dp.toPx() * scale

    val localColors = listOf(palette.card, palette.accent, palette.bg, palette.accentSub)
    val totalWeight = items.sumOf { it.weight }.toFloat()
    if (totalWeight <= 0f || items.isEmpty()) return

    var currentAngle = 0f
    for (i in items.indices) {
        val sliceAngle = items[i].weight / totalWeight * 360f
        val sliceColor = localColors[i % localColors.size]

        val topLeft = Offset(cx - radius, cy - radius)
        val arcSize = Size(radius * 2f, radius * 2f)
        drawArc(
            color = sliceColor,
            startAngle = currentAngle,
            sweepAngle = sliceAngle,
            useCenter = true,
            topLeft = topLeft,
            size = arcSize
        )
        drawArc(
            color = palette.border,
            startAngle = currentAngle,
            sweepAngle = sliceAngle,
            useCenter = true,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(2.5.dp.toPx() * scale)
        )

        // 文字：旋转到扇区中线，右端对齐 radius-20
        val midAngle = currentAngle + sliceAngle / 2f
        val text = items[i].text + (if (items[i].weight > 1) " x${items[i].weight}" else "")
        val textColor = if (sliceColor == palette.accent || sliceColor == palette.accentSub) {
            palette.btnText
        } else {
            palette.textMain
        }
        val maxTextWidth = (radius - 60.dp.toPx() * scale).coerceAtLeast(1f)
        val layout = textMeasurer.measure(
            text,
            style = TextStyle(
                color = textColor,
                fontSize = 15.sp * scale,
                fontWeight = FontWeight.W800,
                fontFamily = NeoSans
            ),
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            constraints = Constraints(maxWidth = maxTextWidth.toInt())
        )
        rotate(degrees = midAngle, pivot = Offset(cx, cy)) {
            val x = cx + radius - 20.dp.toPx() * scale - layout.size.width
            val y = cy - layout.size.height / 2f
            drawText(layout, topLeft = Offset(x, y))
        }

        currentAngle += sliceAngle
    }

    // 外圈描边
    drawCircle(
        color = palette.border,
        radius = radius,
        center = Offset(cx, cy),
        style = Stroke(5.dp.toPx() * scale)
    )
}

/** 中奖结果印章：-6° 旋转 + 回弹缩放弹出，4 秒后消失 */
@Composable
private fun WheelResultStamp(app: AppState, palette: NeoPalette) {
    val visible = app.wheel.stampVisible
    val progress = remember { Animatable(0f) }
    LaunchedEffect(visible) {
        if (visible) {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(500, easing = NeoOvershootEasing))
        } else {
            progress.snapTo(0f)
        }
    }
    if (progress.value <= 0.01f) return

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        NeoSurface(
            palette = palette,
            bg = palette.textMain,
            borderColor = palette.border,
            borderWidth = 2.5.dp,
            radius = 12.dp,
            shadowDx = 6.dp,
            shadowDy = 6.dp,
            modifier = Modifier
                .widthIn(max = 288.dp)
                .graphicsLayer {
                    rotationZ = -6f
                    scaleX = progress.value
                    scaleY = progress.value
                    alpha = progress.value
                }
        ) {
            androidx.compose.foundation.text.BasicText(
                text = app.wheel.resultText,
                style = TextStyle(
                    color = palette.bg,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.W900,
                    fontFamily = NeoSans
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 16.dp)
            )
        }
    }
}

@Composable
private fun WheelSetup(app: AppState, palette: NeoPalette) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                NeoText("OPTIONS", color = palette.textMain, fontSize = 13.sp, fontWeight = FontWeight.W900)
                SmallBadge("支持权重", palette)
            }
            BrutalCheckbox(
                label = "抽中剔除",
                checked = app.wheelAutoRemove,
                palette = palette,
                onCheckedChange = {
                    app.wheelAutoRemove = it
                    app.saveData()
                }
            )
        }

        // 选项输入区（textarea：行高 1.8、15sp、180dp 高、内部滚动、聚焦强化样式）
        val scroll = rememberScrollState()
        var focused by remember { mutableStateOf(false) }
        val shape = RoundedCornerShape(12.dp)
        Box(
            Modifier
                .fillMaxWidth()
                .height(180.dp)
                .graphicsLayer {
                    translationX = if (focused) -2.dp.toPx() else 0f
                    translationY = if (focused) -2.dp.toPx() else 0f
                }
        ) {
            if (focused) {
                Box(
                    Modifier
                        .matchParentSize()
                        .padding(start = 3.dp, top = 3.dp)
                        .background(palette.shadow, shape)
                )
            }
            Box(
                Modifier
                    .matchParentSize()
                    .padding(end = if (focused) 3.dp else 0.dp, bottom = if (focused) 3.dp else 0.dp)
                    .background(if (focused) palette.card else palette.bg, shape)
                    .border(if (focused) 3.dp else 2.5.dp, palette.border, shape)
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                BasicTextField(
                    value = app.wheelInput,
                    onValueChange = {
                        app.wheelInput = it
                        app.scheduleSave()
                    },
                    textStyle = TextStyle(
                        color = palette.textMain,
                        fontSize = 15.sp,
                        lineHeight = 27.sp,
                        fontWeight = FontWeight.W700,
                        fontFamily = NeoSans
                    ),
                    cursorBrush = SolidColor(palette.textMain),
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scroll)
                        .onFocusChanged { state ->
                            focused = state.isFocused
                            app.onFieldFocus(state.isFocused)
                        }
                )
            }
        }
    }
}
