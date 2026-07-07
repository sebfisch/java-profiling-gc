#!/bin/sh
# Batch mode under Shenandoah at a fixed heap size.
# usage: batch-shenandoah.sh [accounts] [linesPerAccount] - defaults are BatchMain's
. "$(dirname -- "$0")/common.sh"
launch "batch-shenandoah" -XX:+UseShenandoahGC -Xmx1g sebfisch.statements.BatchMain "$@"
