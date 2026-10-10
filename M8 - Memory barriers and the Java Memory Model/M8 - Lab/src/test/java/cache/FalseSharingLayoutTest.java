package cache;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class FalseSharingLayoutTest {
    @Test void adjacentWorkersUseAdjacentSlots() {
        assertEquals(0, FalseSharingLayout.indexForWorker(0, 1));
        assertEquals(1, FalseSharingLayout.indexForWorker(1, 1));
        assertEquals(7, FalseSharingLayout.indexForWorker(7, 1));
    }

    @Test void paddedWorkersUse128BytePayloadStride() {
        assertEquals(0, FalseSharingLayout.indexForWorker(0, 16));
        assertEquals(16, FalseSharingLayout.indexForWorker(1, 16));
        assertEquals(128, FalseSharingLayout.indexForWorker(1, 16) * Long.BYTES);
    }

    @Test void counterArraysAreLargeEnoughForConfiguredThreadCeiling() {
        assertEquals(FalseSharingLayout.MAX_BENCHMARK_THREADS, FalseSharingLayout.newCounters(1).length);
        assertEquals(FalseSharingLayout.MAX_BENCHMARK_THREADS * 16, FalseSharingLayout.newCounters(16).length);
    }

    @Test void invalidWorkerAndStrideAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> FalseSharingLayout.indexForWorker(-1, 1));
        assertThrows(IllegalArgumentException.class, () -> FalseSharingLayout.indexForWorker(256, 1));
        assertThrows(IllegalArgumentException.class, () -> FalseSharingLayout.indexForWorker(0, 8));
    }
}
