package memorymodel;

import java.util.function.Supplier;

/**
 * Module 08 experiment: unsafe vs safe publication.
 *
 * <p>Run (from the project root, after {@code mvn -q compile}):
 * <pre>
 *   java -cp target/classes memorymodel.PublicationExperiment [trials] [maxSpins]
 *   (Windows PowerShell: java -cp target\classes memorymodel.PublicationExperiment 20000 2000000)
 * </pre>
 * Defaults: 20000 trials, 2,000,000 polls per trial.
 *
 * <p>For each implementation it runs many one-shot trials. In each trial a fresh box is created, a reader
 * thread is started FIRST (so the reader's accesses are not ordered after the writer's by Thread.start),
 * and then the main thread writes. The main thread reads the reader's result only after join().
 *
 * <p>Outcome classes per trial:
 * <ul>
 *   <li>OK         - reader saw the signal and the payload 42;</li>
 *   <li>STALE      - reader saw the signal but the payload was 0 (default). Allowed for the unsafe box,
 *                    FORBIDDEN by the JLS for the safe boxes (a nonzero count there means a bug or JVM defect);</li>
 *   <li>NOT_SEEN   - reader gave up after maxSpins polls without seeing the signal. NOT a memory-model
 *                    violation for any box: the JLS does not promise visibility within a time bound, and
 *                    the reader may simply have started before the writer ran.</li>
 * </ul>
 *
 * <p>What you may conclude: for SAFE boxes, STALE &gt; 0 would contradict the JLS (investigate).
 * What you may NOT conclude: STALE == 0 for PlainBox does not show PlainBox is safe; STALE &gt; 0 for
 * PlainBox does not show a CPU store reordering (the JIT, the interpreter/compiler tiers or the hardware could
 * each be the cause, and this program cannot tell them apart).
 *
 * <p>Exit code: 0 if no safe box produced STALE, 1 otherwise.
 */
public final class PublicationExperiment {

    /** Counts for one implementation. Plain fields: only touched by the main thread. */
    public static final class Tally {
        public final String name;
        public int ok;
        public int stale;
        public int notSeen;

        Tally(String name) {
            this.name = name;
        }

        public int total() {
            return ok + stale + notSeen;
        }
    }

    private PublicationExperiment() {}

    /** Runs one trial and returns the reader's result (NOT_SEEN, or the observed payload). */
    public static int runTrial(SharedBox box, int maxSpins) throws InterruptedException {
        final int[] result = new int[1];  // written by reader, read by main only after join() => happens-before
        Thread reader = new Thread(() -> result[0] = box.tryRead(maxSpins), "reader");
        reader.start();
        box.write(SharedBox.PAYLOAD);   // main thread is the writer
        reader.join();                  // join: all reader actions happen-before this return (JLS 17.4.5)
        return result[0];
    }

    public static Tally runMany(Supplier<SharedBox> factory, int trials, int maxSpins) throws InterruptedException {
        Tally tally = new Tally(factory.get().name());
        for (int i = 0; i < trials; i++) {
            int r = runTrial(factory.get(), maxSpins);
            if (r == SharedBox.NOT_SEEN) {
                tally.notSeen++;
            } else if (r == SharedBox.PAYLOAD) {
                tally.ok++;
            } else {
                tally.stale++;   // observed the signal but not the payload (value 0)
            }
        }
        return tally;
    }

    public static void main(String[] args) throws Exception {
        int trials = args.length > 0 ? Integer.parseInt(args[0]) : 20_000;
        int maxSpins = args.length > 1 ? Integer.parseInt(args[1]) : 2_000_000;

        System.out.printf("Publication experiment: trials=%d maxSpins=%d%n", trials, maxSpins);
        System.out.printf("Java=%s vm=%s os=%s arch=%s cpus=%d%n",
                System.getProperty("java.version"), System.getProperty("java.vm.name"),
                System.getProperty("os.name"), System.getProperty("os.arch"),
                Runtime.getRuntime().availableProcessors());

        Tally plain = runMany(PlainBox::new, trials, maxSpins);
        Tally vol = runMany(VolatileFlagBox::new, trials, maxSpins);
        Tally sync = runMany(SynchronizedBox::new, trials, maxSpins);

        for (Tally t : new Tally[] {plain, vol, sync}) {
            System.out.printf("%-52s ok=%d stale=%d notSeen=%d total=%d%n",
                    t.name, t.ok, t.stale, t.notSeen, t.total());
        }

        boolean safeBoxesClean = vol.stale == 0 && sync.stale == 0;
        System.out.println(safeBoxesClean
                ? "CHECK PASSED: no safe box showed a stale payload (this is consistent with, not proof of, the JLS guarantee)."
                : "CHECK FAILED: a SAFE box showed a stale payload; this contradicts JLS 17.4.5. Investigate.");
        System.out.println("NOTE: PlainBox stale=0 does NOT mean PlainBox is safe; see class Javadoc.");
        System.exit(safeBoxesClean ? 0 : 1);
    }
}
