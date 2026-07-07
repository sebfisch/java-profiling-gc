package sebfisch.probes;

import java.util.HashMap;
import java.util.Map;

/**
 * One pathology only: a leak. The map remembers every request forever. Take a
 * heap snapshot, wait, take another, diff them: the byte arrays pile up, and
 * the path to GC roots ends at {@link #SEEN}. Heap usage climbs monotonically —
 * run with {@code -Xmx256m} to reach the OutOfMemoryError within minutes.
 * Runs until it does.
 */
public final class MemoryLeak {

    private static final Map<Long, byte[]> SEEN = new HashMap<>();

    private MemoryLeak() {}

    public static void main(String[] args) throws InterruptedException {
        long id = 0;
        while (true) {
            SEEN.put(id++, new byte[1_024]);
            if (id % 1_000 == 0) {
                Runtime runtime = Runtime.getRuntime();
                long usedMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
                IO.println("%d requests seen, %d MB heap in use".formatted(id, usedMb));
                Thread.sleep(100);
            }
        }
    }
}
