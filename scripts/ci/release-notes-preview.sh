#!/usr/bin/env bash
# The PR check's job summary: what merging this pull request would publish (add-ci-workflows, spec "The draft is
# shown on the pull request"). The checked-out commit is GitHub's merge of the PR into main.
set -euo pipefail
cd "$(dirname "$0")/../.."

pr="${1:?usage: scripts/ci/release-notes-preview.sh <pull request number>}"
notes="$(scripts/ci/choose-release-notes.sh --pr "$pr" HEAD)"
echo "### Release notes"
if [ -z "$notes" ]; then
  echo "Nothing to release: no change archived since the last distribution, and no \`## Release notes\` section in this or a merged PR."
else
  echo "Merging this would publish:"
  echo
  echo '```'
  printf '%s\n' "$notes"
  echo '```'
fi
