#!/bin/sh
# MemoryLeak probe: a map that only grows. The small heap keeps the run short -
# time enough for two heap snapshots before the OutOfMemoryError ends it.
. "$(dirname -- "$0")/common.sh"
launch "probe-leak" -XX:+UseG1GC -Xmx256m sebfisch.probes.MemoryLeak
