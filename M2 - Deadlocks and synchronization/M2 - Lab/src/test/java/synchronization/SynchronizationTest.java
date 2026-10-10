
package synchronization;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

class SynchronizationTest {

    @Test
    void orderedLockDemoCompletesAllOperations() throws Exception {
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
        assertTrue(result.contains("PASS: all operations completed"));
    }

    @Test
    void reentrantLockDemoCompletesSuccessfully() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;

        try {
            System.setOut(new PrintStream(output));
            ReentrantLockDemo.main(new String[]{});
        } finally {
            System.setOut(originalOut);
        }

        String result = output.toString();

        assertTrue(result.contains("Thread-1 acquired the lock"));
        assertTrue(result.contains("Thread-1 released the lock"));
        assertTrue(result.contains("Demo completed"));
    }
}
```
