package cas;

import java.util.concurrent.atomic.AtomicInteger;

/** Code 1: CAS success and failure. Run: java -cp target/classes cas.CASExample */
public class CASExample {
    public static void main(String[] args) {
        AtomicInteger value = new AtomicInteger(5);

        boolean first = value.compareAndSet(5, 10);   // expects 5, actual 5 -> succeeds
        System.out.println("First CAS: " + first);
        System.out.println("Value: " + value.get());

        boolean second = value.compareAndSet(5, 20);  // expects 5, actual 10 -> fails
        System.out.println("Second CAS: " + second);
        System.out.println("Value: " + value.get());
    }
}
