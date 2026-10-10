package atomics.lockfree;

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Dependency-free correctness checks with hard timeouts (usable without Maven).
 * Run: java -cp target/classes:target/test-classes atomics.lockfree.LockFreeChecks   (exit 0 = all passed)
 * Covers BOTH TreiberStack and the LockStack baseline so later speed comparisons are between equally correct stacks.
 */
public final class LockFreeChecks {
    static int failures = 0;
    static void check(boolean ok, String name) {
        System.out.println((ok ? "PASS " : "FAIL ") + name);
        if (!ok) failures++;
    }

    public static void main(String[] a) throws Exception {
        TreiberStack<Integer> t = new TreiberStack<>();
        sequentialLifo("treiber", t::push, t::pop);
        LockStack<Integer> l = new LockStack<>();
        sequentialLifo("lock", l::push, l::pop);
        check(new TreiberStack<Integer>().pop() == null, "treiber pop on empty returns null");
        check(new LockStack<Integer>().pop() == null, "lock pop on empty returns null");
        for (int rep = 0; rep < 20; rep++) {
            TreiberStack<Integer> ts = new TreiberStack<>();
            exactlyOnce("treiber", ts::push, ts::pop, 4, 4, 50_000);
            LockStack<Integer> ls = new LockStack<>();
            exactlyOnce("lock", ls::push, ls::pop, 4, 4, 50_000);
        }
        counterExact(8, 100_000);
        System.out.println(failures == 0 ? "ALL CHECKS PASSED" : failures + " FAILED");
        System.exit(failures == 0 ? 0 : 1);
    }

    static void sequentialLifo(String n, Consumer<Integer> push, Supplier<Integer> pop) {
        for (int i = 0; i < 5; i++) push.accept(i);
        boolean ok = true;
        for (int i = 4; i >= 0; i--) { Integer v = pop.get(); ok &= (v != null && v == i); }
        check(ok && pop.get() == null, n + " sequential LIFO order");
    }

    /** Invariant: every pushed value popped exactly once; pops == pushes; stack empty at end. */
    static void exactlyOnce(String n, Consumer<Integer> push, Supplier<Integer> pop, int producers, int consumers, int perProducer) throws Exception {
        final int total = producers * perProducer;
        AtomicIntegerArray seen = new AtomicIntegerArray(total);
        AtomicInteger popped = new AtomicInteger();
        CyclicBarrier start = new CyclicBarrier(producers + consumers);
        ExecutorService ex = Executors.newFixedThreadPool(producers + consumers);
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
        for (int p = 0; p < producers; p++) {
            final int base = p * perProducer;
            ex.execute(() -> { await(start); for (int i = 0; i < perProducer; i++) push.accept(base + i); });
        }
        for (int c = 0; c < consumers; c++) {
            ex.execute(() -> {
                await(start);
                while (popped.get() < total && System.nanoTime() < deadline) {
                    Integer v = pop.get();
                    if (v == null) { Thread.onSpinWait(); continue; }
                    seen.incrementAndGet(v);
                    popped.incrementAndGet();
                }
            });
        }
        ex.shutdown();
        boolean done = ex.awaitTermination(45, TimeUnit.SECONDS);
        if (!done) ex.shutdownNow();
        boolean once = true;
        for (int i = 0; i < total; i++) once &= (seen.get(i) == 1);
        check(done && popped.get() == total && once && pop.get() == null,
              n + " exactly-once " + producers + "P/" + consumers + "C x" + perProducer);
    }

    static void counterExact(int threads, int perThread) throws Exception {
        CasLoopCounter c = new CasLoopCounter();
        ExecutorService ex = Executors.newFixedThreadPool(threads);
        CyclicBarrier start = new CyclicBarrier(threads);
        for (int t = 0; t < threads; t++) ex.execute(() -> {
            await(start);
            for (int i = 0; i < perThread; i++) c.incrementCountingRetries();
        });
        ex.shutdown();
        boolean done = ex.awaitTermination(30, TimeUnit.SECONDS);
        check(done && c.get() == (long) threads * perThread, "CAS counter final == threads*perThread");
    }

    static void await(CyclicBarrier b) {
        try { b.await(10, TimeUnit.SECONDS); } catch (Exception e) { throw new RuntimeException(e); }
    }
}
