#!/usr/bin/env bash
# The spec validation of the final regression run and of CI's spec-validation job: every main spec and every active
# change, strictly, so any error or warning fails, a requirement over 500 characters included (issue #13 split the
# main specs' long ones, and split-long-requirements-d made this strict again).
#
# Usage: scripts/validate-specs.sh [<project root>]    (default: this repository)
set -uo pipefail
scripts="$(cd "$(dirname "$0")" && pwd)"
cd "${1:-$scripts/..}" || { echo "validate-specs: cannot enter ${1:-$scripts/..}" >&2; exit 2; }

openspec validate --all --strict --no-interactive
