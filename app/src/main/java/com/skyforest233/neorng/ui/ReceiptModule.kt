package com.skyforest233.neorng.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skyforest233.neorng.AppState
import com.skyforest233.neorng.HistoryItem
import com.skyforest233.neorng.NeoMono
import com.skyforest233.neorng.NeoPalette
import com.skyforest233.neorng.NeoSans

private val EdgeRed = Color(0xFFFF3366)

/**
 * 🧾 小票侧栏：正面历史记录 + 背面数据统计，3D 翻面切换；
 * 撕毁清空动画（下坠旋转淡出）；分享走系统分享面板。
 */
@Composable
fun ReceiptModule(app: AppState, palette: NeoPalette) {
    Column(Modifier.fillMaxWidth()) {
        Box {
            ReceiptCard(app, palette)
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 15.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            GhostButton(
                text = "📤 分享小票",
                palette = palette,
                modifier = Modifier.weight(1f)
            ) { app.shareReceipt() }
            GhostButton(
                text = "🗑 撕毁清空",
                palette = palette,
                color = EdgeRed,
                modifier = Modifier.weight(1f)
            ) { app.tearReceipt() }
        }
    }
}

/** 虚线边框幽灵按钮（.ghost-btn） */
@Composable
private fun GhostButton(
    text: String,
    palette: NeoPalette,
    modifier: Modifier = Modifier,
    color: Color? = null,
    onClick: () -> Unit
) {
    val lineColor = color ?: palette.border
    val txtColor = color ?: palette.textMuted
    Box(
        modifier
            .drawBehind {
                drawRoundRect(
                    color = lineColor,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx()),
                    style = Stroke(
                        width = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 6.dp.toPx()))
                    )
                )
            }
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            NeoText(text, color = txtColor, fontSize = 14.sp, fontWeight = FontWeight.W900)
        }
    }
}

@Composable
private fun ReceiptCard(app: AppState, palette: NeoPalette) {
    // 翻面动画：平滑缓入缓出（无过冲）、扁平透视，避免扭曲观感
    val flip = remember { Animatable(0f) }
    LaunchedEffect(app.receiptFlipped) {
        flip.animateTo(
            if (app.receiptFlipped) 180f else 0f,
            tween(650, easing = androidx.compose.animation.core.FastOutSlowInEasing)
        )
    }
    // 撕毁动画：0%: 原位 / 40%: 下移30 旋转2° / 100%: 下坠500 旋转-10° 淡出
    val tear = remember { Animatable(0f) }
    LaunchedEffect(app.tearing) {
        if (app.tearing) tear.animateTo(1f, tween(800, easing = FastOutLinearInEasing))
        else tear.snapTo(0f)
    }
    val showBack = flip.value >= 90f

    Box(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 400.dp)
            .graphicsLayer {
                val t = tear.value
                rotationZ = if (t <= 0.4f) (t / 0.4f) * 2f else 2f + ((t - 0.4f) / 0.6f) * -12f
                translationY = if (t <= 0.4f) (t / 0.4f) * 30.dp.toPx()
                else 30.dp.toPx() + ((t - 0.4f) / 0.6f) * 470.dp.toPx()
                alpha = if (t <= 0.4f) 1f else 1f - (t - 0.4f) / 0.6f
            }
            .graphicsLayer {
                rotationY = flip.value
                // 网页版无透视（纯 transform），这里拉远相机消除 3D 扭曲
                cameraDistance = 120.dp.toPx() * 10f
            }
    ) {
        if (showBack) {
            // 背面内容预旋转 180°：翻到背面时文字正向可读，过渡中也不会镜像
            Box(Modifier.graphicsLayer { rotationY = 180f }) {
                ReceiptBack(app, palette)
            }
        } else {
            ReceiptFront(app, palette)
        }
    }
}

// ==================== 小票面板（正面/背面共用骨架） ====================

/**
 * 小票面板：内容驱动尺寸，min 400dp；drawBehind 一次绘制硬阴影/表面/描边。
 * 无 fillMaxSize / matchParentSize / weight，滚动容器中不会塌缩。
 */
@Composable
private fun ReceiptPanel(
    palette: NeoPalette,
    surfaceBg: Color,
    surfaceBorder: Color,
    contentBg: Color,
    contentColor: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 403.dp)
            .drawBehind {
                val dx = 3.dp.toPx()
                val dy = 3.dp.toPx()
                val w = size.width - dx
                val h = size.height - dy
                val cr = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx())
                drawRoundRect(
                    color = palette.shadow,
                    topLeft = androidx.compose.ui.geometry.Offset(dx, dy),
                    size = androidx.compose.ui.geometry.Size(w, h),
                    cornerRadius = cr
                )
                drawRoundRect(
                    color = surfaceBg,
                    topLeft = androidx.compose.ui.geometry.Offset.Zero,
                    size = androidx.compose.ui.geometry.Size(w, h),
                    cornerRadius = cr
                )
                drawRoundRect(
                    color = surfaceBorder,
                    topLeft = androidx.compose.ui.geometry.Offset.Zero,
                    size = androidx.compose.ui.geometry.Size(w, h),
                    cornerRadius = cr,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(2.5.dp.toPx())
                )
            }
            .padding(end = 3.dp, bottom = 3.dp)
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        content = content
    )
}

// ==================== 正面：历史记录 ====================

@Composable
private fun ReceiptFront(app: AppState, palette: NeoPalette) {
    ReceiptPanel(
        palette = palette,
        surfaceBg = palette.card,
        surfaceBorder = palette.border,
        contentBg = palette.card,
        contentColor = palette.textMain
    ) {
        ReceiptHeader(
            palette = palette,
            icon = "🧾",
            title = "RNG RECORD",
            subtitle = "# ${app.recordDate}",
            titleColor = palette.textMain,
            onTitleButton = { app.flipReceipt(true) },
            titleButtonEmoji = "📊",
            dashedColor = palette.textMuted
        )

        Column(
            Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            app.history.forEach { item ->
                HistoryEntry(item, palette)
            }
        }

        ReceiptFooter(palette, barcodeColor = palette.border, note = "VERIFIED RANDOMNESS", noteColor = palette.textMuted, dashedColor = palette.textMuted)
    }
}

@Composable
private fun ReceiptHeader(
    palette: NeoPalette,
    icon: String,
    title: String,
    subtitle: String?,
    titleColor: Color,
    onTitleButton: (() -> Unit)?,
    titleButtonEmoji: String?,
    headerBg: Color = Color.Transparent,
    buttonBg: Color = palette.card,
    buttonTextColor: Color = palette.textMain,
    dashedColor: Color
) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Box(Modifier.size(36.dp))
            Column(
                Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                NeoText(icon, color = titleColor, fontSize = 32.sp, fontWeight = FontWeight.W400)
                Spacer(Modifier.height(6.dp))
                NeoText(
                    title, color = titleColor, fontSize = 18.sp,
                    fontWeight = FontWeight.W900, letterSpacing = 1.sp
                )
                if (subtitle != null) {
                    Spacer(Modifier.height(5.dp))
                    NeoText(subtitle, color = dashedColor, fontSize = 12.sp, fontWeight = FontWeight.W400, fontFamily = NeoMono)
                }
            }
            if (titleButtonEmoji != null && onTitleButton != null) {
                IconCircleButton(
                    emoji = titleButtonEmoji,
                    palette = palette.copyBg(buttonBg).copyText(buttonTextColor),
                    size = 36.dp
                ) { onTitleButton() }
            } else {
                Box(Modifier.size(36.dp))
            }
        }
        Spacer(Modifier.height(15.dp))
        DashedDivider(dashedColor)
        Spacer(Modifier.height(20.dp))
    }
}

/** 小剂量复制工具：让 IconCircleButton 在反色面上也能正确配色 */
private fun NeoPalette.copyBg(c: Color): NeoPalette = copy(card = c)
private fun NeoPalette.copyText(c: Color): NeoPalette = copy(textMain = c)

@Composable
private fun HistoryEntry(item: HistoryItem, palette: NeoPalette) {
    Column(Modifier.fillMaxWidth()) {
        ReceiptRow("TIME", item.time, palette, valueColor = palette.textMuted, mono = true)
        Spacer(Modifier.height(6.dp))
        ReceiptRow("TYPE", item.type, palette, mono = true)
        Spacer(Modifier.height(6.dp))
        ReceiptRow(
            "RESULT", item.res, palette,
            mono = true,
            highlight = item.res.contains("EDGE"),
            pill = true
        )
        Spacer(Modifier.height(15.dp))
        // 每条记录底部的浅虚线（.receipt-item::after, opacity .3）
        Canvas(Modifier.fillMaxWidth().height(1.dp)) {
            drawLine(
                color = palette.textMuted,
                start = Offset.Zero,
                end = Offset(size.width, 0f),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx())),
                alpha = 0.3f
            )
        }
    }
}

@Composable
private fun ReceiptRow(
    label: String,
    value: String,
    palette: NeoPalette,
    mono: Boolean = false,
    valueColor: Color = Color.Unspecified,
    highlight: Boolean = false,
    pill: Boolean = false
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        NeoText(
            label,
            color = palette.textMuted,
            fontSize = 13.sp,
            fontWeight = FontWeight.W400,
            fontFamily = if (mono) NeoMono else NeoSans
        )
        if (pill) {
            // RESULT：强调色小药丸（EDGE 结果红色警示）
            Box(
                Modifier
                    .padding(start = 8.dp)
                    .widthIn(max = 210.dp)
                    .then(
                        if (highlight) {
                            Modifier
                                .background(EdgeRed, RoundedCornerShape(4.dp))
                                .border(1.dp, EdgeRed, RoundedCornerShape(4.dp))
                        } else {
                            Modifier
                                .background(palette.accent, RoundedCornerShape(4.dp))
                                .border(1.dp, palette.border, RoundedCornerShape(4.dp))
                        }
                    )
                    .padding(horizontal = 4.dp)
            ) {
                NeoText(
                    value,
                    color = if (highlight) Color.White else palette.textMain,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.W700,
                    fontFamily = if (mono) NeoMono else NeoSans,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.align(Alignment.CenterEnd)
                )
            }
        } else {
            NeoText(
                value,
                color = if (valueColor == Color.Unspecified) palette.textMain else valueColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.W700,
                fontFamily = if (mono) NeoMono else NeoSans,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ReceiptFooter(palette: NeoPalette, barcodeColor: Color, note: String, noteColor: Color, dashedColor: Color) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        DashedDivider(dashedColor)
        Spacer(Modifier.height(15.dp))
        Barcode(barcodeColor, modifier = Modifier.fillMaxWidth(0.8f).height(40.dp))
        Spacer(Modifier.height(10.dp))
        NeoText(note, color = noteColor, fontSize = 12.sp, fontWeight = FontWeight.W400, fontFamily = NeoMono)
    }
}

/** 装饰条形码（.barcode 的 repeating-linear-gradient 条纹） */
@Composable
private fun Barcode(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val period = 8.dp.toPx()
        val w2 = 2.dp.toPx()
        val w1 = 1.dp.toPx()
        var x = 0f
        while (x < size.width) {
            drawRect(color = color, topLeft = Offset(x, 0f), size = androidx.compose.ui.geometry.Size(w2, size.height))
            drawRect(color = color, topLeft = Offset(x + 4.dp.toPx(), 0f), size = androidx.compose.ui.geometry.Size(w1, size.height))
            x += period
        }
    }
}

// ==================== 背面：数据统计 ====================

@Composable
private fun ReceiptBack(app: AppState, palette: NeoPalette) {
    ReceiptPanel(
        palette = palette,
        surfaceBg = palette.textMain,
        surfaceBorder = palette.border,
        contentBg = palette.textMain,
        contentColor = palette.bg
    ) {
        ReceiptHeader(
            palette = palette,
            icon = "📈",
            title = "DATA INSIGHTS",
            subtitle = null,
            titleColor = palette.bg,
            onTitleButton = { app.flipReceipt(false) },
            titleButtonEmoji = "🔙",
            buttonBg = palette.bg,
            buttonTextColor = palette.textMain,
            dashedColor = palette.bg
        )

        Column(Modifier.fillMaxWidth()) {
            StatsContent(app, palette)
        }

        ReceiptFooter(
            palette,
            barcodeColor = palette.bg,
            note = "DATA DOES NOT LIE",
            noteColor = palette.bg,
            dashedColor = palette.bg
        )
    }
}

/** 统计内容：正/反/EDGE 条形图 + 转盘 TOP3（renderStats 移植） */
@Composable
private fun StatsContent(app: AppState, palette: NeoPalette) {
    val history = app.history

    var coinH = 0
    var coinT = 0
    var coinE = 0
    val wheelDict = LinkedHashMap<String, Int>()
    for (item in history) {
        when (item.type) {
            "COIN FLIP" -> when {
                item.res == app.coinHead -> coinH++
                item.res == app.coinTail -> coinT++
                else -> coinE++
            }
            "SPIN WHEEL" -> wheelDict[item.res] = (wheelDict[item.res] ?: 0) + 1
        }
    }
    val totalCoin = (coinH + coinT + coinE).coerceAtLeast(1)

    Column(Modifier.fillMaxWidth()) {
        // 🪙 COIN FLIPS
        StatHeader("🪙 COIN FLIPS", palette)
        Spacer(Modifier.height(10.dp))
        StatBar("HEADS", coinH.toFloat() / totalCoin, coinH.toString(), palette, palette.bg, null)
        Spacer(Modifier.height(8.dp))
        StatBar("TAILS", coinT.toFloat() / totalCoin, coinT.toString(), palette, palette.bg, null)
        if (coinE > 0) {
            Spacer(Modifier.height(8.dp))
            StatBar("EDGE", coinE.toFloat() / totalCoin, coinE.toString(), palette, palette.bg, EdgeRed)
        }
    }

    val sortedWheel = wheelDict.entries.sortedByDescending { it.value }.take(3)
    if (sortedWheel.isNotEmpty()) {
        Spacer(Modifier.height(20.dp))
        Column(Modifier.fillMaxWidth()) {
            StatHeader("🎡 TOP WHEEL PICKS", palette)
            Spacer(Modifier.height(10.dp))
            val maxW = sortedWheel.first().value.toFloat()
            sortedWheel.forEach { (name, cnt) ->
                StatBar(name, cnt / maxW, cnt.toString(), palette, palette.bg, null)
                Spacer(Modifier.height(8.dp))
            }
        }
    } else {
        Spacer(Modifier.height(20.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            NeoText(
                "NO WHEEL DATA",
                color = palette.bg,
                fontSize = 14.sp,
                fontWeight = FontWeight.W700,
                modifier = Modifier.padding(top = 20.dp)
            )
        }
    }
}

@Composable
private fun StatHeader(text: String, palette: NeoPalette) {
    Column(Modifier.fillMaxWidth()) {
        NeoText(text, color = palette.bg, fontSize = 14.sp, fontWeight = FontWeight.W900)
        Spacer(Modifier.height(5.dp))
        Canvas(Modifier.fillMaxWidth().height(1.dp)) {
            drawLine(
                color = palette.bg,
                start = Offset.Zero,
                end = Offset(size.width, 0f),
                strokeWidth = 1.dp.toPx()
            )
        }
    }
}

/** 统计条形图（.stat-bar-*）：0 起始，1s ease-out 展开动画 */
@Composable
private fun StatBar(
    label: String,
    fraction: Float,
    value: String,
    palette: NeoPalette,
    barColor: Color,
    overrideColor: Color?
) {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(fraction) {
        anim.animateTo(fraction.coerceIn(0f, 1f), tween(1000))
    }
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(Modifier.width(45.dp), contentAlignment = Alignment.CenterEnd) {
            NeoText(
                label,
                color = overrideColor ?: palette.bg,
                fontSize = 12.sp,
                fontWeight = FontWeight.W700,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Box(
            Modifier
                .weight(1f)
                .height(12.dp)
                .background(if (overrideColor != null) Color.Transparent else translucentBar(palette), RoundedCornerShape(6.dp))
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(anim.value)
                    .background(overrideColor ?: barColor, RoundedCornerShape(6.dp))
            )
        }
        NeoText(
            value,
            color = overrideColor ?: palette.bg,
            fontSize = 12.sp,
            fontWeight = FontWeight.W700,
            fontFamily = NeoMono,
            modifier = Modifier.width(30.dp)
        )
    }
}

private fun translucentBar(palette: NeoPalette): Color =
    if (palette.isDark) Color.Black.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.2f)
