#!/usr/bin/env bash
# Prints the release notes generated from the OpenSpec archive: the names of the changes archived since the last
# distribution, one per line (empty when nothing was archived). A change archived by a commit the release recorded as
# skipped (a `dist-skip-v*` tag: it changed nothing in the app; scripts/ci/release-skips.sh) is left out; every other one
# is listed, a change from a merge that should have been released but wasn't included (release-only-app-changes). The last distribution is the newest `dist-v*` tag
# reachable from <commit> (default HEAD); without one, every archived change is listed. Used by scripts/distribute.sh and
# by CI (add-ci-workflows); see docs/app-distribution.md.
#
# Usage: scripts/release-notes.sh [<commit>]
set -euo pipefail
cd "$(dirname "$0")/.."

commit="${1:-HEAD}"
# `sed -n 1p`, not `head -1`: head stops reading, which would SIGPIPE `git tag` (and, with pipefail, fail the script)
# once the tag list outgrows the pipe buffer.
last_tag="$(git tag -l 'dist-v*' --merged "$commit" --sort=-v:refname | sed -n 1p)"
if [ -n "$last_tag" ]; then
  range="$last_tag..$commit"
else
  range="$commit"
fi

# --no-renames: an archived change's files show up as `git mv`-style renames in `git commit`'s own summary, but
# without rename detection each is a plain delete-at-old-path + add-at-new-path, which is what lets
# --diff-filter=A find the new path. Forcing it off keeps this working regardless of the caller's git config.
# The log is read first, so a failing `git log` fails the script; only the filter may legitimately find nothing. Each
# commit's files follow a "commit <sha>" line, so every archived change keeps the commit that archived it.
added="$(git log --no-renames --diff-filter=A --name-only --pretty=format:'commit %H' "$range" -- openspec/changes/archive)"
archived="$(awk '
  /^commit / { sha = $2; next }
  match($0, /^openspec\/changes\/archive\/[^\/]+\//) { name = substr($0, 26, RLENGTH - 26); print name, sha }
' <<< "$added" | sort -u)"

# Listed unless the commit that archived it carries a recorded skip.
while read -r name sha; do
  [ -n "$name" ] || continue
  [ -n "$(git tag --points-at "$sha" -l 'dist-skip-v*')" ] || echo "$name"
done <<< "$archived" | sort -u
