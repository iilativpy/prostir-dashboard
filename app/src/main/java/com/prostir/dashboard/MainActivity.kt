package com.prostir.dashboard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.prostir.dashboard.ui.AppRoot
import com.prostir.dashboard.ui.theme.ProstirTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as ProstirApp).container
        setContent {
            ProstirTheme {
                AppRoot(container)
            }
        }
    }
}
