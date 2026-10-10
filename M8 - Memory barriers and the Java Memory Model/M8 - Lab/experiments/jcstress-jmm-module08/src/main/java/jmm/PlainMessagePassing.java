package jmm;

import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.Description;
import org.openjdk.jcstress.annotations.Expect;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.II_Result;

/**
 * Message passing with ORDINARY fields (data race).
 *
 * <pre>
 * Initially data == 0, flag == 0
 * Writer (actor1): data = 42; flag = 1;
 * Reader (actor2): r1 = flag; r2 = data;      result tuple = (r1, r2)
 * </pre>
 * JMM status: every tuple is permitted. (1, 0) is the interesting one: the reader saw the flag but a stale
 * payload. Nothing in JLS 17.4.4/17.4.5 orders the writer's stores before the reader's loads (no
 * synchronizes-with edge), so the read of data may legally see the default 0. Permitted does NOT mean it
 * will be observed on any particular machine/JIT; on x86-64 the hardware keeps stores in order, so this
 * tuple may never appear even though the language allows it.
 */
@JCStressTest
@Description("Plain message passing: flag and data are ordinary fields")
@Outcome(id = "0, 0", expect = Expect.ACCEPTABLE, desc = "Reader ran before the writer's stores were visible")
@Outcome(id = "0, 42", expect = Expect.ACCEPTABLE, desc = "Flag read first (old), data read later (new)")
@Outcome(id = "1, 42", expect = Expect.ACCEPTABLE, desc = "Both stores observed")
@Outcome(id = "1, 0", expect = Expect.ACCEPTABLE_INTERESTING, desc = "Flag seen but payload stale: allowed by the JMM for plain fields (data race)")
@State
public class PlainMessagePassing {
    int data;
    int flag;

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
