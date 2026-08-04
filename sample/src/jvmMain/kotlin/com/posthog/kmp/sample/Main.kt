package com.posthog.kmp.sample

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.posthog.kmp.PostHogContext

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "PostHog KMP Sample",
    ) {
        App(PostHogContext())
    }
}
