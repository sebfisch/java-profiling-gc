#!/bin/sh
# Instructor tool: run after advancing main to restack the fix chain
# (stages/fixed-d1 .. stages/allocation-storm) and the instructor branch on
# top of it. Branches follow `git rebase --update-refs`, so one rebase moves
# the whole chain; the learner-facing tags are minted from these branches by
# publish.sh and therefore cannot go stale in between.
#
# On a rebase conflict: resolve, `git rebase --continue`, then rerun this
# script (already-restacked branches rebase as a no-op).
set -eu

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$ROOT"

if ! git diff-index --quiet HEAD --; then
    echo "working tree not clean - commit or stash first" >&2
    exit 1
fi

current=$(git symbolic-ref --short HEAD)

git rebase --update-refs main stages/allocation-storm
git rebase main instructor

git checkout -q "$current"
echo
git log --oneline --graph main stages/allocation-storm instructor
