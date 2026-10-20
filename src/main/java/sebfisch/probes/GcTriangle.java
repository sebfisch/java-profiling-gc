package sebfisch.probes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.LongAdder;

/**
 * Not a pathology but a yardstick: workers churn garbage on top of a live set
 * that slowly turns over, and a ticker thread wants to wake up every
 * millisecond and records how late it was — every pause the collector imposes
 * shows up as a late tick. Run it under each collector: the tick lateness
 * percentiles separate the collectors cleanly, and shrinking the live set
 * shows stop-the-world pauses shrinking with it. Allocation throughput and
 * peak resident memory are printed too, but on a workload that does nothing
 * except allocate they do not rank the collectors the way an application
 * does — batch mode of the statement service is the throughput benchmark.
 *
 * <p>Usage: {@code GcTriangle [liveMb] [seconds] [workers]}, defaults 300 MB,
 * 20 s, and half the available cores.
 */
public final class GcTriangle {

    private static volatile boolean running = true;

    private GcTriangle() {}

    public static void main(String[] args) throws InterruptedException, IOException {
        int liveMb = args.length > 0 ? Integer.parseInt(args[0]) : 300;
        int seconds = args.length > 1 ? Integer.parseInt(args[1]) : 20;
        int workers = args.length > 2
                ? Integer.parseInt(args[2])
                : Math.max(1, Runtime.getRuntime().availableProcessors() / 2);

        IO.println("live set %d MB, %d workers, %d s".formatted(liveMb, workers, seconds));

        long[] lateness = new long[seconds * 1_000];
        Thread ticker = Thread.ofPlatform().name("ticker").start(() -> tick(lateness));

        byte[][] live = new byte[liveMb * 1_024][];
        for (int i = 0; i < live.length; i++) {
            live[i] = new byte[1_024];
        }

        LongAdder allocated = new LongAdder();
        long end = System.nanoTime() + seconds * 1_000_000_000L;
        Thread[] threads = new Thread[workers];
        for (int w = 0; w < workers; w++) {
            threads[w] = Thread.ofPlatform().name("worker-" + w).start(() -> churn(live, allocated, end));
        }
        for (Thread thread : threads) {
            thread.join();
        }
        running = false;
        ticker.join();

        long[] late = Arrays.stream(lateness).filter(l -> l != 0).sorted().toArray();
        IO.println("throughput %d MB/s allocated".formatted(allocated.sum() / seconds / 1_000_000));
        IO.println("tick late  p50 %.2f ms, p99 %.2f ms, max %.2f ms".formatted(
                late[late.length / 2] / 1e6, late[late.length * 99 / 100] / 1e6, late[late.length - 1] / 1e6));
        IO.println("footprint  %s".formatted(peakResidentMemory()));
    }

    /** Garbage on top of the live set; one allocation in ten replaces a live chunk. */
    private static void churn(byte[][] live, LongAdder allocated, long end) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        while (System.nanoTime() < end) {
            for (int i = 0; i < 1_000; i++) {
                byte[] garbage = new byte[random.nextInt(16, 512)];
                allocated.add(garbage.length);
                if (i % 10 == 0) {
                    live[random.nextInt(live.length)] = new byte[1_024];
                }
            }
        }
    }

    /** Sleeps one millisecond at a time and records how much later than that it woke up. */
    private static void tick(long[] lateness) {
        int n = 0;
        while (running && n < lateness.length) {
            long before = System.nanoTime();
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                return;
            }
            lateness[n++] = System.nanoTime() - before - 1_000_000;
        }
    }

    /** The resident-set high-water mark from /proc (Linux only). */
    private static String peakResidentMemory() throws IOException {
        Path status = Path.of("/proc/self/status");
        if (!Files.exists(status)) {
            return "unknown (no /proc)";
        }
        return Files.readAllLines(status).stream()
                .filter(line -> line.startsWith("VmHWM:"))
                .map(line -> "peak RSS " + line.substring("VmHWM:".length()).strip())
                .findFirst()
                .orElse("unknown");
    }
}
