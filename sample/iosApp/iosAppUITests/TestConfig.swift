import Foundation

enum TestConfig {
    /// PostHog project API key used by the UI tests.
    /// Set via: xcodebuild test ... TEST_RUNNER_POSTHOG_API_KEY=phc_your_key
    static var apiKey: String {
        ProcessInfo.processInfo.environment["POSTHOG_API_KEY"] ?? "phc_YOUR_PROJECT_API_KEY"
    }
}
