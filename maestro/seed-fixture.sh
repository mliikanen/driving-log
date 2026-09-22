#!/usr/bin/env bash
# speed-up-tests-with-db-fixtures: places a named fixture's database and picture files into the app's private
# storage, so a Maestro flow whose subject is rendering can start from data already in the state it needs instead
# of building it through the UI. Run before `maestro test` for a flow that wants a fixture (run.sh does this
# automatically for a flow with a companion <flow>.fixture file); the flow's own launchApp step must then use
# clearState: false (see docs/test-strategy.md) - this script already establishes a known-clean starting point.
#
#   seed-fixture.sh vehicle-with-log-and-note
#
# Needs adb (ADB=/path/to/adb if it is not on the PATH) and a running emulator/device with the debug app installed
# (run-as needs a debuggable build, which the debug APK already is).
set -euo pipefail
cd "$(dirname "$0")"

[ $# -eq 1 ] || { echo "usage: seed-fixture.sh <fixture-name>"; exit 2; }
name="$1"
db="assets/fixtures/$name.db"
[ -f "$db" ] || { echo "no fixture $db"; exit 1; }

ADB="${ADB:-adb}"
PKG="com.mikonoma.drivinglog"
STAGING="/data/local/tmp/maestro-fixture"

# 1: the app must not hold the database open while its file is replaced underneath it.
$ADB shell am force-stop "$PKG"
# 2: a known-empty starting point, the same guarantee launchApp's clearState: true gives a UI-driven flow.
$ADB shell pm clear "$PKG" > /dev/null

# 3: push to a world-readable staging path, then run-as to copy into the app's own private storage (a plain adb
# push cannot write there directly). mkdir -p first: pm clear can leave the app's data directories not yet created,
# since Android normally creates them on first launch.
$ADB shell rm -rf "$STAGING"
$ADB shell mkdir -p "$STAGING"
$ADB push "$db" "$STAGING/$name.db" > /dev/null
$ADB shell run-as "$PKG" mkdir -p databases files/pictures
$ADB shell run-as "$PKG" cp "$STAGING/$name.db" databases/driving-log.db
# All picture fixtures are pushed alongside every .db: today's fixtures share one vehicle picture; a future fixture
# needing its own would still find every checked-in picture file here, harmlessly unused by the ones that don't
# reference it.
for picture in assets/fixtures/*.png assets/fixtures/*.webp; do
  [ -e "$picture" ] || continue
  base="$(basename "$picture")"
  $ADB push "$picture" "$STAGING/$base" > /dev/null
  $ADB shell run-as "$PKG" cp "$STAGING/$base" "files/pictures/$base"
done
$ADB shell rm -rf "$STAGING"

echo "seeded $name"
