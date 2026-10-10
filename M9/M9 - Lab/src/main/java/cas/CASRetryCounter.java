package cas;

import java.util.concurrent.atomic.AtomicInteger;

/** Code 2: CAS retry loop. Run: java -cp target/classes cas.CASRetryCounter */
public class CASRetryCounter {

    /** Increments with an explicit CAS retry loop. Returns how many CAS attempts FAILED. */
    public static int increment(AtomicInteger counter) {
        int failures = 0;
        int expected;
        int updated;
        do {
            expected = counter.get();          // fresh read on every attempt
            updated = expected + 1;
            if (!counter.compareAndSet(expected, updated)) failures++; else break;
        } while (true);
        return failures;
    }

    public static void main(String[] args) {
        AtomicInteger counter = new AtomicInteger(0);
        increment(counter);
        increment(counter);
        System.out.println("Final value: " + counter.get());
    }
}
