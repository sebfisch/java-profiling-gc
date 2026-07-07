package sebfisch.statements;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Keeps the transactions of all accounts, deduplicating repeated deliveries
 * from the feed.
 *
 * <p>Balances are exact over the account's whole history, but only a window of
 * the most recent transactions per account is retained in detail (duplicate
 * deliveries arrive in bursts, so the window is where duplicates can still
 * occur). Batch mode uses an unbounded window to compute monthly statements;
 * serve mode uses a small window to keep the live set flat.
 */
public final class TransactionStore {

    private final int windowSize;
    private final Map<String, List<Transaction>> recentByAccount = new HashMap<>();
    private final Map<String, Long> balances = new HashMap<>();
    private final Map<String, Long> transactionCounts = new HashMap<>();

    public TransactionStore(int windowSize) {
        this.windowSize = windowSize;
    }

    /**
     * Records the transaction unless it is a duplicate delivery.
     *
     * @return whether the transaction was new
     */
    public synchronized boolean add(Transaction transaction) {
        List<Transaction> recent =
                recentByAccount.computeIfAbsent(transaction.account(), _ -> new ArrayList<>());
        if (recent.contains(transaction)) {
            return false;
        }
        recent.add(transaction);
        if (recent.size() > windowSize) {
            recent.removeFirst();
        }
        balances.merge(transaction.account(), transaction.cents(), Long::sum);
        transactionCounts.merge(transaction.account(), 1L, Long::sum);
        return true;
    }

    /** The exact balance over the account's whole history. */
    public synchronized long balance(String account) {
        return balances.getOrDefault(account, 0L);
    }

    /** How many distinct transactions were ever recorded for the account. */
    public synchronized long transactionCount(String account) {
        return transactionCounts.getOrDefault(account, 0L);
    }

    public synchronized long totalTransactions() {
        return transactionCounts.values().stream().mapToLong(Long::longValue).sum();
    }

    /** How many recent transactions are retained in detail for the account. */
    public synchronized int recentCount(String account) {
        return recentByAccount.getOrDefault(account, List.of()).size();
    }

    /** The retained transactions of the account, oldest first. */
    public synchronized List<Transaction> transactionsFor(String account) {
        return List.copyOf(recentByAccount.getOrDefault(account, List.of()));
    }

    public synchronized Set<String> accounts() {
        return Set.copyOf(recentByAccount.keySet());
    }
}
