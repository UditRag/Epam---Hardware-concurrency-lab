package io.lnctqubit.hwconc.m12.benchmarks;

import io.lnctqubit.hwconc.m11.PaddedSpscRingBuffer;
import io.lnctqubit.hwconc.m11.SpscQueue;
import io.lnctqubit.hwconc.m11.SpscRingBuffer;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Control;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Round-trip (ping-pong) latency. Initiator offers to q1 and spins until it polls from q2; the
 * responder polls q1 and offers to q2. Queue depth is never above 1, so there is no queueing delay.
 * Use ONLY the initiator row ("ping"): its SampleTime distribution is the round-trip time.
 * One-way latency is approximated as RTT/2 (assumes symmetric legs). The responder row measures its
 * own wait for a ping and is not a latency; ignore it.
 * Not measured: per-item latency under load (needs per-item timestamps; would perturb the queue).
 */
@BenchmarkMode(Mode.SampleTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
@Fork(3)
public class SpscLatencyBenchmark {

    private static final Object TOKEN = new Object();

    @State(Scope.Group)
    public static class Pair {
        @Param({"plain", "padded", "abq"}) public String impl;
        SpscQueue<Object> a, b;
        ArrayBlockingQueue<Object> x, y;

        @Setup(Level.Trial)
        public void init() {
            switch (impl) {
                case "plain":  a = new SpscRingBuffer<>(64);       b = new SpscRingBuffer<>(64); break;
                case "padded": a = new PaddedSpscRingBuffer<>(64); b = new PaddedSpscRingBuffer<>(64); break;
                default:       x = new ArrayBlockingQueue<>(64);   y = new ArrayBlockingQueue<>(64);
            }
        }
        boolean offer(boolean fwd, Object o) { return a != null ? (fwd ? a : b).offer(o) : (fwd ? x : y).offer(o); }
        Object poll(boolean fwd) { return a != null ? (fwd ? a : b).poll() : (fwd ? x : y).poll(); }
    }

    @Benchmark @Group("pingPong") @GroupThreads(1)
    public void ping(Pair s, Control c) {
        while (!s.offer(true, TOKEN) && !c.stopMeasurement) { Thread.onSpinWait(); }
        while (s.poll(false) == null && !c.stopMeasurement) { Thread.onSpinWait(); }
    }

    @Benchmark @Group("pingPong") @GroupThreads(1)
    public void pong(Pair s, Control c) {
        while (s.poll(true) == null && !c.stopMeasurement) { Thread.onSpinWait(); }
        while (!s.offer(false, TOKEN) && !c.stopMeasurement) { Thread.onSpinWait(); }
    }
}
