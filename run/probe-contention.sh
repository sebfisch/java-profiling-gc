#!/bin/sh
# LockContention probe: workers funneling through one lock, runs until
# interrupted. Watch the thread history turn red as workers are added.
# usage: probe-contention.sh [workers] - default is LockContention's (8)
. "$(dirname -- "$0")/common.sh"
launch "probe-contention" -XX:+UseG1GC -Xmx1g sebfisch.probes.LockContention "$@"
