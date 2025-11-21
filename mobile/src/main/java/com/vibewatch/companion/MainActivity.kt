package com.vibewatch.companion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vibewatch.companion.ui.MainScreen
import com.vibewatch.companion.ui.SettingsScreen
import com.vibewatch.companion.ui.theme.CustomVibeWatchFacesTheme
import com.vibewatch.companion.viewmodel.WatchFaceViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CustomVibeWatchFacesTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    WatchFaceApp()
                }
            }
        }
    }
}

@Composable
fun WatchFaceApp() {
    val viewModel: WatchFaceViewModel = viewModel()
    var showSettings by remember { mutableStateOf(false) }
    
    if (showSettings) {
        SettingsScreen(
            viewModel = viewModel,
            onNavigateBack = { showSettings = false }
        )
    } else {
        MainScreen(
            viewModel = viewModel,
            onNavigateToSettings = { showSettings = true }
        )
    }
}
