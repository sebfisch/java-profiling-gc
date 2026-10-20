# Worksheet

Record your numbers as you go — the discussion hours compare worksheets across
machines. Absolute numbers vary with hardware; the *shapes* should not.

Machine: ____________________ JDK build: ____________________
Cores: ____ RAM: ____ OS: ____________________

## Part 1 — Profiling

### T1.1 — the hotspot

Guilty frame (written down *before* looking at the source):

    ____________________________________________

| | batch throughput (tx/s) |
|---|---|
| before fix | |
| after fix | |

Speedup: ____×

### T1.2 — the churn

Top allocation hot spot: ____________________________________________

| | GC collections during run | total GC pause time |
|---|---|---|
| before fix | | |
| after fix | | |

Keep these numbers — T2.3 reuses them.

### T1.3 — the leak

Class with the largest retained size in the snapshot diff:

    ____________________________________________

GC root that keeps it alive: ____________________________________________

Heap curve after the fix (sketch or describe): ____________________

### T1.4 — the contention (stretch)

| workers | queries/s (before fix) | queries/s (after fix) |
|---|---|---|
| 4 | | |
| 16 | | |
| 32 | | |

Monitor that blocked the workers: ____________________________________________

## Part 2 — Choosing a collector

### T2.1 — throughput shoot-out (batch mode, fixed code, same -Xmx)

| collector | tx/s | total GC pause time | collections |
|---|---|---|---|
| Serial | | | |
| Parallel | | | |
| G1 | | | |
| ZGC | | | |
| Shenandoah | | | |

Winner on throughput: ____________ Expected? ____

### T2.2 — latency shoot-out (serve mode, fixed code)

| collector | p50 | p99 | p99.9 | max |
|---|---|---|---|---|
| G1 | | | | |
| ZGC | | | | |
| Shenandoah | | | | |

Same table with -Xmx shrunk to ______ :

| collector | p50 | p99 | p99.9 | max |
|---|---|---|---|---|
| G1 | | | | |

What changed, and why? ____________________________________________

### T2.3 — the allocation-rate lesson

G1 serve percentiles on the `allocation-storm` tag (same flags as T2.2):

| | p50 | p99 | p99.9 | max |
|---|---|---|---|---|
| fixed code (from T2.2) | | | | |
| allocation storm | | | | |

In your own words: what bought more tail latency — fixing the allocation
churn, or switching collectors?

    ____________________________________________
    ____________________________________________

### T2.4 — Epsilon as an instrument (stretch)

| | Epsilon: heap used at the end (MB) | JProfiler: total allocated (MB) |
|---|---|---|
| `fixed-all` | | |
| `allocation-storm` | | |
| storm ÷ fixed | ____× | ____× |

Do the two instruments agree on the ratio (roughly)? ____
