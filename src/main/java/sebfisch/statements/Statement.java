package sebfisch.statements;

import java.time.YearMonth;

/**
 * A monthly statement: the balance before and after one calendar month of
 * transactions on one account.
 */
public record Statement(
        String account, YearMonth month, long openingCents, long closingCents, int transactionCount) {}
