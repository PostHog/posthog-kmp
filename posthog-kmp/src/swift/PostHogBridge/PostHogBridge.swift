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
        personProfiles: String = "identifiedOnly",
        sessionRecordingEnabled: Bool = false,
        sessionRecordingMaskAllTextInputs: Bool = true,
        sessionRecordingMaskAllImages: Bool = false,
        sessionRecordingCaptureNetworkTelemetry: Bool = true,
        sessionRecordingCaptureLogs: Bool = true,
        sessionRecordingScreenshotMode: Bool = false,
        autocapture: Bool = false,
        sdkVersion: String = "unknown"
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

        // Person profiles
        if personProfiles.caseInsensitiveCompare("always") == .orderedSame {
            config.personProfiles = .always
        } else if personProfiles.caseInsensitiveCompare("never") == .orderedSame {
            config.personProfiles = .never
        } else {
            config.personProfiles = .identifiedOnly
        }
        config.setDefaultPersonProperties = true

        #if os(iOS) || targetEnvironment(macCatalyst)
        // Element interaction tracking (autocapture)
        config.captureElementInteractions = autocapture

        // Session replay
        if sessionRecordingEnabled {
            config.sessionReplay = true
            config.sessionReplayConfig.maskAllTextInputs = sessionRecordingMaskAllTextInputs
            config.sessionReplayConfig.maskAllImages = sessionRecordingMaskAllImages
            config.sessionReplayConfig.captureNetworkTelemetry = sessionRecordingCaptureNetworkTelemetry
            config.sessionReplayConfig.captureLogs = sessionRecordingCaptureLogs
            config.sessionReplayConfig.screenshotMode = sessionRecordingScreenshotMode
        }

        // Surveys (iOS 15+)
        if #available(iOS 15.0, *) {
            config.surveys = true
        }
        #endif

        postHogSdkName = "posthog-kmp"
        postHogVersion = sdkVersion

        PostHogSDK.shared.setup(config)
    }

    // MARK: - Event Capture

    /// Capture an event with optional properties
    @objc public func capture(event: String, properties: NSDictionary?, timestamp: Date? = nil) {
        let props = properties as? [String: Any]
        PostHogSDK.shared.capture(event, properties: props, timestamp: timestamp)
    }

    /// Track a screen view
    @objc public func screen(title: String, properties: NSDictionary?) {
        if let props = properties as? [String: Any] {
            PostHogSDK.shared.screen(title, properties: props)
        } else {
            PostHogSDK.shared.screen(title)
        }
    }

    /// Capture an exception originating from Kotlin.
    ///
    /// The native iOS SDK builds `$exception_list` from native (symbolicated) stack frames and
    /// cannot consume a Kotlin/Native textual stack as real frames. To avoid discarding the
    /// Kotlin stack, it is forwarded verbatim as the `$exception_stack_trace_raw` property so it
    /// lands on the `$exception` event alongside the type and message.
    ///
    /// - Parameters:
    ///   - type: The Kotlin throwable's class name (used as the NSException name).
    ///   - message: The Kotlin throwable's message, if any.
    ///   - stackTrace: The Kotlin stack trace (`Throwable.stackTraceToString()`).
    ///   - properties: Optional additional properties to attach to the event.
    @objc public func captureException(
        type: String,
        message: String?,
        stackTrace: String,
        properties: NSDictionary?
    ) {
        let exception = NSException(
            name: NSExceptionName(rawValue: type),
            reason: message,
            userInfo: nil
        )

        var props = properties as? [String: Any] ?? [:]
        props["$exception_stack_trace_raw"] = stackTrace

        PostHogSDK.shared.captureException(exception, properties: props)
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
    @objc public func isFeatureEnabled(_ key: String, sendFeatureFlagEvent: Bool = true) -> Bool {
        return PostHogSDK.shared.isFeatureEnabled(key, sendFeatureFlagEvent: sendFeatureFlagEvent)
    }

    /// Get feature flag value
    @objc public func getFeatureFlag(_ key: String, sendFeatureFlagEvent: Bool) -> Any? {
        return PostHogSDK.shared.getFeatureFlag(key, sendFeatureFlagEvent: sendFeatureFlagEvent)
    }

    /// Get all evaluated feature flags as an array of dictionaries.
    ///
    /// Each dictionary mirrors the shape produced by ``getFeatureFlagResult(_:sendFeatureFlagEvent:)``
    /// with `key`, `enabled`, `variant`, and `payload` entries.
    ///
    /// This is a synchronous read of the locally cached flags. It does not send a
    /// `$feature_flag_called` event (unlike `getFeatureFlag`/`getFeatureFlagResult`).
    @objc public func getAllFeatureFlags() -> [NSDictionary]? {
        guard let results = PostHogSDK.shared.getAllFeatureFlags() else { return nil }
        return results.map { r in
            [
                "key": r.key,
                "enabled": r.enabled,
                "variant": r.variant as Any,
                "payload": r.payload as Any,
            ] as NSDictionary
        }
    }

    @objc public func getFeatureFlagResult(_ key: String,  sendFeatureFlagEvent: Bool) -> NSDictionary? {
        guard let result = PostHogSDK.shared.getFeatureFlagResult(key, sendFeatureFlagEvent: sendFeatureFlagEvent) else { return nil }
        var dict: [String: Any] = [
            "key": key, // Use key passed in or result.key if available
            "enabled": false
        ]
        
        // Use KVC to dynamically access properties to avoid compilation errors 
        // if the exact swift interface differs slightly across versions.
        if result.responds(to: NSSelectorFromString("enabled")) {
            dict["enabled"] = result.value(forKey: "enabled") as? Bool ?? false
        }
        if result.responds(to: NSSelectorFromString("variant")) {
            dict["variant"] = result.value(forKey: "variant")
        }
        if result.responds(to: NSSelectorFromString("payload")) {
            dict["payload"] = result.value(forKey: "payload")
        }
        if result.responds(to: NSSelectorFromString("key")) {
            dict["key"] = result.value(forKey: "key") as? String ?? key
        }
        
        return dict as NSDictionary
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

    // MARK: - Session Management

    /// Get the anonymous ID
    @objc public func getAnonymousId() -> String {
        return PostHogSDK.shared.getAnonymousId()
    }

    /// Get the current session ID
    @objc public func getSessionId() -> String? {
        return PostHogSDK.shared.getSessionId()
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

    @objc public func setPersonProperties(
        userProperties: NSDictionary?,
        userPropertiesSetOnce: NSDictionary?
    ) {
        let props = userProperties as? [String: Any]
        let propsSetOnce = userPropertiesSetOnce as? [String: Any]
        PostHogSDK.shared.setPersonProperties(userPropertiesToSet: props, userPropertiesToSetOnce: propsSetOnce)
    }
}
