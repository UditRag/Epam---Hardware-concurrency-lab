package atomics.lockfree;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * DEMONSTRATION, not benchmark evidence (no JMH, no forks, no statistics).
 * Makes two points observable on the machine running it:
 *  (1) lock-free != automatically faster: total ops/s, TreiberStack vs LockStack;
 *  (2) lock-free != wait-free: per-thread completed ops. Lock-freedom permits a large spread; a small
 *      spread proves nothing about wait-freedom either.
 * Usage: java -cp target/classes contention.ProgressDemo [threads] [seconds] > results/raw/m10-progress-demo-tN.csv
 */
public final class ProgressDemo {
    public static void main(String[] args) throws Exception {
        int threads = args.length > 0 ? Integer.parseInt(args[0]) : Runtime.getRuntime().availableProcessors();
        int seconds = args.length > 1 ? Integer.parseInt(args[1]) : 3;
        System.out.println("# env: cpus=" + Runtime.getRuntime().availableProcessors() + " jdk=" + System.getProperty("java.version")
                + " os=" + System.getProperty("os.name") + " " + System.getProperty("os.arch") + " threads=" + threads + " seconds=" + seconds);
        System.out.println("run,impl,threads,total_ops,ops_per_sec,min_thread_ops,max_thread_ops,max_over_min");
        for (int rep = 1; rep <= 3; rep++) { // run 1 includes JIT warm-up; keep all runs in the raw file
            TreiberStack<Integer> t = new TreiberStack<>();
            run(rep, "treiber", threads, seconds, t::push, t::pop);
            LockStack<Integer> l = new LockStack<>();
            run(rep, "lock", threads, seconds, l::push, l::pop);
        }
    }

    static void run(int rep, String name, int threads, int seconds, Consumer<Integer> push, Supplier<Integer> pop) throws Exception {
        long[] ops = new long[threads];
        AtomicBoolean stop = new AtomicBoolean();
        CyclicBarrier start = new CyclicBarrier(threads + 1);
        ExecutorService ex = Executors.newFixedThreadPool(threads);
        for (int t = 0; t < threads; t++) {
            final int id = t;
            ex.execute(() -> {
                try { start.await(); } catch (Exception e) { throw new RuntimeException(e); }
                long n = 0; int sink = 0;
                while (!stop.get()) { push.accept(id); Integer v = pop.get(); if (v != null) sink += v; n += 2; }
                ops[id] = n + (sink == Integer.MIN_VALUE ? 1 : 0); // consume the result
            });
        }
        start.await();
        long t0 = System.nanoTime();
        Thread.sleep(seconds * 1000L);
        stop.set(true);
        ex.shutdown();
        if (!ex.awaitTermination(30, TimeUnit.SECONDS)) { ex.shutdownNow(); throw new IllegalStateException("workers did not stop"); }
        double secs = (System.nanoTime() - t0) / 1e9;
        long total = 0, min = Long.MAX_VALUE, max = 0;
        for (long o : ops) { total += o; min = Math.min(min, o); max = Math.max(max, o); }
        System.out.printf("%d,%s,%d,%d,%.0f,%d,%d,%.2f%n", rep, name, threads, total, total / secs, min, max, (double) max / Math.max(1, min));
    }
}
