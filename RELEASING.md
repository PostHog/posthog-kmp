# Releasing

Releases are semi-automated using [Sampo](https://github.com/PostHog/sampo) and follow the [PostHog SDK releases process](https://posthog.com/handbook/engineering/sdks/releases).

`posthog-kmp` publishes to [Maven Central](https://central.sonatype.com/artifact/com.posthog/posthog-kmp) as `com.posthog:posthog-kmp` (plus the per-target `-android`, `-iosarm64`, `-iossimulatorarm64`, `-iosx64`, and `-js` variants). Consumers are Kotlin Multiplatform projects that resolve the right platform artifact through Gradle — native Swift (SPM/CocoaPods) and web (npm) are served by `posthog-ios` and `posthog-js` respectively, so they are intentionally not published here.

## How versioning works

Sampo does not natively support Gradle/Maven, so the version lives in two places kept in lockstep by CI:

- **`package.json`** — a private version carrier (`"private": true`, never published to npm). Sampo owns and bumps this.
- **`version.properties`** — the source of truth the Gradle build reads. `scripts/bump-version.sh` syncs it from `package.json` during release.

You never edit either by hand for a release — changesets drive the bump.

## Creating a changeset

When making a change that should appear in the changelog, add a changeset:

```bash
# Install the sampo CLI (requires a Rust toolchain)
cargo install sampo

# Describe your change — pick patch / minor / major and write a summary
sampo add
```

This writes a file to `.sampo/changesets/`. **Commit it with your PR.**

## How to trigger a release

1. **Add a changeset** to your PR (see above).
2. **Merge the PR** into `main`. No release label or manual tagging is required.

On merge, the `Release` workflow runs and:

1. Consumes all pending changesets
2. Bumps the version in `package.json` and syncs it to `version.properties`
3. Updates `CHANGELOG.md`
4. Commits the version bump to `main`
5. Publishes all targets to Maven Central
6. Creates the Git tag (e.g. `v0.1.0`) and a GitHub Release

## Release approval

Every release requires approval from the Client Libraries team via the `Release` GitHub environment. The request is posted to `#approvals-client-libraries` on Slack.

## Required configuration

The `Release` workflow depends on the following being set up (see the [handbook](https://posthog.com/handbook/engineering/sdks/releases)):

- **`Release` GitHub environment** with required reviewers and the `GH_APP_POSTHOG_KMP_RELEASER_APP_ID` / `GH_APP_POSTHOG_KMP_RELEASER_PRIVATE_KEY` secrets (a dedicated `Releaser (posthog-kmp)` GitHub App).
- **Maven Central publishing secrets**: `SONATYPE_USERNAME`, `SONATYPE_PASSWORD`, and the GPG signing key as `GPG_PRIVATE_KEY`, `GPG_PASSPHRASE` (same names as posthog-android).
- **Org secrets/vars** scoped to the repo: `SLACK_CLIENT_LIBRARIES_BOT_TOKEN`, `POSTHOG_PROJECT_API_KEY`, `SLACK_APPROVALS_CLIENT_LIBRARIES_CHANNEL_ID`, `GROUP_CLIENT_LIBRARIES_SLACK_GROUP_ID`.

## Rotating Sonatype / GPG credentials

See [posthog-android's RELEASING.md](https://github.com/PostHog/posthog-android/blob/main/RELEASING.md#rotating-sonatype-user-token) — the Sonatype user token and GPG key are shared PostHog credentials and rotate the same way, under the same secret names.
