package com.skyforest233.neorng.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.matchParentSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skyforest233.neorng.AnimState
import com.skyforest233.neorng.AppState
import com.skyforest233.neorng.CoinAnim
import com.skyforest233.neorng.NeoPalette
import com.skyforest233.neorng.NeoSans

/**
 * 🪙 硬币模块：双面文字可编辑、150dp 3D 硬币（14 层厚度）、
 * 点击硬币或按钮开始翻转、底部投影随高度缩放。
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

/**
 * 3D 硬币本体：容器做 rotateY/translateY/rotateX 变换，
 * 内部 14 层圆片（translateZ -7..7px）形成厚度，与网页版 DOM 结构一致。
 */
@Composable
private fun CoinScene(app: AppState, palette: NeoPalette, modifier: Modifier = Modifier) {
    val coin = app.coin
    val showBack = (((coin.angle % 360f) + 360f) % 360f).let { it >= 90f && it < 270f }

    Box(
        modifier
            .size(150.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { app.executeCoinFlip() }
            .graphicsLayer {
                rotationY = coin.angle
                translationY = coin.y.dp.toPx()
                rotationX = coin.rx
            },
        contentAlignment = Alignment.Center
    ) {
        // 厚度层（.coin-edge-layer x14）
        for (i in -7..7) {
            if (i == 0) continue
            Box(
                Modifier
                    .matchParentSize()
                    .graphicsLayer { translationZ = i.dp.toPx() }
                    .coinFaceBackground(palette, accent = false)
            )
        }
        // 当前朝向的面（背面预旋转 180° 保证文字不镜像）
        Box(
            Modifier
                .matchParentSize()
                .graphicsLayer { rotationY = if (showBack) 180f else 0f }
                .coinFaceBackground(palette, accent = showBack),
            contentAlignment = Alignment.Center
        ) {
            val faceText = if (showBack) app.coinTail else app.coinHead
            val faceColor = if (showBack) palette.btnText else palette.textMain
            androidx.compose.foundation.text.BasicText(
                text = faceText,
                style = TextStyle(
                    color = faceColor,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.W900,
                    fontFamily = NeoSans,
                    textAlign = TextAlign.Center
                ),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(10.dp)
            )
        }
    }
}

/** 硬币面样式：圆形 + 边框 + 内嵌 6px 背景色圆环（inset box-shadow） */
private fun Modifier.coinFaceBackground(palette: NeoPalette, accent: Boolean): Modifier =
    this.drawBehind {
        drawCircle(color = if (accent) palette.accent else palette.card, radius = size.minDimension / 2f)
        drawCircle(
            color = palette.border,
            radius = size.minDimension / 2f,
            style = Stroke(2.5.dp.toPx())
        )
        drawCircle(
            color = palette.bg,
            radius = size.minDimension / 2f - 8.dp.toPx(),
            style = Stroke(6.dp.toPx())
        )
    }
