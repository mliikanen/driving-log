#!/usr/bin/env bash
# Whether <from>..<to> changes the app: anything that ends up in the APK testers install (release-only-app-changes,
# issue #22). A release, the release notes and the pull request's preview all ask this, so a merge that changes only
# docs, specs, tests or tooling publishes nothing. <from> may be empty: the whole history up to <to>.
#
# Usage: scripts/ci/app-changed.sh <from> <to>    (exit 0: the app changed; 1: it didn't; 2: error)
#        Run inside the repository. `<to>^1 <to>` asks about one commit.
set -uo pipefail

if [ $# -ne 2 ]; then
  echo "usage: scripts/ci/app-changed.sh <from> <to>" >&2
  exit 2
fi
from="$1"
to="$2"
# No previous distribution: everything up to <to> is new, compared with the empty tree.
[ -n "$from" ] || from="$(git hash-object -t tree /dev/null)" || exit 2

# What ends up in the APK. Broad on purpose: anything under the two modules and the build, minus what is known not to.
# A path missing here means a merge that changes the app publishes nothing; the preview on its pull request shows that.
app_paths=(
  shared                  # the shared module: common and Android code, SQLDelight, resources, its build script
  androidApp              # the Android app: its code, manifest, resources, build script
  gradle                  # dependency versions (libs.versions.toml), the wrapper, the daemon JVM
  build.gradle.kts        # the root build: compiler options, plugin versions
  settings.gradle.kts
  gradle.properties
  ':(exclude,glob)shared/src/*Test/**'      # commonTest, androidHostTest, iosTest: tests are not in the APK
  ':(exclude)shared/src/iosMain'            # iOS only
  ':(exclude,glob)androidApp/src/test*/**'  # the app's tests (testFake)
  ':(exclude)androidApp/src/fake'           # the fake flavor, for Maestro; testers get the production flavor
)

git diff --quiet "$from" "$to" -- "${app_paths[@]}"
case $? in
  0) exit 1 ;; # no difference: the app did not change
  1) exit 0 ;; # a difference: it did
  *) exit 2 ;;
esac
