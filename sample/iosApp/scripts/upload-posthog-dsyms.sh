#!/usr/bin/env bash

set -euo pipefail

if [[ "${POSTHOG_UPLOAD_DSYMS:-0}" != "1" ]]; then
    echo "info: Skipping PostHog dSYM upload. Set POSTHOG_UPLOAD_DSYMS=1 to enable it."
    exit 0
fi

if [[ -n "${CONFIGURATION:-}" && "${CONFIGURATION}" != "Release" && "${POSTHOG_UPLOAD_DEBUG_SYMBOLS:-0}" != "1" ]]; then
    echo "info: Skipping PostHog dSYM upload for ${CONFIGURATION}. Set POSTHOG_UPLOAD_DEBUG_SYMBOLS=1 to upload non-Release symbols."
    exit 0
fi

if [[ -z "${DWARF_DSYM_FOLDER_PATH:-}" || ! -d "${DWARF_DSYM_FOLDER_PATH}" ]]; then
    echo "error: DWARF_DSYM_FOLDER_PATH does not contain a dSYM directory: ${DWARF_DSYM_FOLDER_PATH:-<unset>}"
    exit 1
fi

if [[ -z "$(find "${DWARF_DSYM_FOLDER_PATH}" -name '*.dSYM' -type d -print -quit)" ]]; then
    echo "error: No dSYM bundles found in ${DWARF_DSYM_FOLDER_PATH}"
    exit 1
fi

export PATH="/opt/homebrew/bin:/usr/local/bin:${HOME}/.posthog:${PATH}"
POSTHOG_CLI_PATH="${POSTHOG_CLI_PATH:-$(command -v posthog-cli || true)}"
if [[ -z "${POSTHOG_CLI_PATH}" || ! -x "${POSTHOG_CLI_PATH}" ]]; then
    echo "error: posthog-cli not found. Install it from https://posthog.com/docs/error-tracking/upload-source-maps/cli"
    exit 1
fi

CLI_ARGS=(
    dsym upload
    --directory "${DWARF_DSYM_FOLDER_PATH}"
)

if [[ -n "${DWARF_DSYM_FILE_NAME:-}" ]]; then
    CLI_ARGS+=(--main-dsym "${DWARF_DSYM_FILE_NAME}")
fi
if [[ -n "${PRODUCT_BUNDLE_IDENTIFIER:-}" ]]; then
    CLI_ARGS+=(--release-name "${PRODUCT_BUNDLE_IDENTIFIER}")
fi
if [[ -n "${MARKETING_VERSION:-}" ]]; then
    CLI_ARGS+=(--release-version "${MARKETING_VERSION}")
fi
if [[ -n "${CURRENT_PROJECT_VERSION:-}" ]]; then
    CLI_ARGS+=(--build "${CURRENT_PROJECT_VERSION}")
fi
if [[ "${POSTHOG_INCLUDE_SOURCE:-0}" == "1" ]]; then
    CLI_ARGS+=(--include-source)
fi
if [[ "${POSTHOG_SKIP_ON_CONFLICT:-0}" == "1" ]]; then
    CLI_ARGS+=(--skip-on-conflict)
fi

"${POSTHOG_CLI_PATH}" "${CLI_ARGS[@]}"
