package sebfisch.statements;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.SequencedMap;

/**
 * Answers the two questions the statement service exists for: monthly
 * statements over an account's retained history (batch mode) and balance
 * reports at a point in time (serve mode). Balance reports are cached so
 * repeated queries are served without recomputation; the cache is bounded,
 * evicting the oldest entry once full.
 */
public final class StatementService {

    private static final int MAX_CACHED_REPORTS = 10_000;
    private static final SequencedMap<String, BalanceReport> REPORT_CACHE = new LinkedHashMap<>();

    private final TransactionStore store;

    public StatementService(TransactionStore store) {
        this.store = store;
    }

    /**
     * One statement per calendar month of the account's retained transactions,
     * which arrive in time order from the feed.
     */
    public List<Statement> monthlyStatements(String account) {
        List<Statement> statements = new ArrayList<>();
        YearMonth month = null;
        long opening = 0;
        long closing = 0;
        int count = 0;
        for (Transaction transaction : store.transactionsFor(account)) {
            YearMonth transactionMonth = YearMonth.from(transaction.at().atOffset(ZoneOffset.UTC));
            if (!transactionMonth.equals(month)) {
                if (month != null) {
                    statements.add(new Statement(account, month, opening, closing, count));
                }
                month = transactionMonth;
                opening = closing;
                count = 0;
            }
            closing += transaction.cents();
            count++;
        }
        if (month != null) {
            statements.add(new Statement(account, month, opening, closing, count));
        }
        return List.copyOf(statements);
    }

    /** The account's balance as of the given instant, served from cache when possible. */
    public BalanceReport reportFor(String account, Instant asOf) {
        String key = account + "@" + asOf;
        synchronized (REPORT_CACHE) {
            BalanceReport cached = REPORT_CACHE.get(key);
            if (cached != null) {
                return cached;
            }
        }
        BalanceReport report = computeReport(account, asOf);
        synchronized (REPORT_CACHE) {
            REPORT_CACHE.putLast(key, report);
            if (REPORT_CACHE.size() > MAX_CACHED_REPORTS) {
                REPORT_CACHE.pollFirstEntry();
            }
        }
        return report;
    }

    private BalanceReport computeReport(String account, Instant asOf) {
        long balance = store.balance(account);
        int recent = store.recentCount(account);
        String summary =
                "%s balance %d.%02d as of %s (%d recent transactions)"
                        .formatted(account, balance / 100, Math.abs(balance % 100), asOf, recent);
        return new BalanceReport(account, asOf, balance, recent, summary);
    }
}
