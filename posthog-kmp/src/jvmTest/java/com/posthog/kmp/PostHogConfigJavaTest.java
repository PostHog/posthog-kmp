package com.posthog.kmp;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
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

    @Test
    public void copySignatureWithoutEvaluationContextsRemainsLinkable() throws Exception {
        PostHogConfig config = new PostHogConfig(
            "phc_test", PostHogConfig.HOST_US, false, true, false, true, true, true,
            20, 30, 1000, 50, false, PersonProfiles.IDENTIFIED_ONLY, null, false,
            Collections.emptyList(), null, false, false, Collections.singletonList("checkout"),
            PostHogCompression.GZIP, 100, 60, 8 * 1024 * 1024 + 64 * 1024
        );
        Method copyDefault = PostHogConfig.class.getMethod(
            "copy$default", PostHogConfig.class, String.class, String.class,
            boolean.class, boolean.class, boolean.class, boolean.class, boolean.class, boolean.class,
            int.class, int.class, int.class, int.class, boolean.class, PersonProfiles.class,
            SessionRecordingConfig.class, boolean.class, List.class, ErrorTrackingConfig.class,
            boolean.class, boolean.class, int.class, Object.class
        );
        int allDefaultExceptDebug = 0xFFFFF & ~(1 << 2);
        PostHogConfig copy = (PostHogConfig) copyDefault.invoke(
            null, config, null, null, true, false, false, false, false, false,
            0, 0, 0, 0, false, null, null, false, null, null, false, false,
            allDefaultExceptDebug, null
        );
        assertTrue(copy.getDebug());
        assertFalse(copy.getCapturePushNotificationOpened());
        assertEquals(Collections.singletonList("checkout"), copy.getEvaluationContexts());
    }

    @Test
    public void defaultArgumentConstructorWithoutEvaluationContextsRemainsLinkable() throws Exception {
        PostHogConfig config = PostHogConfig.class.getConstructor(
            String.class, String.class,
            boolean.class, boolean.class, boolean.class, boolean.class, boolean.class, boolean.class,
            int.class, int.class, int.class, int.class, boolean.class, PersonProfiles.class,
            SessionRecordingConfig.class, boolean.class, List.class, ErrorTrackingConfig.class,
            boolean.class, boolean.class, int.class, kotlin.jvm.internal.DefaultConstructorMarker.class
        ).newInstance(
            "phc_test", null, false, false, false, false, false, false,
            0, 0, 0, 0, false, null, null, false, null, null, true, false,
            0xFFFFF & ~(1 | 1 << 19), null
        );
        assertEquals(PostHogConfig.HOST_US, config.getHost());
        assertFalse(config.getCapturePushNotificationOpened());
        assertNull(config.getEvaluationContexts());
    }
}
