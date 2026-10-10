package cache;

import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;

/**
 * Experiment 1 - sequential vs shuffled visiting order.
 *
 * Every benchmark method performs exactly {@code size} logical reads of data[] per invocation.
 * The two compared methods (sequentialIndexed, shuffledIndexed) run the IDENTICAL loop
 * "sum += data[idx[i]]" and also read idx[] strictly sequentially; the ONLY difference is the
 * content of idx[] (0,1,2,... vs a random permutation), i.e. the order in which data[] is visited.
 * Because idx[] is a permutation, both methods read every data element once and return the
 * same checksum.
 *
 * sequentialDirect is a REFERENCE (no index array, different instruction mix) and must not be
 * used as the fair partner of shuffledIndexed.
 *
 * Reported unit: ns per invocation. ns per logical read = score / size (see scripts/analyze_jmh.py).
 * All allocation, data fill and shuffling happen in @Setup(Level.Trial), outside measurement.
 */
@State(Scope.Benchmark)
@Threads(1)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(3)
public class SequentialVsRandomBenchmark {

    /**
     * Number of int elements. data[] payload = 4*size bytes; idx[] payload is another 4*size bytes,
     * so the touched footprint is about 8*size bytes. 262144 -> 2 MiB, 4194304 -> 32 MiB,
     * 33554432 -> 256 MiB (payload only). Compare against YOUR machine's cache sizes.
     */
    @Param({"262144", "4194304", "33554432"})
    public int size;

    private static final long SEED = 42L;

    private int[] data;
    private int[] seqIdx;
    private int[] rndIdx;

    @Setup(Level.Trial)
    public void setup() {
        data = AccessPatterns.buildData(size);
        seqIdx = AccessPatterns.identity(size);
        rndIdx = AccessPatterns.shuffledPermutation(size, SEED);
    }

    @Benchmark
    public long sequentialIndexed() {
        return AccessPatterns.sumIndexed(data, seqIdx);
    }

    @Benchmark
    public long shuffledIndexed() {
        return AccessPatterns.sumIndexed(data, rndIdx);
    }

    /** Reference only - not a fair partner of shuffledIndexed. */
    @Benchmark
    public long sequentialDirect() {
        return AccessPatterns.sumDirect(data);
    }
}
