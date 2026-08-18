package com.skyforest233.neorng

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.skyforest233.neorng.ui.MainScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 上次崩溃？优先显示崩溃报告页（纯 View 实现，不依赖 Compose）
        if (!intent.getBooleanExtra(EXTRA_SKIP_CRASH_REPORT, false)) {
            CrashReporter.consume(this)?.let { report ->
                CrashReporter.showCrashScreen(this, report)
                return
            }
        }
        CrashReporter.install(this)

        enableEdgeToEdge()
        setContent {
            val app = remember { AppState(applicationContext) }
            val view = LocalView.current

            // 状态栏图标颜色跟随应用内深浅模式
            LaunchedEffect(app.dark) {
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !app.dark
            }

            MainScreen(app)
        }
    }

    companion object {
        const val EXTRA_SKIP_CRASH_REPORT = "skip_crash_report"
    }
}
