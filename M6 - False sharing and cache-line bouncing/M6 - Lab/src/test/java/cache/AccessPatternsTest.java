package cache;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Correctness checks for the benchmark inputs. These do NOT measure anything. */
class AccessPatternsTest {

    private static final int N = 1 << 14;   // 16384 ints = 64 KiB, fast

    @Test
    void sequentialAndShuffledReadTheSameDataAndGiveTheSameChecksum() {
        int[] data = AccessPatterns.buildData(N);
        long seq = AccessPatterns.sumIndexed(data, AccessPatterns.identity(N));
        long rnd = AccessPatterns.sumIndexed(data, AccessPatterns.shuffledPermutation(N, 42L));
        assertEquals(seq, rnd);
        assertEquals(seq, AccessPatterns.sumDirect(data));
    }

    @Test
    void shuffledIndexIsAPermutationAndNotTheIdentity() {
        int[] p = AccessPatterns.shuffledPermutation(N, 42L);
        assertTrue(AccessPatterns.isPermutation(p));
        assertFalse(java.util.Arrays.equals(p, AccessPatterns.identity(N)));
    }

    @Test
    void shuffleIsReproducibleForTheSameSeedAndDiffersForAnother() {
        assertArrayEquals(AccessPatterns.shuffledPermutation(N, 1L), AccessPatterns.shuffledPermutation(N, 1L));
        assertFalse(java.util.Arrays.equals(
                AccessPatterns.shuffledPermutation(N, 1L),
                AccessPatterns.shuffledPermutation(N, 2L)));
    }

    @Test
    void randomCycleVisitsEverySlotBeforeReturning() {
        int[] next = AccessPatterns.randomSingleCycle(N, 42L);
        assertTrue(AccessPatterns.isPermutation(next));
        assertEquals(N, AccessPatterns.cycleLengthFromZero(next));
    }

    @Test
    void sequentialCycleHasFullLength() {
        assertEquals(N, AccessPatterns.cycleLengthFromZero(AccessPatterns.sequentialCycle(N)));
    }

    @Test
    void chaseReturnsToStartAfterExactlyOneFullCycle() {
        int[] next = AccessPatterns.randomSingleCycle(N, 7L);
        assertEquals(0, AccessPatterns.chase(next, 0, N));
        assertEquals(0, AccessPatterns.chase(AccessPatterns.sequentialCycle(N), 0, N));
    }

    @Test
    void workingSetSizeArithmetic() {
        assertEquals(256, AccessPatterns.intsForKiB(1));          // 1 KiB = 256 ints
        assertEquals(1_048_576, AccessPatterns.intsForKiB(4096)); // 4 MiB = 1,048,576 ints
        assertEquals(4096L, AccessPatterns.payloadBytes(1024));
    }
}
