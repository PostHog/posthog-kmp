package com.posthog.kmp.sample

import androidx.compose.ui.window.ComposeUIViewController
import com.posthog.kmp.PostHogContext

fun MainViewController() = ComposeUIViewController {
    App(postHogContext = PostHogContext())
}
