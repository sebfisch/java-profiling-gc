package sebfisch.statements;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

/**
 * Parses the CSV line format used by the transaction feed:
 * {@code account;cents;timestamp}, for example
 * {@code ACC-0007;-1250;2024-03-01T10:15:30Z}. Malformed lines are rejected
 * with an {@link IllegalArgumentException}.
 */
public final class Csv {

    private Csv() {}

    public static Transaction parse(String line) {
        // D2: fresh Pattern, split array, boxed Long, throwaway formatter - churn on every line (T1.2)
        if (!line.matches("[A-Z]+-\\d+;-?\\d+;\\d{4}-\\d{2}-\\d{2}T[0-9:]+Z")) {
            throw new IllegalArgumentException("malformed line: " + line);
        }
        String[] fields = line.split(";");
        DateTimeFormatter timestamps = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssX");
        Long cents = Long.valueOf(fields[1]);
        Instant at = timestamps.parse(fields[2], Instant::from);
        return new Transaction(fields[0], cents, at);
    }
}
