#!/usr/bin/env bash
# The PR check's job summary: what merging this pull request would publish (add-ci-workflows, spec "The draft is
# shown on the pull request"). The checked-out commit is GitHub's merge of the PR into main.
set -euo pipefail
cd "$(dirname "$0")/../.."

pr="${1:?usage: scripts/ci/release-notes-preview.sh <pull request number>}"
echo "### Release notes"
# The decisions the release run will record after merging (release-only-app-changes): every merge since the last
# distribution, this pull request's merge (HEAD, GitHub's merge into main) included, that changed nothing in the app is
# a skip. The skip tags are made locally for the notes below and removed on exit; only the release run pushes them.
last_tag="$(git tag -l 'dist-v*' --merged HEAD --sort=-v:refname | sed -n 1p)"
local_skips=""
trap 'if [ -n "$local_skips" ]; then git tag -d $local_skips > /dev/null; fi' EXIT
local_skips="$(scripts/ci/release-skips.sh "$last_tag" HEAD)"
unreleased="$(git rev-list --first-parent HEAD ${last_tag:+"^$last_tag"} | while read -r sha; do
  [ -n "$(git tag --points-at "$sha" -l 'dist-skip-v*')" ] || echo "$sha"; done)"
if [ -z "$unreleased" ]; then
  echo "Nothing to release: no change to the app since ${last_tag:-the first commit}, this pull request included (\`scripts/ci/app-changed.sh\`), so merging it publishes no build."
  exit 0
fi
notes="$(scripts/ci/choose-release-notes.sh --pr "$pr" HEAD)"
if [ -z "$notes" ]; then
  echo "Nothing to release: no change archived since the last distribution, and no \`## Release notes\` section in this or a merged PR."
else
  echo "Merging this would publish:"
  echo
  echo '```'
  printf '%s\n' "$notes"
  echo '```'
fi
