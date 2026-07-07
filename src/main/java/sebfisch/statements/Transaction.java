package sebfisch.statements;

import java.time.Instant;

/**
 * A single money movement on an account. Amounts are integer minor units
 * ({@code long cents}) so money stays exact without a {@code BigDecimal}
 * detour. Two transactions are duplicates exactly when all components match,
 * which is what record equality gives us.
 */
public record Transaction(String account, long cents, Instant at) {}
