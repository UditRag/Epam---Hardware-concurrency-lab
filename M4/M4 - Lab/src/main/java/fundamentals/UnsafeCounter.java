package fundamentals;

/** No synchronization: count++ is read-modify-write, not atomic. Intentionally buggy. */
public class UnsafeCounter implements Counter {
    private int count = 0;

    @Override public void increment() { count++; }
    @Override public int get() { return count; }
}