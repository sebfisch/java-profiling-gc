#!/bin/sh
# CpuHotspot probe: one hot method, runs until interrupted.
# Sample CPU data and the call tree points at countOccurrences.
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
launch "probe-hotspot" $GC -Xmx1g sebfisch.probes.CpuHotspot
