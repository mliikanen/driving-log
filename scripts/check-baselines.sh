#!/usr/bin/env bash
# Fails when a static-analysis baseline has more entries than at <base-ref>: baselines hold the findings that
# predate the gate and may only shrink (add-lint-quality-gates; see docs/code-quality.md). A baseline that doesn't
# exist at <base-ref> yet is being introduced, and is reported but not compared.
#
# Usage: scripts/check-baselines.sh <base-ref>     e.g. scripts/check-baselines.sh origin/main
set -euo pipefail
cd "$(dirname "$0")/.."

base="${1:?usage: scripts/check-baselines.sh <base-ref>}"
git rev-parse --verify --quiet "$base^{commit}" > /dev/null || { echo "Unknown ref: $base" >&2; exit 2; }

# detekt lists one <ID> per finding; Android lint one <issue> element per finding (inside the <issues> root, which
# the pattern must not count). Patterns are extended regular expressions.
baselines=(
  "config/detekt/baseline.xml|<ID>"
  "androidApp/lint-baseline.xml|<issue([[:space:]]|$)"
  "shared/lint-baseline.xml|<issue([[:space:]]|$)"
)

status=0
for entry in "${baselines[@]}"; do
  file="${entry%%|*}"
  marker="${entry#*|}"
  now=0
  [ -f "$file" ] && now="$(grep -cE -- "$marker" "$file" || true)"
  if ! git cat-file -e "$base:$file" 2> /dev/null; then
    echo "$file: $now entries (not at $base: introduced here)"
    continue
  fi
  before="$(git show "$base:$file" | grep -cE -- "$marker" || true)"
  if [ "$now" -gt "$before" ]; then
    echo "$file: grew from $before to $now entries. Fix the new findings, or suppress each in code with a reason; don't add them to the baseline." >&2
    status=1
  else
    echo "$file: $now entries ($before at $base)"
  fi
done
exit "$status"
