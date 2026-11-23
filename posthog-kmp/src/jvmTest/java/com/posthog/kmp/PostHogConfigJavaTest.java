package com.posthog.kmp;

import kotlin.Unit;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PostHogConfigJavaTest {

    @Test
    public void javaBuildsConfigsThroughTheBuilders() {
        PostHogConfig config = PostHogConfigKt.PostHogConfig("phc_test", builder -> {
            builder.setHost(PostHogConfig.HOST_EU);
            builder.setDebug(true);
            builder.sessionReplay(replay -> {
                replay.setMaskAllImages(false);
                return Unit.INSTANCE;
            });
            return Unit.INSTANCE;
        });

        assertEquals(PostHogConfig.HOST_EU, config.getHost());
        assertTrue(config.getDebug());
        assertFalse(config.getSessionReplay().getMaskAllImages());

        CaptureOptions options = PostHogModelsKt.CaptureOptions(builder -> {
            builder.setTimestamp(42L);
            return Unit.INSTANCE;
        });
        assertEquals(Long.valueOf(42L), options.getTimestamp());
    }
}
