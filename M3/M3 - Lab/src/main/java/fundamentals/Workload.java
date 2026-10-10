package fundamentals;

import java.util.concurrent.CountDownLatch;

/** Runs `threads` threads, each calling increment() `iterations` times, released together. */
public final class Workload {
    private Workload() {}

    public static int run(Counter counter, int threads, int iterations) throws InterruptedException {
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        Thread[] pool = new Thread[threads];

        for (int i = 0; i < threads; i++) {
            pool[i] = new Thread(() -> {
                ready.countDown();
                try {
                    start.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                for (int j = 0; j < iterations; j++) {
                    counter.increment();
                }
            });
            pool[i].start();
        }
        ready.await();       // all threads created and waiting
        start.countDown();   // release together to maximize contention
        for (Thread t : pool) t.join();   // join gives a happens-before edge for get()
        return counter.get();
    }
}