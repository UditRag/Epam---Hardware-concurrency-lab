package atomics.lockfree;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Counter built from an explicit CAS retry loop. Lock-free, not wait-free: under contention one
 * thread can lose the CAS race an unbounded number of times. Contrast with
 * AtomicLong.getAndIncrement(), whose progress guarantee is NOT specified by the API/JLS
 * (JDK/JIT/CPU dependent; see module doc, hypothesis H3).
 */
public final class CasLoopCounter {
    private final AtomicLong value = new AtomicLong();

    /** @return number of failed CAS attempts for this call (0 = succeeded first try). */
    public int incrementCountingRetries() {
        int retries = 0;
        long cur;
        do {
            cur = value.get();
            if (value.compareAndSet(cur, cur + 1)) return retries;
            retries++;
        } while (true);
    }

    public long get() { return value.get(); }
}
