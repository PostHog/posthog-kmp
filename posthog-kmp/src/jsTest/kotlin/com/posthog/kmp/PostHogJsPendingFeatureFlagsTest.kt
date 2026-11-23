package com.posthog.kmp

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** [PostHog.onFeatureFlags] registrations made before setup, attached once posthog-js loads. */
class PostHogJsPendingFeatureFlagsTest {

    private var fakeJs: dynamic = null

    @BeforeTest
    fun setup() {
        fakeJs = js("{}")
        mockPostHogJs = fakeJs
        currentConfig = null
    }

    @AfterTest
    fun tearDown() {
        mockPostHogJs = null
        currentConfig = null
    }

    @Test
    fun testPendingListenerUnsubscribingDuringAttachKeepsTheOthers() {
        // posthog-js replays cached flags synchronously inside onFeatureFlags
        var nativeUnsubscribes = 0
        fakeJs.onFeatureFlags = { cb: (dynamic, dynamic, dynamic) -> Unit ->
            cb(js("['beta']"), js("({ beta: true })"), undefined)
            val unSub: () -> Unit = { nativeUnsubscribes++ }
            unSub
        }
        fakeJs.init = { _: String, _: dynamic -> fakeJs.__loaded = true }
        lateinit var first: PostHogFeatureFlagsSubscription
        first = PostHog.onFeatureFlags { first.unsubscribe() }
        var secondCalls = 0
        PostHog.onFeatureFlags { secondCalls++ }

        PostHog.setup(PostHogConfig(projectToken = "key"), PostHogContext())

        assertEquals(1, secondCalls)
        assertEquals(1, nativeUnsubscribes, "the self-unsubscribed listener must release its native registration")
    }

    @Test
    fun testPendingListenersWaitWhenInitFails() {
        var attached = 0
        fakeJs.onFeatureFlags = { _: dynamic ->
            attached++
            val unSub: () -> Unit = { }
            unSub
        }
        fakeJs.init = { _: String, _: dynamic -> }
        PostHog.onFeatureFlags { }

        PostHog.setup(PostHogConfig(projectToken = "key"), PostHogContext())
        assertEquals(0, attached)

        fakeJs.init = { _: String, _: dynamic -> fakeJs.__loaded = true }
        PostHog.setup(PostHogConfig(projectToken = "key"), PostHogContext())
        assertEquals(1, attached)
    }
}
