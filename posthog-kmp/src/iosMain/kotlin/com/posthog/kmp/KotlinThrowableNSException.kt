/*
 * Stack-address normalization adapted from NSExceptionKt:
 * https://github.com/rickclephas/NSExceptionKt
 *
 * MIT License
 *
 * Copyright (c) 2022 Rick Clephas
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

@file:OptIn(ExperimentalForeignApi::class, ExperimentalNativeApi::class, UnsafeNumber::class)

package com.posthog.kmp

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UnsafeNumber
import kotlinx.cinterop.convert
import platform.Foundation.NSException
import platform.Foundation.NSLock
import platform.Foundation.NSLog
import platform.Foundation.NSNumber
import platform.darwin.NSUInteger
import kotlin.concurrent.AtomicInt
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.ReportUnhandledExceptionHook
import kotlin.native.getStackTraceAddresses
import kotlin.native.setUnhandledExceptionHook
import kotlin.native.terminateWithUnhandledException

internal fun Throwable.toNSException(appendCauses: Boolean = true): NSException {
    val addresses = filteredStackTraceAddresses().let { rootAddresses ->
        if (!appendCauses) return@let rootAddresses
        buildList {
            addAll(rootAddresses)
            for (cause in causes) {
                addAll(cause.filteredStackTraceAddresses(keepLastConstructor = true, commonAddresses = rootAddresses))
            }
        }
    }.map { NSNumber(unsignedInteger = it.convert<NSUInteger>()) }

    return KotlinThrowableNSException(
        name = this::class.qualifiedName ?: this::class.simpleName ?: "Throwable",
        reason = reasonWithCauses(appendCauses),
        returnAddresses = addresses
    )
}

private fun Throwable.reasonWithCauses(appendCauses: Boolean): String? {
    if (!appendCauses) return message
    return buildString {
        message?.let(::append)
        for (cause in causes) {
            if (isNotEmpty()) appendLine()
            append("Caused by: ")
            append(cause::class.qualifiedName ?: cause::class.simpleName ?: "Throwable")
            cause.message?.let { append(": $it") }
        }
    }.takeIf { it.isNotEmpty() }
}

internal val Throwable.causes: List<Throwable>
    get() = buildList {
        val visited = mutableSetOf<Throwable>()
        var current = cause
        while (current != null && visited.add(current)) {
            add(current)
            current = current.cause
        }
    }

internal fun Throwable.filteredStackTraceAddresses(
    keepLastConstructor: Boolean = false,
    commonAddresses: List<Long> = emptyList()
): List<Long> = getStackTraceAddresses()
    .dropConstructorAddresses(
        qualifiedClassName = this::class.qualifiedName ?: Throwable::class.qualifiedName!!,
        stackTrace = getStackTrace(),
        keepLast = keepLastConstructor
    )
    .dropCommonAddresses(commonAddresses)

internal fun List<Long>.dropConstructorAddresses(
    qualifiedClassName: String,
    stackTrace: Array<String>,
    keepLast: Boolean = false
): List<Long> {
    val constructorName = "kfun:$qualifiedClassName#<init>"
    var dropCount = 0
    var foundConstructor = false

    for (index in stackTrace.indices) {
        if (stackTrace[index].contains(constructorName)) {
            foundConstructor = true
        } else if (foundConstructor) {
            dropCount = index
            break
        }
    }

    if (keepLast && dropCount > 0) dropCount--
    return if (dropCount == 0) this else drop(dropCount)
}

internal fun List<Long>.dropCommonAddresses(commonAddresses: List<Long>): List<Long> {
    var index = commonAddresses.size
    if (index == 0) return this
    return dropLastWhile { index-- > 0 && commonAddresses[index] == it }
}

private class KotlinThrowableNSException(
    name: String,
    reason: String?,
    private val returnAddresses: List<NSNumber>
) : NSException(name, reason, null) {
    override fun callStackReturnAddresses(): List<NSNumber> = returnAddresses
}

private val captureUnhandledExceptions = AtomicInt(0)
private val installedUnhandledExceptionHook = AtomicInt(0)
private val unhandledExceptionHookLock = NSLock()
private val unhandledKotlinExceptionHook: ReportUnhandledExceptionHook = { throwable ->
    if (captureUnhandledExceptions.value == 1) {
        // Raising lets the native crash reporter own capture, metadata, remote configuration, and termination.
        throwable.toNSException().raise()
    }
    terminateWithUnhandledException(throwable)
}

internal fun configureUnhandledKotlinExceptionCapture(enabled: Boolean, debug: Boolean = false) {
    unhandledExceptionHookLock.lock()
    try {
        captureUnhandledExceptions.value = if (enabled) 1 else 0
        if (enabled) {
            installUnhandledKotlinExceptionHook(debug)
        } else {
            uninstallUnhandledKotlinExceptionHook()
        }
    } finally {
        unhandledExceptionHookLock.unlock()
    }
}

private fun installUnhandledKotlinExceptionHook(debug: Boolean) {
    if (!installedUnhandledExceptionHook.compareAndSet(0, 1)) return

    val previousHook = setUnhandledExceptionHook(unhandledKotlinExceptionHook)
    if (previousHook != null) {
        // Raising is non-returning, so preserve an existing hook rather than installing an unchainable wrapper.
        setUnhandledExceptionHook(previousHook)
        installedUnhandledExceptionHook.value = 0
        if (debug) {
            NSLog("[PostHog] Kotlin crash normalization was not installed because another unhandled-exception hook is active.")
        }
    }
}

private fun uninstallUnhandledKotlinExceptionHook() {
    if (!installedUnhandledExceptionHook.compareAndSet(1, 0)) return

    val removedHook = setUnhandledExceptionHook(null)
    if (removedHook !== unhandledKotlinExceptionHook) {
        // The hook was replaced after installation; put the current owner back.
        setUnhandledExceptionHook(removedHook)
    }
}
