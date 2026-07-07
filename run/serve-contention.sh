#!/bin/sh
# Serve mode sized for contention hunting: many workers, unpaced queries.
# usage: serve-contention.sh [workers] - raise the count and watch queries/s
. "$(dirname -- "$0")/common.sh"
launch "serve-contention" -XX:+UseG1GC -Xmx1g sebfisch.statements.ServeMain "${1:-16}" 60 0 20000
