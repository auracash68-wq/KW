package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.ksdownloader.KSDownloaderBridge
import com.example.ui.screens.KuaiVideoSaveApp
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        KSDownloaderBridge.init(applicationContext)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                KuaiVideoSaveApp()
            }
        }
    }
}
