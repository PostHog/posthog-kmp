# Change intents

Release notes and version bumps are driven by [pnpm's native release
management](https://pnpm.io/versioning).

When making a change that should appear in the changelog, run `pnpm change`
(requires pnpm >= 11.13) and commit the generated markdown file with your PR.
The committed `ledger.yaml` records which intents each release consumed. See
[RELEASING.md](../RELEASING.md) for the full release process.
