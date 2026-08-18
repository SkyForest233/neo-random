package com.skyforest233.neorng.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skyforest233.neorng.AppState
import com.skyforest233.neorng.NeoPalette
import com.skyforest233.neorng.NeoSans
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * 🪙 硬币模块：双面文字可编辑、150dp 3D 硬币、点击硬币或按钮翻转、底部投影随高度缩放。
 * 硬币用 Canvas 手绘圆柱投影（正/背面椭圆 + 侧边带），单一实体、真实厚度感。
 */
@Composable
fun CoinModule(app: AppState, palette: NeoPalette) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LabelRow("HEADS", "正面", palette)
                NeoTextField(
                    value = app.coinHead,
                    onValueChange = {
                        app.coinHead = it
                        app.saveData()
                    },
                    palette = palette
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LabelRow("TAILS", "反面", palette)
                NeoTextField(
                    value = app.coinTail,
                    onValueChange = {
                        app.coinTail = it
                        app.saveData()
                    },
                    palette = palette
                )
            }
        }

        CoinScene(app, palette, modifier = Modifier.padding(top = 40.dp, bottom = 8.dp))

        // 硬币投影（.coin-shadow）：随抛起高度缩小变淡
        val shadowScale = 1f - (app.coin.y / -80f) * 0.4f
        Box(
            Modifier
                .padding(top = 12.dp, bottom = 40.dp)
                .width(120.dp)
                .height(16.dp)
                .graphicsLayer {
                    scaleX = shadowScale
                    scaleY = shadowScale
                    alpha = shadowScale * (if (palette.isDark) 0.45f else 0.22f)
                }
                .blur(if (palette.isDark) 6.dp else 3.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.matchParentSize()) {
                drawOval(color = palette.border, topLeft = Offset.Zero, size = size)
            }
        }

        ActionButton(
            text = "FLIP COIN",
            enabled = app.coin.buttonEnabled,
            palette = palette
        ) { app.executeCoinFlip() }
    }
}

@Composable
private fun LabelRow(label: String, badge: String, palette: NeoPalette) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        NeoText(
            label, color = palette.textMain, fontSize = 13.sp,
            fontWeight = FontWeight.W900
        )
        SmallBadge(badge, palette)
    }
}

/** 3D 硬币：Canvas 圆柱投影绘制（正面/背面椭圆 + 厚度侧边带） */
@Composable
private fun CoinScene(app: AppState, palette: NeoPalette, modifier: Modifier = Modifier) {
    val coin = app.coin
    val textMeasurer = rememberTextMeasurer()

    Box(
        modifier
            .size(150.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { app.executeCoinFlip() }
            .graphicsLayer {
                translationY = coin.y.dp.toPx()
                rotationX = coin.rx
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.matchParentSize()) {
            drawCoin3D(
                angleDeg = coin.angle,
                palette = palette,
                textMeasurer = textMeasurer,
                frontText = app.coinHead,
                backText = app.coinTail
            )
        }
    }
}

/**
 * 圆柱投影硬币：
 * - 正面 z=+8px / 背面 z=-8px，绕 Y 轴旋转 θ 后投影 x = ±8·sinθ，椭圆宽 = 2R·|cosθ|
 * - 侧边带 = 两椭圆外轮廓的胶囊形（圆角 R）
 * - 文字随面一起水平压缩（withTransform scale(cosθ, 1)）
 */
private fun DrawScope.drawCoin3D(
    angleDeg: Float,
    palette: NeoPalette,
    textMeasurer: TextMeasurer,
    frontText: String,
    backText: String
) {
    val R = size.minDimension / 2f - 2.5f.dp.toPx()
    val cx = size.width / 2f
    val cy = size.height / 2f
    val rad = Math.toRadians(angleDeg.toDouble())
    val cosA = abs(cos(rad)).toFloat()
    val sinA = sin(rad).toFloat()
    val halfThick = 4.dp.toPx() // ±4dp = 8dp 厚度（web ±8px）

    val showBack = (((angleDeg % 360f) + 360f) % 360f).let { it >= 90f && it < 270f }
    // 朝向观察者的面（近面）与远离的面（远面）
    val nearX = if (showBack) cx - halfThick * sinA else cx + halfThick * sinA
    val farX = if (showBack) cx + halfThick * sinA else cx - halfThick * sinA

    val nearRx = R * cosA
    val pillLeft = minOf(nearX, farX) - nearRx
    val pillRight = maxOf(nearX, farX) + nearRx

    // 1) 侧边带（胶囊形，card 底色 + 描边）
    drawRoundRect(
        color = palette.card,
        topLeft = Offset(pillLeft, cy - R),
        size = Size(pillRight - pillLeft, R * 2f),
        cornerRadius = CornerRadius(R, R)
    )
    drawRoundRect(
        color = palette.border,
        topLeft = Offset(pillLeft, cy - R),
        size = Size(pillRight - pillLeft, R * 2f),
        cornerRadius = CornerRadius(R, R),
        style = Stroke(2.5f.dp.toPx())
    )

    // 2) 远面残影（侧边另一端能看到的椭圆弧）
    if (cosA > 0.02f) {
        val farFill = if (showBack) palette.card else palette.accent
        drawOval(
            color = farFill,
            topLeft = Offset(farX - nearRx, cy - R),
            size = Size(nearRx * 2f, R * 2f)
        )
    }

    // 3) 近面（正面 card / 背面 accent）
    if (cosA > 0.02f) {
        val nearFill = if (showBack) palette.accent else palette.card
        drawOval(
            color = nearFill,
            topLeft = Offset(nearX - nearRx, cy - R),
            size = Size(nearRx * 2f, R * 2f)
        )
        // 内嵌 6dp 背景色圆环（inset box-shadow）
        val ringR = (R - 7.dp.toPx()).coerceAtLeast(1f)
        drawOval(
            color = palette.bg,
            topLeft = Offset(nearX - ringR * cosA, cy - ringR),
            size = Size(ringR * cosA * 2f, ringR * 2f),
            style = Stroke(6f.dp.toPx())
        )
        drawOval(
            color = palette.border,
            topLeft = Offset(nearX - nearRx, cy - R),
            size = Size(nearRx * 2f, R * 2f),
            style = Stroke(2.5f.dp.toPx())
        )
    }

    // 4) 近面文字（水平压缩模拟 3D）
    if (cosA > 0.25f) {
        val text = if (showBack) backText else frontText
        if (text.isNotBlank()) {
            val textColor = if (showBack) palette.btnText else palette.textMain
            val maxW = ((nearRx * 2f - 16.dp.toPx()).coerceAtLeast(4f)).toInt()
            val layout = textMeasurer.measure(
                text,
                style = TextStyle(
                    color = textColor,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.W900,
                    fontFamily = NeoSans
                ),
                softWrap = false,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                constraints = Constraints(maxWidth = maxW)
            )
            val tl = Offset(nearX - layout.size.width / 2f, cy - layout.size.height / 2f)
            withTransform({
                scale(cosA, 1f, pivot = Offset(nearX, cy))
            }) {
                drawText(layout, topLeft = tl)
            }
        }
    }
}
