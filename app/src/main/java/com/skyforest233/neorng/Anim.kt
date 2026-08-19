package com.skyforest233.neorng

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * 效果回调接口：动画引擎完成物理步进时触发声音/触感/纸屑/历史记录等副作用，
 * 与网页版 renderLoop 中直接调用的 playSound / fireConfetti / addHistory 对应。
 */
interface Fx {
    fun sound(name: String)
    fun vibrate(pattern: LongArray)
    fun toast(msg: String)
    fun confetti()
    fun emergency(on: Boolean)
    fun addHistory(type: String, result: String, onDone: (() -> Unit)? = null)
    fun runDelayed(ms: Long, action: () -> Unit)
}

enum class AnimState { IDLE, SPINNING, STOPPING }

/**
 * 硬币物理：完整移植网页版 animEngine.coin 的逐帧行为
 * （加速旋转、上抛、每 180° 播放 swoosh、弹簧减速、EDGE 竖立倾斜 -25°）。
 */
class CoinAnim(private val fx: Fx) {
    // UI 读取的字段必须是 Compose 状态，否则动画数值变化不会触发重绘
    var state by mutableStateOf(AnimState.IDLE)
    var angle by mutableStateOf(0f)
    var y by mutableStateOf(0f)
    var rx by mutableStateOf(0f)
    var speed = 0f
    var target = 0f
    private var lastSwooshAngle = 0f
    var isEdge = false
    var resultText by mutableStateOf("")
    var buttonEnabled by mutableStateOf(true)
    /** 结果已就绪，等加速完成后进入停止阶段（保证每次手感一致） */
    private var armedStop = false

    fun launchSpin(resultIsEdge: Boolean, text: String, finalTarget: Float) {
        resultText = text
        isEdge = resultIsEdge
        target = finalTarget
        armedStop = true
    }

    fun beginSpin() {
        buttonEnabled = false
        state = AnimState.SPINNING
        armedStop = false
        lastSwooshAngle = angle
    }

    /** 单帧步进（与 JS renderLoop 中 Coin 分支逐行对应）。返回是否仍在活动。 */
    fun stepOnce() {
        if (state == AnimState.IDLE && abs(rx) <= 0.5f) return

        if (state == AnimState.SPINNING) {
            speed = min(speed + 1.5f, 40f)
            angle += speed
            y = max(y - 4f, -80f)
            if (angle - lastSwooshAngle >= 180f) {
                fx.sound("swoosh")
                lastSwooshAngle += 180f
            }
            // 加速到峰值后才允许进入停止阶段（与网络返回时机解耦）
            if (armedStop && speed >= 40f) {
                state = AnimState.STOPPING
                armedStop = false
            }
        } else if (state == AnimState.STOPPING) {
            val diff = target - angle
            y = min(y + 4f, 0f)
            if (diff > 20f) {
                // 只减速不加速：进入停止阶段时保持当前速度平滑衰减，避免突然加速
                speed = min(speed, max(diff * 0.04f, 2f)).coerceAtLeast(2f)
                angle += speed
            } else {
                speed += diff * 0.2f
                speed *= 0.75f
                angle += speed
                if (abs(diff) < 1.5f && abs(speed) < 1.5f && y == 0f) {
                    angle = target
                    state = AnimState.IDLE
                    if (isEdge) {
                        fx.sound("siren")
                        fx.emergency(true)
                        fx.runDelayed(3000) { fx.emergency(false) }
                    } else {
                        fx.sound("ding")
                        fx.confetti()
                    }
                    buttonEnabled = true
                    fx.addHistory("COIN FLIP", resultText)
                }
            }
        }

        val targetRx = if (isEdge && state == AnimState.IDLE) -25f else 0f
        rx += (targetRx - rx) * 0.1f
        if (abs(targetRx - rx) < 0.5f) rx = targetRx
    }

    val isActive: Boolean get() = state != AnimState.IDLE || abs(rx) > 0.5f
}

data class WheelItem(val text: String, val weight: Int)

/**
 * 转盘物理：移植 animEngine.wheel —— 匀加速、按权重角度缓停、
 * 指针跨越扇区边界播放 tick、停止后弹出结果印章。
 */
class WheelAnim(private val fx: Fx) {
    var state by mutableStateOf(AnimState.IDLE)
    var angle by mutableStateOf(0f)
    var speed = 0f
    var target = 0f
    var resultText by mutableStateOf("")
    var buttonEnabled by mutableStateOf(true)
    var stampVisible by mutableStateOf(false)
    private var lastTickIdx = -1
    private var lastTickAt = 0L
    private var armedStop = false

    /** 停止后由外部（转盘模块）指定是否自动剔除中奖项 */
    var autoRemoveWinner = false

    fun beginSpin() {
        buttonEnabled = false
        state = AnimState.SPINNING
        armedStop = false
        stampVisible = false
    }

    fun launchStop(text: String, finalTarget: Float) {
        resultText = text
        target = finalTarget
        armedStop = true
    }

    fun stepOnce(items: List<WheelItem>) {
        if (state == AnimState.IDLE) return

        if (state == AnimState.SPINNING) {
            speed = min(speed + 0.4f, 25f)
            angle += speed
            // 转到峰值速度后才进入停止阶段，确保每次都是"起转-匀速-滑行-停下"
            if (armedStop && speed >= 25f) {
                state = AnimState.STOPPING
                armedStop = false
            }
        } else if (state == AnimState.STOPPING) {
            val diff = target - angle
            if (diff > 15f) {
                // 只减速不加速：速度上限取当前速度，随剩余角度比例平滑衰减
                speed = min(speed, max(diff * 0.025f, 2.5f)).coerceAtLeast(2.5f)
                angle += speed
            } else {
                speed += diff * 0.15f
                speed *= 0.82f
                angle += speed
                if (abs(diff) < 1.2f && abs(speed) < 1.2f) {
                    angle = target
                    state = AnimState.IDLE
                    fx.sound("ding")
                    fx.confetti()
                    buttonEnabled = true
                    stampVisible = true
                    val winner = resultText
                    val shouldRemove = autoRemoveWinner
                    fx.runDelayed(4000) { stampVisible = false }
                    fx.addHistory("SPIN WHEEL", winner) {
                        if (shouldRemove) fx.runDelayed(400) { onWheelWinnerAutoRemove(winner) }
                    }
                }
            }
        }

        // 指针跨越扇区时 tick（对应网页版按 pointerCanvasAngle 计算当前扇区）
        if (items.isNotEmpty()) {
            val totalWeight = items.sumOf { it.weight }.toFloat()
            if (totalWeight > 0f) {
                val pointerCanvasAngle = ((360f - (angle % 360f)) % 360f + 360f) % 360f
                var currAng = 0f
                var currentIdx = 0
                for (i in items.indices) {
                    val span = items[i].weight / totalWeight * 360f
                    if (pointerCanvasAngle >= currAng && pointerCanvasAngle < currAng + span) {
                        currentIdx = i
                        break
                    }
                    currAng += span
                }
                if (lastTickIdx != currentIdx) {
                    // 高速旋转时节流（55ms），避免声音/震动刷屏
                    val now = android.os.SystemClock.elapsedRealtime()
                    if (now - lastTickAt > 55L) {
                        fx.sound("tick")
                        lastTickAt = now
                    }
                    lastTickIdx = currentIdx
                }
            }
        }
    }

    /** 由 AppState 提供的钩子：抽中剔除 */
    var onWheelWinnerAutoRemove: (String) -> Unit = {}

    fun baseAngle(): Float = floor(angle / 360f) * 360f
}
