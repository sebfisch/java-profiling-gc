#!/bin/sh
# Batch mode under the Serial collector at a fixed heap size.
# usage: batch-serial.sh [accounts] [linesPerAccount] - defaults are BatchMain's
. "$(dirname -- "$0")/common.sh"
launch "batch-serial" -XX:+UseSerialGC -Xmx1g sebfisch.statements.BatchMain "$@"
