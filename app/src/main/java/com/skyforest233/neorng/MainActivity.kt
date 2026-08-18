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
        enableEdgeToEdge()
        setContent {
            val app = remember { AppState(applicationContext) }
            val view = LocalView.current

            // 状态栏图标颜色跟随应用内深浅模式（enableEdgeToEdge 已在 onCreate 调用）
            LaunchedEffect(app.dark) {
                window?.let { w ->
                    WindowCompat.getInsetsController(w, view).isAppearanceLightStatusBars = !app.dark
                }
            }

            MainScreen(app)
        }
    }
}
