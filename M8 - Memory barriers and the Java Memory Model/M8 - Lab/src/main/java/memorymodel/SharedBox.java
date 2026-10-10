package memorymodel;

/**
 * Module 08 (Aastha Pandey): one-shot "message passing" container used to compare publication styles.
 *
 * <p>Contract: one writer thread calls {@link #write(int)} once; one reader thread polls with
 * {@link #tryRead(int)}. The three implementations differ ONLY in how the writer's data is
 * published to the reader, so any difference in what the language guarantees comes from that
 * difference alone.
 *
 * <p>JLS reference for all reasoning here: Java SE 17 JLS Chapter 17 (sections 17.4.4 and 17.4.5).
 */
public interface SharedBox {

    /** Returned by {@link #tryRead(int)} when the reader never saw the "ready" signal within its bound. */
    int NOT_SEEN = -1;

    /** The payload value the writer publishes. Different from the default value 0 on purpose. */
    int PAYLOAD = 42;

    /** Writer side: store the payload, then raise the ready signal (in that program order). */
    void write(int value);

    /**
     * Reader side: poll for the ready signal at most {@code maxSpins} times.
     *
     * @return {@link #NOT_SEEN} if the signal was never observed; otherwise the payload value the
     *         reader observed after seeing the signal. A return of 0 means "signal seen but payload
     *         still shows its default value", i.e. a stale payload.
     */
    int tryRead(int maxSpins);

    /** Human-readable name used in experiment output. */
    String name();
}
