package com.posthog.kmp

import kotlin.test.Test
import kotlin.test.assertEquals

class PostHogAndroidConfigTest {

    @Test
    fun screenshotMaskAlignmentVerificationDefaultsToDisabled() {
        for (sessionReplay in listOf(null, SessionReplayConfig())) {
            val nativeConfig = PostHogConfig("phc_test") { sessionReplayConfig = sessionReplay }.toAndroidConfig()

            assertEquals(false, nativeConfig.sessionReplayConfig.verifyScreenshotMaskAlignment)
        }
    }

    @Test
    fun mapsScreenshotMaskAlignmentVerification() {
        for (verify in listOf(false, true)) {
            val nativeConfig = PostHogConfig("phc_test") { sessionReplay { verifyScreenshotMaskAlignment = verify } }.toAndroidConfig()

            assertEquals(true, nativeConfig.sessionReplay)
            assertEquals(verify, nativeConfig.sessionReplayConfig.verifyScreenshotMaskAlignment)
        }
    }

    @Test
    fun pushNotificationCaptureDefaultsMatchNativeSdk() {
        val nativeConfig = PostHogConfig("phc_test").toAndroidConfig()

        assertEquals(true, nativeConfig.capturePushNotificationSubscriptions)
        assertEquals(true, nativeConfig.capturePushNotificationOpened)
    }

    @Test
    fun mapsPushNotificationCaptureOptionsIndependently() {
        for (subscriptions in listOf(false, true)) {
            for (opened in listOf(false, true)) {
                val config = PostHogConfig("phc_test") {
                    capturePushNotificationSubscriptions = subscriptions
                    capturePushNotificationOpened = opened
                }

                val nativeConfig = config.toAndroidConfig()

                assertEquals(subscriptions, nativeConfig.capturePushNotificationSubscriptions)
                assertEquals(opened, nativeConfig.capturePushNotificationOpened)
            }
        }
    }

    @Test
    fun mapsEvaluationContexts() {
        assertEquals(null, PostHogConfig("phc_test").toAndroidConfig().evaluationContexts)
        assertEquals(
            listOf("production", "checkout"),
            PostHogConfig("phc_test") { evaluationContexts = listOf("production", "checkout") }
                .toAndroidConfig().evaluationContexts
        )
    }

    @Test
    fun mapsSurveys() {
        for (surveys in listOf(false, true)) {
            val nativeConfig = PostHogConfig("phc_test") { this.surveys = surveys }.toAndroidConfig()

            assertEquals(surveys, nativeConfig.surveys)
        }
    }
}
