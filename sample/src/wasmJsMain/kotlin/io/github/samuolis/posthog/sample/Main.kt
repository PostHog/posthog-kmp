package io.github.samuolis.posthog.sample

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import io.github.samuolis.posthog.PostHogContext

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport(content = {
        App(postHogContext = PostHogContext())
    })
}
