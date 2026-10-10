package memorymodel;

import fundamentals.AtomicCounter;
import fundamentals.Counter;
import fundamentals.SynchronizedCounter;
import fundamentals.Workload;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Module 08 experiment: volatile does not make count++ atomic.
 *
 * <p>Run (after {@code mvn -q compile}):
 * <pre>
 *   java -cp target/classes memorymodel.VolatileCounterExperiment [threads] [iterations] [repetitions]
 *   (Windows: java -cp target\classes memorymodel.VolatileCounterExperiment 8 200000 10)
 * </pre>
 * Part 1 is DETERMINISTIC: it splits the read and the write of an increment with a barrier so that both
 * threads read the same value; the lost update is then guaranteed by construction. It models the
 * decomposition of {@code count++}; it does not observe the real instruction sequence.
 * Part 2 is STATISTICAL: it runs the existing Workload on VolatileCounter, SynchronizedCounter and
 * AtomicCounter. Whether VolatileCounter loses updates in a given run is nondeterministic. A run with no lost
 * update does not show VolatileCounter is correct.
 */
public final class VolatileCounterExperiment {

    private VolatileCounterExperiment() {}

    /**
     * Two threads each perform "read volatile; wait for the other to have read; write read+1".
     * Starting from 0, a correct atomic increment pair would give 2; this always gives 1.
     *
     * @return the final counter value (always 1 when the barrier works)
     */
    public static int deterministicLostUpdate() throws InterruptedException {
        VolatileCounter counter = new VolatileCounter();
        CyclicBarrier bothHaveRead = new CyclicBarrier(2);
        Runnable task = () -> {
            int seen = counter.get();                               // volatile read
            try {
                bothHaveRead.await(5, TimeUnit.SECONDS);            // both threads now hold seen == 0
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (BrokenBarrierException | TimeoutException e) {
                throw new IllegalStateException("barrier failed", e);
            }
            counter.set(seen + 1);                                  // volatile write of a stale-based value
        };
        Thread a = new Thread(task, "inc-A");
        Thread b = new Thread(task, "inc-B");
        a.start();
        b.start();
        a.join();
        b.join();
        return counter.get();
    }

    public static void main(String[] args) throws Exception {
        int threads = args.length > 0 ? Integer.parseInt(args[0]) : 8;
        int iterations = args.length > 1 ? Integer.parseInt(args[1]) : 200_000;
        int repetitions = args.length > 2 ? Integer.parseInt(args[2]) : 10;
        int expected = threads * iterations;

        System.out.printf("Java=%s vm=%s os=%s arch=%s cpus=%d%n",
                System.getProperty("java.version"), System.getProperty("java.vm.name"),
                System.getProperty("os.name"), System.getProperty("os.arch"),
                Runtime.getRuntime().availableProcessors());

        int det = deterministicLostUpdate();
        System.out.printf("Part 1 (deterministic, barrier-forced interleaving): two increments on volatile 0 -> %d (atomic would be 2)%n", det);

        System.out.printf("Part 2 (statistical): threads=%d iterations=%d expected=%d repetitions=%d%n",
                threads, iterations, expected, repetitions);
        report("VolatileCounter    (volatile, non-atomic ++)", VolatileCounter::new, threads, iterations, repetitions);
        report("SynchronizedCounter (monitor)", SynchronizedCounter::new, threads, iterations, repetitions);
        report("AtomicCounter       (AtomicInteger)", AtomicCounter::new, threads, iterations, repetitions);
        System.out.println("NOTE: 'incorrect=0' for VolatileCounter would NOT show it is safe; the barrier demo in Part 1 does.");
    }

    private static void report(String label, java.util.function.Supplier<Counter> factory,
                               int threads, int iterations, int repetitions) throws InterruptedException {
        int expected = threads * iterations;
        int incorrect = 0;
        long worstLoss = 0;
        for (int r = 0; r < repetitions; r++) {
            int actual = Workload.run(factory.get(), threads, iterations);
            if (actual != expected) {
                incorrect++;
                worstLoss = Math.max(worstLoss, (long) expected - actual);
            }
        }
        System.out.printf("%-42s incorrect=%d/%d worstLoss=%d%n", label, incorrect, repetitions, worstLoss);
    }
}
