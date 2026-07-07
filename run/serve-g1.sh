#!/bin/sh
# Serve mode under G1: paced queries, percentile table.
# usage: serve-g1.sh [workers] [seconds] [queriesPerWorkerPerSecond] [ingestPerSecond]
# Shrink the heap (edit -Xmx here) to see how pause behaviour depends on headroom.
. "$(dirname -- "$0")/common.sh"
launch "serve-g1" -XX:+UseG1GC -Xmx1g sebfisch.statements.ServeMain "$@"
