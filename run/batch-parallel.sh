#!/bin/sh
# Batch mode under the Parallel collector at a fixed heap size.
# usage: batch-parallel.sh [accounts] [linesPerAccount] - defaults are BatchMain's
. "$(dirname -- "$0")/common.sh"
launch "batch-parallel" -XX:+UseParallelGC -Xmx1g sebfisch.statements.BatchMain "$@"
