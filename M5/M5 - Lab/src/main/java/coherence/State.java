package coherence;

/** Stable cache-line states used by the teaching models. O exists only in MOESI. */
public enum State { M, O, E, S, I;
    public boolean valid() { return this != I; }
    public boolean dirty() { return this == M || this == O; }
}
