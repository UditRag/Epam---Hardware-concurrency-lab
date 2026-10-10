
package synchronization;

import java.util.concurrent.CountDownLatch;

public class DeadlockDemo {

    private static final Object LOCK_A = new Object();
    private static final Object LOCK_B = new Object();

    private static final CountDownLatch READY =
            new CountDownLatch(2);

    private static void taskOne() {
        synchronized (LOCK_A) {
            System.out.println("Thread-1 acquired Lock A");
            READY.countDown();
            awaitBothThreads();

            System.out.println("Thread-1 waiting for Lock B");
            synchronized (LOCK_B) {
                System.out.println("Thread-1 acquired Lock B");
            }
        }
    }

    private static void taskTwo() {
        synchronized (LOCK_B) {
            System.out.println("Thread-2 acquired Lock B");
            READY.countDown();
            awaitBothThreads();

            System.out.println("Thread-2 waiting for Lock A");
            synchronized (LOCK_A) {
                System.out.println("Thread-2 acquired Lock A");
            }
        }
    }

    private static void awaitBothThreads() {
        try {
            READY.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        Thread t1 = new Thread(
                DeadlockDemo::taskOne, "Thread-1");
        Thread t2 = new Thread(
                DeadlockDemo::taskTwo, "Thread-2");

        t1.start();
        t2.start();
    }
}
