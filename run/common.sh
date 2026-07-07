#!/bin/sh
# Shared plumbing for the run scripts: locates the module root, checks that
# the classes are compiled, and starts the JVM with GC logging into logs/.
# Override the JVM with JAVA=/path/to/java, e.g. to compare JDK builds.
set -eu

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
CLASSES="$ROOT/target/classes"
JAVA=${JAVA:-java}

if [ ! -d "$CLASSES" ]; then
    echo "$CLASSES not found - run 'mvn compile' first" >&2
    exit 1
fi

# usage: launch <log-name> <jvm-flags...> <main-class> <program-args...>
launch() {
    name="$1"
    shift
    mkdir -p "$ROOT/logs"
    echo "GC log: logs/$name.log"
    exec "$JAVA" "-Xlog:gc*:file=$ROOT/logs/$name.log" -cp "$CLASSES" "$@"
}
