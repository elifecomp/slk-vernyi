package com.example.slk_verniy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.slk_verniy.ui.main.MainScreen
import com.example.slk_verniy.ui.splash.SplashScreen
import com.example.slk_verniy.ui.theme.SLKVERNIYTheme
import com.example.slk_verniy.worker.PaydayWorker
import com.example.slk_verniy.worker.ShiftReminderWorker

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ShiftReminderWorker.schedule(applicationContext)
        PaydayWorker.schedule(applicationContext)
        setContent {
            SLKVERNIYTheme {
                var showSplash by remember { mutableStateOf(true) }
                if (showSplash) {
                    SplashScreen(
                        onFinished = { showSplash = false },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    MainScreen(modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}
