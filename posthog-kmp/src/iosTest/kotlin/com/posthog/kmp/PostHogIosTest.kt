package com.posthog.kmp

import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale
import platform.Foundation.NSTimeZone
import platform.Foundation.timeZoneForSecondsFromGMT
import kotlin.test.Test
import kotlin.test.assertEquals

class PostHogIosTest {

    @Test
    fun testTimestampConversionPreservesUtcInstant() {
        val timestamp = 1_704_164_645_678L

        val date = timestamp.toNSDate()

        val formatter = NSDateFormatter().apply {
            locale = NSLocale(localeIdentifier = "en_US_POSIX")
            timeZone = NSTimeZone.timeZoneForSecondsFromGMT(0)
            dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
        }
        assertEquals("2024-01-02T03:04:05.678Z", formatter.stringFromDate(date))
    }
}
