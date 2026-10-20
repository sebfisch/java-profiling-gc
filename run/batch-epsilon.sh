#!/bin/sh
# Batch mode under Epsilon, which allocates and never collects: the JVM as a
# byte counter. If the run completes, the log's final heap usage is the total
# allocation (grep 'Heap:' logs/batch-epsilon.log | tail -1); if it dies with
# an OutOfMemoryError, that is expected and documented - shrink the input.
. "$(dirname -- "$0")/common.sh"
launch "batch-epsilon" -XX:+UnlockExperimentalVMOptions -XX:+UseEpsilonGC -Xmx2g \
    sebfisch.statements.BatchMain "$@"
