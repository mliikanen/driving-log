#!/usr/bin/env bash
# Prints the release notes a CI distribution of <commit> publishes (add-ci-workflows, design.md decision 5): the text
# of the `## Release notes` section of the pull request body in $PR_BODY (up to the next `## ` heading) when it has
# any, otherwise the archive-generated list from scripts/release-notes.sh. Prints nothing when both are empty, which
# means there is nothing to release.
#
# Usage: PR_BODY="..." scripts/ci/choose-release-notes.sh [<commit>]
set -euo pipefail
cd "$(dirname "$0")/../.."

section="$(printf '%s\n' "${PR_BODY:-}" | tr -d '\r' | awk '
  /^## / { if (in_section) exit; if ($0 ~ /^## Release notes[[:space:]]*$/) { in_section = 1; next } }
  in_section { print }
' | sed -e '/./,$!d' | sed -e ':a' -e '/^\n*$/{$d;N;ba' -e '}')"

if [ -n "$(printf '%s' "$section" | tr -d '[:space:]')" ]; then
  printf '%s\n' "$section"
else
  scripts/release-notes.sh "${1:-HEAD}"
fi
