package sebfisch.probes;

/**
 * One pathology only: a single CPU hotspot. Attach a profiler, record CPU data
 * with sampling, and the call tree points at {@link #countOccurrences} — this
 * is what an unambiguous hot spot looks like. Runs until interrupted.
 */
public final class CpuHotspot {

    private static final String HAYSTACK =
            "lorem ipsum dolor sit amet, a needle, consectetur adipiscing elit ".repeat(2_000);

    private CpuHotspot() {}

    public static void main(String[] args) {
        long found = 0;
        long rounds = 0;
        long nextReport = System.nanoTime() + 1_000_000_000L;
        while (true) {
            found += countOccurrences(HAYSTACK, "needle");
            rounds++;
            if (System.nanoTime() > nextReport) {
                IO.println("%d rounds, %d needles found".formatted(rounds, found));
                nextReport += 1_000_000_000L;
            }
        }
    }

    /** Naive text search: every position is compared character by character. */
    static long countOccurrences(String text, String pattern) {
        long count = 0;
        for (int i = 0; i + pattern.length() <= text.length(); i++) {
            boolean match = true;
            for (int j = 0; j < pattern.length() && match; j++) {
                match = text.charAt(i + j) == pattern.charAt(j);
            }
            if (match) {
                count++;
            }
        }
        return count;
    }
}
