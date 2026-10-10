package memorymodel;

import fundamentals.Counter;

/**
 * Module 08: a counter whose field is volatile but whose increment is NOT atomic.
 *
 * <p>Implements the project's existing {@link Counter} interface so it can be run through the existing
 * {@code fundamentals.Workload} next to {@code UnsafeCounter}, {@code SynchronizedCounter} and
 * {@code AtomicCounter} (Module 01, Udit) without changing any of them.
 *
 * <p>What volatile gives here (JLS 17.4.4, 17.4.5, 8.3.1.4): every read sees the latest volatile write in
 * the synchronization order, and the field is free of data races. What it does NOT give: {@code count++}
 * is still a volatile READ, an addition, then a volatile WRITE. Two threads can both read 5, both write 6;
 * one update is lost. The program is race-free in the JLS sense (all conflicting accesses are volatile,
 * hence synchronization actions) yet incorrect: JLS 17.4.3 notes that freedom from data races still allows
 * errors from groups of operations that need to be perceived atomically.
 *
 * <p>Correct alternatives (contrast only; the CAS/atomics internals belong to Module 09):
 * {@code fundamentals.SynchronizedCounter} (mutual exclusion) and {@code fundamentals.AtomicCounter}
 * (single atomic read-modify-write operation).
 */
public class VolatileCounter implements Counter {
    private volatile int count = 0;

    @Override
    public void increment() {
        count++;   // NOT atomic: volatile read, add 1, volatile write
    }

    @Override
    public int get() {
        return count;
    }

    /** Plain volatile store, exposed so the deterministic lost-update demonstration can split the steps. */
    public void set(int value) {
        count = value;
    }
}
