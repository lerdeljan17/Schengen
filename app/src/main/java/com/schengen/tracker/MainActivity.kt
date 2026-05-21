package com.schengen.tracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.schengen.tracker.ui.navigation.AppNavigation
import com.schengen.tracker.ui.theme.SchengenTrackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SchengenTrackerTheme {
                AppNavigation()
            }
        }
    }
}
