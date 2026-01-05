package com.samuolis.posthog.sample

import androidx.compose.ui.window.ComposeUIViewController
import com.samuolis.posthog.PostHogContext

fun MainViewController() = ComposeUIViewController {
    App(postHogContext = PostHogContext())
}
