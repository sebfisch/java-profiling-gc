package sebfisch.statements;

/**
 * Records latencies in nanoseconds and reports percentiles, without any
 * dependency. Values land in logarithmic buckets — 32 sub-buckets per power of
 * two — so memory stays constant no matter how many samples arrive, at the
 * price of about three percent resolution: plenty for percentile *shapes*,
 * which is all this course compares. (For publishable benchmarks, use a real
 * histogram library such as HdrHistogram.)
 *
 * <p>Not thread-safe by design: give each worker its own recorder and merge
 * them with {@link #add} after the workers have stopped.
 */
public final class LatencyRecorder {

    private static final int MINOR_BITS = 5;
    private static final int MINORS = 1 << MINOR_BITS;

    private final long[] counts = new long[(64 - MINOR_BITS) * MINORS];
    private long total;
    private long min = Long.MAX_VALUE;
    private long max = 0;

    public void record(long nanos) {
        long value = Math.max(nanos, 0);
        counts[indexOf(value)]++;
        total++;
        min = Math.min(min, value);
        max = Math.max(max, value);
    }

    /** Merges another recorder's samples into this one. */
    public void add(LatencyRecorder other) {
        for (int i = 0; i < counts.length; i++) {
            counts[i] += other.counts[i];
        }
        total += other.total;
        min = Math.min(min, other.min);
        max = Math.max(max, other.max);
    }

    public long count() {
        return total;
    }

    public long min() {
        return total == 0 ? 0 : min;
    }

    public long max() {
        return max;
    }

    /**
     * The smallest recorded value such that at least {@code percent} percent of
     * all samples are less than or equal to it, up to bucket resolution.
     */
    public long percentile(double percent) {
        if (total == 0) {
            return 0;
        }
        long rank = Math.clamp((long) Math.ceil(percent / 100.0 * total), 1, total);
        long seen = 0;
        for (int i = 0; i < counts.length; i++) {
            seen += counts[i];
            if (seen >= rank) {
                return Math.clamp(lowerBoundOf(i), min, max);
            }
        }
        return max;
    }

    private static int indexOf(long value) {
        if (value < MINORS) {
            return (int) value;
        }
        int major = 63 - Long.numberOfLeadingZeros(value);
        int shift = major - MINOR_BITS;
        int minor = (int) ((value >>> shift) - MINORS);
        return (major - MINOR_BITS + 1) * MINORS + minor;
    }

    private static long lowerBoundOf(int index) {
        if (index < MINORS) {
            return index;
        }
        int major = index / MINORS + MINOR_BITS - 1;
        int minor = index % MINORS;
        return ((long) (minor + MINORS)) << (major - MINOR_BITS);
    }
}
