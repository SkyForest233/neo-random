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
 * 对应网页版 .card / .status-badge / .wheel-center-btn 等公共视觉语言。
 * pressedTranslate = true 时按下位移(3,3)并消除阴影（:active 效果）。
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
    val shape: Shape = if (radius >= 100.dp) CircleShape else RoundedCornerShape(radius)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val active = pressedTranslate && pressed
    val showShadow = shadowEnabled && !active

    Box(modifier) {
        if (showShadow) {
            Box(
                Modifier
                    .matchParentSize()
                    .padding(start = shadowDx, top = shadowDy)
                    .background(palette.shadow, shape)
            )
        }
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(
                end = if (showShadow) shadowDx else 0.dp,
                bottom = if (showShadow) shadowDy else 0.dp
            )
            .graphicsLayer {
                translationX = if (active) shadowDx.toPx() else 0f
                translationY = if (active) shadowDy.toPx() else 0f
            }
            .background(bg, shape)
            .border(borderWidth, borderColor, shape)
        Box(
            if (onClick != null) {
                contentModifier.clickable(interactionSource = interaction, indication = null, onClick = onClick)
            } else {
                contentModifier
            },
            contentAlignment
        ) {
            content()
        }
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
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier
            .fillMaxWidth()
            .graphicsLayer {
                translationX = if (focused) -2.dp.toPx() else 0f
                translationY = if (focused) -2.dp.toPx() else 0f
            }
            .heightIn(min = 52.dp)
    ) {
        if (focused) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(start = 3.dp, top = 3.dp)
                    .background(palette.shadow, shape)
            )
        }
        Box(
            Modifier
                .fillMaxSize()
                .padding(end = if (focused) 3.dp else 0.dp, bottom = if (focused) 3.dp else 0.dp)
                .background(if (focused) palette.card else palette.bg, shape)
                .border(if (focused) 3.dp else 2.5.dp, palette.border, shape)
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
