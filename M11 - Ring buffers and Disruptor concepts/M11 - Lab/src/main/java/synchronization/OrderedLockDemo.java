
package synchronization;

import java.util.concurrent.atomic.AtomicInteger;

public class OrderedLockDemo {
    private static final Object LOCK_A = new Object();
    private static final Object LOCK_B = new Object();
    private static final AtomicInteger completed = new AtomicInteger();

    private static void performOperation() {
        synchronized (LOCK_A) {
            synchronized (LOCK_B) {
                completed.incrementAndGet();
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        int iterations = 10_000;

        Thread t1 = new Thread(() -> {
            for (int i = 0; i < iterations; i++) {
                performOperation();
            }
        }, "Thread-1");

        Thread t2 = new Thread(() -> {
            for (int i = 0; i < iterations; i++) {
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

        int expected = 2 * iterations;
        System.out.println("Expected: " + expected);
        System.out.println("Completed: " + completed.get());

        if (completed.get() != expected) {
            System.err.println("FAIL: incorrect count");
            System.exit(1);
        }

        System.out.println("PASS: all operations completed");
    }
}
