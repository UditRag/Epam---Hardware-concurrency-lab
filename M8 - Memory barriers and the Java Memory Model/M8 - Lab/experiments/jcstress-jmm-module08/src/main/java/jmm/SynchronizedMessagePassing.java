package jmm;

import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.Description;
import org.openjdk.jcstress.annotations.Expect;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.II_Result;

/**
 * Message passing inside synchronized blocks on ONE shared lock.
 *
 * <pre>
 * Writer (actor1): synchronized (lock) { data = 42; flag = 1; }
 * Reader (actor2): synchronized (lock) { r1 = flag; r2 = data; }
 * </pre>
 * The two critical sections cannot overlap, so exactly two outcomes are possible: reader first gives
 * (0, 0); writer first gives (1, 42) because unlock synchronizes-with the later lock (JLS 17.4.4) and
 * hence hb(writer's writes, reader's reads). (1, 0) and (0, 42) are FORBIDDEN.
 */
@JCStressTest
@Description("Monitor message passing: both actors lock the same object")
@Outcome(id = "0, 0", expect = Expect.ACCEPTABLE, desc = "Reader's critical section came first")
@Outcome(id = "1, 42", expect = Expect.ACCEPTABLE, desc = "Writer's critical section came first")
@Outcome(id = "1, 0", expect = Expect.FORBIDDEN, desc = "Would show a partly published state")
@Outcome(id = "0, 42", expect = Expect.FORBIDDEN, desc = "Would show a partly published state")
@State
public class SynchronizedMessagePassing {
    private final Object lock = new Object();
    int data;
    int flag;

    @Actor
    public void writer() {
        synchronized (lock) {
            data = 42;
            flag = 1;
        }
    }

    @Actor
    public void reader(II_Result r) {
        synchronized (lock) {
            r.r1 = flag;
            r.r2 = data;
        }
    }
}
