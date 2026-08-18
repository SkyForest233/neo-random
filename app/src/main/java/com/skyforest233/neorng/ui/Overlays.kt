package com.skyforest233.neorng.ui

import android.graphics.Bitmap
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skyforest233.neorng.NeoPalette
import com.skyforest233.neorng.NeoSans
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * 顶部 Toast（#toast）：从屏幕上方 -150dp 滑入，硬阴影药丸形，
 * cubic-bezier(0.175, 0.885, 0.32, 1.275) 弹性曲线，3 秒自动收回。
 */
@Composable
fun ToastHost(visible: Boolean, message: String?, palette: NeoPalette) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(visible) {
        if (visible) {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(400, easing = NeoOvershootEasing))
        } else {
            progress.animateTo(0f, tween(400, easing = NeoOvershootEasing))
        }
    }
    if (progress.value < 0.01f && !visible) return

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        NeoSurface(
            palette = palette,
            bg = palette.textMain,
            borderColor = palette.border,
            borderWidth = 2.5.dp,
            radius = 100.dp,
            shadowDx = 3.dp,
            shadowDy = 3.dp,
            modifier = Modifier
                .padding(top = 12.dp)
                .graphicsLayer {
                    val hidden = -150.dp.toPx()
                    translationY = hidden * (1f - progress.value)
                    alpha = if (progress.value < 0.05f && !visible) 0f else 1f
                }
        ) {
            androidx.compose.foundation.text.BasicText(
                text = message ?: "",
                style = TextStyle(
                    color = palette.bg,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.W700,
                    fontFamily = NeoSans
                ),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
            )
        }
    }
}

private data class ConfettiParticle(
    val x: Float, val y: Float,
    val vx: Float, val vy: Float,
    val rot: Float, val rotSpeed: Float,
    val opacity: Float, val sizeDp: Float,
    val color: Color, val circle: Boolean
)

/**
 * 五彩纸屑（fireConfetti）：屏幕中心爆出 30 个带描边的方/圆粒子，
 * 重力 0.5px/帧²、透明度每帧 -0.02 —— 与网页版粒子物理一致。
 */
@Composable
fun ConfettiHost(trigger: Int, palette: NeoPalette) {
    val particles = remember { mutableStateListOf<ConfettiParticle>() }
    val density = LocalDensity.current

    LaunchedEffect(trigger) {
        if (trigger == 0) return@LaunchedEffect
        val colors = listOf(palette.accent, palette.accentSub, palette.textMain, palette.card)
        val rand = Random(System.currentTimeMillis())
        repeat(30) {
            particles += ConfettiParticle(
                x = 0f, y = 0f,
                vx = (cos(rand.nextFloat() * 2f * Math.PI.toFloat()) * (rand.nextFloat() * 20f + 5f)),
                vy = (sin(rand.nextFloat() * 2f * Math.PI.toFloat()) * (rand.nextFloat() * 20f + 5f)),
                rot = 0f,
                rotSpeed = (rand.nextFloat() - 0.5f) * 20f,
                opacity = 1f,
                sizeDp = rand.nextFloat() * 15f + 10f,
                color = colors[rand.nextInt(colors.size)],
                circle = rand.nextFloat() > 0.5f
            )
        }
        // 帧率无关步进：按 60fps 标准帧累积
        var lastNanos = 0L
        var acc = 0f
        while (particles.isNotEmpty()) {
            withFrameNanos { now ->
                if (lastNanos != 0L) acc += (now - lastNanos) / 1_000_000f / 16.6667f
                lastNanos = now
            }
            var steps = acc.toInt()
            acc -= steps
            if (steps > 4) steps = 4
            repeat(steps) {
                val next = ArrayList<ConfettiParticle>(particles.size)
                for (p in particles) {
                    val newP = p.copy(
                        vy = p.vy + 0.5f,
                        x = p.x + p.vx,
                        y = p.y + p.vy,
                        rot = p.rot + p.rotSpeed,
                        opacity = p.opacity - 0.02f
                    )
                    if (newP.opacity > 0f) next += newP
                }
                particles.clear()
                particles.addAll(next)
            }
        }
    }

    if (particles.isEmpty()) return
    Canvas(Modifier.fillMaxSize()) {
        drawConfetti(particles, palette)
    }
}

private fun DrawScope.drawConfetti(particles: List<ConfettiParticle>, palette: NeoPalette) {
    val center = this.size.center
    for (p in particles) {
        val cx = center.x + p.x.dp.toPx()
        val cy = center.y + p.y.dp.toPx()
        val r = p.sizeDp.dp.toPx() / 2f
        val border = 2.dp.toPx()
        rotate(degrees = p.rot, pivot = Offset(cx, cy)) {
            if (p.circle) {
                drawCircle(color = p.color, radius = r, center = Offset(cx, cy), alpha = p.opacity)
                drawCircle(
                    color = palette.border, radius = r, center = Offset(cx, cy),
                    alpha = p.opacity, style = androidx.compose.ui.graphics.drawscope.Stroke(border)
                )
            } else {
                val tl = Offset(cx - r, cy - r)
                val s = androidx.compose.ui.geometry.Size(r * 2f, r * 2f)
                drawRect(color = p.color, topLeft = tl, size = s, alpha = p.opacity)
                drawRect(
                    color = palette.border, topLeft = tl, size = s,
                    alpha = p.opacity, style = androidx.compose.ui.graphics.drawscope.Stroke(border)
                )
            }
        }
    }
}

/** 波点背景（body 的 radial-gradient 24px 网格） */
fun DrawScope.drawDotGrid(palette: NeoPalette) {
    val step = 24.dp.toPx()
    val dotRadius = 1.5.dp.toPx()
    var y = step / 2f
    while (y < size.height) {
        var x = step / 2f
        while (x < size.width) {
            drawCircle(color = palette.grayLight, radius = dotRadius, center = Offset(x, y))
            x += step
        }
        y += step
    }
}

/** 噪点纹理（body::after 的 SVG fractalNoise，multiply/overlay 混合，5%/8% 透明度） */
@Composable
fun rememberNoiseBrush(): androidx.compose.ui.graphics.Brush? {
    return remember {
        runCatching {
            val size = 128
            val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val pixels = IntArray(size * size)
            val rand = java.util.Random(7)
            for (i in pixels.indices) {
                val g = rand.nextInt(256)
                pixels[i] = (0xFF shl 24) or (g shl 16) or (g shl 8) or g
            }
            bmp.setPixels(pixels, 0, size, 0, 0, size, size)
            val image: ImageBitmap = bmp.asImageBitmap()
            androidx.compose.ui.graphics.BitmapBrush(image)
        }.getOrNull()
    }
}
