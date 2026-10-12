#!/usr/bin/env bash
# The spec validation of the final regression run and of CI's spec-validation job (ratchet-spec-validation):
# - every active change strictly: any error or warning fails, a new requirement over 500 characters included;
# - the main specs leniently: errors fail, their existing long-requirement warnings are shown and pass;
# - no requirement over 500 characters is new or grew (scripts/check-requirement-length.mjs), in an active change or in
#   the main specs compared with the base branch, so an archived change can't slip one in.
# Every step runs even when an earlier one failed, so one run shows every problem; the script fails if any did.
#
# Issue #13 ends this: once the main specs' long requirements are split, its last change replaces the steps below
# with `openspec validate --all --strict --no-interactive` and deletes check-requirement-length.mjs.
#
# Usage: scripts/validate-specs.sh [<project root>]    (default: this repository)
set -uo pipefail
scripts="$(cd "$(dirname "$0")" && pwd)"
cd "${1:-$scripts/..}" || { echo "validate-specs: cannot enter ${1:-$scripts/..}" >&2; exit 2; }

failed=0
step() {
  echo "== $*"
  "$@" || failed=1
}

step openspec validate --changes --strict --no-interactive
step openspec validate --specs --no-interactive
step node "$scripts/check-requirement-length.mjs"

exit "$failed"
