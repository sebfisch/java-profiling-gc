package sebfisch.statements;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Pins the observable behaviour of the service. Account names are unique per
 * test because report caching may be shared within the JVM.
 */
class StatementServiceTests {

    private static Transaction tx(String account, long cents, String at) {
        return new Transaction(account, cents, Instant.parse(at));
    }

    @Test
    void statementsOfUnknownAccountAreEmpty() {
        StatementService service = new StatementService(new TransactionStore(Integer.MAX_VALUE));
        assertEquals(List.of(), service.monthlyStatements("STMT-EMPTY"));
    }

    @Test
    void oneStatementPerCalendarMonth() {
        TransactionStore store = new TransactionStore(Integer.MAX_VALUE);
        StatementService service = new StatementService(store);
        String account = "STMT-MONTHS";
        store.add(tx(account, 1500, "2024-01-10T09:00:00Z"));
        store.add(tx(account, -300, "2024-01-20T09:00:00Z"));
        store.add(tx(account, -800, "2024-02-05T09:00:00Z"));
        store.add(tx(account, 2000, "2024-04-01T09:00:00Z"));

        assertEquals(
                List.of(
                        new Statement(account, YearMonth.of(2024, 1), 0, 1200, 2),
                        new Statement(account, YearMonth.of(2024, 2), 1200, 400, 1),
                        new Statement(account, YearMonth.of(2024, 4), 400, 2400, 1)),
                service.monthlyStatements(account));
    }

    @Test
    void closingBalancesChainAcrossMonths() {
        TransactionStore store = new TransactionStore(Integer.MAX_VALUE);
        StatementService service = new StatementService(store);
        String account = "STMT-CHAIN";
        store.add(tx(account, 100, "2024-01-01T00:00:00Z"));
        store.add(tx(account, 100, "2024-02-01T00:00:00Z"));
        store.add(tx(account, 100, "2024-03-01T00:00:00Z"));

        List<Statement> statements = service.monthlyStatements(account);
        for (int i = 1; i < statements.size(); i++) {
            assertEquals(statements.get(i - 1).closingCents(), statements.get(i).openingCents());
        }
        assertEquals(store.balance(account), statements.getLast().closingCents());
    }

    @Test
    void reportCarriesTheStoredBalance() {
        TransactionStore store = new TransactionStore(Integer.MAX_VALUE);
        StatementService service = new StatementService(store);
        String account = "RPT-BALANCE";
        store.add(tx(account, 1234, "2024-01-01T00:00:00Z"));

        BalanceReport report = service.reportFor(account, Instant.parse("2024-06-01T00:00:00Z"));
        assertEquals(account, report.account());
        assertEquals(1234, report.balanceCents());
        assertEquals(1, report.recentTransactions());
        assertTrue(report.summary().contains(account));
    }

    @Test
    void repeatedQueriesAgree() {
        TransactionStore store = new TransactionStore(Integer.MAX_VALUE);
        StatementService service = new StatementService(store);
        String account = "RPT-REPEAT";
        store.add(tx(account, 500, "2024-01-01T00:00:00Z"));

        Instant asOf = Instant.parse("2024-06-01T00:00:00Z");
        assertEquals(service.reportFor(account, asOf), service.reportFor(account, asOf));
    }
}
