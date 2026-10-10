package ordering;

import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.Description;
import org.openjdk.jcstress.annotations.Expect;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.II_Result;

/**
 * Store-buffering litmus test with ordinary (non-volatile) fields.
 * The (0, 0) outcome is allowed by the Java Memory Model and is the outcome
 * of interest; observing it on a particular machine is not guaranteed.
 */
@JCStressTest
@Description("Plain store buffering: each actor stores one variable then reads the other")
@Outcome(id = "1, 1", expect = Expect.ACCEPTABLE, desc = "Both stores observed")
@Outcome(id = "0, 1", expect = Expect.ACCEPTABLE, desc = "Actor 1 did not observe x's peer store")
@Outcome(id = "1, 0", expect = Expect.ACCEPTABLE, desc = "Actor 2 did not observe y's peer store")
@Outcome(id = "0, 0", expect = Expect.ACCEPTABLE_INTERESTING, desc = "Store-buffering outcome; permitted for plain fields")
@State
public class PlainStoreBuffering {
    int x;
    int y;

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
