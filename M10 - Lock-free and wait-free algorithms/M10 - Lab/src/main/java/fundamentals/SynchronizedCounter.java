package fundamentals;

public class SynchronizedCounter implements Counter {
    private int count = 0;

    @Override public synchronized void increment() { count++; }
    @Override public synchronized int get() { return count; }
}