package sebfisch.statements;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class AuditLogTests {

    @Test
    void recordsEventAndDetail() {
        AuditLog log = new AuditLog(8);
        log.record("query", "ACC-0001");
        List<String> entries = log.recent(10);
        assertEquals(1, entries.size());
        assertTrue(entries.getFirst().contains("query"));
        assertTrue(entries.getFirst().contains("ACC-0001"));
    }

    @Test
    void keepsOnlyTheMostRecentEntries() {
        AuditLog log = new AuditLog(3);
        for (int i = 0; i < 5; i++) {
            log.record("event", "detail-" + i);
        }
        assertEquals(5, log.count());
        List<String> entries = log.recent(10);
        assertEquals(3, entries.size());
        assertTrue(entries.getFirst().contains("detail-2"), "oldest surviving entry comes first");
        assertTrue(entries.getLast().contains("detail-4"));
    }

    @Test
    void limitsSnapshotSize() {
        AuditLog log = new AuditLog(8);
        for (int i = 0; i < 6; i++) {
            log.record("event", "detail-" + i);
        }
        assertEquals(2, log.recent(2).size());
        assertTrue(log.recent(2).getLast().contains("detail-5"));
    }
}
