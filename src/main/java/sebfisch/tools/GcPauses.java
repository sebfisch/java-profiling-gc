package sebfisch.tools;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Sums the stop-the-world pauses in a GC log written with {@code -Xlog:gc*}:
 * how many, how long in total, the longest, and the median heap occupancy
 * after them, per kind of pause. The log has every pause, but one line at a
 * time; this adds them up.
 *
 * <p>Every collector logs a finished pause as one line that names a
 * {@code Pause} and ends with its duration — G1, Serial, Parallel and
 * Shenandoah under the {@code gc} tag, ZGC under {@code gc,phases}. The
 * {@code gc,start} line that opens a pause carries no duration, and ZGC's
 * periodic statistics table puts a space before its unit, so neither is
 * counted twice. Pause kinds are cut after their first parenthesis:
 * {@code Pause Young (Normal)} rather than every cause G1 appends to it.
 *
 * <p>Serial, Parallel and G1 log the heap before and after each pause; the
 * median of the after values approximates what survives a collection, which
 * is what sizing the heap has to leave room for. ZGC and Shenandoah log no
 * sizes on their pause lines, so their pauses show none.
 *
 * <p>ZGC also logs allocation stalls: a thread that allocates faster than the
 * concurrent collector reclaims waits until memory is free, one line per
 * stall. A stall stops only the thread that allocates, a pause stops all of
 * them, so stalls are summed apart from the pauses, per thread, most stalled
 * first, and for all threads. Stall times of different threads overlap;
 * their total can exceed the run time.
 *
 * <p>Usage: {@code GcPauses logFile...}
 */
public final class GcPauses {

    // [2.312s][info][gc] GC(17) Pause Young (Normal) (G1 Evacuation Pause) 87M->23M(1024M) 3.402ms
    // [0.512s][info][gc,phases] GC(3) y: Pause Mark Start 0.010ms
    private static final Pattern PAUSE = Pattern.compile(
            "\\] (?:GC\\(\\d+\\) )?((?:\\w: )?Pause.*?)(?: \\d+[BKMG]->(\\d+)([BKMG])\\(\\d+[BKMG]\\))? (\\d+(?:\\.\\d+)?)ms$");

    // [1.336s][info][gc] Allocation Stall (worker-6) 13.655ms
    private static final Pattern STALL = Pattern.compile(
            "\\] Allocation Stall \\((.+)\\) (\\d+(?:\\.\\d+)?)ms$");

    private GcPauses() {}

    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            IO.println("usage: GcPauses logFile...");
            return;
        }
        for (String arg : args) {
            summarize(Path.of(arg));
        }
    }

    /**
     * One finished pause: its kind, how long it stopped the application, and
     * the heap in use after it, in MB, if the collector logs that.
     */
    record Pause(String kind, double millis, OptionalDouble heapAfterMb) {}

    /** The pause a log line reports, or empty if it reports none. */
    static Optional<Pause> parse(String line) {
        Matcher matcher = PAUSE.matcher(line);
        if (!matcher.find()) {
            return Optional.empty();
        }
        String kind = matcher.group(1);
        int paren = kind.indexOf(')');
        OptionalDouble heapAfterMb = matcher.group(2) == null
                ? OptionalDouble.empty()
                : OptionalDouble.of(megabytes(Long.parseLong(matcher.group(2)), matcher.group(3)));
        return Optional.of(new Pause(
                paren < 0 ? kind : kind.substring(0, paren + 1),
                Double.parseDouble(matcher.group(4)),
                heapAfterMb));
    }

    /** One allocation stall: the thread that waited, and how long. */
    record Stall(String thread, double millis) {}

    /** The allocation stall a log line reports, or empty if it reports none. */
    static Optional<Stall> parseStall(String line) {
        Matcher matcher = STALL.matcher(line);
        if (!matcher.find()) {
            return Optional.empty();
        }
        return Optional.of(new Stall(matcher.group(1), Double.parseDouble(matcher.group(2))));
    }

    private static double megabytes(long size, String unit) {
        return switch (unit) {
            case "B" -> size / (1024.0 * 1024.0);
            case "K" -> size / 1024.0;
            case "M" -> size;
            default -> size * 1024.0;
        };
    }

    private static void summarize(Path log) throws IOException {
        Map<String, Total> byKind = new LinkedHashMap<>();
        Total all = new Total();
        Map<String, StallTotal> byThread = new LinkedHashMap<>();
        StallTotal allStalls = new StallTotal();
        try (Stream<String> lines = Files.lines(log)) {
            lines.forEach(line -> {
                parse(line).ifPresent(pause -> {
                    byKind.computeIfAbsent(pause.kind(), _ -> new Total()).add(pause);
                    all.add(pause);
                });
                parseStall(line).ifPresent(stall -> {
                    byThread.computeIfAbsent(stall.thread(), _ -> new StallTotal()).add(stall);
                    allStalls.add(stall);
                });
            });
        }
        IO.println(log);
        if (all.count == 0) {
            IO.println("  no pauses logged");
        } else {
            IO.println("  %7s  %12s  %10s  %10s  %s".formatted("pauses", "total", "max", "heap after", "kind"));
            byKind.forEach((kind, total) -> IO.println(total.row(kind)));
            IO.println(all.row("all pauses"));
        }
        if (allStalls.count > 0) {
            IO.println("  %7s  %12s  %10s  %s".formatted("stalls", "total", "max", "thread"));
            byThread.entrySet().stream()
                    .sorted(Comparator.comparingDouble((Map.Entry<String, StallTotal> e) -> e.getValue().millis)
                            .reversed())
                    .forEach(e -> IO.println(e.getValue().row(e.getKey())));
            IO.println(allStalls.row("all threads"));
        }
    }

    private static final class StallTotal {
        private long count;
        private double millis;
        private double max;

        void add(Stall stall) {
            count++;
            millis += stall.millis();
            max = Math.max(max, stall.millis());
        }

        String row(String thread) {
            return "  %7d  %9.3f ms  %7.3f ms  %s".formatted(count, millis, max, thread);
        }
    }

    private static final class Total {
        private long count;
        private double millis;
        private double max;
        private final List<Double> heapAfterMb = new ArrayList<>();

        void add(Pause pause) {
            count++;
            millis += pause.millis();
            max = Math.max(max, pause.millis());
            pause.heapAfterMb().ifPresent(heapAfterMb::add);
        }

        /** The median heap after these pauses, or a dash if none was logged. */
        private String medianHeapAfter() {
            if (heapAfterMb.isEmpty()) {
                return "-";
            }
            List<Double> sorted = heapAfterMb.stream().sorted().toList();
            return "%.0f MB".formatted(sorted.get(sorted.size() / 2));
        }

        String row(String kind) {
            return "  %7d  %9.3f ms  %7.3f ms  %10s  %s".formatted(count, millis, max, medianHeapAfter(), kind);
        }
    }
}
