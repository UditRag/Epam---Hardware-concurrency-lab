package fundamentals;

import java.util.concurrent.atomic.AtomicInteger;

public class AtomicCounter implements Counter {
    private final AtomicInteger count = new AtomicInteger();

    @Override public void increment() { count.incrementAndGet(); }
    @Override public int get() { return count.get(); }
}
