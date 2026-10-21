#!/bin/sh
# Sums the pauses in GC logs: count, total and longest pause, per kind of
# pause and for all of them - the numbers the worksheet asks for.
# usage: gc-pauses.sh logs/batch-g1.log [more logs...]
. "$(dirname -- "$0")/common.sh"
exec "$JAVA" -cp "$CLASSES" sebfisch.tools.GcPauses "$@"
