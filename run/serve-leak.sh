#!/bin/sh
# Serve mode sized for leak hunting: a small heap and a long, gently paced run,
# so there is time for two heap snapshots before anything gives out.
. "$(dirname -- "$0")/common.sh"
launch "serve-leak" -XX:+UseG1GC -Xmx384m sebfisch.statements.ServeMain 8 300 2000 20000
