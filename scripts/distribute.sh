#!/usr/bin/env bash
# Cuts a signed, versioned release build and uploads it to Firebase App Distribution testers.
#
# Release notes are generated from the OpenSpec changes archived under openspec/changes/archive/ since the last
# distribution (see openspec/changes/archive/*/add-app-distribution or docs/app-distribution.md for the full
# design) and opened for review/edit before the upload runs. versionCode/versionName come from
# androidApp/build.gradle.kts, both derived from git — nothing here is hand-typed.
#
# Requires: a release keystore at ~/.android-keystores/ (see docs/distribution.md) and `firebase login`.
set -euo pipefail
cd "$(dirname "$0")/.."

last_tag="$(git tag -l 'dist-v*' --sort=-v:refname | head -1)"

if [ -n "$last_tag" ]; then
  last_tag_commit="$(git rev-list -n 1 "$last_tag")"
  head_commit="$(git rev-parse HEAD)"
  if [ "$last_tag_commit" = "$head_commit" ]; then
    echo "Nothing to distribute: HEAD is the same commit already tagged $last_tag." >&2
    exit 1
  fi
  range="$last_tag..HEAD"
else
  range="HEAD"
fi

notes_file="$(mktemp -t distribute-notes.XXXXXX)"
trap 'rm -f "$notes_file"' EXIT

# --no-renames: an archived change's files show up as `git mv`-style renames in `git commit`'s own summary, but
# without rename detection each is a plain delete-at-old-path + add-at-new-path, which is what lets
# --diff-filter=A find the new path. Forcing it off keeps this working regardless of the caller's git config.
git log --no-renames --diff-filter=A --name-only --pretty=format: "$range" -- openspec/changes/archive \
  | grep -E '^openspec/changes/archive/[^/]+/' \
  | sed -E 's#^openspec/changes/archive/([^/]+)/.*#\1#' \
  | sort -u > "$notes_file"

if [ -s "$notes_file" ]; then
  echo "Draft release notes generated from $(wc -l < "$notes_file") archived change(s). Review/edit, then save and close."
else
  echo "Nothing archived since the last distribution — nothing to generate. Type release notes by hand, then save and close."
fi
"${EDITOR:-nano}" "$notes_file"

if [ ! -s "$notes_file" ]; then
  echo "Release notes are empty — refusing to upload with nothing to say." >&2
  exit 1
fi

version_name="$(git rev-list --count HEAD)-$(git rev-parse --short HEAD)"

# The upload task uploads whatever release APK is already on disk and does not build one, so the APK is built in the same run:
# without assembleRelease, an APK left from an earlier build (another commit, another versionCode) is what testers get.
./gradlew :androidApp:assembleRelease :androidApp:appDistributionUploadRelease "-PdistributionReleaseNotesFile=$notes_file"

git tag "dist-v$version_name"
echo "Tagged dist-v$version_name (local only — not pushed)."
