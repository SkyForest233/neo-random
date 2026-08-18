package com.skyforest233.neorng.ui

import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skyforest233.neorng.AppState
import com.skyforest233.neorng.NeoMono
import com.skyforest233.neorng.NeoPalette

/**
 * 🔢 数字生成器：MIN/MAX/COUNT 输入、绝对唯一开关、
 * 结果药丸逐个弹出（popIn 回弹动画 + tabular-nums 等宽数字）。
 */
@Composable
fun RngModule(app: AppState, palette: NeoPalette) {
    var reportedZero by remember { androidx.compose.runtime.mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxWidth()
            .onGloballyPositioned { coords ->
                // 诊断：如果面板被测量成零尺寸，弹出提示帮助定位布局问题
                if (!reportedZero && (coords.size.width == 0 || coords.size.height == 0)) {
                    reportedZero = true
                    app.toast("诊断: RNG面板尺寸异常 ${coords.size.width}x${coords.size.height}")
                }
            }
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                NeoText("MIN", color = palette.textMain, fontSize = 13.sp, fontWeight = FontWeight.W900)
                NeoTextField(
                    value = app.rngMin,
                    onValueChange = { app.rngMin = it; app.saveData() },
                    palette = palette,
                    numeric = true
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                NeoText("MAX", color = palette.textMain, fontSize = 13.sp, fontWeight = FontWeight.W900)
                NeoTextField(
                    value = app.rngMax,
                    onValueChange = { app.rngMax = it; app.saveData() },
                    palette = palette,
                    numeric = true
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NeoText("COUNT", color = palette.textMain, fontSize = 13.sp, fontWeight = FontWeight.W900)
                    SmallBadge("MAX 200", palette)
                }
                NeoTextField(
                    value = app.rngCount,
                    onValueChange = { app.rngCount = it; app.saveData() },
                    palette = palette,
                    numeric = true
                )
            }
        }

        // 绝对唯一开关右对齐
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            BrutalCheckbox(
                label = "绝对唯一 (Unique)",
                checked = app.rngUnique,
                palette = palette,
                onCheckedChange = {
                    app.rngUnique = it
                    app.saveData()
                }
            )
        }

        ActionButton(
            text = "GENERATE NUMBERS",
            enabled = app.rngButtonEnabled,
            palette = palette,
            modifier = Modifier.padding(top = 20.dp)
        ) { app.executeRng() }

        // 结果药丸（.rng-number，popIn 逐个出现）—— 每行 4 个手动换行，稳定可靠
        Column(
            Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            app.rngResults.chunked(4).forEach { rowNums ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowNums.forEach { num ->
                        Box(Modifier.weight(1f)) {
                            PopInPill(num.toString(), palette)
                        }
                    }
                    repeat(4 - rowNums.size) {
                        Box(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

/** 单个结果药丸：cubic-bezier 回弹式 popIn 动画（Animatable 驱动，稳定可靠） */
@Composable
private fun PopInPill(text: String, palette: NeoPalette) {
    val progress = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(300, easing = NeoOvershootEasing))
    }
    NeoSurface(
        palette = palette,
        radius = 100.dp,
        borderWidth = 2.5.dp,
        shadowDx = 2.dp,
        shadowDy = 2.dp,
        modifier = Modifier.graphicsLayer {
            scaleX = progress.value
            scaleY = progress.value
            alpha = progress.value
        }
    ) {
        androidx.compose.foundation.text.BasicText(
            text = text,
            style = TextStyle(
                color = palette.textMain,
                fontSize = 20.sp,
                fontWeight = FontWeight.W900,
                fontFamily = NeoMono,
                fontFeatureSettings = "tnum"
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
        )
    }
}
