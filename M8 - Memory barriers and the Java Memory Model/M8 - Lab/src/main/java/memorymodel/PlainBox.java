package memorymodel;

/**
 * INTENTIONALLY UNSAFE publication: both fields are ordinary (non-volatile), no lock is used.
 *
 * <p>Data race (JLS 17.4.5): the writer's write of {@code data} / {@code ready} and the reader's
 * reads of the same fields are conflicting accesses (JLS 17.4.1) that are NOT ordered by
 * happens-before. There is no synchronizes-with edge (JLS 17.4.4) between the two threads.
 *
 * <p>Consequences the Java Memory Model PERMITS (it does not REQUIRE any of them):
 * <ul>
 *   <li>the reader may never see {@code ready == true} (reads may be hoisted out of the loop; see
 *       the broken {@code while (!done)} example in JLS 17.3);</li>
 *   <li>the reader may see {@code ready == true} and still read {@code data == 0}, because no
 *       happens-before edge forces the write of {@code data} to be visible.</li>
 * </ul>
 * Whether either happens on a given machine depends on the JIT, the CPU and timing. On x86-64 the
 * hardware does not reorder two stores with each other (Intel SDM Vol. 3A, memory-ordering chapter),
 * so a stale payload is often NOT observed even though the language allows it. A run that shows only
 * correct values therefore proves nothing about the safety of this class.
 */
public final class PlainBox implements SharedBox {
    private int data;          // plain on purpose
    private boolean ready;     // plain on purpose

    @Override
    public void write(int value) {
        data = value;   // (1) plain write
        ready = true;   // (2) plain write; no release semantics, so (1) is not guaranteed visible with (2)
    }

    @Override
    public int tryRead(int maxSpins) {
        for (int i = 0; i < maxSpins; i++) {
            if (ready) {        // (3) plain read: races with (2)
                return data;    // (4) plain read: races with (1); may legally return 0
            }
            Thread.onSpinWait();
        }
        return NOT_SEEN;
    }

    @Override
    public String name() {
        return "PlainBox (UNSAFE: data race)";
    }
}
