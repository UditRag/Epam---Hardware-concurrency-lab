package coherence;

import java.util.function.Consumer;

/** Derives the transition table by driving the simulator, so the table cannot drift from the code. */
public class TransitionTableDump {

    /** Build a 2-cache system with C0 in state s (and C1 in the matching companion state). */
    private static SnoopingBus setup(Protocol p, State s) {
        SnoopingBus b = new SnoopingBus(p, 2);
        switch (s) {
            case I -> { }
            case E -> b.read(0);
            case S -> { b.read(0); b.read(1); }
            case M -> b.write(0);
            case O -> { b.write(0); b.read(1); }   // MOESI only
        }
        return b;
    }

    private static boolean reachable(Protocol p, State s) { return !(p == Protocol.MESI && s == State.O); }

    private static void line(String side, State cur, String ev, String bus, State next) {
        System.out.printf("%-10s %-4s %-18s %-40s %-4s%n", side, cur, ev, bus, next);
    }

    private static void proc(Protocol p, State s, String ev, Consumer<SnoopingBus> action) {
        SnoopingBus b = setup(p, s);
        action.accept(b);
        line("Processor", s, ev, b.lastBus(), b.state(0));
    }

    private static void snoop(Protocol p, State s, String ev, Consumer<SnoopingBus> remote) {
        SnoopingBus b = setup(p, s);
        State before = b.state(0);
        if (before != s) throw new IllegalStateException("setup failed for " + s);
        remote.accept(b);
        line("Snoop", s, ev, b.lastBus(), b.state(0));
    }

    public static void main(String[] args) {
        Protocol[] which = args.length > 0 ? new Protocol[]{Protocol.valueOf(args[0].toUpperCase())} : Protocol.values();
        for (Protocol p : which) {
            System.out.println("\n=== Transition table [" + p + "] (derived from SnoopingBus) ===");
            System.out.printf("%-10s %-4s %-18s %-40s %-4s%n", "Side", "Cur", "Event", "Bus action (observed)", "Next");
            System.out.println("-".repeat(82));
            for (State s : State.values()) {
                if (!reachable(p, s)) continue;
                proc(p, s, "PrRd", b -> b.read(0));
                proc(p, s, "PrWr", b -> b.write(0));
            }
            for (State s : State.values()) {
                if (!reachable(p, s)) continue;
                // C1 is in I for every setup except S and O, so a remote read/write is BusRd/BusRdX there.
                snoop(p, s, "BusRd seen", b -> { if (b.state(1) == State.I) b.read(1); else { b.evict(1); b.read(1); } });
                snoop(p, s, "BusRdX seen", b -> { if (b.state(1) != State.I) b.evict(1); b.write(1); });
                if (s == State.S || s == State.O)
                    snoop(p, s, "BusUpgr seen", b -> b.write(1));   // C1 holds S, so its store is an upgrade
            }
        }
    }
}
