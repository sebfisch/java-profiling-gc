#!/bin/sh
# Serve mode under Shenandoah: paced queries, percentile table.
# usage: serve-shenandoah.sh [workers] [seconds] [queriesPerWorkerPerSecond] [ingestPerSecond]
# Shrink the heap (edit -Xmx here) to see how pause behaviour depends on headroom.
. "$(dirname -- "$0")/common.sh"
launch "serve-shenandoah" -XX:+UseShenandoahGC -Xmx1g sebfisch.statements.ServeMain "$@"
