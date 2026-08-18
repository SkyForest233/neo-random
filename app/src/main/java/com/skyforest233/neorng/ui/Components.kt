package com.skyforest233.neorng.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skyforest233.neorng.NeoPalette
import com.skyforest233.neorng.NeoSans

/** 网页版 cubic-bezier(0.175, 0.885, 0.32, 1.275) 回弹曲线 */
val NeoOvershootEasing = CubicBezierEasing(0.175f, 0.885f, 0.32f, 1.275f)

/**
 * Neo-Brutalism 表面：实色背景 + 粗描边 + 硬偏移阴影。
 * 尺寸完全由内容驱动：外层用 padding 在右/下预留阴影空间，drawBehind 一次性
 * 画出阴影层、表面层与描边 —— 不依赖 fillMaxSize/matchParentSize，
 * 在任何约束（含无限高度的滚动容器）下都不会塌缩或撑满。
 */
@Composable
fun NeoSurface(
    modifier: Modifier = Modifier,
    palette: NeoPalette,
    onClick: (() -> Unit)? = null,
    bg: Color = palette.card,
    borderColor: Color = palette.border,
    borderWidth: Dp = 2.5.dp,
    radius: Dp = 12.dp,
    shadowDx: Dp = 3.dp,
    shadowDy: Dp = 3.dp,
    shadowEnabled: Boolean = true,
    pressedTranslate: Boolean = false,
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable BoxScope.() -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val active = pressedTranslate && pressed
    val reserveEnd = if (shadowEnabled) shadowDx else 0.dp
    val reserveBottom = if (shadowEnabled) shadowDy else 0.dp

    Box(
        modifier
            .graphicsLayer {
                if (active) {
                    translationX = shadowDx.toPx()
                    translationY = shadowDy.toPx()
                }
            }
            .drawBehind {
                val dx = reserveEnd.toPx()
                val dy = reserveBottom.toPx()
                val w = size.width - dx
                val h = size.height - dy
                val cr = if (radius >= 100.dp) {
                    androidx.compose.ui.geometry.CornerRadius(minOf(w, h) / 2f)
                } else {
                    androidx.compose.ui.geometry.CornerRadius(radius.toPx())
                }
                // 硬阴影（按下时消失，对应 :active）
                if (shadowEnabled && !active) {
                    drawRoundRect(
                        color = palette.shadow,
                        topLeft = androidx.compose.ui.geometry.Offset(dx, dy),
                        size = androidx.compose.ui.geometry.Size(w, h),
                        cornerRadius = cr
                    )
                }
                // 表面 + 描边
                drawRoundRect(
                    color = bg,
                    topLeft = androidx.compose.ui.geometry.Offset.Zero,
                    size = androidx.compose.ui.geometry.Size(w, h),
                    cornerRadius = cr
                )
                drawRoundRect(
                    color = borderColor,
                    topLeft = androidx.compose.ui.geometry.Offset.Zero,
                    size = androidx.compose.ui.geometry.Size(w, h),
                    cornerRadius = cr,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(borderWidth.toPx())
                )
            }
            .padding(end = reserveEnd, bottom = reserveBottom)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interaction,
                        indication = null,
                        onClick = onClick
                    )
                } else Modifier
            ),
        contentAlignment = contentAlignment
    ) {
        content()
    }
}

/** 主行动按钮（.btn-action）：药丸形、强调色、按下位移消阴影；disabled 状态灰化 */
@Composable
fun ActionButton(
    text: String,
    enabled: Boolean,
    palette: NeoPalette,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 16.sp,
    onClick: () -> Unit
) {
    val textColor = if (enabled) palette.btnText else palette.textMuted
    val bg = if (enabled) palette.accent else palette.grayLight
    NeoSurface(
        modifier = modifier.fillMaxWidth(),
        palette = palette,
        onClick = if (enabled) onClick else null,
        bg = bg,
        borderColor = palette.border,
        borderWidth = 2.5.dp,
        radius = 100.dp,
        shadowEnabled = enabled,
        pressedTranslate = true,
        contentAlignment = Alignment.Center
    ) {
        NeoText(
            text = text,
            color = textColor,
            fontSize = fontSize,
            fontWeight = FontWeight.W900,
            modifier = Modifier.padding(vertical = 16.dp)
        )
    }
}

/** 头部圆形图标按钮（.icon-btn） */
@Composable
fun IconCircleButton(
    emoji: String,
    palette: NeoPalette,
    size: Dp = 36.dp,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    NeoSurface(
        modifier = modifier.size(size + 2.dp),
        palette = palette,
        onClick = onClick,
        bg = palette.card,
        borderWidth = 2.dp,
        radius = 100.dp,
        shadowDx = 2.dp,
        shadowDy = 2.dp,
        pressedTranslate = true,
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.text.BasicText(
            text = emoji,
            style = TextStyle(fontSize = with(androidx.compose.ui.platform.LocalDensity.current) { (size.value * 0.44f).sp }),
            modifier = Modifier
        )
    }
}

/** 小徽章（.badge-small） */
@Composable
fun SmallBadge(text: String, palette: NeoPalette, modifier: Modifier = Modifier) {
    Box(
        modifier
            .background(palette.accent, RoundedCornerShape(100.dp))
            .border(1.5.dp, palette.border, RoundedCornerShape(100.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        NeoText(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.W900,
            color = palette.btnText
        )
    }
}

/** 基础文本快捷方式（统一主字体） */
@Composable
fun NeoText(
    text: String,
    color: Color,
    fontSize: TextUnit = 14.sp,
    fontWeight: FontWeight = FontWeight.W700,
    modifier: Modifier = Modifier,
    fontFamily: androidx.compose.ui.text.font.FontFamily = NeoSans,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip
) {
    androidx.compose.foundation.text.BasicText(
        text = text,
        modifier = modifier,
        style = TextStyle(
            color = color,
            fontSize = fontSize,
            fontWeight = fontWeight,
            fontFamily = fontFamily,
            letterSpacing = letterSpacing
        ),
        maxLines = maxLines,
        overflow = overflow
    )
}

/** 粗野勾选框（.brutal-checkbox） */
@Composable
fun BrutalCheckbox(
    label: String,
    checked: Boolean,
    palette: NeoPalette,
    modifier: Modifier = Modifier,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onCheckedChange(!checked) },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)
    ) {
        Box(
            Modifier
                .size(16.dp)
                .background(if (checked) palette.textMain else palette.bg, RoundedCornerShape(4.dp))
                .border(2.dp, if (checked) palette.textMain else palette.textMuted, RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (checked) {
                NeoText("✔", color = palette.bg, fontSize = 10.sp, fontWeight = FontWeight.W900)
            }
        }
        NeoText(label, color = palette.textMuted, fontSize = 12.sp, fontWeight = FontWeight.W700)
    }
}

/**
 * 输入框（input[type=text/number]）：
 * 平时 bg-color 背景；聚焦时 card 背景 + 硬阴影 + 上移2px + 3px 描边。
 * 同样使用内容驱动 + drawBehind 绘制，无塌缩风险。
 */
@Composable
fun NeoTextField(
    value: String,
    onValueChange: (String) -> Unit,
    palette: NeoPalette,
    modifier: Modifier = Modifier,
    numeric: Boolean = false,
    fontSize: TextUnit = 16.sp,
    singleLine: Boolean = true,
    padding: Dp = 14.dp,
    onFocusChanged: ((Boolean) -> Unit)? = null
) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .graphicsLayer {
                if (focused) {
                    translationX = -2.dp.toPx()
                    translationY = -2.dp.toPx()
                }
            }
            .drawBehind {
                val dx = 3.dp.toPx()
                val dy = 3.dp.toPx()
                val w = size.width - dx
                val h = size.height - dy
                val cr = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx())
                if (focused) {
                    drawRoundRect(
                        color = palette.shadow,
                        topLeft = androidx.compose.ui.geometry.Offset(dx, dy),
                        size = androidx.compose.ui.geometry.Size(w, h),
                        cornerRadius = cr
                    )
                }
                drawRoundRect(
                    color = if (focused) palette.card else palette.bg,
                    topLeft = androidx.compose.ui.geometry.Offset.Zero,
                    size = androidx.compose.ui.geometry.Size(w, h),
                    cornerRadius = cr
                )
                drawRoundRect(
                    color = palette.border,
                    topLeft = androidx.compose.ui.geometry.Offset.Zero,
                    size = androidx.compose.ui.geometry.Size(w, h),
                    cornerRadius = cr,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        (if (focused) 3.dp else 2.5.dp).toPx()
                    )
                )
            }
            .padding(end = 3.dp, bottom = 3.dp)
            .padding(horizontal = 20.dp, vertical = padding),
        contentAlignment = Alignment.CenterStart
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            textStyle = TextStyle(
                color = palette.textMain,
                fontSize = fontSize,
                fontWeight = FontWeight.W700,
                fontFamily = NeoSans
            ),
            keyboardOptions = if (numeric) {
                KeyboardOptions(keyboardType = KeyboardType.Number)
            } else {
                KeyboardOptions.Default
            },
            cursorBrush = SolidColor(palette.textMain),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { state ->
                    focused = state.isFocused
                    onFocusChanged?.invoke(state.isFocused)
                }
        )
    }
}

/** 虚线分隔线（receipt 中的 dashed border） */
@Composable
fun DashedDivider(
    color: Color,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 2.dp,
    dashOn: Dp = 8.dp,
    dashOff: Dp = 6.dp
) {
    androidx.compose.foundation.Canvas(modifier.fillMaxWidth().height(2.dp)) {
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(0f, size.height / 2f),
            end = androidx.compose.ui.geometry.Offset(size.width, size.height / 2f),
            strokeWidth = strokeWidth.toPx(),
            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                floatArrayOf(dashOn.toPx(), dashOff.toPx())
            )
        )
    }
}
