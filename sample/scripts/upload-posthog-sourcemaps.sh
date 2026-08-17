#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/../.." && pwd)"
DIST_DIR="${POSTHOG_SOURCEMAP_DIR:-${ROOT_DIR}/sample/build/dist/js/productionExecutable}"
RELEASE_NAME="${POSTHOG_RELEASE_NAME:-com.posthog.kmp.sample.web}"
RELEASE_VERSION="${POSTHOG_RELEASE_VERSION:-$(git -C "${ROOT_DIR}" rev-parse HEAD)}"

if [[ ! -f "${DIST_DIR}/sample.js" || ! -f "${DIST_DIR}/sample.js.map" ]]; then
    echo "error: Production JavaScript bundle and source map not found in ${DIST_DIR}."
    echo "error: Run ./gradlew :sample:jsBrowserDistribution first."
    exit 1
fi

export PATH="/opt/homebrew/bin:/usr/local/bin:${HOME}/.posthog:${PATH}"
POSTHOG_CLI_PATH="${POSTHOG_CLI_PATH:-$(command -v posthog-cli || true)}"
if [[ -z "${POSTHOG_CLI_PATH}" || ! -x "${POSTHOG_CLI_PATH}" ]]; then
    echo "error: posthog-cli not found. Install it from https://posthog.com/docs/error-tracking/upload-source-maps/web"
    exit 1
fi

RELEASE_ARGS=(
    --release-name "${RELEASE_NAME}"
    --release-version "${RELEASE_VERSION}"
)
if [[ -n "${POSTHOG_RELEASE_BUILD:-}" ]]; then
    RELEASE_ARGS+=(--build "${POSTHOG_RELEASE_BUILD}")
fi

"${POSTHOG_CLI_PATH}" sourcemap inject \
    --directory "${DIST_DIR}" \
    "${RELEASE_ARGS[@]}"

UPLOAD_ARGS=(
    sourcemap upload
    --directory "${DIST_DIR}"
    "${RELEASE_ARGS[@]}"
)
if [[ "${POSTHOG_FORCE:-0}" == "1" ]]; then
    UPLOAD_ARGS+=(--force)
fi
if [[ "${POSTHOG_SKIP_ON_CONFLICT:-0}" == "1" ]]; then
    UPLOAD_ARGS+=(--skip-on-conflict)
fi

"${POSTHOG_CLI_PATH}" "${UPLOAD_ARGS[@]}"
