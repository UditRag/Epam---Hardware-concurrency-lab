package cache;

import java.util.SplittableRandom;

/**
 * Plain-Java helpers shared by the Module 03 JMH benchmarks and the JUnit sanity tests.
 * No JMH dependency here, so the logic can be tested without running a benchmark.
 *
 * Terminology (Module 03):
 *   - "logical read": one Java array-element load that the benchmark source code performs.
 *     Whether it is served by L1, L2, L3 or DRAM is NOT visible from Java.
 *   - "working set": the bytes of array data the benchmark repeatedly touches.
 */
public final class AccessPatterns {

    /** Size of a Java int in bytes (JLS 4.2.1: int is 32-bit). */
    public static final int INT_BYTES = Integer.BYTES;

    private AccessPatterns() {}

    /** Number of int elements whose payload is exactly {@code kib} KiB (1 KiB = 1024 bytes). */
    public static int intsForKiB(int kib) {
        long n = (long) kib * 1024L / INT_BYTES;
        if (n < 2 || n > Integer.MAX_VALUE - 8) {
            throw new IllegalArgumentException("kib out of supported range: " + kib);
        }
        return (int) n;
    }

    /** Payload bytes of an int[] with n elements (excludes the array object header). */
    public static long payloadBytes(int n) {
        return (long) n * INT_BYTES;
    }

    /** Deterministic data values: small non-negative ints, so a long sum cannot overflow. */
    public static int[] buildData(int n) {
        int[] data = new int[n];
        for (int i = 0; i < n; i++) {
            data[i] = (i * 31 + 7) & 0xFFFF;
        }
        return data;
    }

    /** Identity permutation 0,1,2,...,n-1 (sequential visiting order). */
    public static int[] identity(int n) {
        int[] idx = new int[n];
        for (int i = 0; i < n; i++) {
            idx[i] = i;
        }
        return idx;
    }

    /** Uniformly random permutation of 0..n-1 (Fisher-Yates), reproducible via seed. */
    public static int[] shuffledPermutation(int n, long seed) {
        int[] idx = identity(n);
        SplittableRandom rnd = new SplittableRandom(seed);
        for (int i = n - 1; i > 0; i--) {
            int j = rnd.nextInt(i + 1);
            int t = idx[i];
            idx[i] = idx[j];
            idx[j] = t;
        }
        return idx;
    }

    /**
     * next[] describing ONE cycle through all n slots in random order (Sattolo's algorithm).
     * Following p = next[p] visits every slot exactly once before returning to the start,
     * so the whole array is the working set. A general random permutation may split into
     * several short cycles, which would silently shrink the working set - hence Sattolo.
     */
    public static int[] randomSingleCycle(int n, long seed) {
        int[] next = identity(n);
        SplittableRandom rnd = new SplittableRandom(seed);
        for (int i = n - 1; i > 0; i--) {
            int j = rnd.nextInt(i);          // 0 <= j < i  (NOT i+1): this is what makes one cycle
            int t = next[i];
            next[i] = next[j];
            next[j] = t;
        }
        return next;
    }

    /** next[i] = (i+1) mod n : a sequential single cycle. */
    public static int[] sequentialCycle(int n) {
        int[] next = new int[n];
        for (int i = 0; i < n; i++) {
            next[i] = (i + 1 == n) ? 0 : i + 1;
        }
        return next;
    }

    /** True iff idx is a permutation of 0..n-1. */
    public static boolean isPermutation(int[] idx) {
        boolean[] seen = new boolean[idx.length];
        for (int v : idx) {
            if (v < 0 || v >= idx.length || seen[v]) {
                return false;
            }
            seen[v] = true;
        }
        return true;
    }

    /** Length of the cycle that contains slot 0 when following p = next[p]. */
    public static int cycleLengthFromZero(int[] next) {
        int p = 0;
        int len = 0;
        do {
            p = next[p];
            len++;
        } while (p != 0 && len <= next.length);
        return len;
    }

    /** Sum of data[idx[i]] for i in [0,n): n logical data reads, order decided by idx. */
    public static long sumIndexed(int[] data, int[] idx) {
        long sum = 0;
        for (int i = 0; i < idx.length; i++) {
            sum += data[idx[i]];
        }
        return sum;
    }

    /** Sum of data[i] directly. REFERENCE ONLY: different instruction mix than sumIndexed. */
    public static long sumDirect(int[] data) {
        long sum = 0;
        for (int i = 0; i < data.length; i++) {
            sum += data[i];
        }
        return sum;
    }

    /**
     * Dependent chase: each load's address comes from the previous load's value, so loads
     * cannot overlap. Returns the final position so the result is consumed.
     */
    public static int chase(int[] next, int start, int hops) {
        int p = start;
        for (int h = 0; h < hops; h++) {
            p = next[p];
        }
        return p;
    }
}
