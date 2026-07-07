package sebfisch.statements;

import java.time.Instant;

/**
 * The answer to a balance query in serve mode: the exact balance of the
 * account at the time of the query, how many recent transactions the store
 * currently retains for it, and a human-readable summary line.
 */
public record BalanceReport(
        String account, Instant asOf, long balanceCents, int recentTransactions, String summary) {}
