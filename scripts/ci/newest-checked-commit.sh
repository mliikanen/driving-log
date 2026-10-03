#!/usr/bin/env bash
# Prints the newest commit on <ref> (default origin/main), since the last distribution, whose PR check passed on push;
# prints nothing when there is none (add-ci-workflows, design.md decision 1). The release job publishes this commit,
# not the one that triggered it: GitHub keeps only the last-arriving pending run, which isn't always the newest commit,
# so the run that does go ahead reconciles to the newest one that's ready.
#
# Usage: scripts/ci/newest-checked-commit.sh [<ref>]    (needs gh with GH_TOKEN, full history and tags)
set -euo pipefail
# A failing command inside $(...) fails the script too (bash leaves errexit off in command substitutions by default).
shopt -s inherit_errexit
cd "$(dirname "$0")/../.."

ref="${1:-origin/main}"
repo="${GITHUB_REPOSITORY:-$(gh repo view --json nameWithOwner --jq .nameWithOwner)}"
last_tag="$(git tag -l 'dist-v*' --merged "$ref" --sort=-v:refname | sed -n 1p)"

# Captured first: a command substitution in a `for` list has its exit status ignored, so a failing rev-list would look
# like "nothing to release".
candidates="$(git rev-list --first-parent "$ref" ${last_tag:+"^$last_tag"})"
for sha in $candidates; do
  passed="$(gh api "repos/$repo/actions/workflows/pr-check.yml/runs?head_sha=$sha&event=push&status=success" --jq .total_count)"
  if [ "$passed" != "0" ]; then
    echo "$sha"
    exit 0
  fi
done
