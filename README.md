# Java Profiling and Garbage Collection

A one-day workshop on profiling Java applications with JProfiler and choosing
among the garbage collectors of recent JDKs. One application — a statement
service with four planted performance defects — carries the whole day: it is
profiled and fixed in the morning, then stressed under five collectors in the
afternoon. The course material lives in the project's GitHub Wiki; a local
copy is checked out at [wiki/](wiki/) as a submodule — if that directory is
empty, run `git submodule update --init`.

The tests are green in every state of the code; the defects cost time and
memory, not correctness. Start from the `seeded` tag, or jump ahead with
`fixed-d1` … `fixed-all`.

© Sebastian Fischer [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/)
