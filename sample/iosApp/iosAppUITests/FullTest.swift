import XCTest

final class FullTests: XCTestCase {
    let app = XCUIApplication()

    var status: String {
        app.windows.element(boundBy: 0).staticTexts.element(boundBy: 1).label
    }

    func log(_ step: String) {
        print("FULLTEST|\(step)|\(status)")
    }

    func drag(_ dy: CGFloat) {
        let start = app.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.4))
        let end = app.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.4 + dy))
        start.press(forDuration: 0.05, thenDragTo: end)
        usleep(500_000)
    }

    // Return closes the Compose keyboard (probe-verified); taps on buttons under
    // the keyboard window are silently swallowed, so always close it first.
    func dismissKeyboard() {
        if app.keyboards.count > 0 {
            app.typeText("\n")
            usleep(700_000)
        }
    }

    // Off-screen Compose elements drop their a11y labels, so an unmatched query
    // means "scroll around until it exists", sweeping up first, then down.
    func show(_ el: XCUIElement) {
        if !el.exists {
            for _ in 0..<8 where !el.exists { drag(0.35) }
            for _ in 0..<12 where !el.exists { drag(-0.35) }
        }
        var tries = 0
        while tries < 15 {
            let limit: CGFloat = app.keyboards.count > 0 ? 490 : 820
            if el.exists && el.isHittable && el.frame.minY > 120 && el.frame.maxY < limit { return }
            if el.exists && el.frame.minY <= 120 {
                drag(0.25)
            } else {
                drag(-0.25)
            }
            tries += 1
        }
    }

    func tapButton(_ label: String) {
        dismissKeyboard()
        let b = app.buttons[label]
        show(b)
        b.tap()
        usleep(700_000)
    }

    func clearAndType(_ field: XCUIElement, _ text: String) {
        show(field)
        var tries = 0
        while tries < 4 {
            field.tap()
            usleep(400_000)
            if (field.value(forKey: "hasKeyboardFocus") as? Bool) ?? false { break }
            drag(-0.1)
            show(field)
            tries += 1
        }
        if let current = field.value as? String, !current.isEmpty, current != field.label {
            // Put the cursor at the end before deleting so no tail survives
            field.coordinate(withNormalizedOffset: CGVector(dx: 0.97, dy: 0.5)).tap()
            usleep(300_000)
            field.typeText(String(repeating: XCUIKeyboardKey.delete.rawValue, count: current.count + 4))
        }
        field.typeText(text + "\n")
        usleep(400_000)
    }

    func testAnonAndReset() throws {
        app.launch()

        let apiKey = app.textViews.element(boundBy: 0)
        XCTAssertTrue(apiKey.waitForExistence(timeout: 30))
        clearAndType(apiKey, TestConfig.apiKey)
        tapButton("Initialize")
        sleep(3)
        log("ar_initialize")

        tapButton("Anon ID")
        sleep(1)
        log("ar_anon_id")
        tapButton("Anon ID")
        sleep(1)
        log("ar_anon_id_retry")

        clearAndType(app.textViews.element(boundBy: 2), "kmp-ios-full-test")
        tapButton("Identify")
        sleep(1)
        tapButton("Dist ID")
        sleep(1)
        log("ar_dist_before_reset")

        tapButton("Reset")
        sleep(1)
        log("ar_reset")
        tapButton("Reset")
        sleep(1)
        log("ar_reset_retry")
        tapButton("Dist ID")
        sleep(1)
        log("ar_dist_after_reset")
        tapButton("Dist ID")
        sleep(1)
        log("ar_dist_after_reset_retry")

        // Capture after reset: its server-side distinct_id must be a fresh anon UUID
        clearAndType(app.textViews.element(boundBy: 1), "kmp_ios_post_reset")
        tapButton("Capture Event")
        sleep(1)
        log("ar_post_reset_capture")
        tapButton("Flush")
        sleep(8)
        XCUIDevice.shared.press(.home)
        sleep(5)
    }

    func testTailButtons() throws {
        app.launch()

        let apiKey = app.textViews.element(boundBy: 0)
        XCTAssertTrue(apiKey.waitForExistence(timeout: 30))
        clearAndType(apiKey, TestConfig.apiKey)
        tapButton("Initialize")
        sleep(3)
        log("tail_initialize")

        clearAndType(app.textViews.element(boundBy: 2), "kmp-ios-full-test")
        tapButton("Identify")
        sleep(1)
        log("tail_identify")

        tapButton("Anon ID")
        log("anon_id")
        tapButton("Dist ID")
        log("dist_id")
        tapButton("Sess ID")
        log("sess_id")
        tapButton("Capture Sample Exception")
        log("capture_exception")
        tapButton("Flush")
        log("flush")
        sleep(8)

        tapButton("Reset")
        log("reset")
        tapButton("Dist ID")
        log("dist_id_after_reset")

        tapButton("Close")
        sleep(1)
        log("close")
        let flush = app.buttons["Flush"]
        show(flush)
        XCTAssertFalse(flush.isEnabled, "buttons should disable after Close")

        XCUIDevice.shared.press(.home)
        sleep(5)
    }

    func testAllButtons() throws {
        app.launch()

        let apiKey = app.textViews.element(boundBy: 0)
        XCTAssertTrue(apiKey.waitForExistence(timeout: 30))

        // 1. Initialize
        clearAndType(apiKey, TestConfig.apiKey)
        tapButton("Initialize")
        sleep(3)
        log("initialize")

        // 3. Identify first so events attach to a known person
        clearAndType(app.textViews.element(boundBy: 2), "kmp-ios-full-test")
        tapButton("Identify")
        sleep(1)
        log("identify")

        // 2. Capture Event + Screen View
        clearAndType(app.textViews.element(boundBy: 1), "kmp_ios_full_capture")
        tapButton("Capture Event")
        log("capture_event")
        tapButton("Capture Screen View")
        log("capture_screen")

        // 3b. Person properties
        clearAndType(app.textViews.element(boundBy: 3), "ios_test_prop")
        clearAndType(app.textViews.element(boundBy: 4), "premium_v1")
        tapButton("Set Property")
        log("set_property")
        clearAndType(app.textViews.element(boundBy: 3), "ios_test_once")
        clearAndType(app.textViews.element(boundBy: 4), "v_once")
        tapButton("Set Once")
        log("set_once")

        // 4. Feature flags
        clearAndType(app.textViews.element(boundBy: 5), "client-test-bool")
        tapButton("Check Flag")
        log("check_flag_bool")
        tapButton("Get Result")
        log("get_result_bool")
        clearAndType(app.textViews.element(boundBy: 5), "client-test-variant")
        tapButton("Get Result")
        log("get_result_variant")
        tapButton("Reload Flags")
        sleep(3)
        log("reload_flags")
        tapButton("Get All Flags")
        log("get_all_flags")

        // 5. Group analytics (group type prefilled 'company')
        clearAndType(app.textViews.element(boundBy: 7), "kmp-ios-co")
        tapButton("Set Group")
        log("set_group")

        // 6. Super properties
        clearAndType(app.textViews.element(boundBy: 8), "ios_super")
        clearAndType(app.textViews.element(boundBy: 9), "yes")
        tapButton("Register")
        log("register")
        clearAndType(app.textViews.element(boundBy: 1), "kmp_ios_super_check")
        tapButton("Capture Event")
        log("capture_super_check")
        tapButton("Unregister")
        log("unregister")
        clearAndType(app.textViews.element(boundBy: 1), "kmp_ios_super_removed")
        tapButton("Capture Event")
        log("capture_super_removed")

        // 7. Session & privacy
        tapButton("Opt Status")
        log("opt_status_initial")
        tapButton("Opt Out")
        log("opt_out")
        tapButton("Opt Status")
        log("opt_status_after_optout")
        clearAndType(app.textViews.element(boundBy: 1), "kmp_ios_optout_event")
        tapButton("Capture Event")
        log("capture_while_opted_out")
        tapButton("Opt In")
        log("opt_in")
        clearAndType(app.textViews.element(boundBy: 1), "kmp_ios_optin_event")
        tapButton("Capture Event")
        log("capture_after_optin")
        tapButton("Anon ID")
        log("anon_id")
        tapButton("Dist ID")
        log("dist_id")
        tapButton("Sess ID")
        log("sess_id")

        // 8. Error tracking
        tapButton("Capture Sample Exception")
        log("capture_exception")

        // 9. Flush before reset/close so the queue reaches the server
        tapButton("Flush")
        log("flush")
        sleep(8)

        tapButton("Reset")
        log("reset")
        tapButton("Dist ID")
        log("dist_id_after_reset")

        tapButton("Close")
        sleep(1)
        log("close")
        XCTAssertFalse(app.buttons["Flush"].isEnabled, "buttons should disable after Close")

        XCUIDevice.shared.press(.home)
        sleep(5)
    }
}
