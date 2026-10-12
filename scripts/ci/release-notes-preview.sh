#!/usr/bin/env bash
# The PR check's job summary: what merging this pull request would publish (add-ci-workflows, spec "The draft is
# shown on the pull request"). The checked-out commit is GitHub's merge of the PR into main.
set -euo pipefail
cd "$(dirname "$0")/../.."

pr="${1:?usage: scripts/ci/release-notes-preview.sh <pull request number>}"
echo "### Release notes"
# The same decision the release makes after merging (release-only-app-changes): did anything in the APK change since the
# last distribution, up to GitHub's merge of this pull request (HEAD)? If not, merging publishes nothing, whatever it
# archives or says. If so, a pull request that changed no app code itself still adds nothing to the notes.
last_tag="$(git tag -l 'dist-v*' --merged HEAD --sort=-v:refname | sed -n 1p)"
app=0
scripts/ci/app-changed.sh "$last_tag" HEAD || app=$?
if [ "$app" -eq 1 ]; then
  echo "Nothing to release: no change to the app since ${last_tag:-the first commit}, this pull request included (\`scripts/ci/app-changed.sh\`), so merging it publishes no build."
  exit 0
elif [ "$app" -ne 0 ]; then
  exit "$app"
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
