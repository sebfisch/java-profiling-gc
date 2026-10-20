#!/bin/sh
# MemoryLeak probe: a map that only grows. The small heap keeps the run short -
# time enough for two heap snapshots before the OutOfMemoryError ends it.
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
launch "probe-leak" $GC -Xmx256m sebfisch.probes.MemoryLeak
