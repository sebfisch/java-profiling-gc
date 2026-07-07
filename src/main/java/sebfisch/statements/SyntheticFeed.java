package sebfisch.statements;

import java.time.Instant;
import java.util.Random;
import java.util.random.RandomGenerator;

/**
 * A deterministic source of CSV transaction lines, standing in for a message
 * queue or an export file so runs are reproducible and free of I/O variance.
 *
 * <p>The feed models at-least-once delivery: about one line in a hundred is a
 * repetition of the previous line, so ingestion has to deduplicate. Lines are
 * dealt round-robin across accounts, and each account's timestamps advance by
 * a fixed step, so every account's transactions arrive in time order and span
 * several calendar months.
 *
 * <p>The feed is deliberately frugal — account names are precomputed and each
 * timestamp string is formatted once per round — so that what you measure
 * downstream of it is the service, not the test harness.
 */
public final class SyntheticFeed {

    private static final Instant BASE = Instant.parse("2024-01-01T00:00:00Z");
    private static final long STEP_SECONDS = 2_000;
    private static final int DUPLICATE_PERCENT = 1;

    private final String[] accountNames;
    private final RandomGenerator random;
    private long index;
    private String previous;
    private long timestampPosition = -1;
    private String timestamp;

    public SyntheticFeed(int accounts, long seed) {
        this.accountNames = new String[accounts];
        for (int account = 0; account < accounts; account++) {
            accountNames[account] = accountName(account);
        }
        this.random = new Random(seed);
    }

    public static String accountName(long number) {
        return "ACC-%04d".formatted(number);
    }

    public synchronized String nextLine() {
        if (previous != null && random.nextInt(100) < DUPLICATE_PERCENT) {
            return previous;
        }
        int account = (int) (index % accountNames.length);
        long position = index / accountNames.length;
        if (position != timestampPosition) {
            timestamp = BASE.plusSeconds(position * STEP_SECONDS).toString();
            timestampPosition = position;
        }
        long cents = random.nextLong(-80_00, 150_00);
        index++;
        previous = new StringBuilder(40)
                .append(accountNames[account])
                .append(';')
                .append(cents)
                .append(';')
                .append(timestamp)
                .toString();
        return previous;
    }
}
