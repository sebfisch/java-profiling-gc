# JProfiler agent drop folder

This folder is git-ignored on purpose: JProfiler is proprietary software and
nothing here gets committed.

To let a JVM running inside the dev container be profiled by a JProfiler GUI
running on your host, put the **Linux** JProfiler download here before
(re)building the container:

- `jprofiler_linux*.tar.gz` — the tarball distribution, e.g.
  `jprofiler_linux-x64_16_1_1.tar.gz` (preferred; no installer wizard to
  script around), or
- `libjprofilerti.so` itself, if you've already extracted or installed
  JProfiler elsewhere and just want to copy the agent library in — note that
  recent JProfiler versions abort at JVM startup without the rest of their
  install tree (`bin/agent.jar` and friends), so the tarball is the reliable
  choice.

Get the file from your own JProfiler license, or from the
[evaluation download](https://www.ej-technologies.com/download/jprofiler/files)
mentioned in the wiki — you need to accept ej-technologies' license yourself;
nothing in this repo fetches it for you.

`.devcontainer/install-jprofiler-agent.sh` runs automatically as the
container's `postCreateCommand`, finds whatever you dropped here, and copies
the agent library to `/opt/jprofiler-agent/libjprofilerti.so`. If this folder
is empty, the script does nothing and the container still builds fine —
JProfiler is optional, per the wiki's *without JProfiler* notes.

Once staged, run any script with `JPROFILER=1` (e.g. `JPROFILER=1
run/serve-g1.sh`) to launch the JVM with the agent loaded; it waits on
forwarded port 8849 until the JProfiler GUI on your host attaches.
