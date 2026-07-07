package sebfisch.statements;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * An in-memory audit trail. Every operation of the service records one entry;
 * the log keeps the most recent entries in a fixed-size ring, so it never
 * grows, and hands out snapshots for inspection.
 */
public final class AuditLog {

    /** The shared audit log all service operations record to. */
    public static final AuditLog GLOBAL = new AuditLog(65_536);

    private final String[] ring;
    private long count;

    public AuditLog(int capacity) {
        this.ring = new String[capacity];
    }

    public synchronized void record(String event, String detail) {
        DateTimeFormatter stamps =
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").withZone(ZoneOffset.UTC);
        String entry = String.format("%s %-8s %s", stamps.format(Instant.now()), event, detail);
        ring[(int) (count % ring.length)] = entry;
        count++;
    }

    /** How many entries were ever recorded. */
    public synchronized long count() {
        return count;
    }

    /** The most recent entries, oldest first, at most {@code limit} of them. */
    public synchronized List<String> recent(int limit) {
        int stored = (int) Math.min(count, ring.length);
        int wanted = Math.min(limit, stored);
        List<String> entries = new ArrayList<>(wanted);
        for (long i = count - wanted; i < count; i++) {
            entries.add(ring[(int) (i % ring.length)]);
        }
        return List.copyOf(entries);
    }
}
