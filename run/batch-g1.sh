#!/bin/sh
# Batch mode under G1 at a fixed heap size.
# usage: batch-g1.sh [accounts] [linesPerAccount] - defaults are BatchMain's
. "$(dirname -- "$0")/common.sh"
launch "batch-g1" -XX:+UseG1GC -Xmx1g sebfisch.statements.BatchMain "$@"
