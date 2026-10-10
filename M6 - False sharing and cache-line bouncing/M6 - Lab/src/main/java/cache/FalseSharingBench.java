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
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.ThreadParams;

/**
 * Each JMH worker increments its own long[] slot. Run with -t 1, 2, 4, and 8
 * to compare the same operation with adjacent (stride=1) versus spaced
 * (stride=16, 128-byte payload spacing) slots. Array alignment and actual
 * cache-line placement are JVM/CPU-dependent, so timing is evidence of a
 * performance difference, not proof of a specific coherence transition.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(3)
public class FalseSharingBench {
    @State(Scope.Benchmark)
    public static class CounterState {
        /** 1 = adjacent slots; 16 = 128-byte stride between long payloads. */
        @Param({"1", "16"})
        public int stride;
        long[] counters;

        @Setup(Level.Trial)
        public void setup() {
            counters = FalseSharingLayout.newCounters(stride);
        }
    }

    @Benchmark
    public long incrementOwnedSlot(CounterState state, ThreadParams threadParams) {
        int index = FalseSharingLayout.indexForWorker(threadParams.getThreadIndex(), state.stride);
        return ++state.counters[index];
    }
}
