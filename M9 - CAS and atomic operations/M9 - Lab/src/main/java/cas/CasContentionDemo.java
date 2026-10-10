package cas;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Counts FAILED CAS attempts while N threads increment one shared AtomicInteger via a CAS loop.
 * Failed attempts = wasted work caused by contention. Not a throughput benchmark.
 * Usage: java -cp target/classes cas.CasContentionDemo [threads] [iterations]
 */
public class CasContentionDemo {
    public static void main(String[] args) throws InterruptedException {
        int threads    = args.length > 0 ? Integer.parseInt(args[0]) : 8;
        int iterations = args.length > 1 ? Integer.parseInt(args[1]) : 1_000_000;

        System.out.println("CONFIG threads=" + threads + " iterations=" + iterations);
        System.out.println("threads,expected,actual,failedCasAttempts,failedPerSuccess");
        for (int n : new int[] {1, 2, 4, threads}) {
            AtomicInteger counter = new AtomicInteger();
            AtomicLong failed = new AtomicLong();
            Thread[] ts = new Thread[n];
            for (int t = 0; t < n; t++) {
                ts[t] = new Thread(() -> {
                    long local = 0;
                    for (int i = 0; i < iterations; i++) local += CASRetryCounter.increment(counter);
                    failed.addAndGet(local);
                });
                ts[t].start();
            }
            for (Thread t : ts) t.join();
            long expected = (long) n * iterations;
            System.out.printf("%d,%d,%d,%d,%.4f%n", n, expected, counter.get(), failed.get(),
                    failed.get() / (double) expected);
        }
    }
}
