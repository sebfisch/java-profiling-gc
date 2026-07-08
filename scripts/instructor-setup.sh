#!/bin/sh
# Instructor tool: (re)generate the untracked instructor devcontainer config
# at .devcontainer/instructor/devcontainer.json. It is the learner config plus
# a bind mount that maps the host sibling repo java-profiling-gc.wiki (the
# local upstream of the wiki submodule) to /workspaces/java-profiling-gc.wiki,
# so it survives container rebuilds. Learners never get this file - it is
# gitignored - and therefore see no devcontainer config picker.
#
# The heredoc below is a copy of .devcontainer/devcontainer.json; keep it in
# sync by hand when the learner config changes.
#
# One-time host bootstrap (this script prints these steps again when run):
#   1. on the host, in the parent directory of this repo's checkout:
#        git init -b master java-profiling-gc.wiki
#        git -C java-profiling-gc.wiki config receive.denyCurrentBranch updateInstead
#   2. VS Code: "Dev Containers: Rebuild Container", pick
#      "java-profiling-gc (instructor)"
#   3. in the new container, populate the host sibling from the submodule:
#        git -C wiki push origin master
set -eu

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)

mkdir -p "$ROOT/.devcontainer/instructor"
cat > "$ROOT/.devcontainer/instructor/devcontainer.json" <<'EOF'
{
  "name": "java-profiling-gc (instructor)",
  "image": "mcr.microsoft.com/devcontainers/base:ubuntu",
  "mounts": [
    "type=tmpfs,destination=/tmp,tmpfs-mode=1777",
    "source=${localWorkspaceFolder}/../java-profiling-gc.wiki,target=/workspaces/java-profiling-gc.wiki,type=bind"
  ],
  "features": {
    "ghcr.io/devcontainers/features/java:1": {
      "version": "25",
      "jdkDistro": "tem",
      "installMaven": "true"
    }
  },
  "forwardPorts": [8849],
  "portsAttributes": {
    "8849": { "label": "JProfiler agent" }
  },
  "postCreateCommand": "git config protocol.file.allow always; git submodule update --init wiki; bash .devcontainer/install-jprofiler-agent.sh",
  "customizations": {
    "vscode": {
      "extensions": ["vscjava.vscode-java-pack", "mhutchie.git-graph"]
    }
  }
}
EOF

echo "wrote .devcontainer/instructor/devcontainer.json"
echo
echo "one-time host bootstrap, if the host sibling repo does not exist yet:"
echo "  1. on the host, in the parent directory of this repo's checkout:"
echo "       git init -b master java-profiling-gc.wiki"
echo "       git -C java-profiling-gc.wiki config receive.denyCurrentBranch updateInstead"
echo "  2. VS Code: 'Dev Containers: Rebuild Container', pick 'java-profiling-gc (instructor)'"
echo "  3. in the new container: git -C wiki push origin master"
