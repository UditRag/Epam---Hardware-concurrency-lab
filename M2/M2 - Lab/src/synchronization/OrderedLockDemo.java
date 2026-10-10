
package synchronization;

import java.util.concurrent.atomic.AtomicInteger;

public class OrderedLockDemo {

    private static final Object LOCK_A = new Object();
    private static final Object LOCK_B = new Object();

    private static final AtomicInteger completed =
            new AtomicInteger(0);

    private static void performOperation() {
        // Always acquire Lock A before Lock B.
        synchronized (LOCK_A) {
            synchronized (LOCK_B) {
                completed.incrementAndGet();
            }
        }
    }

    public static void main(String[] args)
            throws InterruptedException {

        int operationsPerThread = 10_000;

        Thread t1 = new Thread(() -> {
            for (int i = 0; i < operationsPerThread; i++) {
                performOperation();
            }
        }, "Thread-1");

        Thread t2 = new Thread(() -> {
            for (int i = 0; i < operationsPerThread; i++) {
                performOperation();
            }
        }, "Thread-2");

        t1.start();
        t2.start();

        t1.join(5000);
        t2.join(5000);

        if (t1.isAlive() || t2.isAlive()) {
            System.err.println("FAIL: timeout");
            System.exit(1);
        }

        int expected = 2 * operationsPerThread;

        System.out.println("Expected: " + expected);
        System.out.println("Completed: " + completed.get());

        if (completed.get() != expected) {
            System.err.println("FAIL: incorrect count");
            System.exit(1);
        }

        System.out.println("PASS: all operations completed");
    }
}
