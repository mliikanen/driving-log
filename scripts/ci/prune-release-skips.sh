#!/usr/bin/env bash
# Deletes the skip tags a release has made obsolete (release-only-app-changes): every `dist-skip-v*` tag on a commit the
# release <dist tag> contains. They were only read while those merges were "since the last release"; once a release
# covers them they are never read again. Prints the tags it deleted, one per line (the caller pushes the deletions).
# Release tags (`dist-v*`) are never touched: only the `dist-skip-v*` pattern is listed.
#
# Usage: scripts/ci/prune-release-skips.sh <dist tag>    (run inside the repository)
set -euo pipefail
shopt -s inherit_errexit

if [ $# -ne 1 ]; then
  echo "usage: scripts/ci/prune-release-skips.sh <dist tag>" >&2
  exit 2
fi
release="$1"
git rev-parse --verify --quiet "$release^{commit}" > /dev/null || { echo "prune-release-skips: no commit $release" >&2; exit 2; }

obsolete="$(git tag -l 'dist-skip-v*' --merged "$release")"
for tag in $obsolete; do
  git tag -d "$tag" > /dev/null
  echo "$tag"
done
