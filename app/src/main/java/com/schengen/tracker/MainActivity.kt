package com.schengen.tracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.schengen.tracker.ui.screens.SchengenScreen
import com.schengen.tracker.ui.theme.SchengenTrackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SchengenTrackerTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SchengenScreen()
                }
            }
        }
    }
}
