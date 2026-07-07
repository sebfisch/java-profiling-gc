#!/bin/sh
# Batch mode under ZGC at a fixed heap size.
# usage: batch-zgc.sh [accounts] [linesPerAccount] - defaults are BatchMain's
. "$(dirname -- "$0")/common.sh"
launch "batch-zgc" -XX:+UseZGC -Xmx1g sebfisch.statements.BatchMain "$@"
