package com.posthog.kmp;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PostHogConfigJavaTest {

    @Test
    public void configConstructorsTakeAnyLeadingArguments() {
        assertEquals(PostHogConfig.HOST_US, new PostHogConfig("phc_test").getHost());

        PostHogConfig config = new PostHogConfig("phc_test", PostHogConfig.HOST_EU, true);
        assertEquals(PostHogConfig.HOST_EU, config.getHost());
        assertTrue(config.getDebug());
        assertTrue(config.getCaptureScreenViews());

        assertFalse(new SessionRecordingConfig(false).getEnabled());
        assertTrue(new ErrorTrackingConfig(true).getAutoCapture());
    }
}
