package cas;

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicStampedReference;

/**
 * Deterministic ABA illustration (interleaving is scripted, so the result is repeatable).
 * Run: java -cp target/classes cas.AbaDemo
 */
public class AbaDemo {
    public static void main(String[] args) {
        // --- Reference-only CAS: ABA goes undetected ---
        AtomicReference<String> ref = new AtomicReference<>("A");
        String seen = ref.get();                       // Thread A observes "A"
        ref.compareAndSet("A", "B");                   // Thread B: A -> B
        ref.compareAndSet("B", "A");                   // Thread B: B -> A
        boolean plain = ref.compareAndSet(seen, "C");  // Thread A: CAS(A -> C)
        System.out.println("AtomicReference CAS(A->C) after A->B->A: " + plain + "  (ABA missed)");

        // --- AtomicStampedReference: stamp exposes the intermediate changes ---
        AtomicStampedReference<String> sref = new AtomicStampedReference<>("A", 1);
        int[] holder = new int[1];
        String seenRef = sref.get(holder);
        int seenStamp = holder[0];                     // Thread A observes (A, 1)
        sref.compareAndSet("A", "B", 1, 2);            // Thread B: (A,1) -> (B,2)
        sref.compareAndSet("B", "A", 2, 3);            // Thread B: (B,2) -> (A,3)
        boolean stamped = sref.compareAndSet(seenRef, "C", seenStamp, seenStamp + 1);
        System.out.println("AtomicStampedReference CAS((A,1)->(C,2)) after A->B->A: " + stamped
                + "  (stale stamp rejected; current stamp=" + sref.getStamp() + ")");
    }
}
