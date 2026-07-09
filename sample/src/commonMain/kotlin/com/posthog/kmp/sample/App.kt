package com.posthog.kmp.sample

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.posthog.kmp.PostHog
import com.posthog.kmp.PostHogConfig
import com.posthog.kmp.PostHogContext

/**
 * Sample app demonstrating PostHog KMP usage.
 *
 * @param postHogContext Platform-specific context passed from the platform entry point.
 *                       On Android: PostHogContext(application)
 *                       On iOS/Web: PostHogContext()
 */
@Composable
fun App(postHogContext: PostHogContext) {
    var isInitialized by remember { mutableStateOf(false) }
    var apiKey by remember { mutableStateOf("") }
    var eventName by remember { mutableStateOf("button_clicked") }
    var userId by remember { mutableStateOf("") }
    var featureFlagKey by remember { mutableStateOf("") }
    var personPropertyKey by remember { mutableStateOf("") }
    var personPropertyValue by remember { mutableStateOf("") }
    var groupType by remember { mutableStateOf("company") }
    var groupKey by remember { mutableStateOf("") }
    var superPropKey by remember { mutableStateOf("") }
    var superPropValue by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf("PostHog not initialized") }

    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize().windowInsetsPadding(insets = WindowInsets.systemBars),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "PostHog KMP Sample",
                    style = MaterialTheme.typography.headlineMedium
                )

                // Status
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isInitialized)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = statusMessage,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                HorizontalDivider()

                // Initialization Section
                Text(
                    text = "1. Initialize PostHog",
                    style = MaterialTheme.typography.titleMedium
                )

                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API Key") },
                    placeholder = { Text("phc_your_api_key") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Button(
                    onClick = {
                        if (apiKey.isNotBlank()) {
                            isInitialized = true
                            PostHog.setup(
                                config = PostHogConfig(
                                    apiKey = apiKey,
                                    debug = true,
                                ),
                                context = postHogContext
                            )
                            statusMessage = if (isInitialized) {
                                "PostHog initialized successfully!"
                            } else {
                                "Failed to initialize PostHog"
                            }
                        } else {
                            statusMessage = "Please enter an API key"
                        }
                    },
                    enabled = !isInitialized && apiKey.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isInitialized) "Already Initialized" else "Initialize")
                }

                HorizontalDivider()

                // Event Capture Section
                Text(
                    text = "2. Capture Events",
                    style = MaterialTheme.typography.titleMedium
                )

                OutlinedTextField(
                    value = eventName,
                    onValueChange = { eventName = it },
                    label = { Text("Event Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Button(
                    onClick = {
                        PostHog.capture(
                            event = eventName,
                            properties = mapOf(
                                "source" to "sample_app",
                                "platform" to getPlatformName()
                            )
                        )
                        statusMessage = "Event '$eventName' captured!"
                    },
                    enabled = isInitialized,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Capture Event")
                }

                Button(
                    onClick = {
                        PostHog.screen(
                            screenName = "SampleScreen",
                            properties = mapOf("section" to "demo")
                        )
                        statusMessage = "Screen view captured!"
                    },
                    enabled = isInitialized,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Capture Screen View")
                }

                HorizontalDivider()

                // User Identification Section
                Text(
                    text = "3. User Identification",
                    style = MaterialTheme.typography.titleMedium
                )

                OutlinedTextField(
                    value = userId,
                    onValueChange = { userId = it },
                    label = { Text("User ID") },
                    placeholder = { Text("user_123") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            if (userId.isNotBlank()) {
                                PostHog.identify(
                                    distinctId = userId,
                                    userProperties = mapOf(
                                        "app" to "sample",
                                        "platform" to getPlatformName()
                                    )
                                )
                                statusMessage = "Identify method called"
                            }
                        },
                        enabled = isInitialized && userId.isNotBlank(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Identify")
                    }

                    Button(
                        onClick = {
                            PostHog.reset()
                            statusMessage = "User reset!"
                        },
                        enabled = isInitialized,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Text("Reset")
                    }
                }

                HorizontalDivider()

                // Person Properties Section
                Text(
                    text = "3b. Set Person Properties",
                    style = MaterialTheme.typography.titleMedium
                )

                OutlinedTextField(
                    value = personPropertyKey,
                    onValueChange = { personPropertyKey = it },
                    label = { Text("Property Key") },
                    placeholder = { Text("user_tier") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = personPropertyValue,
                    onValueChange = { personPropertyValue = it },
                    label = { Text("Property Value") },
                    placeholder = { Text("premium") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            if (personPropertyKey.isNotBlank()) {
                                PostHog.setPersonProperties(mapOf(personPropertyKey to personPropertyValue))
                                statusMessage = "Person property set: $personPropertyKey = $personPropertyValue"
                            }
                        },
                        enabled = isInitialized && personPropertyKey.isNotBlank(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Set Property")
                    }

                    Button(
                        onClick = {
                            if (personPropertyKey.isNotBlank()) {
                                PostHog.setPersonProperties(
                                    userPropertiesToSet = null,
                                    userPropertiesToSetOnce = mapOf(personPropertyKey to personPropertyValue)
                                )
                                statusMessage = "Person property set once: $personPropertyKey = $personPropertyValue"
                            }
                        },
                        enabled = isInitialized && personPropertyKey.isNotBlank(),
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Text("Set Once")
                    }
                }

                HorizontalDivider()

                // Feature Flags Section
                Text(
                    text = "4. Feature Flags",
                    style = MaterialTheme.typography.titleMedium
                )

                OutlinedTextField(
                    value = featureFlagKey,
                    onValueChange = { featureFlagKey = it },
                    label = { Text("Feature Flag Key") },
                    placeholder = { Text("my-feature-flag") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            if (featureFlagKey.isNotBlank()) {
                                val isEnabled = PostHog.isFeatureEnabled(featureFlagKey)
                                statusMessage = "Feature '$featureFlagKey' is ${if (isEnabled) "ENABLED" else "DISABLED"}"
                            }
                        },
                        enabled = isInitialized && featureFlagKey.isNotBlank(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Check Flag")
                    }

                    Button(
                        onClick = {
                            if (featureFlagKey.isNotBlank()) {
                                val result = PostHog.getFeatureFlagResult(featureFlagKey)
                                statusMessage = "Result: ${result?.value} (variant: ${result?.variant}) (payload: ${result?.payload})"
                            }
                        },
                        enabled = isInitialized && featureFlagKey.isNotBlank(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Get Result")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            PostHog.reloadFeatureFlags {
                                statusMessage = "Feature flags reloaded!"
                            }
                        },
                        enabled = isInitialized,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Reload Flags")
                    }

                    Button(
                        onClick = {
                            val all = PostHog.getAllFeatureFlags()
                            statusMessage = "All flags retrieved (count: ${all.size})"
                        },
                        enabled = isInitialized,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Get All Flags")
                    }
                }

                HorizontalDivider()

                // Group Analytics
                Text(text = "5. Group Analytics", style = MaterialTheme.typography.titleMedium)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = groupType, onValueChange = { groupType = it },
                        label = { Text("Group Type") }, modifier = Modifier.weight(1f), singleLine = true
                    )
                    OutlinedTextField(
                        value = groupKey, onValueChange = { groupKey = it },
                        label = { Text("Group Key") }, modifier = Modifier.weight(1f), singleLine = true
                    )
                }
                Button(
                    onClick = {
                        PostHog.group(groupType, groupKey, mapOf("demo_property" to "yes"))
                        statusMessage = "Group updated!"
                    },
                    enabled = isInitialized && groupType.isNotBlank() && groupKey.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Set Group") }

                HorizontalDivider()

                // Super Properties
                Text(text = "6. Super Properties", style = MaterialTheme.typography.titleMedium)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = superPropKey, onValueChange = { superPropKey = it },
                        label = { Text("Key") }, modifier = Modifier.weight(1f), singleLine = true
                    )
                    OutlinedTextField(
                        value = superPropValue, onValueChange = { superPropValue = it },
                        label = { Text("Value") }, modifier = Modifier.weight(1f), singleLine = true
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            PostHog.register(superPropKey, superPropValue)
                            statusMessage = "Super property registered!"
                        },
                        enabled = isInitialized && superPropKey.isNotBlank(), modifier = Modifier.weight(1f)
                    ) { Text("Register") }
                    Button(
                        onClick = {
                            PostHog.unregister(superPropKey)
                            statusMessage = "Super property unregistered!"
                        },
                        enabled = isInitialized && superPropKey.isNotBlank(), modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) { Text("Unregister") }
                }

                HorizontalDivider()

                // Session & Privacy
                Text(text = "7. Session & Privacy", style = MaterialTheme.typography.titleMedium)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { statusMessage = "Opted out: ${PostHog.isOptedOut()}" },
                        enabled = isInitialized, modifier = Modifier.weight(1f)
                    ) { Text("Opt Status") }
                    Button(
                        onClick = { PostHog.optIn(); statusMessage = "Opted in!" },
                        enabled = isInitialized, modifier = Modifier.weight(1f)
                    ) { Text("Opt In") }
                    Button(
                        onClick = { PostHog.optOut(); statusMessage = "Opted out!" },
                        enabled = isInitialized, modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) { Text("Opt Out") }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { statusMessage = "Anon: ${PostHog.getAnonymousId()}" },
                        enabled = isInitialized, modifier = Modifier.weight(1f)
                    ) { Text("Anon ID") }
                    Button(
                        onClick = { statusMessage = "Dist: ${PostHog.getDistinctId()}" },
                        enabled = isInitialized, modifier = Modifier.weight(1f)
                    ) { Text("Dist ID") }
                    Button(
                        onClick = { statusMessage = "Sess: ${PostHog.getSessionId()}" },
                        enabled = isInitialized, modifier = Modifier.weight(1f)
                    ) { Text("Sess ID") }
                }

                HorizontalDivider()

                // Error Tracking
                Text(text = "8. Error Tracking", style = MaterialTheme.typography.titleMedium)
                Button(
                    onClick = {
                        PostHog.captureException(Exception("Sample test exception"))
                        statusMessage = "Exception captured!"
                    },
                    enabled = isInitialized, modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Capture Sample Exception") }

                HorizontalDivider()

                // Flush & Close
                Text(
                    text = "9. Flush & Close",
                    style = MaterialTheme.typography.titleMedium
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            PostHog.flush()
                            statusMessage = "Events flushed!"
                        },
                        enabled = isInitialized,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Flush")
                    }

                    Button(
                        onClick = {
                            PostHog.close()
                            isInitialized = false
                            statusMessage = "PostHog closed"
                        },
                        enabled = isInitialized,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Platform info
                Text(
                    text = "Running on: ${getPlatformName()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

expect fun getPlatformName(): String
