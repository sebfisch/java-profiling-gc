#!/bin/sh
# Batch mode under Epsilon, which allocates and never collects: the JVM as an
# allocation-rate meter. Either the run completes (the log's final heap usage
# is the total allocation) or it dies with an OutOfMemoryError (expected and
# documented - divide the heap size by the runtime for the allocation rate).
. "$(dirname -- "$0")/common.sh"
launch "batch-epsilon" -XX:+UnlockExperimentalVMOptions -XX:+UseEpsilonGC -Xmx2g \
    sebfisch.statements.BatchMain "$@"
