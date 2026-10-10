package fundamentals;

import org.junit.jupiter.api.Test;

import synchronization.OrderedLockDemo;
import synchronization.ReentrantLockDemo;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

class SynchronizationTest {

    @Test
    void orderedLockDemoCompletesWithoutLostUpdates() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;

        try {
            System.setOut(new PrintStream(output));
            OrderedLockDemo.main(new String[]{});
        } finally {
            System.setOut(originalOut);
        }

        String result = output.toString();
        assertTrue(result.contains("Expected: 20000"));
        assertTrue(result.contains("Completed: 20000"));
        assertTrue(result.contains("PASS"));
    }


    @Test
    void reentrantLockDemoCompletes() throws InterruptedException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;

        try {
            System.setOut(new PrintStream(output));
            ReentrantLockDemo.main(new String[]{});
        } finally {
            System.setOut(originalOut);
        }

        assertTrue(output.toString().contains("Demo completed"));
    }
}

```

