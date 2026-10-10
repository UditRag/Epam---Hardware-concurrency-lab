package jmm;

import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.Description;
import org.openjdk.jcstress.annotations.Expect;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.II_Result;

/**
 * Store-buffering shape with BOTH actors' whole sequence inside one shared monitor.
 *
 * <pre>
 * Initially x == 0, y == 0 (ordinary fields)
 * Thread 1 (actor1): synchronized (lock) { x = 1; r1 = y; }
 * Thread 2 (actor2): synchronized (lock) { y = 1; r2 = x; }
 * </pre>
 * The plain and volatile variants of this litmus test already exist in Module 07
 * (experiments/jcstress-store-buffering, ordering.PlainStoreBuffering / ordering.VolatileStoreBuffering) and
 * are NOT duplicated here. This third variant shows a different synchronizes-with source: the monitor.
 * Critical sections are mutually exclusive, so one runs entirely before the other: (0, 1) or (1, 0).
 * Both (0, 0) and (1, 1) are FORBIDDEN (JLS 17.4.4: unlock synchronizes-with later lock; 17.4.5: hence hb).
 */
@JCStressTest
@Description("Store buffering shape under a common monitor")
@Outcome(id = "0, 1", expect = Expect.ACCEPTABLE, desc = "Thread 1's block ran first")
@Outcome(id = "1, 0", expect = Expect.ACCEPTABLE, desc = "Thread 2's block ran first")
@Outcome(id = "0, 0", expect = Expect.FORBIDDEN, desc = "Impossible: the second block always sees the first block's store")
@Outcome(id = "1, 1", expect = Expect.FORBIDDEN, desc = "Impossible: the first block cannot see the second block's store")
@State
public class SynchronizedStoreBuffering {
    private final Object lock = new Object();
    int x;
    int y;

    @Actor
    public void actor1(II_Result r) {
        synchronized (lock) {
            x = 1;
            r.r1 = y;
        }
    }

    @Actor
    public void actor2(II_Result r) {
        synchronized (lock) {
            y = 1;
            r.r2 = x;
        }
    }
}
