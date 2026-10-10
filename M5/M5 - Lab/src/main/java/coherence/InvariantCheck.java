package coherence;

import java.util.Random;

/**
 * Randomised self-check: millions of random loads/stores/evictions on 4 caches, asserting after every
 * operation that (1) M/E are exclusive, (2) at most one O, (3) MESI never shows O,
 * (4) every valid copy holds the latest stored value, (5) with no dirty copy memory is current.
 * Passing proves the TEACHING MODEL is self-consistent. It says nothing about real hardware.
 */
public class InvariantCheck {
    public static void main(String[] args) {
        long ops = args.length > 0 ? Long.parseLong(args[0]) : 2_000_000L;
        System.out.println("Invariant check: " + ops + " random operations per protocol, 4 caches, seed 42");
        boolean allOk = true;
        for (Protocol p : Protocol.values()) {
            SnoopingBus b = new SnoopingBus(p, 4);
            Random rnd = new Random(42);
            long[] seen = new long[State.values().length];
            long reads = 0, writes = 0, evicts = 0;
            String violation = null;
            for (long i = 0; i < ops && violation == null; i++) {
                int c = rnd.nextInt(4);
                int r = rnd.nextInt(100);
                if (r < 50) { int v = b.read(c); reads++; if (v != b.latest()) violation = "read returned stale value at op " + i; }
                else if (r < 90) { b.write(c); writes++; }
                else { b.evict(c); evicts++; }
                if (violation == null) violation = b.checkInvariants();
                for (int k = 0; k < 4; k++) seen[b.state(k).ordinal()]++;
            }
            System.out.printf("%n[%s] reads=%d writes=%d evictions=%d%n", p, reads, writes, evicts);
            System.out.print("  state occupancy (cache-samples):");
            for (State s : State.values()) System.out.printf("  %s=%d", s, seen[s.ordinal()]);
            System.out.println();
            if (violation == null) System.out.println("  RESULT: PASS (no invariant violated)");
            else { System.out.println("  RESULT: FAIL -> " + violation); allOk = false; }
            if (p == Protocol.MESI && seen[State.O.ordinal()] != 0) { System.out.println("  FAIL: O seen in MESI"); allOk = false; }
            if (p == Protocol.MOESI && seen[State.O.ordinal()] == 0) { System.out.println("  FAIL: O never reached in MOESI"); allOk = false; }
        }
        System.out.println(allOk ? "\nALL CHECKS PASSED" : "\nCHECKS FAILED");
        if (!allOk) System.exit(1);
    }
}
