#!/bin/sh
# Shared plumbing for the run scripts: locates the module root, checks that
# the classes are compiled, and starts the JVM with GC logging into logs/.
# Override the JVM with JAVA=/path/to/java, e.g. to compare JDK builds.
# Inject extra JVM flags with JAVA_OPTS='...'. Set JPROFILER=1 to load the
# staged JProfiler agent (optionally JPROFILER_PORT=, default 8849).
set -eu

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
CLASSES="$ROOT/target/classes"
JAVA=${JAVA:-java}
JAVA_OPTS=${JAVA_OPTS:-}

# JPROFILER=1 loads the staged profiling agent (see .devcontainer/jprofiler-vendor/).
# The JVM then waits until the JProfiler GUI attaches to the agent's port.
if [ "${JPROFILER:-0}" = 1 ]; then
    agent=/opt/jprofiler-agent/libjprofilerti.so
    if [ ! -f "$agent" ]; then
        echo "JPROFILER=1 but $agent not found - see .devcontainer/jprofiler-vendor/README.md" >&2
        exit 1
    fi
    JAVA_OPTS="$JAVA_OPTS -agentpath:$agent=port=${JPROFILER_PORT:-8849}"
    echo "JProfiler agent enabled - JVM waits for the GUI to attach on port ${JPROFILER_PORT:-8849}"
fi

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
    # JAVA_OPTS is a flag list, split on purpose
    # shellcheck disable=SC2086
    exec "$JAVA" $JAVA_OPTS "-Xlog:gc*:file=$ROOT/logs/$name.log" -cp "$CLASSES" "$@"
}
