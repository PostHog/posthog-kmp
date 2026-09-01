@file:OptIn(BetaInteropApi::class, ExperimentalForeignApi::class)

package com.posthog.kmp

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSJSONSerialization
import platform.Foundation.NSJSONWritingSortedKeys
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 * The native SDK serializes event properties with `JSONSerialization`, so these tests assert on the
 * JSON text it would produce rather than on the values, which Kotlin converts back on the way in.
 */
class NativePropertiesTest {

    private fun json(value: Any): String {
        val data = NSJSONSerialization.dataWithJSONObject(value, NSJSONWritingSortedKeys, null)!!
        return NSString.create(data, NSUTF8StringEncoding).toString()
    }

    private fun parse(literal: String): Any =
        NSJSONSerialization.JSONObjectWithData(
            NSString.create(string = literal).dataUsingEncoding(NSUTF8StringEncoding)!!,
            0uL,
            null
        )!!

    @Test
    fun testBooleansSerializeAsJsonBooleans() {
        assertEquals(
            """{"disabled":false,"enabled":true}""",
            json(mapOf("enabled" to true, "disabled" to false).toNativeProperties())
        )
    }

    @Test
    fun testNestedBooleansSerializeAsJsonBooleans() {
        val properties = mapOf(
            "${'$'}set" to mapOf("is_pro" to true),
            "flags" to listOf(true, false),
            "deep" to listOf(mapOf("on" to true))
        )

        assertEquals(
            """{"${'$'}set":{"is_pro":true},"deep":[{"on":true}],"flags":[true,false]}""",
            json(properties.toNativeProperties())
        )
    }

    @Test
    fun testOtherValueTypesAreUnchanged() {
        val properties = mapOf(
            "byte" to 1.toByte(),
            "double" to 1.5,
            "int" to 2,
            "long" to 3L,
            "null" to null,
            "string" to "yes"
        )

        assertEquals(
            """{"byte":1,"double":1.5,"int":2,"long":3,"null":null,"string":"yes"}""",
            json(properties.toNativeProperties())
        )
    }

    @Test
    fun testNullValuesArePreserved() {
        // The native SDK sets NSNull-valued properties such as ${'$'}feature_flag_response; they survive
        // the round trip as JSON null rather than being dropped.
        assertEquals(
            """{"absent":null}""",
            json(mapOf("absent" to null).toNativeProperties())
        )
    }

    @Test
    fun testSelfReferentialValueDoesNotRecurseForever() {
        val cycle = mutableListOf<Any?>()
        cycle.add(cycle)

        val converted = mapOf("cycle" to cycle).toNativeProperties()

        // Handed to the native SDK intact past the depth cap, for its own serialization check to
        // reject, which is what happened before these values were rebuilt here at all.
        assertEquals(1, converted.size)
        assertFalse(NSJSONSerialization.isValidJSONObject(converted))
    }

    @Test
    fun testSdkMetadataStampOverwritesNativeSdkValues() {
        @Suppress("UNCHECKED_CAST")
        val nativeProperties = parse("""{"${'$'}lib":"posthog-ios","${'$'}lib_version":"3.64.1"}""") as Map<Any?, *>

        assertEquals(
            """{"${'$'}lib":"posthog-kmp","${'$'}lib_version":"${PostHogKmpVersion.VERSION}"}""",
            json(nativeProperties.withSdkMetadata())
        )
    }

    @Test
    fun testSdkMetadataStampKeepsNativeBooleans() {
        @Suppress("UNCHECKED_CAST")
        val nativeProperties = parse("""{"${'$'}is_identified":true,"${'$'}feature/beta":false}""") as Map<Any?, *>

        val stamped = nativeProperties.withSdkMetadata()

        assertEquals(
            """{"${'$'}feature\/beta":false,"${'$'}is_identified":true,""" +
                """"${'$'}lib":"posthog-kmp","${'$'}lib_version":"${PostHogKmpVersion.VERSION}"}""",
            json(stamped)
        )
    }
}
