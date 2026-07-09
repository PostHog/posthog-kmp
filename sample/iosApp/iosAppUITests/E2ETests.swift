import XCTest

final class E2ETests: XCTestCase {
    func testCaptureEventE2E() throws {
        let app = XCUIApplication()
        app.launch()

        let apiKeyField = app.textViews["API Key"]
        XCTAssertTrue(apiKeyField.waitForExistence(timeout: 30), "API key field should appear")
        apiKeyField.tap()
        apiKeyField.typeText(TestConfig.apiKey)

        let initButton = app.buttons["Initialize"]
        XCTAssertTrue(initButton.waitForExistence(timeout: 5))
        initButton.tap()
        sleep(3)

        let eventField = app.textViews["Event Name"]
        XCTAssertTrue(eventField.waitForExistence(timeout: 5))
        eventField.tap()
        if let current = eventField.value as? String, !current.isEmpty {
            let deletes = String(repeating: XCUIKeyboardKey.delete.rawValue, count: current.count + 2)
            eventField.typeText(deletes)
        }
        eventField.typeText("kmp_e2e_ios")

        let captureButton = app.buttons["Capture Event"]
        XCTAssertTrue(captureButton.waitForExistence(timeout: 5))
        captureButton.tap()
        sleep(1)
        captureButton.tap()
        sleep(2)

        // Background the app to force posthog-ios to flush its queue
        XCUIDevice.shared.press(.home)
        sleep(12)

        app.activate()
        sleep(2)
    }
}
