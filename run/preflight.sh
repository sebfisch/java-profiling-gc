#!/bin/sh
# Run this once at the start of the exercise block: confirms that the local
# JDK is a major version 25 and probes each collector's availability, printing
# one go/no-go line per collector instead of letting the first shoot-out die
# on a flag error. Override the JVM with JAVA=/path/to/java.

JAVA=${JAVA:-java}

version=$("$JAVA" -version 2>&1 | head -n 1)
major=$(printf '%s' "$version" | sed -E 's/.*"([0-9]+)[^"]*".*/\1/')

echo "java:        $version"
if [ "$major" = "25" ]; then
    echo "JDK 25:      go"
else
    echo "JDK 25:      NO-GO (major version $major; the course is written against JDK 25)"
fi

probe() {
    name="$1"
    shift
    if "$JAVA" "$@" -version >/dev/null 2>&1; then
        printf '%-12s go\n' "$name:"
    else
        printf '%-12s NO-GO (%s)\n' "$name:" "$*"
    fi
}

probe Serial -XX:+UseSerialGC
probe Parallel -XX:+UseParallelGC
probe G1 -XX:+UseG1GC
probe ZGC -XX:+UseZGC
probe Shenandoah -XX:+UseShenandoahGC
probe Epsilon -XX:+UnlockExperimentalVMOptions -XX:+UseEpsilonGC

# The course material lives in the wiki submodule.
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
if [ -f "$ROOT/wiki/Home.md" ]; then
    echo "wiki:        go"
else
    echo "wiki:        NO-GO (run 'git submodule update --init')"
fi

# Informational only - the exercises don't require JProfiler.
if [ -f /opt/jprofiler-agent/libjprofilerti.so ]; then
    echo "JProfiler:   agent staged (run any script with JPROFILER=1 to load it)"
else
    echo "JProfiler:   agent not staged (optional; see .devcontainer/jprofiler-vendor/README.md)"
fi
