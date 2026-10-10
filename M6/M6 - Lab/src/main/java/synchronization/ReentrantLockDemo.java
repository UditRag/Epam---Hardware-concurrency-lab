
package synchronization;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class ReentrantLockDemo {
    private static final ReentrantLock lock = new ReentrantLock();

    public static void main(String[] args) throws InterruptedException {
        Thread t1 = new Thread(() -> {
            lock.lock();
            try {
                System.out.println("Thread-1 acquired the lock");
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            } finally {
                lock.unlock();
                System.out.println("Thread-1 released the lock");
            }
        }, "Thread-1");

        Thread t2 = new Thread(() -> {
            try {
                System.out.println("Thread-2 trying to acquire the lock");

                if (lock.tryLock(1, TimeUnit.SECONDS)) {
                    try {
                        System.out.println("Thread-2 acquired the lock");
                    } finally {
                        lock.unlock();
                    }
                } else {
                    System.out.println(
                        "Thread-2 could not acquire the lock within 1 second"
                    );
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("Thread-2 was interrupted");
            }
        }, "Thread-2");

        t1.start();
        Thread.sleep(200);
        t2.start();

        t1.join();
        t2.join();

        System.out.println("Demo completed");
    }
}
