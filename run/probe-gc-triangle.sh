#!/bin/sh
# GcTriangle probe: a turning-over live set, garbage on top, and a ticker that
# catches every pause. Finishes with throughput, tick lateness and peak RSS -
# rerun under each collector below and compare the tick lateness.
# usage: probe-gc-triangle.sh [liveMb] [seconds] [workers] - defaults are
# GcTriangle's (300 MB, 20 s, half the cores)
. "$(dirname -- "$0")/common.sh"
# Collector: keep exactly one GC= line uncommented.
# GC=-XX:+UseG1GC
# GC=-XX:+UseSerialGC
# GC=-XX:+UseParallelGC
GC=-XX:+UseZGC
# GC=-XX:+UseShenandoahGC
# GC="-XX:+UnlockExperimentalVMOptions -XX:+UseEpsilonGC"
# GC is a flag list, split on purpose
# shellcheck disable=SC2086
launch "probe-gc-triangle" $GC -Xmx2g sebfisch.probes.GcTriangle "$@"
