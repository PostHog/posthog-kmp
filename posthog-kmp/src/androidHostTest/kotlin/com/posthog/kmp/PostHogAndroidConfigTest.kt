package com.posthog.kmp

import kotlin.test.Test
import kotlin.test.assertEquals

class PostHogAndroidConfigTest {

    @Test
    fun screenshotMaskAlignmentVerificationDefaultsToDisabled() {
        for (sessionRecording in listOf(null, SessionRecordingConfig())) {
            val nativeConfig = PostHogConfig(
                projectToken = "phc_test",
                sessionRecording = sessionRecording
            ).toAndroidConfig()

            assertEquals(false, nativeConfig.sessionReplayConfig.verifyScreenshotMaskAlignment)
        }
    }

    @Test
    fun mapsScreenshotMaskAlignmentVerification() {
        for (verify in listOf(false, true)) {
            val nativeConfig = PostHogConfig(
                projectToken = "phc_test",
                sessionRecording = SessionRecordingConfig(verifyScreenshotMaskAlignment = verify)
            ).toAndroidConfig()

            assertEquals(true, nativeConfig.sessionReplay)
            assertEquals(verify, nativeConfig.sessionReplayConfig.verifyScreenshotMaskAlignment)
        }
    }

    @Test
    fun pushNotificationCaptureDefaultsMatchNativeSdk() {
        val nativeConfig = PostHogConfig(projectToken = "phc_test").toAndroidConfig()

        assertEquals(true, nativeConfig.capturePushNotificationSubscriptions)
        assertEquals(true, nativeConfig.capturePushNotificationOpened)
    }

    @Test
    fun mapsPushNotificationCaptureOptionsIndependently() {
        for (subscriptions in listOf(false, true)) {
            for (opened in listOf(false, true)) {
                val config = PostHogConfig(
                    projectToken = "phc_test",
                    capturePushNotificationSubscriptions = subscriptions,
                    capturePushNotificationOpened = opened
                )

                val nativeConfig = config.toAndroidConfig()

                assertEquals(subscriptions, nativeConfig.capturePushNotificationSubscriptions)
                assertEquals(opened, nativeConfig.capturePushNotificationOpened)
            }
        }
    }
}
