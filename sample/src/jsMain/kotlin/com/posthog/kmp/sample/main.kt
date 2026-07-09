package com.posthog.kmp.sample

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.posthog.kmp.PostHogContext
import kotlinx.browser.document

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport(viewportContainer = document.body!!) {
        App(postHogContext = PostHogContext())
    }
}
