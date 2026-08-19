package com.skyforest233.neorng

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import kotlin.math.floor
import kotlin.math.min
import kotlin.random.Random

/**
 * 核心随机源引擎：完整移植网页版 fetchRandom() 的三种模式与全部降级容错逻辑。
 *
 * 1. LOCAL RNG      —— 本地 Math.random（Kotlin Random）
 * 2. CF DRAND       —— Cloudflare Drand 联盟随机数 + SHA-256 哈希派生
 * 3. RANDOM.ORG     —— 大气噪声真随机 API
 *
 * 失败/超时(4s)/配额限制时自动降级本地随机并弹出提示，与网页版完全一致。
 */
object RngEngine {

    /** 四种模式（含混合） */
    val MODES = listOf("randomorg", "drand", "local", "hybrid")
    val MODE_LABELS = mapOf(
        "randomorg" to "RANDOM.ORG",
        "drand" to "CF DRAND",
        "local" to "LOCAL RNG",
        "hybrid" to "HYBRID MIX"
    )

    /** 顶部徽章使用的短标签（单行紧凑排版） */
    val MODE_LABELS_SHORT = mapOf(
        "randomorg" to "RNG.ORG",
        "drand" to "DRAND",
        "local" to "LOCAL",
        "hybrid" to "MIX"
    )

    private val client: OkHttpClient = OkHttpClient.Builder()
        .callTimeout(4, TimeUnit.SECONDS)
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(4, TimeUnit.SECONDS)
        .build()

    private var drandLastRound = 0L
    private var drandNonce = 0

    /**
     * 返回 null 表示“生成区间过小，无法满足不重复条件”（对应网页版抛出的 Range too small）。
     * onNotice 对应网页版 showToast 的降级提示。
     */
    suspend fun fetch(
        min: Int,
        max: Int,
        count: Int,
        unique: Boolean,
        mode: String,
        onNotice: (String) -> Unit
    ): List<Int>? {
        if (unique && (max - min + 1 < count)) {
            onNotice("🔴 错误：生成区间过小，无法满足不重复条件！")
            return null
        }

        return when (mode) {
            "local" -> {
                delay(300) // 网页版同样有 300ms 延迟模拟"计算感"
                localRandom(min, max, count, unique, Int.MAX_VALUE)
            }
            "drand" -> fetchDrand(min, max, count, unique, onNotice)
            "hybrid" -> fetchHybrid(min, max, count, unique, onNotice)
            else -> fetchRandomOrg(min, max, count, unique, onNotice)
        }
    }

    /**
     * 混合模式：drand + random.org + 本地随机 三源并行，逐位求和取模混合。
     * 任一远程源失败自动降级为剩余来源（两个都失败则纯本地并提示）。
     */
    private suspend fun fetchHybrid(
        min: Int, max: Int, count: Int, unique: Boolean, onNotice: (String) -> Unit
    ): List<Int> {
        val range = max - min + 1
        val want = if (unique) min(count * 3, 10000) else count

        return kotlinx.coroutines.coroutineScope {
            val orgDeferred = kotlinx.coroutines.async(Dispatchers.IO) {
                runCatching { randomOrgRaw(min, max, want) }.getOrNull()
            }
            val drandDeferred = kotlinx.coroutines.async(Dispatchers.IO) {
                runCatching { drandRaw(min, max, want) }.getOrNull()
            }
            val localPool = List(want) { min + floor(Random.nextDouble() * range).toInt() }
            val org = orgDeferred.await()
            val drand = drandDeferred.await()
            val remotes = listOfNotNull(org, drand)
            if (remotes.isEmpty()) {
                onNotice("远程源均不可用，混合模式临时使用本地随机")
            } else if (org == null || drand == null) {
                onNotice("混合模式: 一个远程源不可用，已用其余来源混合")
            }

            fun mixedAt(i: Int): Int {
                var sum = localPool[i] - min
                for (arr in remotes) sum += (arr[i % arr.size] - min)
                return min + ((sum % range) + range) % range
            }

            if (!unique) {
                List(count) { mixedAt(it) }
            } else {
                val set = LinkedHashSet<Int>()
                var i = 0
                while (set.size < count && i < want) {
                    set.add(mixedAt(i)); i++
                }
                while (set.size < count) {
                    set.add(min + floor(Random.nextDouble() * range).toInt())
                }
                set.toList()
            }
        }
    }

    private fun localRandom(min: Int, max: Int, count: Int, unique: Boolean, attemptCap: Int): List<Int> {
        val range = max - min + 1
        val results = ArrayList<Int>()
        val set = HashSet<Int>()
        var attempts = 0
        while ((if (unique) set.size else results.size) < count && attempts < attemptCap) {
            val num = min + floor(Random.nextDouble() * range).toInt()
            if (unique) set.add(num) else results.add(num)
            attempts++
        }
        return if (unique) set.toList() else results
    }

    /** drand 原始抓取（失败抛异常，无降级） */
    private suspend fun drandRaw(min: Int, max: Int, count: Int): List<Int> {
        val range = max - min + 1
        val body = withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url("https://drand.cloudflare.com/public/latest")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IllegalStateException("Drand API Limit")
                response.body?.string() ?: throw IllegalStateException("Drand empty body")
            }
        }
        val root = JSONObject(body)
        val round = root.getLong("round")
        val hex = root.getString("randomness")

        // 防碰撞：同一轮 beacon 多次请求时增加 nonce 偏移
        if (round == drandLastRound) drandNonce += 100 else { drandLastRound = round; drandNonce = 0 }

        val results = ArrayList<Int>(count)
        var index = 0
        while (results.size < count && index < 100000) {
            val randomFloat = sha256FirstUint32("$hex" + "_" + (drandNonce + index)) / 4294967296.0
            results.add(min + floor(randomFloat * range).toInt())
            index++
        }
        drandNonce += index
        return results
    }

    private suspend fun fetchDrand(
        min: Int, max: Int, count: Int, unique: Boolean, onNotice: (String) -> Unit
    ): List<Int> {
        return try {
            if (unique) {
                val range = max - min + 1
                val nums = drandRaw(min, max, count)
                val set = LinkedHashSet(nums)
                while (set.size < count) {
                    set.add(min + floor(Random.nextDouble() * range).toInt())
                }
                set.toList()
            } else {
                drandRaw(min, max, count)
            }
        } catch (err: Throwable) {
            onNotice("DRAND 限制或非安全环境，临时降级本地随机")
            localRandom(min, max, count, unique, Int.MAX_VALUE)
        }
    }

    /** SHA-256 摘要的前 4 字节按大端序解释为 UInt32（与网页版 Uint32Array(hashBuffer)[0] 一致） */
    private fun sha256FirstUint32(input: String): Long {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        var value = 0L
        for (i in 0 until 4) {
            value = (value shl 8) or (digest[i].toLong() and 0xFF)
        }
        return value
    }

    /** random.org 原始抓取（失败抛异常，无降级） */
    private suspend fun randomOrgRaw(min: Int, max: Int, count: Int): List<Int> {
        val body = withContext(Dispatchers.IO) {
            val url = "https://www.random.org/integers/?num=$count&min=$min&max=$max" +
                "&col=1&base=10&format=plain&rnd=new"
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IllegalStateException("API Limit")
                response.body?.string() ?: throw IllegalStateException("empty body")
            }
        }
        val nums = body.trim().split('\n').mapNotNull { it.trim().toIntOrNull() }
        if (nums.isEmpty()) throw IllegalStateException("Parse Error")
        return nums
    }

    private suspend fun fetchRandomOrg(
        min: Int, max: Int, count: Int, unique: Boolean, onNotice: (String) -> Unit
    ): List<Int> {
        val range = max - min + 1
        return try {
            val reqCount = if (unique) min(count * 3, 10000) else count
            val nums = randomOrgRaw(min, max, reqCount)
            if (unique) {
                val uniqueNums = nums.distinct()
                if (uniqueNums.size >= count) {
                    uniqueNums.take(count)
                } else {
                    onNotice("API 去重后数量不足，部分由本地补齐")
                    val set = LinkedHashSet(uniqueNums)
                    while (set.size < count) set.add(min + floor(Random.nextDouble() * range).toInt())
                    set.toList()
                }
            } else {
                nums.take(count)
            }
        } catch (err: Throwable) {
            onNotice("RANDOM.ORG 限制或超时, 降级本地随机")
            localRandom(min, max, count, unique, Int.MAX_VALUE)
        }
    }
}
