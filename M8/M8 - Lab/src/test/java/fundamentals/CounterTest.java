package fundamentals;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

class CounterTest {
    private static final int THREADS = 8;
    private static final int ITERATIONS = 200_000;
    private static final int EXPECTED = THREADS * ITERATIONS;

    @RepeatedTest(5)
    void synchronizedCounterIsExact() throws Exception {
        assertEquals(EXPECTED, Workload.run(new SynchronizedCounter(), THREADS, ITERATIONS));
    }

    @RepeatedTest(5)
    void atomicCounterIsExact() throws Exception {
        assertEquals(EXPECTED, Workload.run(new AtomicCounter(), THREADS, ITERATIONS));
    }

    /**
     * Deliberately does NOT assert failure: whether a race manifests is nondeterministic.
     * A pass here does not prove the class is thread-safe. It only checks the result is
     * in a sane range and logs the gap.
     */
    @Test
    void unsafeCounterIsNotGuaranteedExact() throws Exception {
        int actual = Workload.run(new UnsafeCounter(), THREADS, ITERATIONS);
        System.out.printf("[Unsafe] expected=%d actual=%d lost=%d%n", EXPECTED, actual, EXPECTED - actual);
        assertTrue(actual > 0 && actual <= EXPECTED);
    }
}