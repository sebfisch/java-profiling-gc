package sebfisch.probes;

/**
 * One pathology only: allocation churn. Attach a profiler, record allocations,
 * and the allocation hot spots point at {@link #buildReport} — every {@code +=}
 * copies the whole string built so far. The GC telemetry shows the storm too:
 * lots of collections, but heap usage stays flat, because nothing survives.
 * Runs until interrupted.
 */
public final class AllocationChurn {

    private AllocationChurn() {}

    public static void main(String[] args) {
        long built = 0;
        long nextReport = System.nanoTime() + 1_000_000_000L;
        while (true) {
            built += buildReport(1_000).length();
            if (System.nanoTime() > nextReport) {
                IO.println("%d characters built".formatted(built));
                nextReport += 1_000_000_000L;
            }
        }
    }

    /** Builds a report by string concatenation in a loop. */
    static String buildReport(int lines) {
        String report = "";
        for (int i = 0; i < lines; i++) {
            report += "line " + i + ": all good\n";
        }
        return report;
    }
}
