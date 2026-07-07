#!/bin/sh
# Serve mode under ZGC: paced queries, percentile table.
# usage: serve-zgc.sh [workers] [seconds] [queriesPerWorkerPerSecond] [ingestPerSecond]
# Shrink the heap (edit -Xmx here) to see how pause behaviour depends on headroom.
. "$(dirname -- "$0")/common.sh"
launch "serve-zgc" -XX:+UseZGC -Xmx1g sebfisch.statements.ServeMain "$@"
