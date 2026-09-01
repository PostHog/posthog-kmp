@file:OptIn(BetaInteropApi::class, ExperimentalForeignApi::class)

package com.posthog.kmp

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDictionary
import platform.Foundation.NSJSONSerialization
import platform.Foundation.NSMutableArray
import platform.Foundation.NSNull
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.addObjectsFromArray
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Foundation.dictionaryWithObjects

/**
 * Kotlin/Native bridges a boxed Kotlin [Boolean] as `KotlinBoolean`, an `NSNumber` subclass that is
 * not one of the `CFBoolean` singletons. `JSONSerialization` writes `true`/`false` only for
 * `CFBoolean` and serializes every other `NSNumber` numerically, so a boolean property handed to the
 * native SDK from Kotlin reaches PostHog as `1`/`0` while the same common code on Android sends
 * `true`/`false`.
 *
 * A `CFBoolean` cannot be held in Kotlin: `NSNumber.numberWithBool`, `kCFBooleanTrue` and a boolean
 * parsed out of JSON are all converted back to a Kotlin [Boolean] the moment they cross into Kotlin,
 * and are re-boxed as `KotlinBoolean` on the way out again. The containers are therefore assembled on
 * the Objective-C side from [trueBox]/[falseBox] and the boolean values are never read back.
 */
private val trueBox: List<*>? = parseJsonArray("[true]")
private val falseBox: List<*>? = parseJsonArray("[false]")

private fun parseJsonArray(literal: String): List<*>? {
    val data = NSString.create(string = literal).dataUsingEncoding(NSUTF8StringEncoding) ?: return null
    return NSJSONSerialization.JSONObjectWithData(data, 0uL, null) as? List<*>
}

/**
 * Nesting deeper than this is handed to the native SDK as-is rather than rebuilt. Real payloads are
 * tens of levels deep at most, so the only things this stops are pathological ones — a self
 * referential property value would otherwise recurse until the stack runs out, where the native
 * SDK's own `JSONSerialization` validation drops it and logs.
 */
private const val MAX_DEPTH = 64

/**
 * Rebuilds the property map as an Objective-C dictionary in which Kotlin booleans, including those
 * nested in maps and lists, are real `CFBoolean`s. All other values are passed through unchanged.
 */
internal fun Map<String, Any?>.toNativeProperties(): Map<Any?, *> = toNativeDictionary(0)

private fun Map<*, *>.toNativeDictionary(depth: Int): Map<Any?, *> {
    val keys = ArrayList<Any?>(size)
    val values = NSMutableArray()
    for ((key, value) in this) {
        keys.add(key)
        values.appendNative(value, depth)
    }
    return NSDictionary.dictionaryWithObjects(values.copy() as List<*>, forKeys = keys)
}

private fun List<*>.toNativeArray(depth: Int): List<*> {
    val values = NSMutableArray()
    for (value in this) {
        values.appendNative(value, depth)
    }
    return values.copy() as List<*>
}

private fun NSMutableArray.appendNative(value: Any?, depth: Int) {
    when {
        value == null -> addObject(NSNull())
        value is Boolean -> {
            // Copying out of a Foundation-built array keeps the CFBoolean singleton; falling back to
            // the boxed Kotlin value only restores the 1/0 behaviour, it never fails.
            val box = if (value) trueBox else falseBox
            if (box != null) addObjectsFromArray(box) else addObject(value)
        }
        depth >= MAX_DEPTH -> addObject(value)
        value is Map<*, *> -> addObject(value.toNativeDictionary(depth + 1))
        value is List<*> -> addObject(value.toNativeArray(depth + 1))
        else -> addObject(value)
    }
}
