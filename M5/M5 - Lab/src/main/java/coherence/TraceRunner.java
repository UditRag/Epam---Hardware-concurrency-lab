package coherence;

/** Prints the three event traces from the study module for MESI and MOESI. */
public class TraceRunner {
    private static int step;

    private static void header(String title, Protocol p) {
        System.out.println("\n=== " + title + "  [" + p + "] ===");
        System.out.printf("%-5s %-22s %-40s %-4s %-4s%n", "Step", "Event", "Bus traffic", "C0", "C1");
        System.out.println("-".repeat(80));
        step = 0;
    }

    private static void row(String ev, SnoopingBus b) {
        System.out.printf("%-5d %-22s %-40s %-4s %-4s%n", ++step, ev, b.lastBus(), b.state(0), b.state(1));
    }

    static void trace1(Protocol p) {
        SnoopingBus b = new SnoopingBus(p, 2);
        header("Trace 1: read-only sharing (both start in I)", p);
        b.read(0); row("C0 reads x", b);
        b.read(1); row("C1 reads x", b);
        b.read(0); b.read(1); row("Both read x again", b);
    }

    static void trace2(Protocol p) {
        SnoopingBus b = new SnoopingBus(p, 2);
        b.read(0); b.read(1);                         // setup: both in S, not printed
        header("Trace 2: C0 writes after both read (both start in S)", p);
        b.write(0); row("C0 executes x = 1", b);
        b.read(1);  row("C1 later reads x", b);
        b.read(1);  row("C1 reads x again", b);
    }

    static void trace3(Protocol p) {
        SnoopingBus b = new SnoopingBus(p, 2);
        header("Trace 3: competing writers (ownership ping-pong)", p);
        b.write(0); row("C0 writes x", b);
        b.write(1); row("C1 writes x", b);
        b.write(0); row("C0 writes x again", b);
        b.write(1); row("C1 writes x again", b);
    }

    public static void main(String[] args) {
        Protocol[] which = args.length > 0 ? new Protocol[]{Protocol.valueOf(args[0].toUpperCase())} : Protocol.values();
        System.out.println("Snooping-bus teaching model, 1 cache line, 2 cores (THEORETICAL, not hardware output)");
        for (Protocol p : which) { trace1(p); trace2(p); trace3(p); }
        System.out.println("\nKey difference: Trace 2 step 2 -> MESI gives C0=S, MOESI gives C0=O.");
    }
}
