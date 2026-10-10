package jmm;

import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.Description;
import org.openjdk.jcstress.annotations.Expect;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.II_Result;

/**
 * Message passing with a VOLATILE flag and a plain payload (safe publication).
 *
 * <pre>
 * Initially data == 0, volatile flag == 0
 * Writer (actor1): data = 42; flag = 1;      // volatile write
 * Reader (actor2): r1 = flag; r2 = data;     // volatile read, then plain read
 * </pre>
 * JMM status: (1, 0) is FORBIDDEN. If the volatile read returns 1 it reads the volatile write, so
 * write(flag) synchronizes-with read(flag) (JLS 17.4.4); with program order and transitivity
 * hb(write data, read data) (JLS 17.4.5), and the default-value write is an intervening write that
 * happens-before the read, so the read cannot return 0. The other three tuples are permitted.
 */
@JCStressTest
@Description("Volatile flag message passing: payload written before the volatile write must be visible after the volatile read")
@Outcome(id = "0, 0", expect = Expect.ACCEPTABLE, desc = "Reader ran before the writer")
@Outcome(id = "0, 42", expect = Expect.ACCEPTABLE, desc = "Flag read old, payload read new: no edge required for this combination")
@Outcome(id = "1, 42", expect = Expect.ACCEPTABLE, desc = "Both observed")
@Outcome(id = "1, 0", expect = Expect.FORBIDDEN, desc = "Would contradict hb(write data, read data) via the volatile flag")
@State
public class VolatileFlagMessagePassing {
    int data;
    volatile int flag;

    @Actor
    public void writer() {
        data = 42;
        flag = 1;
    }

    @Actor
    public void reader(II_Result r) {
        r.r1 = flag;
        r.r2 = data;
    }
}
