// swift-tools-version: 5.9
import PackageDescription
let package = Package(
  name: "_posthog_kmp",
  platforms: [
    .iOS("15.0")
  ],
  products: [
    .library(
      name: "_posthog_kmp",
      type: .none,
      targets: ["_posthog_kmp"]
    )
  ],
  dependencies: [
    .package(
      url: "https://github.com/PostHog/posthog-ios.git",
      exact: "3.64.1"
    )
  ],
  targets: [
    .target(
      name: "_posthog_kmp",
      dependencies: [
        .product(
          name: "PostHog",
          package: "posthog-ios"
        )
      ]
    )
  ]
)
