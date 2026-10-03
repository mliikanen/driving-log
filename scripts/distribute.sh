#!/usr/bin/env bash
# Cuts a signed, versioned release build and uploads it to Firebase App Distribution testers.
#
# Release notes are generated from the OpenSpec changes archived under openspec/changes/archive/ since the last
# distribution (see openspec/changes/archive/*/add-app-distribution or docs/app-distribution.md for the full
# design) and opened for review/edit before the upload runs. versionCode/versionName come from
# androidApp/build.gradle.kts, both derived from git — nothing here is hand-typed.
#
# Requires: a release keystore at ~/.android-keystores/ (see docs/distribution.md) and `firebase login`.
# CI publishes on every merge to main (add-ci-workflows); this is the manual fallback. It fetches the remote's
# dist-v* tags first and pushes the tag it creates, so a local and a CI distribution agree on what went out last.
set -euo pipefail
cd "$(dirname "$0")/.."

git fetch --tags origin
# A tag whose push failed last time (the upload had succeeded) is pushed now, so the remote knows it was distributed.
git push --quiet origin 'refs/tags/dist-v*:refs/tags/dist-v*'

already="$(git tag --contains HEAD -l 'dist-v*' | tr '\n' ' ')"
if [ -n "$already" ]; then
  echo "Nothing to distribute: HEAD is already in a distributed build ($already)." >&2
  exit 1
fi

notes_file="$(mktemp -t distribute-notes.XXXXXX)"
trap 'rm -f "$notes_file"' EXIT

scripts/release-notes.sh HEAD > "$notes_file"

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
# without assembleProductionRelease, an APK left from an earlier build (another commit, another versionCode) is what testers get.
# Flavor-qualified (not the unqualified assembleRelease/appDistributionUploadRelease) since add-firebase-auth added a
# second `fake` flavor whose release variant has no distribution config at all (see docs/distribution.md).
./gradlew :androidApp:assembleProductionRelease :androidApp:appDistributionUploadProductionRelease "-PdistributionReleaseNotesFile=$notes_file"

git tag "dist-v$version_name"
git push origin "dist-v$version_name"
echo "Tagged dist-v$version_name and pushed the tag."
