package sebfisch.probes;

/**
 * One pathology only: one contended lock. Every worker does the same work
 * twice — once privately, once under the shared lock — so at most one thread
 * makes progress in the second half. Watch the thread history: mostly blocked,
 * and adding workers (first argument, default 8) adds no throughput.
 * Runs until interrupted.
 */
public final class LockContention {

    private static final Object LOCK = new Object();
    private static long shared = 0;

    @SuppressWarnings("unused") // written so the JIT cannot eliminate the spinning
    private static volatile long blackhole;

    private LockContention() {}

    public static void main(String[] args) throws InterruptedException {
        int workers = args.length > 0 ? Integer.parseInt(args[0]) : 8;
        for (int w = 0; w < workers; w++) {
            Thread.ofPlatform().name("worker-" + w).start(LockContention::work);
        }
        while (true) {
            Thread.sleep(1_000);
            synchronized (LOCK) {
                IO.println("%d units of work done by %d workers".formatted(shared, workers));
            }
        }
    }

    private static void work() {
        while (true) {
            blackhole ^= spin(10_000);
            synchronized (LOCK) {
                blackhole ^= spin(10_000);
                shared++;
            }
        }
    }

    /** Burns CPU; the caller publishes the result so the loop cannot be optimized away. */
    private static long spin(int iterations) {
        long mix = 25;
        for (int i = 0; i < iterations; i++) {
            mix = mix * 6364136223846793005L + 1442695040888963407L;
        }
        return mix;
    }
}
