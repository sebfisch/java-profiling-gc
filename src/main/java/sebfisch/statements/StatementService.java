package sebfisch.statements;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Answers the two questions the statement service exists for: monthly
 * statements over an account's retained history (batch mode) and balance
 * reports at a point in time (serve mode). Balance reports are cached so
 * repeated queries are served without recomputation.
 */
public final class StatementService {

    // D3: unbounded static cache; serve mode makes every key unique - a leak (T1.3)
    private static final Map<String, BalanceReport> REPORT_CACHE = new HashMap<>();

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
            REPORT_CACHE.put(key, report);
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
