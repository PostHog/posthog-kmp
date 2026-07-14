#!/usr/bin/env bash

# ./scripts/bump-version.sh [new version]
# eg ./scripts/bump-version.sh "0.1.0"
#
# Syncs the version from the root package.json (managed by sampo) into
# version.properties, the source of truth the Gradle build reads. If no
# argument is given, the version is read from package.json.
#
# sampo bumps package.json (its only supported ecosystem here); this script
# mirrors that version into version.properties so the published Maven artifacts
# carry the right version.

set -euo pipefail

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$SCRIPT_DIR/.."

if [[ $# -ge 1 ]]; then
  NEW_VERSION="$1"
else
  NEW_VERSION="$(grep -m1 '"version"' package.json | sed -E 's/.*"version"[[:space:]]*:[[:space:]]*"([^"]+)".*/\1/')"
fi

if [[ ! "$NEW_VERSION" =~ ^([0-9]+)\.([0-9]+)\.([0-9]+)$ ]]; then
  echo "bump-version: expected MAJOR.MINOR.PATCH, got '$NEW_VERSION'" >&2
  exit 1
fi

MAJOR="${BASH_REMATCH[1]}"
MINOR="${BASH_REMATCH[2]}"
PATCH="${BASH_REMATCH[3]}"

cat > version.properties <<EOF
VERSION_MAJOR=$MAJOR
VERSION_MINOR=$MINOR
VERSION_PATCH=$PATCH
EOF

echo "✓ Synced version $NEW_VERSION to version.properties"
