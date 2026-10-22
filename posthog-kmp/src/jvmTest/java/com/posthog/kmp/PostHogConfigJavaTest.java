package com.posthog.kmp;

import java.util.Collections;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PostHogConfigJavaTest {

    @Test
    public void existingConstructorAndCompanionSignaturesRemainCallable() {
        PostHogConfig config = new PostHogConfig(
            "phc_test", PostHogConfig.HOST_US, false, true, false, true, true, true,
            20, 30, 1000, 50, false, PersonProfiles.IDENTIFIED_ONLY, null, false,
            Collections.emptyList(), null
        );
        assertTrue(config.getCapturePushNotificationSubscriptions());
        assertTrue(config.getCapturePushNotificationOpened());

        PostHogConfig legacy = PostHogConfig.Companion.invoke(
            "phc_test", PostHogConfig.HOST_US, false, true, false, true, true, true,
            20, 30, 1000, 50, false, PersonProfiles.IDENTIFIED_ONLY, null, false,
            Collections.emptyList(), null
        );
        assertTrue(legacy.getCapturePushNotificationSubscriptions());
        assertTrue(legacy.getCapturePushNotificationOpened());
    }

    @Test
    public void existingCopySignaturePreservesPushCaptureOptions() {
        PostHogConfig config = new PostHogConfig(
            "phc_test", PostHogConfig.HOST_US, false, true, false, true, true, true,
            20, 30, 1000, 50, false, PersonProfiles.IDENTIFIED_ONLY, null, false,
            Collections.emptyList(), null, false, false
        );
        PostHogConfig copy = config.copy(
            "phc_test", PostHogConfig.HOST_US, true, true, false, true, true, true,
            20, 30, 1000, 50, false, PersonProfiles.IDENTIFIED_ONLY, null, false,
            Collections.emptyList(), null
        );
        assertTrue(copy.getDebug());
        assertFalse(copy.getCapturePushNotificationSubscriptions());
        assertFalse(copy.getCapturePushNotificationOpened());
    }
}
