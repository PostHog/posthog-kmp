package com.posthog.kmp.sample

import android.app.Application
import com.posthog.kmp.PostHogContext

/**
 * Owns the [PostHogContext] at the application level.
 *
 * In a real app, call PostHog.setup(config, PostHogContext(this)) here in
 * onCreate so it runs exactly once per process. This sample defers setup to a
 * button in [App] because the API key is entered in the UI.
 */
class SampleApplication : Application() {
    val postHogContext: PostHogContext by lazy { PostHogContext(this) }
}
