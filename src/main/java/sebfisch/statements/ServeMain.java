package sebfisch.statements;

import java.time.Instant;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;
import java.util.random.RandomGenerator;

/**
 * Serve mode: answer balance queries from a pool of query workers while an
 * ingest thread keeps feeding transactions into the store. The metric is the
 * latency distribution of the queries — percentiles, never averages.
 *
 * <p>Usage: {@code ServeMain [workers] [seconds] [queriesPerWorkerPerSecond] [ingestPerSecond]}
 *
 * <p>A query rate of {@code 0} means unpaced: workers query as fast as they
 * can, and queries per second becomes the interesting number. Any positive
 * rate paces each worker, and the percentile table becomes the interesting
 * number. Progress lines print every ten seconds; their counts are approximate
 * because the workers are not stopped to be counted.
 */
public final class ServeMain {

    private static final int ACCOUNTS = 100;
    private static final int WINDOW_SIZE = 1_000;
    private static final long SEED = 25;

    private ServeMain() {}

    public static void main(String[] args) throws InterruptedException {
        int workers = args.length > 0 ? Integer.parseInt(args[0]) : 8;
        int seconds = args.length > 1 ? Integer.parseInt(args[1]) : 60;
        int queryRate = args.length > 2 ? Integer.parseInt(args[2]) : 5_000;
        int ingestRate = args.length > 3 ? Integer.parseInt(args[3]) : 20_000;

        IO.println("serve mode: %d workers for %d s, %s queries/s per worker, %d ingested tx/s"
                .formatted(workers, seconds, queryRate > 0 ? String.valueOf(queryRate) : "unpaced", ingestRate));

        TransactionStore store = new TransactionStore(WINDOW_SIZE);
        StatementService service = new StatementService(store);
        SyntheticFeed feed = new SyntheticFeed(ACCOUNTS, SEED);
        for (int i = 0; i < ACCOUNTS * WINDOW_SIZE; i++) {
            store.add(Csv.parse(feed.nextLine()));
        }
        IO.println("preloaded %d transactions for %d accounts".formatted(store.totalTransactions(), ACCOUNTS));

        AtomicBoolean running = new AtomicBoolean(true);
        Thread ingest = Thread.ofPlatform().name("ingest").start(() -> ingest(store, feed, ingestRate, running));

        String[] accountNames = new String[ACCOUNTS];
        for (int a = 0; a < ACCOUNTS; a++) {
            accountNames[a] = SyntheticFeed.accountName(a);
        }
        LatencyRecorder[] recorders = new LatencyRecorder[workers];
        Thread[] pool = new Thread[workers];
        for (int w = 0; w < workers; w++) {
            LatencyRecorder recorder = new LatencyRecorder();
            RandomGenerator random = new Random(SEED * 31 + w);
            recorders[w] = recorder;
            pool[w] = Thread.ofPlatform()
                    .name("query-" + w)
                    .start(() -> query(service, accountNames, random, queryRate, recorder, running));
        }

        long start = System.nanoTime();
        int elapsedSeconds = 0;
        while (elapsedSeconds < seconds) {
            int step = Math.min(10, seconds - elapsedSeconds);
            Thread.sleep(step * 1_000L);
            elapsedSeconds += step;
            long queriesSoFar = 0;
            for (LatencyRecorder recorder : recorders) {
                queriesSoFar += recorder.count();
            }
            IO.println("t+%03ds: ~%d queries, %d MB heap in use"
                    .formatted(elapsedSeconds, queriesSoFar, usedMegabytes()));
        }
        running.set(false);
        ingest.join();
        for (Thread worker : pool) {
            worker.join();
        }
        double elapsed = (System.nanoTime() - start) / 1e9;

        LatencyRecorder all = new LatencyRecorder();
        for (LatencyRecorder recorder : recorders) {
            all.add(recorder);
        }
        IO.println("");
        IO.println("queries    %d (%.0f queries/s)".formatted(all.count(), all.count() / elapsed));
        IO.println("audit      %d entries".formatted(AuditLog.GLOBAL.count()));
        IO.println("store      %d transactions ingested".formatted(store.totalTransactions()));
        IO.println("");
        IO.println("latency    p50     %s".formatted(human(all.percentile(50))));
        IO.println("           p90     %s".formatted(human(all.percentile(90))));
        IO.println("           p99     %s".formatted(human(all.percentile(99))));
        IO.println("           p99.9   %s".formatted(human(all.percentile(99.9))));
        IO.println("           max     %s".formatted(human(all.max())));
    }

    private static void ingest(TransactionStore store, SyntheticFeed feed, int perSecond, AtomicBoolean running) {
        long sliceNanos = 50_000_000L;
        long perSlice = Math.max(1, perSecond / 20);
        while (running.get()) {
            long sliceEnd = System.nanoTime() + sliceNanos;
            for (long i = 0; i < perSlice && running.get(); i++) {
                store.add(Csv.parse(feed.nextLine()));
            }
            long rest = sliceEnd - System.nanoTime();
            if (rest > 0) {
                LockSupport.parkNanos(rest);
            }
        }
    }

    /**
     * Paced workers measure each query from its *scheduled* start, not from
     * whenever the thread got around to it — otherwise a stall (a GC pause,
     * say) would inflate one sample instead of the whole queue behind it, and
     * the percentiles would flatter the tail. The literature calls that
     * mistake coordinated omission.
     */
    private static void query(
            StatementService service,
            String[] accountNames,
            RandomGenerator random,
            int perSecond,
            LatencyRecorder recorder,
            AtomicBoolean running) {
        long intervalNanos = perSecond > 0 ? 1_000_000_000L / perSecond : 0;
        long scheduled = System.nanoTime();
        while (running.get()) {
            if (intervalNanos > 0) {
                long wait = scheduled - System.nanoTime();
                if (wait > 0) {
                    LockSupport.parkNanos(wait);
                }
            } else {
                scheduled = System.nanoTime();
            }
            String account = accountNames[random.nextInt(accountNames.length)];
            BalanceReport report = service.reportFor(account, Instant.now());
            AuditLog.GLOBAL.record("query", report.account());
            recorder.record(System.nanoTime() - scheduled);
            scheduled += intervalNanos;
        }
    }

    private static long usedMegabytes() {
        Runtime runtime = Runtime.getRuntime();
        return (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
    }

    private static String human(long nanos) {
        if (nanos < 1_000) {
            return "%d ns".formatted(nanos);
        }
        if (nanos < 1_000_000) {
            return "%.1f µs".formatted(nanos / 1e3);
        }
        if (nanos < 1_000_000_000) {
            return "%.2f ms".formatted(nanos / 1e6);
        }
        return "%.2f s".formatted(nanos / 1e9);
    }
}
