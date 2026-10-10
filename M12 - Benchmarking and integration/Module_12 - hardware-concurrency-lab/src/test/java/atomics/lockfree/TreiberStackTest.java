package atomics.lockfree;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/** JUnit 5 wrapper. NOT RUN (JUnit/Maven unavailable where this was generated). Every concurrent test has a timeout. */
class TreiberStackTest {

    @Test
    void emptyPopIsNull() {
        assertNull(new TreiberStack<Integer>().pop());
    }

    @Test
    void rejectsNullItems() {
        assertThrows(NullPointerException.class, () -> new TreiberStack<Integer>().push(null));
    }

    @Test
    void sequentialLifo() {
        TreiberStack<Integer> s = new TreiberStack<>();
        for (int i = 0; i < 100; i++) s.push(i);
        for (int i = 99; i >= 0; i--) assertEquals(i, s.pop());
        assertTrue(s.isEmpty());
    }

    @Test
    @Timeout(value = 120, unit = TimeUnit.SECONDS)
    void concurrentExactlyOnceAndCounterExact() throws Exception {
        TreiberStack<Integer> s = new TreiberStack<>();
        int before = LockFreeChecks.failures;
        LockFreeChecks.exactlyOnce("treiber", s::push, s::pop, 4, 4, 50_000);
        LockFreeChecks.counterExact(8, 100_000);
        assertEquals(before, LockFreeChecks.failures, "a concurrent check failed; see stdout");
    }
}
