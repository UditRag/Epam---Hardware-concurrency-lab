package memorymodel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fundamentals.AtomicCounter;
import fundamentals.SynchronizedCounter;
import fundamentals.Workload;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

/**
 * Module 08 tests for "volatile is not atomic".
 *
 * <p>The deterministic test proves the point by construction (barrier-forced interleaving of the read and
 * write halves). The statistical test deliberately does NOT assert a lost update, because whether one occurs
 * in a given run is nondeterministic; it only checks a sane range. A pass there does not show VolatileCounter
 * is thread-safe.
 */
class VolatileCounterTest {
    private static final int THREADS = 8;
    private static final int ITERATIONS = 100_000;
    private static final int EXPECTED = THREADS * ITERATIONS;

    @Test
    void singleThreadedVolatileIncrementIsExact() {
        VolatileCounter c = new VolatileCounter();
        for (int i = 0; i < 1000; i++) {
            c.increment();
        }
        assertEquals(1000, c.get());
    }

    @Test
    void deterministicInterleavingLosesAnUpdateEvenThoughFieldIsVolatile() throws Exception {
        assertEquals(1, VolatileCounterExperiment.deterministicLostUpdate(),
                "two increments that both read 0 must leave 1, not 2");
    }

    @Test
    void volatileCounterUnderContentionStaysInSaneRange() throws Exception {
        int actual = Workload.run(new VolatileCounter(), THREADS, ITERATIONS);
        System.out.printf("[Volatile] expected=%d actual=%d lost=%d%n", EXPECTED, actual, EXPECTED - actual);
        assertTrue(actual > 0 && actual <= EXPECTED);
    }

    @RepeatedTest(3)
    void synchronizedAndAtomicContrastAreExact() throws Exception {
        assertEquals(EXPECTED, Workload.run(new SynchronizedCounter(), THREADS, ITERATIONS));
        assertEquals(EXPECTED, Workload.run(new AtomicCounter(), THREADS, ITERATIONS));
    }
}
