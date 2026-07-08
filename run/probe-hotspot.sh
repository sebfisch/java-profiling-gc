#!/bin/sh
# CpuHotspot probe: one hot method, runs until interrupted.
# Sample CPU data and the call tree points at countOccurrences.
. "$(dirname -- "$0")/common.sh"
launch "probe-hotspot" -XX:+UseG1GC -Xmx1g sebfisch.probes.CpuHotspot
