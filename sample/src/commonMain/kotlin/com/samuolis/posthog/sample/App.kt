package com.samuolis.posthog.sample

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.samuolis.posthog.PostHog
import com.samuolis.posthog.PostHogConfig
import com.samuolis.posthog.PostHogContext

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
                            PostHog.setup(
                                config = PostHogConfig(
                                    apiKey = apiKey,
                                    debug = true
                                ),
                                context = postHogContext
                            )
                            isInitialized = PostHog.isSetup()
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
                                statusMessage = "User '$userId' identified!"
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
                            PostHog.reloadFeatureFlags {
                                statusMessage = "Feature flags reloaded!"
                            }
                        },
                        enabled = isInitialized,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Reload Flags")
                    }
                }

                HorizontalDivider()

                // Flush & Close
                Text(
                    text = "5. Flush & Close",
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
