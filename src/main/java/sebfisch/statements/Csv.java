package sebfisch.statements;

import java.time.Instant;
import java.util.regex.Pattern;

/**
 * Parses the CSV line format used by the transaction feed:
 * {@code account;cents;timestamp}, for example
 * {@code ACC-0007;-1250;2024-03-01T10:15:30Z}. Malformed lines are rejected
 * with an {@link IllegalArgumentException}.
 */
public final class Csv {

    private static final Pattern VALID =
            Pattern.compile("[A-Z]+-\\d+;-?\\d+;\\d{4}-\\d{2}-\\d{2}T[0-9:]+Z");

    private Csv() {}

    public static Transaction parse(String line) {
        if (!VALID.matcher(line).matches()) {
            throw new IllegalArgumentException("malformed line: " + line);
        }
        int first = line.indexOf(';');
        int second = line.indexOf(';', first + 1);
        String account = line.substring(0, first);
        long cents = Long.parseLong(line, first + 1, second, 10);
        Instant at = Instant.parse(line.substring(second + 1));
        return new Transaction(account, cents, at);
    }
}
