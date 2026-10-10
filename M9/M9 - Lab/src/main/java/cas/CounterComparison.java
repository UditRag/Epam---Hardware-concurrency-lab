package cas;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Code 3: plain vs synchronized vs AtomicInteger vs hand-written CAS loop.
 * Correctness demonstration, NOT a performance benchmark.
 * Usage: java -cp target/classes cas.CounterComparison [threads] [iterations] [repetitions]
 */
public class CounterComparison {
    private static int racyCount;
    private static int synchronizedCount;
    private static final Object lock = new Object();
    private static final AtomicInteger atomicCount = new AtomicInteger();
    private static final AtomicInteger casLoopCount = new AtomicInteger();

    private static void reset() {
        racyCount = 0; synchronizedCount = 0; atomicCount.set(0); casLoopCount.set(0);
    }

    private static void incrementSynchronized() {
        synchronized (lock) { synchronizedCount++; }
    }

    public static void main(String[] args) throws InterruptedException {
        int threads     = args.length > 0 ? Integer.parseInt(args[0]) : 2;
        int iterations  = args.length > 1 ? Integer.parseInt(args[1]) : 100_000;
        int repetitions = args.length > 2 ? Integer.parseInt(args[2]) : 5;
        int expected = threads * iterations;

        System.out.printf("CONFIG threads=%d iterations=%d repetitions=%d expected=%d%n",
                threads, iterations, repetitions, expected);
        System.out.println("run,racy,synchronized,atomic,casLoop,expected");

        int[] wrong = new int[4];
        for (int r = 1; r <= repetitions; r++) {
            reset();
            Runnable task = () -> {
                for (int i = 0; i < iterations; i++) {
                    racyCount++;
                    incrementSynchronized();
                    atomicCount.incrementAndGet();
                    CASRetryCounter.increment(casLoopCount);
                }
            };
            Thread[] ts = new Thread[threads];
            for (int t = 0; t < threads; t++) { ts[t] = new Thread(task, "worker-" + (t + 1)); ts[t].start(); }
            for (Thread t : ts) t.join();

            int[] v = { racyCount, synchronizedCount, atomicCount.get(), casLoopCount.get() };
            for (int k = 0; k < 4; k++) if (v[k] != expected) wrong[k]++;
            System.out.printf("%d,%d,%d,%d,%d,%d%n", r, v[0], v[1], v[2], v[3], expected);
        }
        System.out.printf("SUMMARY runs=%d incorrect: racy=%d synchronized=%d atomic=%d casLoop=%d%n",
                repetitions, wrong[0], wrong[1], wrong[2], wrong[3]);
    }
}
