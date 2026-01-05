package com.samuolis.posthog.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.samuolis.posthog.PostHogContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            // Pass the Android Application context to PostHog
            App(postHogContext = PostHogContext(application))
        }
    }
}
