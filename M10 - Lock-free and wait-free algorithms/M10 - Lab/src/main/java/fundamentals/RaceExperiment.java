package fundamentals;

import java.util.function.Supplier;

/** Usage: java -cp target/classes fundamentals.RaceExperiment [threads] [iterations] [repetitions] */
public class RaceExperiment {

    public static void main(String[] args) throws Exception {
        int threads     = args.length > 0 ? Integer.parseInt(args[0]) : 8;
        int iterations  = args.length > 1 ? Integer.parseInt(args[1]) : 1_000_000;
        int repetitions = args.length > 2 ? Integer.parseInt(args[2]) : 30;

        long expectedLong = (long) threads * iterations;
        if (expectedLong > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("threads*iterations exceeds int range");
        }
        int expected = (int) expectedLong;

        System.out.printf("CONFIG threads=%d iterations=%d repetitions=%d expected=%d%n",
                threads, iterations, repetitions, expected);
        System.out.println("implementation,run,expected,actual,lost,result");

        run("Unsafe", UnsafeCounter::new, threads, iterations, repetitions, expected);
        run("Synchronized", SynchronizedCounter::new, threads, iterations, repetitions, expected);
        run("Atomic", AtomicCounter::new, threads, iterations, repetitions, expected);
    }

    private static void run(String name, Supplier<Counter> factory,
                            int threads, int iterations, int repetitions, int expected) throws Exception {
        int incorrect = 0;
        for (int r = 1; r <= repetitions; r++) {
            int actual = Workload.run(factory.get(), threads, iterations);   // fresh counter each run
            boolean pass = actual == expected;
            if (!pass) incorrect++;
            System.out.printf("%s,%d,%d,%d,%d,%s%n", name, r, expected, actual,
                    expected - actual, pass ? "PASS" : "FAIL");
        }
        System.out.printf("SUMMARY %s runs=%d incorrect=%d correct=%d%n",
                name, repetitions, incorrect, repetitions - incorrect);
    }
}