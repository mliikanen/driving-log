#!/usr/bin/env bash
# The PR check's job summary: what merging this pull request would publish (add-ci-workflows, spec "The draft is
# shown on the pull request"). The checked-out commit is GitHub's merge of the PR into main.
set -euo pipefail
cd "$(dirname "$0")/../.."

notes="$(scripts/ci/choose-release-notes.sh HEAD)"
echo "### Release notes"
if [ -z "$notes" ]; then
  echo "Nothing to release: no change archived since the last distribution, and no \`## Release notes\` section in the PR."
else
  echo "Merging this would publish:"
  echo
  echo '```'
  printf '%s\n' "$notes"
  echo '```'
fi
