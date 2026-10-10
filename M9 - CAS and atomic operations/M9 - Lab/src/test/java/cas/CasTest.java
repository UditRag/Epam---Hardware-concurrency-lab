package cas;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicStampedReference;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

class CasTest {
    @Test
    void casSucceedsWhenExpectedMatches() {
        AtomicInteger v = new AtomicInteger(5);
        assertTrue(v.compareAndSet(5, 10));
        assertEquals(10, v.get());
    }

    @Test
    void casFailsWhenExpectedDiffersAndLeavesValueUnchanged() {
        AtomicInteger v = new AtomicInteger(10);
        assertFalse(v.compareAndSet(5, 20));
        assertEquals(10, v.get());
    }

    @Test
    void retryLoopIncrementsSequentially() {
        AtomicInteger c = new AtomicInteger();
        CASRetryCounter.increment(c);
        CASRetryCounter.increment(c);
        assertEquals(2, c.get());
    }

    @RepeatedTest(5)
    void retryLoopIsExactUnderContention() throws Exception {
        int threads = 8, iterations = 50_000;
        AtomicInteger c = new AtomicInteger();
        Thread[] ts = new Thread[threads];
        for (int t = 0; t < threads; t++) {
            ts[t] = new Thread(() -> { for (int i = 0; i < iterations; i++) CASRetryCounter.increment(c); });
            ts[t].start();
        }
        for (Thread t : ts) t.join();
        assertEquals(threads * iterations, c.get());
    }

    @Test
    void referenceOnlyCasMissesAba() {
        AtomicReference<String> r = new AtomicReference<>("A");
        String seen = r.get();
        r.compareAndSet("A", "B");
        r.compareAndSet("B", "A");
        assertTrue(r.compareAndSet(seen, "C"));
    }

    @Test
    void stampedReferenceDetectsAba() {
        AtomicStampedReference<String> r = new AtomicStampedReference<>("A", 1);
        r.compareAndSet("A", "B", 1, 2);
        r.compareAndSet("B", "A", 2, 3);
        assertFalse(r.compareAndSet("A", "C", 1, 2));
        assertEquals("A", r.getReference());
        assertEquals(3, r.getStamp());
    }
}
