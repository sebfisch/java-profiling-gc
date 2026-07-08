#!/bin/sh
# AllocationChurn probe: huge allocation rate, flat live heap, runs until
# interrupted. Record allocations and the hot spots point at buildReport.
. "$(dirname -- "$0")/common.sh"
launch "probe-churn" -XX:+UseG1GC -Xmx1g sebfisch.probes.AllocationChurn
