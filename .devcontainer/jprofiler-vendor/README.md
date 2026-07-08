# JProfiler agent drop folder

This folder is git-ignored on purpose: JProfiler is proprietary software and
nothing here gets committed.

To let a JVM running inside the dev container be profiled by a JProfiler GUI
running on your host, put the **Linux** JProfiler download here before
(re)building the container:

- `jprofiler_linux_*.tar.gz` — the tarball distribution (preferred; no
  installer wizard to script around), or
- `libjprofilerti.so` itself, if you've already extracted or installed
  JProfiler elsewhere and just want to copy the agent library in.

Get the file from your own JProfiler license, or from the
[evaluation download](https://www.ej-technologies.com/download/jprofiler/files)
mentioned in the wiki — you need to accept ej-technologies' license yourself;
nothing in this repo fetches it for you.

`.devcontainer/install-jprofiler-agent.sh` runs automatically as the
container's `postCreateCommand`, finds whatever you dropped here, and copies
the agent library to `/opt/jprofiler-agent/libjprofilerti.so`. If this folder
is empty, the script does nothing and the container still builds fine —
JProfiler is optional, per the wiki's *without JProfiler* notes.
