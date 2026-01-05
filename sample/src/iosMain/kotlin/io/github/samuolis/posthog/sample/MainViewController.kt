package io.github.samuolis.posthog.sample

import androidx.compose.ui.window.ComposeUIViewController
import io.github.samuolis.posthog.PostHogContext

fun MainViewController() = ComposeUIViewController {
    App(postHogContext = PostHogContext())
}
