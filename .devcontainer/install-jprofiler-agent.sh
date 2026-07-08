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

# The tarball ships the agent for every platform under bin/linux-<arch>/;
# prefer the one matching this container, fall back to the first found
# (covers a bare libjprofilerti.so dropped directly into the vendor dir).
case "$(uname -m)" in
  x86_64) ARCH_DIR=linux-x64 ;;
  aarch64) ARCH_DIR=linux-aarch64 ;;
  *) ARCH_DIR="" ;;
esac

find_agent_so() {
  local match=""
  if [ -n "$ARCH_DIR" ]; then
    match="$(find "$1" -path "*/$ARCH_DIR/libjprofilerti.so" -print -quit 2>/dev/null)"
  fi
  if [ -z "$match" ]; then
    match="$(find "$1" -name libjprofilerti.so -print -quit 2>/dev/null)"
  fi
  printf '%s\n' "$match"
}

so_path="$(find_agent_so "$VENDOR_DIR")"

if [ -z "$so_path" ]; then
  tarball="$(find "$VENDOR_DIR" -maxdepth 1 -name 'jprofiler_linux*.tar.gz' -print -quit 2>/dev/null)"
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
if [ -f "$(dirname "$so_path")/../agent.jar" ]; then
  # The agent locates sibling files from its install tree via the library's
  # real path (bin/agent.jar etc.) and aborts without them, so link into the
  # extracted tree instead of copying the bare library.
  sudo ln -sf "$so_path" "$TARGET_SO"
else
  sudo cp "$so_path" "$TARGET_SO"
  echo "Warning: no agent.jar next to the library; recent JProfiler versions" >&2
  echo "need their full install tree — prefer dropping the tarball instead." >&2
fi
echo "Staged JProfiler agent at $TARGET_SO"
echo "Use: -agentpath:$TARGET_SO=port=8849"
