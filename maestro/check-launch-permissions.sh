#!/usr/bin/env bash
# Every launchApp step of every flow starts the app with all its permissions unset, as a fresh install has them (docs/test-strategy.md).
# Maestro's launchApp grants every permission by default, which hid #29: any function needing a permission the user hasn't allowed then
# goes untested, including one the platform starts to require later. A flow that needs a permission allowed for another reason says so
# after `all: unset` (e.g. `camera: allow`). Fails listing each launchApp step without `all: unset`. Needs no device; run.sh runs it first.
set -euo pipefail
cd "$(dirname "$0")"

missing="$(find . -name '*.yaml' -not -path './manifests/*' -print0 | sort -z | xargs -0 awk '
  function check() { if (open && !unset) print where; open = 0 }
  FNR == 1 { check() }
  /^- launchApp:?[[:space:]]*$/ { check(); open = 1; unset = 0; where = FILENAME ":" FNR; next }
  open && /^      all: unset[[:space:]]*$/ { unset = 1 }
  open && !/^    / { check() }
  END { check() }
')"
if [ -n "$missing" ]; then
  echo "FAIL: launchApp without permissions: { all: unset } (docs/test-strategy.md):"; echo "$missing" | sed 's/^/  /'
  exit 1
fi
echo "ok: every launchApp starts with all permissions unset"
