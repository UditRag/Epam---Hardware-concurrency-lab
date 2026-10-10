package atomics.lockfree;
import java.util.concurrent.*;

/**
 * Counts failed CAS attempts of CasLoopCounter per thread count (fixed work: opsPerThread each).
 * Retries measure contention at the algorithm level only; they are NOT cache-miss or coherence evidence.
 * Usage: java -cp target/classes contention.RetryDemo [maxThreads] [opsPerThread] > results/raw/m10-retry-demo.csv
 */
public final class RetryDemo {
    public static void main(String[] args) throws Exception {
        int maxThreads = args.length > 0 ? Integer.parseInt(args[0]) : 8;
        int ops = args.length > 1 ? Integer.parseInt(args[1]) : 1_000_000;
        System.out.println("# env: cpus=" + Runtime.getRuntime().availableProcessors() + " jdk=" + System.getProperty("java.version")
                + " os=" + System.getProperty("os.name") + " " + System.getProperty("os.arch"));
        System.out.println("threads,ops_per_thread,total_ops,total_failed_cas,failed_per_op,final_value_ok");
        for (int th = 1; th <= maxThreads; th *= 2) {
            CasLoopCounter c = new CasLoopCounter();
            long[] retries = new long[th];
            CyclicBarrier start = new CyclicBarrier(th);
            ExecutorService ex = Executors.newFixedThreadPool(th);
            for (int t = 0; t < th; t++) {
                final int id = t;
                ex.execute(() -> {
                    try { start.await(10, TimeUnit.SECONDS); } catch (Exception e) { throw new RuntimeException(e); }
                    long r = 0;
                    for (int i = 0; i < ops; i++) r += c.incrementCountingRetries();
                    retries[id] = r;
                });
            }
            ex.shutdown();
            if (!ex.awaitTermination(120, TimeUnit.SECONDS)) { ex.shutdownNow(); throw new IllegalStateException("timeout"); }
            long total = (long) th * ops, failed = 0;
            for (long r : retries) failed += r;
            System.out.printf("%d,%d,%d,%d,%.4f,%b%n", th, ops, total, failed, (double) failed / total, c.get() == total);
        }
    }
}
