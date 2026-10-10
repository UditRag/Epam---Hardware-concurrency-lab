package io.lnctqubit.hwconc.m12.benchmarks;

import io.lnctqubit.hwconc.m11.PaddedSpscRingBuffer;
import io.lnctqubit.hwconc.m11.SpscQueue;
import io.lnctqubit.hwconc.m11.SpscRingBuffer;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.infra.Control;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Module 12 throughput benchmark: 1 producer + 1 consumer per group.
 * 1 op = 1 completed offer (producer method) or 1 completed poll (consumer method).
 * Report the CONSUMER row (transfers completed); the group aggregate sums both rows and
 * double-counts transfers.
 *
 * Differences from the Module 11 skeleton (which is left unchanged):
 *  - one State class per queue with a single @Param (no 2x2x2 cross-product of unrelated params);
 *  - spin loops exit on Control.stopMeasurement, otherwise the thread whose peer already stopped
 *    can spin forever at iteration end;
 *  - ABQ uses the same retry-until-success policy so 1 op = 1 transfer for every variant.
 *
 * Caveat: ABQ is a locked, multi-producer/consumer queue; comparison is a reference point only.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
@Fork(3)
public class SpscThroughputBenchmark {

    private static final Object TOKEN = new Object();

    @State(Scope.Group)
    public static class Plain extends Base {
        @Param({"64", "1024"}) public int capacity;
        @Setup(Level.Trial) public void init() { q = new SpscRingBuffer<>(capacity); }
    }

    @State(Scope.Group)
    public static class Padded extends Base {
        @Param({"64", "1024"}) public int capacity;
        @Setup(Level.Trial) public void init() { q = new PaddedSpscRingBuffer<>(capacity); }
    }

    @State(Scope.Group)
    public static class Abq {
        @Param({"64", "1024"}) public int capacity;
        ArrayBlockingQueue<Object> q;
        @Setup(Level.Trial) public void init() { q = new ArrayBlockingQueue<>(capacity); }
    }

    public static class Base { SpscQueue<Object> q; }

    @Benchmark @Group("spscPlain") @GroupThreads(1)
    public void plainProduce(Plain s, Control c) { produce(s.q, c); }
    @Benchmark @Group("spscPlain") @GroupThreads(1)
    public void plainConsume(Plain s, Control c, Blackhole bh) { consume(s.q, c, bh); }

    @Benchmark @Group("spscPadded") @GroupThreads(1)
    public void paddedProduce(Padded s, Control c) { produce(s.q, c); }
    @Benchmark @Group("spscPadded") @GroupThreads(1)
    public void paddedConsume(Padded s, Control c, Blackhole bh) { consume(s.q, c, bh); }

    @Benchmark @Group("abqBaseline") @GroupThreads(1)
    public void abqProduce(Abq s, Control c) {
        while (!s.q.offer(TOKEN) && !c.stopMeasurement) { Thread.onSpinWait(); }
    }
    @Benchmark @Group("abqBaseline") @GroupThreads(1)
    public void abqConsume(Abq s, Control c, Blackhole bh) {
        Object o;
        while ((o = s.q.poll()) == null && !c.stopMeasurement) { Thread.onSpinWait(); }
        if (o != null) bh.consume(o);
    }

    private static void produce(SpscQueue<Object> q, Control c) {
        while (!q.offer(TOKEN) && !c.stopMeasurement) { Thread.onSpinWait(); }
    }

    private static void consume(SpscQueue<Object> q, Control c, Blackhole bh) {
        Object o;
        while ((o = q.poll()) == null && !c.stopMeasurement) { Thread.onSpinWait(); }
        if (o != null) bh.consume(o);
    }
}
