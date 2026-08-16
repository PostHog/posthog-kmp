@file:OptIn(ExperimentalNativeApi::class)

package com.posthog.kmp

import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.ReportUnhandledExceptionHook
import kotlin.native.getUnhandledExceptionHook
import kotlin.native.setUnhandledExceptionHook
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class KotlinThrowableNSExceptionTest {

    @Test
    fun convertsThrowableToExceptionWithNativeAddresses() {
        val exception = IllegalStateException("checkout failed").toNSException(appendCauses = false)

        assertEquals("kotlin.IllegalStateException", exception.name)
        assertEquals("checkout failed", exception.reason)
        assertTrue(exception.callStackReturnAddresses.isNotEmpty())
    }

    @Test
    fun appendsCauseMessagesAndAddresses() {
        val cause = IllegalArgumentException("invalid cart")
        val throwable = IllegalStateException("checkout failed", cause)

        val exception = throwable.toNSException()

        assertEquals(
            "checkout failed\nCaused by: kotlin.IllegalArgumentException: invalid cart",
            exception.reason
        )
        assertTrue(exception.callStackReturnAddresses.isNotEmpty())
    }

    @Test
    fun discoversCausesWithoutLoopingOnCycles() {
        val cause = MutableCauseThrowable("cause")
        val throwable = Throwable("root", cause)
        cause.mutableCause = throwable

        assertEquals(listOf(cause, throwable), throwable.causes)
    }

    @Test
    fun dropsConstructorAddresses() {
        val addresses = listOf<Long>(0, 1, 2, 3, 4, 5)
        val stackTrace = arrayOf(
            "kfun:kotlin.Throwable#<init>(kotlin.String?){}",
            "kfun:kotlin.Exception#<init>(kotlin.String?){}",
            "kfun:sample.CustomException#<init>(kotlin.String?){}",
            "kfun:sample.CustomException#<init>(){}",
            "kfun:sample.Checkout#submit(){}",
            "kfun:sample.App#main(){}"
        )

        assertEquals(
            listOf<Long>(4, 5),
            addresses.dropConstructorAddresses("sample.CustomException", stackTrace)
        )
        assertEquals(
            listOf<Long>(3, 4, 5),
            addresses.dropConstructorAddresses("sample.CustomException", stackTrace, keepLast = true)
        )
    }

    @Test
    fun keepsAddressesWhenConstructorIsUnknown() {
        val addresses = listOf<Long>(0, 1, 2)
        val stackTrace = arrayOf(
            "kfun:kotlin.Throwable#<init>(){}",
            "kfun:sample.App#main(){}"
        )

        assertSame(addresses, addresses.dropConstructorAddresses("sample.CustomException", stackTrace))
    }

    @Test
    fun dropsOnlySharedTailAddresses() {
        val commonAddresses = listOf<Long>(5, 4, 3, 2, 1, 0)
        val addresses = listOf<Long>(8, 7, 6, 2, 1, 0)

        assertEquals(listOf<Long>(8, 7, 6), addresses.dropCommonAddresses(commonAddresses))
        assertSame(addresses, addresses.dropCommonAddresses(emptyList()))
    }

    @Test
    fun releasesOnlyItsOwnUnhandledExceptionHook() {
        val originalHook = setUnhandledExceptionHook(null)
        try {
            configureUnhandledKotlinExceptionCapture(true)
            val installedHook = assertNotNull(getUnhandledExceptionHook())

            configureUnhandledKotlinExceptionCapture(false)
            assertNull(getUnhandledExceptionHook())

            configureUnhandledKotlinExceptionCapture(true)
            assertSame(installedHook, getUnhandledExceptionHook())

            val replacementHook: ReportUnhandledExceptionHook = { _ -> }
            setUnhandledExceptionHook(replacementHook)
            configureUnhandledKotlinExceptionCapture(false)
            assertSame(replacementHook, getUnhandledExceptionHook())

            configureUnhandledKotlinExceptionCapture(true)
            assertSame(replacementHook, getUnhandledExceptionHook())
        } finally {
            configureUnhandledKotlinExceptionCapture(false)
            setUnhandledExceptionHook(originalHook)
        }
    }

    private class MutableCauseThrowable(override val message: String) : Throwable() {
        var mutableCause: Throwable? = null
        override val cause: Throwable?
            get() = mutableCause
    }
}
