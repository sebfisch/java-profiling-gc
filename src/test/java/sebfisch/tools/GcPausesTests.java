package sebfisch.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import org.junit.jupiter.api.Test;
import sebfisch.tools.GcPauses.Pause;
import sebfisch.tools.GcPauses.Stall;

class GcPausesTests {

    @Test
    void parsesG1PausesWithHeapSizes() {
        assertEquals(
                Optional.of(new Pause("Pause Young (Normal)", 3.402, OptionalDouble.of(23))),
                GcPauses.parse("[2.312s][info][gc          ] GC(17) Pause Young (Normal) (G1 Evacuation Pause)"
                        + " (Evacuation Failure: Allocation) 87M->23M(1024M) 3.402ms"));
        assertEquals(
                Optional.of(new Pause("Pause Remark", 1.2, OptionalDouble.of(400))),
                GcPauses.parse("[4.1s][info][gc          ] GC(102) Pause Remark 400M->400M(1024M) 1.200ms"));
        assertEquals(
                Optional.of(new Pause("Pause Full (G1 Compaction Pause)", 65.547, OptionalDouble.of(309))),
                GcPauses.parse("[1.740s][info][gc] GC(47) Pause Full (G1 Compaction Pause) 2048M->309M(1054M) 65.547ms"));
    }

    @Test
    void convertsHeapAfterToMegabytes() {
        assertEquals(
                Optional.of(new Pause("Pause Young (Allocation Failure)", 0.512, OptionalDouble.of(0.5))),
                GcPauses.parse("[0.1s][info][gc] GC(0) Pause Young (Allocation Failure) 4M->512K(16M) 0.512ms"));
        assertEquals(
                Optional.of(new Pause("Pause Full (Ergonomics)", 812.0, OptionalDouble.of(2048))),
                GcPauses.parse("[9.0s][info][gc] GC(40) Pause Full (Ergonomics) 3G->2G(4G) 812.000ms"));
    }

    @Test
    void parsesShenandoahAndZgcPausesWithoutHeapSizes() {
        assertEquals(
                Optional.of(new Pause("Pause Init Mark (unload classes)", 0.123, OptionalDouble.empty())),
                GcPauses.parse("[0.9s][info][gc          ] GC(2) Pause Init Mark (unload classes) 0.123ms"));
        assertEquals(
                Optional.of(new Pause("Y: Pause Mark Start (Major)", 0.021, OptionalDouble.empty())),
                GcPauses.parse("[0.5s][info][gc,phases   ] GC(3) Y: Pause Mark Start (Major) 0.021ms"));
    }

    @Test
    void ignoresLinesThatAreNotFinishedPauses() {
        for (String line : List.of(
                "[2.309s][info][gc,start    ] GC(17) Pause Young (Normal) (G1 Evacuation Pause)",
                "[2.312s][info][gc,phases   ] GC(17)   Evacuate Collection Set: 3.1ms",
                "[2.4s][info][gc          ] GC(18) Concurrent Mark Cycle 120.0ms",
                "[10.0s][info][gc,stats    ]       Young Pause: Pause Mark Start"
                        + "      0.020 / 0.023         0.020 / 0.023       ms")) {
            assertTrue(GcPauses.parse(line).isEmpty(), line);
        }
    }

    @Test
    void parsesZgcAllocationStalls() {
        assertEquals(
                Optional.of(new Stall("worker-6", 13.655)),
                GcPauses.parseStall("[1.336s][info][gc          ] Allocation Stall (worker-6) 13.655ms"));
        assertTrue(GcPauses.parse("[1.336s][info][gc          ] Allocation Stall (worker-6) 13.655ms").isEmpty());
    }

    @Test
    void ignoresLinesThatAreNotStalls() {
        for (String line : List.of(
                "[0.347s][info][gc,alloc    ] GC(0) Y: Allocation Stalls:          0                0"
                        + "                0                0         ",
                "[2.312s][info][gc] GC(17) Pause Young (Normal) (G1 Evacuation Pause) 87M->23M(1024M) 3.402ms")) {
            assertTrue(GcPauses.parseStall(line).isEmpty(), line);
        }
    }
}
