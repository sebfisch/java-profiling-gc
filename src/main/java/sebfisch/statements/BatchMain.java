package sebfisch.statements;

/**
 * Batch mode: ingest the whole synthetic feed, then produce monthly statements
 * for every account. The metric is throughput — transactions per second — and
 * the checksum lets you confirm that a faster run still computed the same
 * result.
 *
 * <p>Usage: {@code BatchMain [accounts] [linesPerAccount] [seed]}
 */
public final class BatchMain {

    private BatchMain() {}

    public static void main(String[] args) {
        int accounts = args.length > 0 ? Integer.parseInt(args[0]) : 50;
        int linesPerAccount = args.length > 1 ? Integer.parseInt(args[1]) : 20_000;
        long seed = args.length > 2 ? Long.parseLong(args[2]) : 25;
        long totalLines = (long) accounts * linesPerAccount;

        IO.println("batch mode: %d accounts, %d feed lines, seed %d".formatted(accounts, totalLines, seed));

        TransactionStore store = new TransactionStore(Integer.MAX_VALUE);
        StatementService service = new StatementService(store);
        SyntheticFeed feed = new SyntheticFeed(accounts, seed);

        long start = System.nanoTime();
        for (long i = 0; i < totalLines; i++) {
            store.add(Csv.parse(feed.nextLine()));
        }
        long ingested = System.nanoTime();

        long checksum = 0;
        long statements = 0;
        for (String account : store.accounts()) {
            for (Statement statement : service.monthlyStatements(account)) {
                checksum += statement.closingCents();
                statements++;
            }
        }
        long done = System.nanoTime();

        IO.println("ingested   %d lines (%d unique transactions) in %.1f s"
                .formatted(totalLines, store.totalTransactions(), seconds(ingested - start)));
        IO.println("statements %d (checksum %d) in %.1f s"
                .formatted(statements, checksum, seconds(done - ingested)));
        IO.println("throughput %.0f tx/s".formatted(totalLines / seconds(done - start)));
    }

    private static double seconds(long nanos) {
        return nanos / 1e9;
    }
}
