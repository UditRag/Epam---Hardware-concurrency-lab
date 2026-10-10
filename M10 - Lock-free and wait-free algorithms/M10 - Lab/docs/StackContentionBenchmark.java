package contention;

import atomics.lockfree.LockStack;
import atomics.lockfree.TreiberStack;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

/**
 * JMH comparison: lock-free TreiberStack vs blocking LockStack, identical work per invocation (push then pop,
 * same semantics). Thread count is a RUN-TIME option (-t). NOT COMPILED, NOT RUN where generated.
 * Hypothesis: none about direction; the outcome may be faster, slower, equal, or thread-count dependent.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
@Fork(3)
@State(Scope.Benchmark)
public class StackContentionBenchmark {
    final TreiberStack<Integer> treiber = new TreiberStack<>();
    final LockStack<Integer> lock = new LockStack<>();

    @Benchmark public void treiberPushPop(Blackhole bh) { treiber.push(1); bh.consume(treiber.pop()); }
    @Benchmark public void lockPushPop(Blackhole bh)    { lock.push(1);    bh.consume(lock.pop()); }
}
