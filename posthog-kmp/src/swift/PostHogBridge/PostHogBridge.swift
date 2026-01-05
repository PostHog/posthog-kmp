import Foundation
import PostHog

/// Swift bridge for PostHog SDK to enable Kotlin/Native interop.
/// Uses singleton pattern for better Objective-C compatibility.
@objc public class PostHogBridge: NSObject {

    /// Shared singleton instance
    @objc public static let shared = PostHogBridge()

    /// Private initializer to enforce singleton
    private override init() {
        super.init()
    }

    // MARK: - Setup

    /// Initialize PostHog with full configuration options
    @objc public func setup(
        apiKey: String,
        host: String,
        debug: Bool = false,
        captureApplicationLifecycleEvents: Bool = true,
        captureScreenViews: Bool = false,
        sendFeatureFlagEvent: Bool = true,
        preloadFeatureFlags: Bool = true,
        flushAt: Int = 20,
        flushIntervalSeconds: Double = 30.0,
        maxQueueSize: Int = 1000,
        maxBatchSize: Int = 50,
        optOut: Bool = false,
        sessionRecordingEnabled: Bool = false,
        autocapture: Bool = false,
        environment: String = "production"
    ) {
        let config = PostHogConfig(apiKey: apiKey, host: host)

        // Debug mode
        config.debug = debug

        // Lifecycle and screen tracking
        config.captureApplicationLifecycleEvents = captureApplicationLifecycleEvents
        config.captureScreenViews = captureScreenViews

        // Feature flags
        config.sendFeatureFlagEvent = sendFeatureFlagEvent
        config.preloadFeatureFlags = preloadFeatureFlags

        // Batch settings
        config.flushAt = flushAt
        config.flushIntervalSeconds = flushIntervalSeconds
        config.maxQueueSize = maxQueueSize
        config.maxBatchSize = maxBatchSize

        // Opt out
        config.optOut = optOut

        // Person profiles (only identified users)
        config.personProfiles = .identifiedOnly
        config.setDefaultPersonProperties = true

        #if os(iOS) || targetEnvironment(macCatalyst)
        // Element interaction tracking (autocapture)
        config.captureElementInteractions = autocapture

        // Session replay
        if sessionRecordingEnabled {
            config.sessionReplay = true
            config.sessionReplayConfig.maskAllTextInputs = true
            config.sessionReplayConfig.maskAllImages = false
            config.sessionReplayConfig.captureNetworkTelemetry = true
            config.sessionReplayConfig.captureLogs = true
        }

        // Surveys (iOS 15+)
        if #available(iOS 15.0, *) {
            config.surveys = true
        }
        #endif

        PostHogSDK.shared.setup(config)

        // Register environment as a super property
        PostHogSDK.shared.register(["environment": environment])
    }

    // MARK: - Event Capture

    /// Capture an event with optional properties
    @objc public func capture(event: String, properties: NSDictionary?) {
        if let props = properties as? [String: Any] {
            PostHogSDK.shared.capture(event, properties: props)
        } else {
            PostHogSDK.shared.capture(event)
        }
    }

    /// Track a screen view
    @objc public func screen(title: String, properties: NSDictionary?) {
        if let props = properties as? [String: Any] {
            PostHogSDK.shared.screen(title, properties: props)
        } else {
            PostHogSDK.shared.screen(title)
        }
    }

    // MARK: - User Identification

    /// Identify a user with properties
    @objc public func identify(distinctId: String, userProperties: NSDictionary?, userPropertiesSetOnce: NSDictionary?) {
        let props = userProperties as? [String: Any]
        let propsSetOnce = userPropertiesSetOnce as? [String: Any]

        PostHogSDK.shared.identify(distinctId, userProperties: props, userPropertiesSetOnce: propsSetOnce)
    }

    /// Create an alias for the current user
    @objc public func alias(alias: String) {
        PostHogSDK.shared.alias(alias)
    }

    /// Reset the PostHog session (logout)
    @objc public func reset() {
        PostHogSDK.shared.reset()
    }

    /// Get the current distinct ID
    @objc public func getDistinctId() -> String {
        return PostHogSDK.shared.getDistinctId()
    }

    // MARK: - Super Properties

    /// Register a super property
    @objc public func register(key: String, value: Any) {
        PostHogSDK.shared.register([key: value])
    }

    /// Register multiple super properties
    @objc public func registerAll(properties: NSDictionary) {
        if let props = properties as? [String: Any] {
            PostHogSDK.shared.register(props)
        }
    }

    /// Unregister a super property
    @objc public func unregister(key: String) {
        PostHogSDK.shared.unregister(key)
    }

    // MARK: - Group Analytics

    /// Associate the current user with a group
    @objc public func group(type: String, key: String, groupProperties: NSDictionary?) {
        if let props = groupProperties as? [String: Any] {
            PostHogSDK.shared.group(type: type, key: key, groupProperties: props)
        } else {
            PostHogSDK.shared.group(type: type, key: key)
        }
    }

    // MARK: - Feature Flags

    /// Check if a feature flag is enabled
    @objc public func isFeatureEnabled(_ key: String) -> Bool {
        return PostHogSDK.shared.isFeatureEnabled(key)
    }

    /// Get feature flag value
    @objc public func getFeatureFlag(_ key: String) -> Any? {
        return PostHogSDK.shared.getFeatureFlag(key)
    }

    /// Get feature flag payload
    @objc public func getFeatureFlagPayload(_ key: String) -> Any? {
        return PostHogSDK.shared.getFeatureFlagPayload(key)
    }

    /// Reload feature flags from server
    @objc public func reloadFeatureFlags() {
        PostHogSDK.shared.reloadFeatureFlags()
    }

    /// Reload feature flags with callback
    @objc public func reloadFeatureFlagsWithCallback(callback: @escaping () -> Void) {
        PostHogSDK.shared.reloadFeatureFlags {
            callback()
        }
    }

    // MARK: - Opt In/Out

    /// Opt out of analytics
    @objc public func optOut() {
        PostHogSDK.shared.optOut()
    }

    /// Opt into analytics
    @objc public func optIn() {
        PostHogSDK.shared.optIn()
    }

    /// Check if opted out
    @objc public func isOptedOut() -> Bool {
        return PostHogSDK.shared.isOptOut()
    }

    // MARK: - Flush & Close

    /// Flush all queued events
    @objc public func flush() {
        PostHogSDK.shared.flush()
    }

    /// Close the PostHog instance
    @objc public func close() {
        PostHogSDK.shared.close()
    }

    // MARK: - Debug

    /// Enable or disable debug mode
    @objc public func setDebug(enabled: Bool) {
        PostHogSDK.shared.debug(enabled)
    }
}
