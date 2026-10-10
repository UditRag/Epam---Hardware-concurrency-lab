package jmm;

import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.Arbiter;
import org.openjdk.jcstress.annotations.Description;
import org.openjdk.jcstress.annotations.Expect;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.I_Result;

/**
 * Two threads each execute {@code v++} on a VOLATILE int; an arbiter reads the final value.
 *
 * <p>The volatile field removes the data race but not the lost update: {@code v++} is a volatile read, an
 * addition and a volatile write, which two threads can interleave (both read 0, both write 1).
 * Final value 2 and final value 1 are both permitted by the JMM. Final value 1 is the "interesting" outcome.
 * Permitted does not mean observed; whether 1 appears depends on timing and the JIT.
 * (Contrast: with AtomicInteger.incrementAndGet or a synchronized increment the value 1 would be forbidden;
 * those contrasts are covered by fundamentals.* in Module 01 and by Module 09.)
 */
@JCStressTest
@Description("Volatile increment is not atomic: lost update allowed")
@Outcome(id = "2", expect = Expect.ACCEPTABLE, desc = "Both increments took effect")
@Outcome(id = "1", expect = Expect.ACCEPTABLE_INTERESTING, desc = "Lost update: both threads read 0")
@State
public class VolatileIncrementLostUpdate {
    volatile int v;

    @Actor
    public void actor1() {
        v++;
    }

    @Actor
    public void actor2() {
        v++;
    }

    @Arbiter
    public void arbiter(I_Result r) {
        r.r1 = v;
    }
}
