#!/usr/bin/env bash
# Optional postCreateCommand step: stage the JProfiler agent library so a
# JProfiler GUI on the host can attach to a JVM inside this container via
# -agentpath:/opt/jprofiler-agent/libjprofilerti.so=port=8849
#
# No-ops (exit 0) if nothing was dropped in jprofiler-vendor/ — JProfiler is
# optional here, see wiki/Home.md's "without JProfiler" notes.
set -euo pipefail

VENDOR_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/jprofiler-vendor"
INSTALL_DIR=/opt/jprofiler-agent
TARGET_SO="$INSTALL_DIR/libjprofilerti.so"

if [ -f "$TARGET_SO" ]; then
  echo "JProfiler agent already staged at $TARGET_SO"
  exit 0
fi

if [ ! -d "$VENDOR_DIR" ]; then
  exit 0
fi

find_agent_so() {
  find "$1" -name libjprofilerti.so -print -quit 2>/dev/null
}

so_path="$(find_agent_so "$VENDOR_DIR")"

if [ -z "$so_path" ]; then
  tarball="$(find "$VENDOR_DIR" -maxdepth 1 -name 'jprofiler_linux_*.tar.gz' -print -quit 2>/dev/null)"
  if [ -n "$tarball" ]; then
    echo "Extracting $tarball ..."
    extract_dir="$VENDOR_DIR/.extracted"
    mkdir -p "$extract_dir"
    tar xzf "$tarball" -C "$extract_dir"
    so_path="$(find_agent_so "$extract_dir")"
  fi
fi

if [ -z "$so_path" ]; then
  echo "No JProfiler agent found in $VENDOR_DIR — skipping (see its README.md)."
  exit 0
fi

sudo mkdir -p "$INSTALL_DIR"
sudo cp "$so_path" "$TARGET_SO"
echo "Staged JProfiler agent at $TARGET_SO"
echo "Use: -agentpath:$TARGET_SO=port=8849"
