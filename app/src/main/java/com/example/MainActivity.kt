package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.ui.main.MainScreen
import com.example.ui.splash.VideoSplashScreen
import com.example.ui.viewmodel.IdeViewModel

class MainActivity : ComponentActivity() {
    private val ideViewModel: IdeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var showSplash by remember { mutableStateOf(true) }

            Crossfade(
                targetState = showSplash,
                animationSpec = tween(durationMillis = 600),
                label = "SplashToMainTransition"
            ) { isSplash ->
                if (isSplash) {
                    VideoSplashScreen(
                        onSplashFinished = {
                            showSplash = false
                        }
                    )
                } else {
                    MainScreen(viewModel = ideViewModel)
                }
            }
        }
    }
}

