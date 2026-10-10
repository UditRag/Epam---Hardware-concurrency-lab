package memorymodel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

/**
 * Module 08 tests for publication.
 *
 * <p>What these tests CAN show: the safe boxes never return a stale payload in the runs executed, which is
 * what the JLS guarantees (17.4.5). What they CANNOT show: that PlainBox is unsafe (a race may not manifest)
 * or that the safe boxes are correct on every JVM/CPU (a passing run is not a proof). PlainBox is therefore
 * tested only for "result is one of the permitted classes", never for failure.
 */
class PublicationTest {
    private static final int TRIALS = 2_000;
    private static final int MAX_SPINS = 500_000;

    @Test
    void singleThreadedWriteThenReadSeesPayload() {
        // Same thread: program order alone gives happens-before, so even PlainBox must work here.
        for (SharedBox box : new SharedBox[] {new PlainBox(), new VolatileFlagBox(), new SynchronizedBox()}) {
            box.write(SharedBox.PAYLOAD);
            assertEquals(SharedBox.PAYLOAD, box.tryRead(1), box.name());
        }
    }

    @Test
    void readBeforeWriteReturnsNotSeen() {
        for (SharedBox box : new SharedBox[] {new PlainBox(), new VolatileFlagBox(), new SynchronizedBox()}) {
            assertEquals(SharedBox.NOT_SEEN, box.tryRead(10), box.name());
        }
    }

    @RepeatedTest(3)
    void volatileFlagBoxNeverShowsStalePayload() throws Exception {
        PublicationExperiment.Tally t = PublicationExperiment.runMany(VolatileFlagBox::new, TRIALS, MAX_SPINS);
        assertEquals(0, t.stale, "JLS 17.4.5 forbids a stale payload after a volatile read sees the flag");
        assertEquals(TRIALS, t.total());
    }

    @RepeatedTest(3)
    void synchronizedBoxNeverShowsStalePayload() throws Exception {
        PublicationExperiment.Tally t = PublicationExperiment.runMany(SynchronizedBox::new, TRIALS, MAX_SPINS);
        assertEquals(0, t.stale, "JLS 17.4.5 forbids a stale payload after lock follows unlock");
        assertEquals(TRIALS, t.total());
    }

    /** Deliberately does NOT assert that a race manifests; it only checks the outcome accounting is consistent. */
    @Test
    void plainBoxResultsAreAccountedFor() throws Exception {
        PublicationExperiment.Tally t = PublicationExperiment.runMany(PlainBox::new, TRIALS, MAX_SPINS);
        System.out.printf("[PlainBox] ok=%d stale=%d notSeen=%d%n", t.ok, t.stale, t.notSeen);
        assertEquals(TRIALS, t.total());
        assertTrue(t.ok >= 0 && t.stale >= 0 && t.notSeen >= 0);
    }
}
