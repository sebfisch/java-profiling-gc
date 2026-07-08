# Eclipse launch configurations

One launch configuration per script in [run/](../run), with the same JVM
flags, program arguments, and GC log destination (`logs/<name>.log`).
Import the project into Eclipse (File > Import > Existing Maven Projects)
and they appear under *Run > Run Configurations > Java Application*.

With the JProfiler Eclipse plugin installed, the same configurations show
up under *Profile As*, which replaces the scripts' `JPROFILER=1` switch:
the plugin loads the profiling agent itself.

`run/preflight.sh` has no counterpart here - it is a shell-level check,
not a Java program.
