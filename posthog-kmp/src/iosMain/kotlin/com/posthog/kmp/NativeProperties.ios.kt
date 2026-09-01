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
 * not one of the `CFBoolean` singletons, and `JSONSerialization` writes `true`/`false` only for
 * `CFBoolean`. Boolean properties therefore reach PostHog as `1`/`0` from iOS while the same common
 * code on Android sends `true`/`false`.
 *
 * A `CFBoolean` cannot be held in Kotlin: `NSNumber.numberWithBool`, `kCFBooleanTrue` and a boolean
 * parsed out of JSON all convert back to a Kotlin [Boolean] on arrival and re-box on the way out.
 * The containers are assembled on the Objective-C side instead, and the values are never read back.
 */
private val trueBox: List<*>? = parseJsonArray("[true]")
private val falseBox: List<*>? = parseJsonArray("[false]")

private fun parseJsonArray(literal: String): List<*>? {
    val data = NSString.create(string = literal).dataUsingEncoding(NSUTF8StringEncoding) ?: return null
    return NSJSONSerialization.JSONObjectWithData(data, 0uL, null) as? List<*>
}

/**
 * Matches the deepest structure `JSONSerialization` accepts: past it the payload is already unusable
 * to the native SDK, and stopping any earlier would leave booleans below the bound as `1`/`0`.
 * Without a bound, a self-referential property value recurses until the stack runs out.
 */
private const val MAX_DEPTH = 512

/**
 * Rebuilds the map so Kotlin booleans, including those nested in maps and lists, become real
 * `CFBoolean`s. Every other value is passed through untouched.
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
            // The fallback only restores the 1/0 behaviour; it never fails.
            val box = if (value) trueBox else falseBox
            if (box != null) addObjectsFromArray(box) else addObject(value)
        }
        depth >= MAX_DEPTH -> addObject(value)
        value is Map<*, *> -> addObject(value.toNativeDictionary(depth + 1))
        value is List<*> -> addObject(value.toNativeArray(depth + 1))
        else -> addObject(value)
    }
}
