import Foundation
import XCTest

enum TestConfig {
    static func requireAPIKey() throws -> String {
        guard let key = ProcessInfo.processInfo.environment["POSTHOG_API_KEY"],
              !key.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty,
              key != "phc_YOUR_PROJECT_API_KEY" else {
            throw XCTSkip("Live-project smoke tests require an explicitly configured POSTHOG_API_KEY")
        }
        return key
    }
}
