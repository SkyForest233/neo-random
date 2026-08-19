package com.skyforest233.neorng

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color

/**
 * 二维主题系统引擎：3 套色系 x 深浅模式，与网页版 CSS 变量一一对应。
 */
@Immutable
data class NeoPalette(
    val key: String,
    val label: String,
    val isDark: Boolean,
    val bg: Color,          // --bg-color
    val card: Color,        // --card-bg
    val textMain: Color,    // --text-main
    val border: Color,      // --border-main
    val accent: Color,      // --accent-main
    val accentSub: Color,   // --accent-sub
    val btnText: Color,     // --btn-text
    val grayLight: Color,   // --gray-light
    val textMuted: Color,   // --text-muted
    val shadow: Color       // --shadow-main
)

private val CYBER_LIGHT = NeoPalette(
    key = "cyber", label = "⚡ 赛博酸性", isDark = false,
    bg = Color(0xFFF4F5F5), card = Color(0xFFFFFFFF), textMain = Color(0xFF141414),
    border = Color(0xFF141414), accent = Color(0xFFD4FF00), accentSub = Color(0xFFB28DFF),
    btnText = Color(0xFF141414), grayLight = Color(0xFFE0E2E5), textMuted = Color(0xFF666666),
    shadow = Color(0xFF141414)
)

private val CYBER_DARK = NeoPalette(
    key = "cyber", label = "⚡ 赛博酸性", isDark = true,
    bg = Color(0xFF121212), card = Color(0xFF1C1C1C), textMain = Color(0xFFE4E4E4),
    border = Color(0xFF3A3A3A), accent = Color(0xFFD4FF00), accentSub = Color(0xFFB28DFF),
    btnText = Color(0xFF141414), grayLight = Color(0xFF2A2A2A), textMuted = Color(0xFF888888),
    shadow = Color(0xFF000000)
)

private val RETRO_LIGHT = NeoPalette(
    key = "retro", label = "📻 复古印刷", isDark = false,
    bg = Color(0xFFEFE9E1), card = Color(0xFFFAFAFA), textMain = Color(0xFF2C2A29),
    border = Color(0xFF2C2A29), accent = Color(0xFFFF7B54), accentSub = Color(0xFF4ECDC4),
    btnText = Color(0xFFFAFAFA), grayLight = Color(0xFFDFD8CE), textMuted = Color(0xFF7A7571),
    shadow = Color(0xFF2C2A29)
)

private val RETRO_DARK = NeoPalette(
    key = "retro", label = "📻 复古印刷", isDark = true,
    bg = Color(0xFF2A2421), card = Color(0xFF352E2B), textMain = Color(0xFFEFE9E1),
    border = Color(0xFF5A4E49), accent = Color(0xFFFF7B54), accentSub = Color(0xFF4ECDC4),
    btnText = Color(0xFF2A2421), grayLight = Color(0xFF3D3531), textMuted = Color(0xFF9E8F87),
    shadow = Color(0xFF000000)
)

private val PASTEL_LIGHT = NeoPalette(
    key = "pastel", label = "🍵 柔和薄荷", isDark = false,
    bg = Color(0xFFF0F4F8), card = Color(0xFFFFFFFF), textMain = Color(0xFF2D3748),
    border = Color(0xFF2D3748), accent = Color(0xFF9DECF9), accentSub = Color(0xFFFBB6CE),
    btnText = Color(0xFF2D3748), grayLight = Color(0xFFE2E8F0), textMuted = Color(0xFF718096),
    shadow = Color(0xFF2D3748)
)

private val PASTEL_DARK = NeoPalette(
    key = "pastel", label = "🍵 柔和薄荷", isDark = true,
    bg = Color(0xFF1A202C), card = Color(0xFF2D3748), textMain = Color(0xFFF0F4F8),
    border = Color(0xFF4A5568), accent = Color(0xFF9DECF9), accentSub = Color(0xFFFBB6CE),
    btnText = Color(0xFF1A202C), grayLight = Color(0xFF2A3441), textMuted = Color(0xFFA0AEC0),
    shadow = Color(0xFF000000)
)

val THEME_KEYS = listOf("cyber", "retro", "pastel")

fun paletteFor(themeKey: String, dark: Boolean): NeoPalette = when (themeKey) {
    "retro" -> if (dark) RETRO_DARK else RETRO_LIGHT
    "pastel" -> if (dark) PASTEL_DARK else PASTEL_LIGHT
    else -> if (dark) CYBER_DARK else CYBER_LIGHT
}

/** 主字体：Space Grotesk（500/700，900 与网页一致地回退到最重可用字重） */
val NeoSans = FontFamily(
    Font(R.font.spacegrotesk_medium, FontWeight.W500),
    Font(R.font.spacegrotesk_bold, FontWeight.W700),
    Font(R.font.spacegrotesk_bold, FontWeight.W800),
    Font(R.font.spacegrotesk_bold, FontWeight.W900)
)

/** 等宽字体：JetBrains Mono（小票 / 数值） */
val NeoMono = FontFamily(
    Font(R.font.jetbrainsmono_regular, FontWeight.W400),
    Font(R.font.jetbrainsmono_bold, FontWeight.W700),
    Font(R.font.jetbrainsmono_bold, FontWeight.W900)
)
