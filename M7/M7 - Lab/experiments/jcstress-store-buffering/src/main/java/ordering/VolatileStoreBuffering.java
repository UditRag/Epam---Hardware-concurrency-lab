package ordering;

import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.Description;
import org.openjdk.jcstress.annotations.Expect;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.II_Result;

/** Same litmus shape, but volatile accesses impose JMM synchronization ordering. */
@JCStressTest
@Description("Volatile store buffering: volatile writes precede volatile reads in each actor")
@Outcome(id = "1, 1", expect = Expect.ACCEPTABLE, desc = "Both stores observed")
@Outcome(id = "0, 1", expect = Expect.ACCEPTABLE, desc = "Only actor 2 observes x")
@Outcome(id = "1, 0", expect = Expect.ACCEPTABLE, desc = "Only actor 1 observes y")
@Outcome(id = "0, 0", expect = Expect.FORBIDDEN, desc = "Would contradict the volatile synchronization order")
@State
public class VolatileStoreBuffering {
    volatile int x;
    volatile int y;

    @Actor
    public void actor1(II_Result r) {
        x = 1;
        r.r1 = y;
    }

    @Actor
    public void actor2(II_Result r) {
        y = 1;
        r.r2 = x;
    }
}
