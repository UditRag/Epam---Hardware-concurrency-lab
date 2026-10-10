package memorymodel;

/**
 * SAFE publication through a volatile flag.
 *
 * <p>{@code data} is an ordinary field; {@code ready} is volatile. The happens-before chain is:
 * <pre>
 *   writer: data = v   --po-->   writer: ready = true (volatile write)
 *   writer: ready = true  --synchronizes-with-->  reader: read of ready that returns true   (JLS 17.4.4)
 *   reader: read of ready --po--> reader: read of data
 *   =>  by transitivity (JLS 17.4.5)  hb(write data, read data)
 * </pre>
 * Because the default-value write of {@code data} happens-before the payload write, the reader's read
 * of {@code data} cannot legally return 0 once the volatile read has returned true.
 *
 * <p>Correctness condition: the reader must read {@code data} AFTER it has read {@code ready == true}
 * from the SAME volatile field; reading {@code data} before, or without checking the flag, gives no
 * guarantee. Also, the writer must write {@code data} BEFORE the volatile write.
 *
 * <p>Limits: this only orders what happens before the volatile write relative to what happens after the
 * matching volatile read. It does not make compound operations atomic and does not by itself bound how
 * long the reader takes to observe the flag (a reader that starts too early may simply return NOT_SEEN).
 */
public final class VolatileFlagBox implements SharedBox {
    private int data;                 // plain: protected only by the volatile edge below
    private volatile boolean ready;   // the synchronization variable

    @Override
    public void write(int value) {
        data = value;   // (1) ordinary write, program-ordered before (2)
        ready = true;   // (2) volatile write: release
    }

    @Override
    public int tryRead(int maxSpins) {
        for (int i = 0; i < maxSpins; i++) {
            if (ready) {        // (3) volatile read: acquire; if it sees (2), then sw(2,3) and hb(1,4)
                return data;    // (4) must observe (1) or a later write; never the default 0
            }
            Thread.onSpinWait();
        }
        return NOT_SEEN;
    }

    @Override
    public String name() {
        return "VolatileFlagBox (safe: volatile flag edge)";
    }
}
