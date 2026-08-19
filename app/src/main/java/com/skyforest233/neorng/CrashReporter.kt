package com.skyforest233.neorng

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.system.exitProcess

/**
 * 崩溃捕获器：任何未处理异常（包括 Compose 组合期异常）写入文件并结束进程；
 * 下次启动时优先显示纯原生（不依赖 Compose）的崩溃报告页，可一键复制反馈。
 */
object CrashReporter {
    private const val FILE = "last_crash.txt"

    fun install(context: Context) {
        val appCtx = context.applicationContext
        Thread.setDefaultUncaughtExceptionHandler { _, e ->
            runCatching {
                val sw = StringWriter()
                e.printStackTrace(PrintWriter(sw))
                appCtx.openFileOutput(FILE, Context.MODE_PRIVATE).use {
                    it.write(sw.toString().toByteArray(Charsets.UTF_8))
                }
            }
            android.os.Process.killProcess(android.os.Process.myPid())
            exitProcess(10)
        }
    }

    fun consume(context: Context): String? {
        return runCatching {
            val f = context.getFileStreamPath(FILE) ?: return null
            if (!f.exists()) return null
            val txt = f.readText()
            f.delete()
            txt.ifBlank { null }
        }.getOrNull()
    }

    /** 纯 View 实现的崩溃报告页（不经过 Compose，保证一定能渲染出来） */
    fun showCrashScreen(activity: MainActivity, report: String) {
        val ctx = activity
        val dp = { v: Int -> (v * ctx.resources.displayMetrics.density).toInt() }
        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF1A0000.toInt())
            setPadding(dp(16), dp(40), dp(16), dp(16))
        }

        root.addView(TextView(ctx).apply {
            text = "⚠️ NEO RNG 崩溃报告"
            textSize = 18f
            setTextColor(0xFFFF3366.toInt())
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 0, 0, dp(8))
        })
        root.addView(TextView(ctx).apply {
            text = "抱歉，应用上次异常退出。请截图或复制以下内容反馈给开发者："
            textSize = 13f
            setTextColor(0xFFBBBBBB.toInt())
            setPadding(0, 0, 0, dp(12))
        })

        val scroll = ScrollView(ctx)
        val trace = TextView(ctx).apply {
            text = report
            textSize = 11f
            setTextColor(0xFFEEEEEE.toInt())
            setTextIsSelectable(true)
        }
        scroll.addView(trace)
        root.addView(scroll, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        ))

        val copyBtn = Button(ctx).apply {
            text = "📋 复制报告"
            setOnClickListener {
                val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("crash", report))
                Toast.makeText(ctx, "已复制", Toast.LENGTH_SHORT).show()
            }
        }
        root.addView(copyBtn, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(8) })

        root.addView(Button(ctx).apply {
            text = "▶ 仍要尝试进入应用"
            setOnClickListener {
                ctx.startActivity(
                    Intent(ctx, MainActivity::class.java)
                        .putExtra(MainActivity.EXTRA_SKIP_CRASH_REPORT, true)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
                ctx.finish()
            }
        }, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(8) })

        activity.setContentView(root)
    }
}
