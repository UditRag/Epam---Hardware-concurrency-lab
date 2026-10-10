package cache;

import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OperationsPerInvocation;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;

/**
 * Experiment 2 - working-set size sweep with dependent loads (pointer chase).
 *
 * next[] has n = workingSetKiB*256 ints (4 bytes each), so the array payload is exactly
 * workingSetKiB KiB (plus a small array-object header). Each hop is p = next[p]; the next address
 * depends on the previous load, so hops cannot overlap and the score approximates the average
 * latency of one dependent load for that working-set size.
 *
 *   chaseRandom     : next[] is ONE random cycle (Sattolo) - defeats stride prefetching, touches
 *                     the whole working set.
 *   chaseSequential : next[i] = i+1 - consecutive addresses, friendly to prefetching.
 *
 * Both do exactly HOPS logical reads per invocation, for every size, so
 * the score is ns per hop (OperationsPerInvocation normalises it). Setup (allocation, shuffle)
 * is outside measurement.
 */
@State(Scope.Benchmark)
@Threads(1)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(3)
public class WorkingSetBenchmark {

    /** Constant number of dependent loads per invocation (2^18), identical for all sizes. */
    public static final int HOPS = 1 << 18;

    /** Working-set payload in KiB. 4 KiB ... 128 MiB; spans typical L1/L2/L3/DRAM ranges. */
    @Param({"4", "8", "16", "32", "64", "128", "256", "512", "1024", "2048",
            "4096", "8192", "16384", "32768", "65536", "131072"})
    public int workingSetKiB;

    private static final long SEED = 42L;

    private int[] randomNext;
    private int[] sequentialNext;
    private int start;

    @Setup(Level.Trial)
    public void setup() {
        int n = AccessPatterns.intsForKiB(workingSetKiB);
        randomNext = AccessPatterns.randomSingleCycle(n, SEED);
        sequentialNext = AccessPatterns.sequentialCycle(n);
        start = 0;
    }

    @Benchmark
    @OperationsPerInvocation(HOPS)
    public int chaseRandom() {
        return AccessPatterns.chase(randomNext, start, HOPS);
    }

    @Benchmark
    @OperationsPerInvocation(HOPS)
    public int chaseSequential() {
        return AccessPatterns.chase(sequentialNext, start, HOPS);
    }
}
