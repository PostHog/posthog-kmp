# Releasing

Releases are semi-automated using [pnpm's native release management](https://pnpm.io/versioning) and follow the [PostHog SDK releases process](https://posthog.com/handbook/engineering/sdks/releases).

`posthog-kmp` publishes to [Maven Central](https://central.sonatype.com/artifact/com.posthog/posthog-kmp) as `com.posthog:posthog-kmp` (plus the per-target `-android`, `-jvm`, `-iosarm64`, `-iossimulatorarm64`, `-iosx64`, and `-js` variants). Consumers are Kotlin Multiplatform projects that resolve the right platform artifact through Gradle — native Swift (SPM/CocoaPods) and web (npm) are served by `posthog-ios` and `posthog-js` respectively, so they are intentionally not published here.

## How versioning works

pnpm's release management does not natively support Gradle/Maven, so the version lives in two places kept in lockstep by CI:

- **`package.json`** — a private version carrier (`"private": true`, never published to npm). `pnpm version -r` owns and bumps this.
- **`version.properties`** — the source of truth the Gradle build reads. `scripts/bump-version.sh` syncs it from `package.json` during release.

You never edit either by hand for a release — change intents drive the bump.

## Recording a change intent

When making a change that should appear in the changelog, record a change intent (requires pnpm >= 11.13 — no `pnpm install` needed, the tooling is built into pnpm):

```bash
# Describe your change — pick patch / minor / major and write a summary
pnpm change
```

This writes a file to `.changeset/`. **Commit it with your PR.**

## How to trigger a release

1. **Add a change intent** to your PR (see above).
2. **Merge the PR** into `main`. No release label or manual tagging is required.

On merge, the `Release` workflow runs:

1. **Prepare** — uses `pnpm version -r` to consume all pending change intents,
   bump `package.json`, update `CHANGELOG.md` and `.changeset/ledger.yaml`, then
   syncs `version.properties` and captures the result as a patch artifact (with
   a pinned sha256).
2. **Verify** — re-applies the patch on a clean checkout, checks the versions
   are consistent, and checks the tag and GitHub release don't already exist.
3. **Approval** — waits for a maintainer to approve the `Release` environment
   (requested in Slack, see below).
4. **Release** — applies the verified patch (after confirming `main` hasn't
   moved), commits the version bump to `main`, publishes all targets to Maven
   Central, then creates the Git tag (e.g. `0.1.0`, no `v` prefix) and a GitHub
   Release.

Build and tests are intentionally **not** re-run during release — CI already
gates every PR and push to `main`; the release publishes the approved commit.

## If a release fails

- **Before the version-bump commit lands on `main`** (prepare, verify, or the
  patch/publish steps up to the commit): nothing was mutated and the change
  intents are untouched. Fix the cause and use **Re-run all jobs**, or dispatch
  the `Release` workflow again.
- **After the version-bump commit** (publish, tag, or GitHub release failed):
  the change intents are consumed, so re-running `Release` finds nothing. Run the
  **Republish Release** workflow instead (Actions → Republish Release). It
  publishes the version already committed on `main` and creates the tag and
  GitHub release, skipping whatever already exists. Tick `skip_publish` if the
  artifacts already made it to Maven Central. It requires the same `Release`
  environment approval.

## Release approval

Every release requires approval from the Client Libraries team via the `Release` GitHub environment. The request is posted to `#approvals-client-libraries` on Slack.

## Required configuration

The `Release` workflow depends on the following being set up (see the [handbook](https://posthog.com/handbook/engineering/sdks/releases)):

- **`Release` GitHub environment** with required reviewers and the `GH_APP_POSTHOG_KMP_RELEASER_APP_ID` / `GH_APP_POSTHOG_KMP_RELEASER_PRIVATE_KEY` secrets (a dedicated `Releaser (posthog-kmp)` GitHub App).
- **Maven Central publishing secrets**: `SONATYPE_USERNAME`, `SONATYPE_PASSWORD`, and the GPG signing key as `GPG_PRIVATE_KEY`, `GPG_PASSPHRASE` (same names as posthog-android).
- **Org secrets/vars** scoped to the repo: `SLACK_CLIENT_LIBRARIES_BOT_TOKEN`, `POSTHOG_PROJECT_API_KEY`, `SLACK_APPROVALS_CLIENT_LIBRARIES_CHANNEL_ID`, `GROUP_CLIENT_LIBRARIES_SLACK_GROUP_ID`.

## Rotating Sonatype / GPG credentials

See [posthog-android's RELEASING.md](https://github.com/PostHog/posthog-android/blob/main/RELEASING.md#rotating-sonatype-user-token) — the Sonatype user token and GPG key are shared PostHog credentials and rotate the same way, under the same secret names.
