package coherence;

import java.util.Arrays;

/**
 * THEORETICAL teaching model: N caches, one shared snooping bus, ONE cache line.
 * Each copy carries a data version so the model can check that no stale copy is ever read.
 * This is not a model of any real CPU (no transient states, no directory, no snoop filter).
 */
public final class SnoopingBus {
    public final Protocol protocol;
    private final State[] st;
    private final int[] val;
    private int mem = 0;      // value in main memory
    private int latest = 0;   // value of the most recent store (the "truth")
    private String lastBus = "None (hit)";

    public SnoopingBus(Protocol protocol, int caches) {
        this.protocol = protocol;
        this.st = new State[caches];
        this.val = new int[caches];
        Arrays.fill(st, State.I);
    }

    public int size() { return st.length; }
    public State state(int i) { return st[i]; }
    public State[] states() { return st.clone(); }
    public String lastBus() { return lastBus; }
    public int memory() { return mem; }
    public int latest() { return latest; }
    public int copyValue(int i) { return val[i]; }

    private boolean othersValid(int me) {
        for (int i = 0; i < st.length; i++) if (i != me && st[i].valid()) return true;
        return false;
    }

    /** Processor load on cache me. Returns the value the core observes. */
    public int read(int me) {
        if (st[me].valid()) { lastBus = "None (hit)"; return val[me]; }
        boolean shared = othersValid(me);
        int supplier = -1;
        lastBus = shared ? "BusRd; sharer signals shared" : "BusRd; no sharer answers";
        for (int i = 0; i < st.length; i++) {
            if (i == me) continue;
            switch (st[i]) {
                case M -> {
                    supplier = i;
                    if (protocol == Protocol.MOESI) { st[i] = State.O; lastBus = "BusRd; C" + i + " flushes, stays dirty owner"; }
                    else { mem = val[i]; st[i] = State.S; lastBus = "BusRd; C" + i + " flushes + writes back"; }
                }
                case O -> { supplier = i; lastBus = "BusRd; C" + i + " (owner) supplies"; }
                case E -> st[i] = State.S;
                default -> { }
            }
        }
        val[me] = supplier >= 0 ? val[supplier] : mem;
        st[me] = shared ? State.S : State.E;
        return val[me];
    }

    /** Processor store on cache me. */
    public void write(int me) {
        switch (st[me]) {
            case M -> lastBus = "None (hit)";
            case E -> { st[me] = State.M; lastBus = "None (hit, silent E to M)"; }
            case S, O -> { lastBus = "BusUpgr; others invalidate"; invalidateOthers(me); st[me] = State.M; }
            case I -> {
                lastBus = "BusRdX";
                int supplier = -1;
                for (int i = 0; i < st.length; i++)
                    if (i != me && st[i].dirty()) { supplier = i; lastBus = "BusRdX; C" + i + " flushes + invalidates"; }
                val[me] = supplier >= 0 ? val[supplier] : mem;
                invalidateOthers(me);
                st[me] = State.M;
            }
        }
        val[me] = ++latest;
    }

    /** Local replacement of the line in cache me (not necessarily announced on a real bus). */
    public void evict(int me) {
        if (st[me].dirty()) { mem = val[me]; lastBus = "Write-back to memory"; }
        else lastBus = "Silent drop";
        st[me] = State.I;
    }

    private void invalidateOthers(int me) {
        for (int i = 0; i < st.length; i++) if (i != me) st[i] = State.I;
    }

    /** Returns null if all invariants hold, otherwise a description of the violation. */
    public String checkInvariants() {
        int exclusive = 0, owned = 0, valid = 0, dirtyCount = 0;
        for (State s : st) {
            if (s == State.M || s == State.E) exclusive++;
            if (s == State.O) owned++;
            if (s.valid()) valid++;
            if (s.dirty()) dirtyCount++;
        }
        if (exclusive > 0 && valid > 1) return "M/E coexists with another valid copy " + Arrays.toString(st);
        if (exclusive > 1) return "more than one M/E " + Arrays.toString(st);
        if (owned > 1) return "more than one O " + Arrays.toString(st);
        if (protocol == Protocol.MESI && owned > 0) return "O appeared in MESI " + Arrays.toString(st);
        for (int i = 0; i < st.length; i++)
            if (st[i].valid() && val[i] != latest) return "stale copy in C" + i + " " + Arrays.toString(st);
        if (dirtyCount == 0 && mem != latest) return "no dirty copy but memory is stale";
        return null;
    }
}
