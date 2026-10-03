#!/usr/bin/env bash
# Prints the release notes a CI distribution of <commit> publishes (add-ci-workflows, design.md decision 5). Prints
# nothing when there is nothing to release.
#
# The notes cover everything since the last distribution (the newest dist-v* tag reachable from <commit>):
# - every pull request merged into main since then contributes the text of its `## Release notes` section (up to the
#   next `## ` heading), when it has one;
# - every change archived since then that no such pull request archived is listed by name, as scripts/release-notes.sh
#   lists them. A pull request without a section, or a commit that came without one, contributes this way.
# So a release that replaced a pending one (GitHub keeps one pending run) still carries both pull requests' notes.
#
# With --pr <number>, that open pull request counts too, with its current description: what merging it would publish.
#
# Usage: scripts/ci/choose-release-notes.sh [--pr <number>] [<commit>]    (needs gh with GH_TOKEN, and full history)
set -euo pipefail
# A failing command inside $(...) fails the script too (bash leaves errexit off in command substitutions by default).
shopt -s inherit_errexit
cd "$(dirname "$0")/../.."

extra_pr=""
if [ "${1:-}" = "--pr" ]; then
  extra_pr="$2"
  shift 2
fi
commit="${1:-HEAD}"
repo="${GITHUB_REPOSITORY:-$(gh repo view --json nameWithOwner --jq .nameWithOwner)}"

# The `## Release notes` section of a pull request body on stdin, without surrounding blank lines.
section_of() {
  tr -d '\r' | awk '
    /^## / { if (in_section) exit; if ($0 ~ /^## Release notes[[:space:]]*$/) { in_section = 1; next } }
    in_section { print }
  ' | sed -e '/./,$!d' | sed -e ':a' -e '/^\n*$/{$d;N;ba' -e '}'
}

# The changes a pull request archived: the change directories it adds under openspec/changes/archive/. A failing API
# call fails the script (notes built from a partial answer would be wrong); only "no such files" is an empty result.
archived_by() {
  local files
  files="$(gh api "repos/$repo/pulls/$1/files" --paginate --jq '.[] | select(.status == "added" or .status == "renamed") | .filename')"
  { grep -E '^openspec/changes/archive/[^/]+/' <<< "$files" || true; } | sed -E 's#^openspec/changes/archive/([^/]+)/.*#\1#' | sort -u
}

last_tag="$(git tag -l 'dist-v*' --merged "$commit" --sort=-v:refname | head -1)"
if [ -n "$last_tag" ]; then
  in_range="$(git rev-list "$last_tag..$commit")"
else
  in_range="$(git rev-list "$commit")"
fi

# Pull requests merged into main since the last distribution, oldest first, plus --pr: merged PRs whose merge commit
# (the commit on main, for merge, squash and rebase merges alike) is in the range. Pages of 100, most recently updated
# first, stopping at the first page reaching back before the last distribution: a PR is never merged after its last
# update, so nothing older can be in the range. Usually a single request.
cutoff=""
[ -n "$last_tag" ] && cutoff="$(TZ=UTC git log -1 --format=%cd --date=format-local:%Y-%m-%dT%H:%M:%SZ "$last_tag")"
merged=""
page=1
while :; do
  batch="$(gh api "repos/$repo/pulls?state=closed&base=main&sort=updated&direction=desc&per_page=100&page=$page" \
    --jq '.[] | "\(.updated_at) \(.merged_at // "-") \(.merge_commit_sha // "-") \(.number)"')"
  [ -z "$batch" ] && break
  while read -r updated merged_at sha number; do
    if [ "$merged_at" != "-" ] && grep -qx "$sha" <<< "$in_range"; then merged="$merged"$'\n'"$merged_at $number"; fi
  done <<< "$batch"
  oldest="$(tail -1 <<< "$batch" | cut -d' ' -f1)"
  if [ -n "$cutoff" ] && [[ "$oldest" < "$cutoff" ]]; then break; fi
  page=$((page + 1))
done
prs="$(printf '%s\n' "$merged" | sed '/^$/d' | sort -k1,1 | awk '!seen[$2]++ { print $2 }')"
[ -n "$extra_pr" ] && prs="$(printf '%s\n%s\n' "$prs" "$extra_pr" | sed '/^$/d')"

sections=""
covered=""
for number in $prs; do
  text="$(gh api "repos/$repo/pulls/$number" --jq '.body // ""' | section_of)"
  if [ -n "$(printf '%s' "$text" | tr -d '[:space:]')" ]; then
    sections="${sections:+$sections$'\n\n'}$text"
    covered="$covered"$'\n'"$(archived_by "$number")"
  fi
done

# Archived changes not already described by a pull request's own section. An empty pattern list excludes nothing.
remaining="$(scripts/release-notes.sh "$commit" | grep -vxF -f <(printf '%s\n' "$covered" | sed '/^$/d') || true)"

if [ -n "$sections" ] && [ -n "$remaining" ]; then
  printf '%s\n\n%s\n' "$sections" "$remaining"
elif [ -n "$sections" ]; then
  printf '%s\n' "$sections"
elif [ -n "$remaining" ]; then
  printf '%s\n' "$remaining"
fi
