package sebfisch.statements;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import org.junit.jupiter.api.Test;

class LatencyRecorderTests {

    @Test
    void startsEmpty() {
        LatencyRecorder recorder = new LatencyRecorder();
        assertEquals(0, recorder.count());
        assertEquals(0, recorder.min());
        assertEquals(0, recorder.max());
        assertEquals(0, recorder.percentile(50));
    }

    @Test
    void tracksExtremesExactly() {
        LatencyRecorder recorder = new LatencyRecorder();
        recorder.record(17);
        recorder.record(1_000_000);
        recorder.record(423);
        assertEquals(3, recorder.count());
        assertEquals(17, recorder.min());
        assertEquals(1_000_000, recorder.max());
    }

    @Test
    void smallValuesAreExact() {
        LatencyRecorder recorder = new LatencyRecorder();
        for (int value = 0; value < 32; value++) {
            recorder.record(value);
        }
        assertEquals(0, recorder.percentile(1));
        assertEquals(15, recorder.percentile(50));
        assertEquals(31, recorder.percentile(100));
    }

    @Test
    void percentilesFallWithinBucketResolution() {
        LatencyRecorder recorder = new LatencyRecorder();
        for (long value = 1; value <= 100_000; value++) {
            recorder.record(value);
        }
        for (double percent : new double[] {50, 90, 99, 99.9}) {
            long expected = (long) (percent / 100.0 * 100_000);
            long reported = recorder.percentile(percent);
            assertTrue(
                    Math.abs(reported - expected) <= expected * 0.04 + 1,
                    "p" + percent + " reported " + reported + " expected about " + expected);
        }
    }

    @Test
    void percentilesAreMonotone() {
        LatencyRecorder recorder = new LatencyRecorder();
        Random random = new Random(25);
        for (int i = 0; i < 10_000; i++) {
            recorder.record(random.nextLong(1, 10_000_000));
        }
        long previous = 0;
        for (double percent = 5; percent <= 100; percent += 5) {
            long current = recorder.percentile(percent);
            assertTrue(current >= previous);
            previous = current;
        }
    }

    @Test
    void mergingRecordersPreservesCountsAndExtremes() {
        LatencyRecorder one = new LatencyRecorder();
        LatencyRecorder two = new LatencyRecorder();
        one.record(10);
        one.record(20);
        two.record(5);
        two.record(40_000);

        LatencyRecorder merged = new LatencyRecorder();
        merged.add(one);
        merged.add(two);
        assertEquals(4, merged.count());
        assertEquals(5, merged.min());
        assertEquals(40_000, merged.max());
    }
}
