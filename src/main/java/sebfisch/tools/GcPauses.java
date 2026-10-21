package sebfisch.tools;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Sums the stop-the-world pauses in a GC log written with {@code -Xlog:gc*}:
 * how many, how long in total, and the longest, per kind of pause. The log
 * has every pause, but one line at a time; this adds them up.
 *
 * <p>Every collector logs a finished pause as one line that names a
 * {@code Pause} and ends with its duration — G1, Serial, Parallel and
 * Shenandoah under the {@code gc} tag, ZGC under {@code gc,phases}. The
 * {@code gc,start} line that opens a pause carries no duration, and ZGC's
 * periodic statistics table puts a space before its unit, so neither is
 * counted twice. Pause kinds are cut after their first parenthesis:
 * {@code Pause Young (Normal)} rather than every cause G1 appends to it.
 *
 * <p>Usage: {@code GcPauses logFile...}
 */
public final class GcPauses {

    // [2.312s][info][gc] GC(17) Pause Young (Normal) (G1 Evacuation Pause) 87M->23M(1024M) 3.402ms
    // [0.512s][info][gc,phases] GC(3) y: Pause Mark Start 0.010ms
    private static final Pattern PAUSE = Pattern.compile(
            "\\] (?:GC\\(\\d+\\) )?((?:\\w: )?Pause.*?)(?: \\d+[BKMG]->\\d+[BKMG]\\(\\d+[BKMG]\\))? (\\d+(?:\\.\\d+)?)ms$");

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

    /** One finished pause: its kind and how long it stopped the application. */
    record Pause(String kind, double millis) {}

    /** The pause a log line reports, or empty if it reports none. */
    static Optional<Pause> parse(String line) {
        Matcher matcher = PAUSE.matcher(line);
        if (!matcher.find()) {
            return Optional.empty();
        }
        String kind = matcher.group(1);
        int paren = kind.indexOf(')');
        return Optional.of(new Pause(
                paren < 0 ? kind : kind.substring(0, paren + 1), Double.parseDouble(matcher.group(2))));
    }

    private static void summarize(Path log) throws IOException {
        Map<String, Total> byKind = new LinkedHashMap<>();
        Total all = new Total();
        try (Stream<String> lines = Files.lines(log)) {
            lines.map(GcPauses::parse).flatMap(Optional::stream).forEach(pause -> {
                byKind.computeIfAbsent(pause.kind(), _ -> new Total()).add(pause.millis());
                all.add(pause.millis());
            });
        }
        IO.println(log);
        if (all.count == 0) {
            IO.println("  no pauses logged");
            return;
        }
        IO.println("  %7s  %12s  %10s  %s".formatted("pauses", "total", "max", "kind"));
        byKind.forEach((kind, total) -> IO.println(total.row(kind)));
        IO.println(all.row("all pauses"));
    }

    private static final class Total {
        private long count;
        private double millis;
        private double max;

        void add(double pauseMillis) {
            count++;
            millis += pauseMillis;
            max = Math.max(max, pauseMillis);
        }

        String row(String kind) {
            return "  %7d  %9.3f ms  %7.3f ms  %s".formatted(count, millis, max, kind);
        }
    }
}
