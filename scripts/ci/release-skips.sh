#!/usr/bin/env bash
# Records the release's skip decisions (release-only-app-changes, issue #22): every commit on <target>'s first-parent
# line since <last dist tag> that has no decision yet is judged by scripts/ci/app-changed.sh, and one that changed nothing
# in the app gets a `dist-skip-v<count>-<short sha>` tag. Prints the tags it created, one per line (the caller pushes
# them). A commit already tagged is not judged again, so a decision, once recorded, stays.
#
# The notes skip exactly these commits (scripts/release-notes.sh, scripts/ci/choose-release-notes.sh): a merge with no
# skip tag is always collected, so one that should have been released but wasn't (a failed upload, say) has its notes
# in the next release that is made.
#
# Usage: scripts/ci/release-skips.sh <last dist tag, or empty> <target>    (run inside the repository)
set -euo pipefail
# A failing command inside $(...) fails the script too (bash leaves errexit off in command substitutions by default).
shopt -s inherit_errexit

if [ $# -ne 2 ]; then
  echo "usage: scripts/ci/release-skips.sh <last dist tag, or empty> <target>" >&2
  exit 2
fi
from="$1"
to="$2"
here="$(cd "$(dirname "$0")" && pwd)"

# Oldest first, captured first: a failing rev-list in a `for` list would look like "nothing to judge".
commits="$(git rev-list --first-parent --reverse "$to" ${from:+"^$from"})"
for sha in $commits; do
  [ -z "$(git tag --points-at "$sha" -l 'dist-skip-v*')" ] || continue
  parent="$(git rev-parse --verify --quiet "$sha^1" || true)"
  status=0
  "$here/app-changed.sh" "$parent" "$sha" || status=$?
  case "$status" in
    0) ;; # the app changed: no skip, its notes are collected
    1)
      tag="dist-skip-v$(git rev-list --count "$sha")-$(git rev-parse --short "$sha")"
      git tag "$tag" "$sha"
      echo "$tag"
      ;;
    *)
      echo "release-skips: could not tell whether $sha changed the app" >&2
      exit 2
      ;;
  esac
done
