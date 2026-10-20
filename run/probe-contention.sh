#!/bin/sh
# LockContention probe: workers funneling through one lock, runs until
# interrupted. Watch the thread history turn red as workers are added.
# usage: probe-contention.sh [workers] - default is LockContention's (8)
. "$(dirname -- "$0")/common.sh"
# Collector: keep exactly one GC= line uncommented.
GC=-XX:+UseG1GC
# GC=-XX:+UseSerialGC
# GC=-XX:+UseParallelGC
# GC=-XX:+UseZGC
# GC=-XX:+UseShenandoahGC
# GC="-XX:+UnlockExperimentalVMOptions -XX:+UseEpsilonGC"
# GC is a flag list, split on purpose
# shellcheck disable=SC2086
launch "probe-contention" $GC -Xmx1g sebfisch.probes.LockContention "$@"
