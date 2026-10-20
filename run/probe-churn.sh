#!/bin/sh
# AllocationChurn probe: huge allocation rate, flat live heap, runs until
# interrupted. Record allocations and the hot spots point at buildReport.
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
launch "probe-churn" $GC -Xmx1g sebfisch.probes.AllocationChurn
