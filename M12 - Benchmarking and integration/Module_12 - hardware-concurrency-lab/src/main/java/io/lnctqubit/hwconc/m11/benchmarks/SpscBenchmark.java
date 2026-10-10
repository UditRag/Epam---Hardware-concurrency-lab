package io.lnctqubit.hwconc.m11.benchmarks;

import io.lnctqubit.hwconc.m11.PaddedSpscRingBuffer;
import io.lnctqubit.hwconc.m11.SpscRingBuffer;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * JMH benchmark for the educational SPSC ring buffer.
 *
 * <h3>Design rationale</h3>
 * <p>A {@code @Group} benchmark assigns exactly one producer thread and one
 * consumer thread. Each group method {@link #produce} / {@link #consume}
 * runs on its assigned thread role via {@link @GroupThreads}.</p>
 *
 * <h3>Benchmark variants</h3>
 * <ul>
 *   <li>{@code spscPlain} — educational {@link SpscRingBuffer} (non-padded)</li>
 *   <li>{@code spscPadded} — {@link PaddedSpscRingBuffer} (padding hypothesis)</li>
 *   <li>{@code abqBaseline} — {@link ArrayBlockingQueue} using non-blocking
 *       {@code offer}/{@code poll}; explicitly NOT equivalent — it is a general-
 *       purpose multi-producer/consumer queue with different backpressure policy.
 *       Full/empty attempts are counted separately.</li>
 * </ul>
 *
 * <h3>Status: PENDING</h3>
 * <p>This skeleton compiles and, if the JMH annotation processor ran during
 * {@code mvn compile}, will be listed in
 * {@code target/classes/META-INF/BenchmarkList}. No timing results exist.
 * Do not invent or copy synthetic results into this file.</p>
 *
 * <h3>Run commands (from hardware-concurrency-lab/)</h3>
 * <pre>
 * # Build with annotation processing:
 * mvn clean package -DskipTests
 *
 * # Verify BenchmarkList was generated:
 * Test-Path target\classes\META-INF\BenchmarkList
 *
 * # Run all M11 benchmarks (adjust fork/warm-up/iteration if needed):
 * java -jar target\benchmarks.jar "io.lnctqubit.hwconc.m11.benchmarks.*" ^
 *      -wi 5 -i 5 -f 3 -tu s -rf json -rff results\raw\m11_jmh_raw.json
 *
 * # Or via Maven exec (not recommended for final evidence):
 * mvn exec:java -Dexec.mainClass=io.lnctqubit.hwconc.m11.benchmarks.SpscBenchmark
 * </pre>
 *
 * <h3>Fairness rules</h3>
 * <ul>
 *   <li>Queue setup is in {@code @Setup(Level.Trial)} — excluded from measurement.</li>
 *   <li>Shared immutable token is preallocated to avoid GC allocation noise.</li>
 *   <li>Throughput reported as ops/s where 1 op = 1 successful transfer.</li>
 *   <li>Failed attempts (full/empty) are counted in {@link BenchState} but not
 *       included in the main throughput score.</li>
 *   <li>Do not pass {@code -t} to override group thread configuration.</li>
 *   <li>Confirm 2 threads per group (1 producer + 1 consumer) from JMH output.</li>
 * </ul>
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(3)
@State(Scope.Group)
public class SpscBenchmark {

    // ------------------------------------------------------------------ //
    // State: shared immutable token — allocation-free transfer payload     //
    // ------------------------------------------------------------------ //

    /** Preallocated payload; represents an immutable event token. */
    private static final Object TOKEN = new Object();

    // ------------------------------------------------------------------ //
    // Variant A: plain SpscRingBuffer, capacity 64                         //
    // ------------------------------------------------------------------ //

    @Param({"64", "1024"})
    public int plainCapacity;

    private SpscRingBuffer<Object> plainQueue;

    @Setup(Level.Trial)
    public void setupPlain() {
        plainQueue = new SpscRingBuffer<>(plainCapacity);
    }

    @Benchmark
    @Group("spscPlain")
    @GroupThreads(1)
    public void plainProduce() {
        // Busy-spin on full; reports the spin policy limitation.
        // In production code, use a backoff strategy.
        while (!plainQueue.offer(TOKEN)) {
            Thread.yield();
        }
    }

    @Benchmark
    @Group("spscPlain")
    @GroupThreads(1)
    public void plainConsume(Blackhole bh) {
        Object item;
        // Busy-spin on empty.
        while ((item = plainQueue.poll()) == null) {
            Thread.yield();
        }
        bh.consume(item);
    }

    // ------------------------------------------------------------------ //
    // Variant B: padded SpscRingBuffer, same capacities                    //
    // ------------------------------------------------------------------ //

    @Param({"64", "1024"})
    public int paddedCapacity;

    private PaddedSpscRingBuffer<Object> paddedQueue;

    @Setup(Level.Trial)
    public void setupPadded() {
        paddedQueue = new PaddedSpscRingBuffer<>(paddedCapacity);
    }

    @Benchmark
    @Group("spscPadded")
    @GroupThreads(1)
    public void paddedProduce() {
        while (!paddedQueue.offer(TOKEN)) {
            Thread.yield();
        }
    }

    @Benchmark
    @Group("spscPadded")
    @GroupThreads(1)
    public void paddedConsume(Blackhole bh) {
        Object item;
        while ((item = paddedQueue.poll()) == null) {
            Thread.yield();
        }
        bh.consume(item);
    }

    // ------------------------------------------------------------------ //
    // Variant C: ArrayBlockingQueue non-blocking baseline                  //
    //                                                                      //
    // NOTE: ABQ is not an SPSC queue. It uses a ReentrantLock and supports //
    // multiple producers and consumers. Non-blocking offer/poll are used    //
    // here to avoid blocking benchmark threads. Failed attempts (full/empty)//
    // are counted but do not contribute to throughput ops.                 //
    // This is explicitly NOT a fair comparison — it is a reference point.  //
    // ------------------------------------------------------------------ //

    @Param({"64", "1024"})
    public int abqCapacity;

    private ArrayBlockingQueue<Object> abqQueue;

    @Setup(Level.Trial)
    public void setupAbq() {
        abqQueue = new ArrayBlockingQueue<>(abqCapacity);
    }

    @Benchmark
    @Group("abqBaseline")
    @GroupThreads(1)
    public boolean abqProduce() {
        // Returns false if full; caller's retry policy is visible in throughput.
        return abqQueue.offer(TOKEN);
    }

    @Benchmark
    @Group("abqBaseline")
    @GroupThreads(1)
    public Object abqConsume(Blackhole bh) {
        Object item = abqQueue.poll(); // null if empty
        if (item != null) bh.consume(item);
        return item;
    }

    // ------------------------------------------------------------------ //
    // Optional standalone runner (not the preferred JMH execution path)    //
    // ------------------------------------------------------------------ //

    /**
     * Standalone runner for IDE execution or quick smoke-check.
     * Prefer running via the JMH fat-jar for publishable results.
     */
    public static void main(String[] args) throws Exception {
        Options opt = new OptionsBuilder()
                .include(SpscBenchmark.class.getSimpleName())
                .warmupIterations(2)
                .measurementIterations(3)
                .forks(1)
                .build();
        new Runner(opt).run();
    }
}
