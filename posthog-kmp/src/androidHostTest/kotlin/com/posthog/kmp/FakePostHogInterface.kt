package com.posthog.kmp

import com.posthog.PostHogInterface
import java.lang.reflect.Proxy
import java.util.UUID

class FakePostHogInterface {
    val capturedEvents = mutableListOf<String>()
    val capturedProperties = mutableListOf<Map<String, Any>?>()
    
    val capturedIdentify = mutableListOf<String>()
    val capturedIdentifyProperties = mutableListOf<Map<String, Any>?>()
    
    val capturedScreen = mutableListOf<String>()
    val capturedScreenProperties = mutableListOf<Map<String, Any>?>()

    val calledMethods = mutableListOf<Pair<String, List<Any?>>>()

    var currentSessionId: String? = "00000000-0000-0000-0000-000000000123"
    var currentDistinctId: String = "test-distinct-id"
    var currentAnonymousId: String = "anon-id"

    val proxy: PostHogInterface = Proxy.newProxyInstance(
        PostHogInterface::class.java.classLoader,
        arrayOf(PostHogInterface::class.java)
    ) { _, method, args ->
        val methodName = method.name
        val methodArgs = args?.toList() ?: emptyList()
        calledMethods.add(methodName to methodArgs)

        when (methodName) {
            "capture" -> {
                val event = args[0] as String
                capturedEvents.add(event)
                if (args.size > 2) {
                    @Suppress("UNCHECKED_CAST")
                    capturedProperties.add(args[2] as? Map<String, Any>)
                }
                Unit
            }
            "identify" -> {
                val distinctId = args[0] as String
                capturedIdentify.add(distinctId)
                if (args.size > 1) {
                    @Suppress("UNCHECKED_CAST")
                    capturedIdentifyProperties.add(args[1] as? Map<String, Any>)
                }
                Unit
            }
            "screen" -> {
                val title = args[0] as String
                capturedScreen.add(title)
                if (args.size > 1) {
                    @Suppress("UNCHECKED_CAST")
                    capturedScreenProperties.add(args[1] as? Map<String, Any>)
                }
                Unit
            }
            "getSessionId" -> {
                currentSessionId?.let { UUID.fromString(it) }
            }
            "distinctId" -> currentDistinctId
            "getAnonymousId" -> currentAnonymousId
            "isFeatureEnabled" -> false
            "getFeatureFlag" -> null
            "getFeatureFlagResult" -> null
            "getAllFeatureFlags" -> emptyList<Any>()
            "isOptOut" -> false
            else -> {
                val returnType = method.returnType
                when {
                    returnType == Boolean::class.javaPrimitiveType -> false
                    returnType == Int::class.javaPrimitiveType -> 0
                    returnType == Long::class.javaPrimitiveType -> 0L
                    returnType == Void.TYPE -> Unit
                    else -> null
                }
            }
        }
    } as PostHogInterface
}
