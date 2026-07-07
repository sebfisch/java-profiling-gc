package sebfisch.statements;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class TransactionStoreTests {

    private static Transaction tx(String account, long cents, int second) {
        return new Transaction(account, cents, Instant.parse("2024-01-01T00:00:00Z").plusSeconds(second));
    }

    @Test
    void acceptsNewTransactions() {
        TransactionStore store = new TransactionStore(Integer.MAX_VALUE);
        assertTrue(store.add(tx("A", 100, 0)));
        assertTrue(store.add(tx("A", 100, 1)));
        assertEquals(2, store.totalTransactions());
    }

    @Test
    void rejectsDuplicateDeliveries() {
        TransactionStore store = new TransactionStore(Integer.MAX_VALUE);
        assertTrue(store.add(tx("A", 100, 0)));
        assertFalse(store.add(tx("A", 100, 0)));
        assertEquals(1, store.transactionCount("A"));
        assertEquals(100, store.balance("A"));
    }

    @Test
    void balanceSumsAllTransactions() {
        TransactionStore store = new TransactionStore(Integer.MAX_VALUE);
        store.add(tx("A", 1500, 0));
        store.add(tx("A", -300, 1));
        store.add(tx("A", -800, 2));
        assertEquals(400, store.balance("A"));
        assertEquals(0, store.balance("unknown"));
    }

    @Test
    void keepsAccountsApart() {
        TransactionStore store = new TransactionStore(Integer.MAX_VALUE);
        store.add(tx("A", 100, 0));
        store.add(tx("B", 200, 0));
        assertEquals(100, store.balance("A"));
        assertEquals(200, store.balance("B"));
        assertEquals(2, store.accounts().size());
    }

    @Test
    void retainsTransactionsInArrivalOrder() {
        TransactionStore store = new TransactionStore(Integer.MAX_VALUE);
        List<Transaction> transactions = List.of(tx("A", 1, 0), tx("A", 2, 1), tx("A", 3, 2));
        transactions.forEach(store::add);
        assertEquals(transactions, store.transactionsFor("A"));
    }

    @Test
    void evictsOldestBeyondWindowButKeepsBalanceExact() {
        TransactionStore store = new TransactionStore(2);
        store.add(tx("A", 100, 0));
        store.add(tx("A", 200, 1));
        store.add(tx("A", 400, 2));
        assertEquals(List.of(tx("A", 200, 1), tx("A", 400, 2)), store.transactionsFor("A"));
        assertEquals(2, store.recentCount("A"));
        assertEquals(700, store.balance("A"), "balance covers evicted transactions too");
        assertEquals(3, store.transactionCount("A"));
    }
}
