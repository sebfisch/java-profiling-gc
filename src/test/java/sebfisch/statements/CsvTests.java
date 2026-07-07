package sebfisch.statements;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class CsvTests {

    @Test
    void parsesCanonicalLine() {
        assertEquals(
                new Transaction("ACC-0007", -1250, Instant.parse("2024-03-01T10:15:30Z")),
                Csv.parse("ACC-0007;-1250;2024-03-01T10:15:30Z"));
    }

    @Test
    void rejectsMalformedLines() {
        for (String line :
                List.of("", "garbage", "ACC-1;12", "acc-1;12;2024-01-01T00:00:00Z", "ACC-1;twelve;2024-01-01T00:00:00Z")) {
            assertThrows(IllegalArgumentException.class, () -> Csv.parse(line), line);
        }
    }

    @Test
    void parsesPositiveAmounts() {
        assertEquals(
                new Transaction("ACC-0000", 99, Instant.parse("2024-01-01T00:00:00Z")),
                Csv.parse("ACC-0000;99;2024-01-01T00:00:00Z"));
    }

    @Test
    void everyFeedLineIsParseable() {
        SyntheticFeed feed = new SyntheticFeed(7, 42);
        for (int i = 0; i < 10_000; i++) {
            Transaction transaction = Csv.parse(feed.nextLine());
            assertTrue(transaction.account().startsWith("ACC-"));
        }
    }

    @Test
    void feedIsDeterministic() {
        SyntheticFeed one = new SyntheticFeed(5, 25);
        SyntheticFeed two = new SyntheticFeed(5, 25);
        List<String> fromOne = Stream.generate(one::nextLine).limit(1_000).toList();
        List<String> fromTwo = Stream.generate(two::nextLine).limit(1_000).toList();
        assertEquals(fromOne, fromTwo);
    }

    @Test
    void feedRepeatsSomeLines() {
        SyntheticFeed feed = new SyntheticFeed(5, 25);
        long duplicates = 0;
        String previous = null;
        for (int i = 0; i < 10_000; i++) {
            String line = feed.nextLine();
            if (line.equals(previous)) {
                duplicates++;
            }
            previous = line;
        }
        assertTrue(duplicates > 0, "the feed models at-least-once delivery");
        assertTrue(duplicates < 1_000, "duplicates should stay the exception");
    }

    @Test
    void feedTimestampsAdvancePerAccount() {
        SyntheticFeed feed = new SyntheticFeed(2, 7);
        Instant previousForFirstAccount = null;
        for (int i = 0; i < 1_000; i++) {
            Transaction transaction = Csv.parse(feed.nextLine());
            if (transaction.account().equals(SyntheticFeed.accountName(0))) {
                if (previousForFirstAccount != null) {
                    assertTrue(!transaction.at().isBefore(previousForFirstAccount));
                }
                previousForFirstAccount = transaction.at();
            }
        }
    }
}
