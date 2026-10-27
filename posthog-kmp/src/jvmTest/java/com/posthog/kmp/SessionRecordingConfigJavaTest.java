package com.posthog.kmp;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SessionRecordingConfigJavaTest {

    @Test
    public void existingConstructorSignatureRemainsCallable() {
        SessionRecordingConfig config = new SessionRecordingConfig(
            true, false, true, false, true, true, false, 250L
        );

        assertTrue(config.getEnabled());
        assertFalse(config.getMaskAllTextInputs());
        assertTrue(config.getMaskAllImages());
        assertFalse(config.getCaptureNetworkTelemetry());
        assertTrue(config.getCaptureLogs());
        assertTrue(config.getScreenshot());
        assertFalse(config.getCaptureLogcat());
        assertEquals(250L, config.getDebouncerDelayMs());
        assertFalse(config.getVerifyScreenshotMaskAlignment());
    }

    @Test
    public void existingCopySignaturePreservesVerificationOption() {
        SessionRecordingConfig config = new SessionRecordingConfig(
            true, true, true, true, false, true, true, 1000L, true
        );
        SessionRecordingConfig copy = config.copy(
            true, false, true, false, true, true, false, 250L
        );

        assertFalse(copy.getMaskAllTextInputs());
        assertFalse(copy.getCaptureNetworkTelemetry());
        assertTrue(copy.getCaptureLogs());
        assertFalse(copy.getCaptureLogcat());
        assertEquals(250L, copy.getDebouncerDelayMs());
        assertTrue(copy.getVerifyScreenshotMaskAlignment());
    }
}
