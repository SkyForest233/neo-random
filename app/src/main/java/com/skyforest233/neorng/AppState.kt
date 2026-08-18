package com.skyforest233.neorng

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

data class HistoryItem(val time: String, val type: String, val res: String)

/**
 * 全局状态中枢：对应网页版所有 localStorage 键、全局变量与动作函数。
 * 持久化格式与网页版 JSON 完全一致，方便两端互迁。
 */
class AppState(private val context: Context) : Fx {

    private val prefs = context.getSharedPreferences("neo_rng", Context.MODE_PRIVATE)
    private val handler = Handler(Looper.getMainLooper())
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // ---------- 设置项（neoTheme / neoDarkMode / neoMute / neoRngMode） ----------
    var theme by mutableStateOf(prefs.getString(KEY_THEME, "cyber") ?: "cyber")
        private set
    var dark by mutableStateOf(prefs.getString(KEY_DARK, "light") == "dark")
        private set
    var muted by mutableStateOf(prefs.getString(KEY_MUTE, "false") == "true")
        private set
    var rngMode by mutableStateOf(
        run {
            var saved = prefs.getString(KEY_RNG_MODE, null)
            if (saved == null && prefs.getString("neoTrueRng", null) == "false") saved = "local"
            if (saved == null) "randomorg" else saved
        }
    )
        private set

    // ---------- 输入状态（neoRngData JSON） ----------
    var coinHead by mutableStateOf("YES")
    var coinTail by mutableStateOf("NO")
    var rngMin by mutableStateOf("1")
    var rngMax by mutableStateOf("100")
    var rngCount by mutableStateOf("1")
    var rngUnique by mutableStateOf(false)
    var wheelInput by mutableStateOf("霸王茶姬 x2\n瑞幸咖啡\n喜茶\n回家做饭 x3")
    var wheelAutoRemove by mutableStateOf(false)

    val history = mutableStateListOf<HistoryItem>()

    // ---------- 运行时状态 ----------
    val coin = CoinAnim(this)
    val wheel = WheelAnim(this)
    val rngResults = mutableStateListOf<Int>()
    var rngButtonEnabled by mutableStateOf(true)
        private set

    var toastMsg by mutableStateOf<String?>(null)
        private set
    private var toastId by mutableIntStateOf(0)
    private var toastRunnable: Runnable? = null

    var confettiTrigger by mutableIntStateOf(0)
        private set

    var emergency by mutableStateOf(false)
        private set

    /** 记录小票日期（对应网页版 recordDate，进入应用时固定） */
    val recordDate: String = run {
        val now = LocalDate.now()
        "${now.year}/${now.monthValue}/${now.dayOfMonth}"
    }

    /** 任意文本框聚焦时为 true（键盘空格触发当前模块时需要排除） */
    var anyInputFocused by mutableStateOf(false)

    init {
        wheel.onWheelWinnerAutoRemove = { winner -> eliminateItem(winner, showToast = true) }
        loadData()
        SoundFx.preload()
        Haptics.init(context)
    }

    // =====================================================
    // 数据加载与保存（与网页版 loadData / saveData 一致）
    // =====================================================

    private fun loadData() {
        val saved = prefs.getString(KEY_DATA, null) ?: return
        runCatching {
            val root = JSONObject(saved)
            root.optString("coinHead").takeIf { it.isNotEmpty() }?.let { coinHead = it }
            root.optString("coinTail").takeIf { it.isNotEmpty() }?.let { coinTail = it }
            root.optString("rngMin").takeIf { it.isNotEmpty() }?.let { rngMin = it }
            root.optString("rngMax").takeIf { it.isNotEmpty() }?.let { rngMax = it }
            root.optString("rngCount").takeIf { it.isNotEmpty() }?.let { rngCount = it }
            if (root.has("rngUnique")) rngUnique = root.getBoolean("rngUnique")
            if (root.has("wheelInput")) wheelInput = root.getString("wheelInput")
            if (root.has("wheelAutoRemove")) wheelAutoRemove = root.getBoolean("wheelAutoRemove")
            if (root.has("history")) {
                val arr = root.getJSONArray("history")
                for (i in 0 until arr.length()) {
                    val h = arr.getJSONObject(i)
                    history.add(HistoryItem(h.getString("time"), h.getString("type"), h.getString("res")))
                }
            }
        }
    }

    fun saveData() {
        val root = JSONObject()
        root.put("coinHead", coinHead)
        root.put("coinTail", coinTail)
        root.put("rngMin", rngMin)
        root.put("rngMax", rngMax)
        root.put("rngCount", rngCount)
        root.put("rngUnique", rngUnique)
        root.put("wheelInput", wheelInput)
        root.put("wheelAutoRemove", wheelAutoRemove)
        // 与网页版一致：存储时把最新在前翻转为最旧在前
        root.put("history", JSONArray().apply {
            for (i in history.indices.reversed()) {
                val h = history[i]
                put(JSONObject().put("time", h.time).put("type", h.type).put("res", h.res))
            }
        })
        prefs.edit().putString(KEY_DATA, root.toString()).apply()
    }

    // =====================================================
    // 设置切换（cycleTheme / toggleDarkMode / toggleMute / toggleMode）
    // =====================================================

    fun cycleTheme() {
        val idx = THEME_KEYS.indexOf(theme)
        theme = THEME_KEYS[(idx + 1) % THEME_KEYS.size]
        prefs.edit().putString(KEY_THEME, theme).apply()
        vibrateIfEnabled(10)
        toast("主题已切换: ${paletteFor(theme, dark).label}")
    }

    fun toggleDarkMode() {
        dark = !dark
        prefs.edit().putString(KEY_DARK, if (dark) "dark" else "light").apply()
        vibrateIfEnabled(10)
        toast(if (dark) "已切换至深色模式" else "已切换至浅色模式")
    }

    fun toggleMute() {
        muted = !muted
        prefs.edit().putString(KEY_MUTE, muted.toString()).apply()
        vibrateIfEnabled(10)
        toast(if (muted) "已全局静音" else "声音与震动已开启")
    }

    fun toggleMode() {
        val idx = RngEngine.MODES.indexOf(rngMode)
        rngMode = RngEngine.MODES[(idx + 1) % RngEngine.MODES.size]
        prefs.edit().putString(KEY_RNG_MODE, rngMode).apply()
        vibrateIfEnabled(10)
        // 与网页版 updateRngVisuals 一致：远程模式最多 200 个
        val c = rngCount.toIntOrNull() ?: 1
        if (rngMode != "local" && c > 200) rngCount = "200"
    }

    // =====================================================
    // Fx 实现：声音 / 震动 / Toast / 纸屑 / 警报 / 历史
    // =====================================================

    override fun sound(name: String) {
        if (muted) return
        SoundFx.play(name)
        // 网页版 playSound 内部附带的震动
        when (name) {
            "tick" -> Haptics.vibrate(10)
            "siren" -> Haptics.vibrate(longArrayOf(100, 100, 100, 100, 100))
        }
    }

    override fun vibrate(pattern: LongArray) {
        if (muted) return
        Haptics.vibrate(pattern)
    }

    private fun vibrateIfEnabled(ms: Long) {
        if (muted) return
        Haptics.vibrate(ms)
    }

    override fun toast(msg: String) {
        toastMsg = msg
        toastId++
        toastRunnable?.let { handler.removeCallbacks(it) }
        val r = Runnable { toastMsg = null }
        toastRunnable = r
        handler.postDelayed(r, 3000)
    }

    override fun confetti() {
        confettiTrigger++
    }

    override fun emergency(on: Boolean) {
        emergency = on
    }

    override fun addHistory(type: String, result: String, onDone: (() -> Unit)?) {
        val time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
        history.add(0, HistoryItem(time, type, result))
        while (history.size > 30) history.removeAt(history.size - 1)
        saveData()
        onDone?.invoke()
    }

    override fun runDelayed(ms: Long, action: () -> Unit) {
        handler.postDelayed(action, ms)
    }

    // =====================================================
    // 小票：撕毁 / 分享（tearReceipt / shareReceipt）
    // =====================================================

    var tearing by mutableStateOf(false)
        private set

    fun tearReceipt() {
        if (history.isEmpty()) {
            toast("小票已是空白")
            return
        }
        vibrate(longArrayOf(50, 50, 50))
        sound("tear")
        tearing = true
        runDelayed(800) {
            history.clear()
            tearing = false
            receiptFlipped = false
            saveData()
        }
    }

    var receiptFlipped by mutableStateOf(false)

    fun flipReceipt(toBack: Boolean) {
        vibrateIfEnabled(10)
        sound("swoosh")
        receiptFlipped = toBack
    }

    fun shareReceipt() {
        if (history.isEmpty()) {
            toast("暂无记录可分享")
            return
        }
        val sb = StringBuilder()
        sb.append("🧾 命运小票 (NEO RNG)\n")
        sb.append("=".repeat(20)).append('\n')
        for (h in history) {
            sb.append('[').append(h.time).append("] ").append(h.type).append('\n')
            sb.append("👉 结果: ").append(h.res).append('\n')
            sb.append("-".repeat(20)).append('\n')
        }
        sb.append("Powered by Math & True RNG")
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, sb.toString())
            putExtra(Intent.EXTRA_TITLE, "NEO RNG 小票记录")
        }
        runCatching {
            context.startActivity(Intent.createChooser(intent, null))
        }
    }

    // =====================================================
    // 转盘：选项解析 / 抽中剔除（drawWheel 的解析部分 + eliminateItem）
    // =====================================================

    private val wheelLineRegex = Regex("(.*?)(?:\\s*[x*]\\s*(\\d+))?$")

    fun parseWheelItems(input: String): List<WheelItem> {
        val items = input.split('\n')
            .filter { it.isNotBlank() }
            .map { line ->
                val m = wheelLineRegex.find(line)
                val text = m?.groupValues?.get(1)?.trim().orEmpty()
                val weight = m?.groupValues?.get(2)?.toIntOrNull() ?: 1
                WheelItem(text, weight)
            }
        return if (items.isEmpty()) listOf(WheelItem("EMPTY", 1)) else items
    }

    fun eliminateItem(itemText: String, showToast: Boolean) {
        val lines = wheelInput.split('\n')
        val newLines = lines.filter { line ->
            val m = wheelLineRegex.find(line)
            (m?.groupValues?.get(1)?.trim() ?: line.trim()) != itemText
        }
        wheelInput = newLines.joinToString("\n")
        saveData()
        vibrate(longArrayOf(20, 20))
        if (showToast) toast("已自动剔除 [$itemText]") else toastMsg = null
    }

    // =====================================================
    // 三大模块动作（executeCoinFlip / executeRNG / executeWheel）
    // =====================================================

    fun executeCoinFlip() {
        if (coin.state != AnimState.IDLE) return
        coin.beginSpin()
        scope.launch {
            val nums = RngEngine.fetch(1, 1000, 1, false, rngMode) { toast(it) } ?: return@launch
            val n = nums.firstOrNull() ?: return@launch
            val resultType = when {
                n <= 495 -> "heads"
                n <= 990 -> "tails"
                else -> "edge"
            }
            if (resultType == "edge") {
                coin.launchSpin(true, "[SYSTEM ERROR: EDGE LANDING]", coin.baseAngle360() + 6 * 360 + 90f)
            } else {
                val text = if (resultType == "heads") coinHead else coinTail
                coin.launchSpin(false, text, coin.baseAngle360() + 6 * 360 + (if (resultType == "heads") 0f else 180f))
            }
        }
    }

    private fun CoinAnim.baseAngle360(): Float = kotlin.math.floor(angle / 360f) * 360f

    fun executeRng() {
        var minV = rngMin.toIntOrNull() ?: 1
        var maxV = rngMax.toIntOrNull() ?: 100
        var count = rngCount.toIntOrNull() ?: 1
        if (minV > maxV) { val t = minV; minV = maxV; maxV = t }
        if (rngMode != "local" && count > 200) count = 200
        if (count < 1) count = 1

        rngButtonEnabled = false
        rngResults.clear()
        scope.launch {
            val nums = RngEngine.fetch(minV, maxV, count, rngUnique, rngMode) { toast(it) }
            if (nums != null) {
                // 与网页版 setTimeout(idx * 100) 一致的逐个弹出节奏
                for ((idx, num) in nums.withIndex()) {
                    if (idx > 0) delay(100)
                    rngResults.add(num)
                    sound("tick")
                    vibrateIfEnabled(10)
                }
                delay(100)
                rngButtonEnabled = true
                sound("ding")
                confetti()
                addHistory("GENERATOR", "[$minV-$maxV] → ${nums.joinToString(", ")}")
            } else {
                rngButtonEnabled = true
            }
        }
    }

    fun executeWheel() {
        val items = parseWheelItems(wheelInput)
        if (wheel.state != AnimState.IDLE || items.isEmpty()) return
        wheel.autoRemoveWinner = wheelAutoRemove
        wheel.beginSpin()
        scope.launch {
            val totalWeight = items.sumOf { it.weight }
            val nums = RngEngine.fetch(1, totalWeight, 1, false, rngMode) { toast(it) } ?: return@launch
            val targetWeight = nums.firstOrNull() ?: return@launch

            var currentSum = 0
            var winIdx = 0
            var startAng = 0f
            var sliceAng = 0f
            for (i in items.indices) {
                currentSum += items[i].weight
                sliceAng = items[i].weight / totalWeight.toFloat() * 360f
                if (targetWeight <= currentSum) { winIdx = i; break }
                startAng += sliceAng
            }

            wheel.launchStop(items[winIdx].text, wheel.baseAngle() + 8 * 360f + (360f - (startAng + sliceAng / 2f)))
        }
    }

    companion object {
        private const val KEY_THEME = "neoTheme"
        private const val KEY_DARK = "neoDarkMode"
        private const val KEY_MUTE = "neoMute"
        private const val KEY_RNG_MODE = "neoRngMode"
        private const val KEY_DATA = "neoRngData"
    }
}
