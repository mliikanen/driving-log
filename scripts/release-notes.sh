#!/usr/bin/env bash
# Prints the release notes generated from the OpenSpec archive: the names of the changes archived since the last
# distribution, one per line (empty when nothing was archived). The last distribution is the newest `dist-v*` tag
# reachable from <commit> (default HEAD); without one, every archived change is listed. Used by scripts/distribute.sh and
# by CI (add-ci-workflows); see docs/app-distribution.md.
#
# Usage: scripts/release-notes.sh [<commit>]
set -euo pipefail
cd "$(dirname "$0")/.."

commit="${1:-HEAD}"
last_tag="$(git tag -l 'dist-v*' --merged "$commit" --sort=-v:refname | head -1)"
if [ -n "$last_tag" ]; then
  range="$last_tag..$commit"
else
  range="$commit"
fi

# --no-renames: an archived change's files show up as `git mv`-style renames in `git commit`'s own summary, but
# without rename detection each is a plain delete-at-old-path + add-at-new-path, which is what lets
# --diff-filter=A find the new path. Forcing it off keeps this working regardless of the caller's git config.
# The log is read first, so a failing `git log` fails the script; only the filter may legitimately find nothing.
added="$(git log --no-renames --diff-filter=A --name-only --pretty=format: "$range" -- openspec/changes/archive)"
{ grep -E '^openspec/changes/archive/[^/]+/' <<< "$added" || true; } \
  | sed -E 's#^openspec/changes/archive/([^/]+)/.*#\1#' \
  | sort -u
