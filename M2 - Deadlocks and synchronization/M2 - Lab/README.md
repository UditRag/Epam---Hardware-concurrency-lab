# hardware-concurrency-lab: Udit section (concurrency fundamentals)

Java PoC: lost updates on an unsynchronized shared counter, vs `synchronized` and `AtomicInteger`.

- Code: `src/main/java/fundamentals` (Counter, UnsafeCounter, SynchronizedCounter, AtomicCounter, Workload, RaceExperiment)
- Tests: `src/test/java/fundamentals/CounterTest.java`
- Run: `mvn test`, then `mvn -q compile` and `java -cp target\classes fundamentals.RaceExperiment [threads] [iterations] [repetitions]`
- Method and results: `docs/experiment-methodology.md`; raw output: `results/raw/`; environment: `results/environment.md`; sources: `docs/references.md`

## Contribution record (Udit)
- Code: 6 main classes. Tests: `CounterTest` (11 tests, 0 failures, TEAM-MEASURED)
- Experiment: 6 launches x 30 repetitions per implementation, TEAM-MEASURED (4 archived in `results/raw`, 2 console-only)
- Docs: methodology, references, environment, README
- Issues: files initially in wrong folders, fixed; VS Code package-mismatch squiggles came from that
- Open question for JIT/reordering owners: Unsafe failures only within the first 3 runs of each launch
- Handoff to Adrika (deadlocks): this work shows why synchronization is needed and that a race condition (unordered access to shared state) is narrower than the wider class of synchronization problems
- KT STATUS: PENDING
## Adrika's Section: Deadlocks and Synchronization

Java PoC demonstrating deadlock reproduction, deadlock detection, consistent lock ordering, and timed lock acquisition.

### Code

* `src/main/java/synchronization/DeadlockDemo.java`
* `src/main/java/synchronization/OrderedLockDemo.java`
* `src/main/java/synchronization/ReentrantLockDemo.java`

### Demonstrations

* **DeadlockDemo:** Two threads acquire locks in opposite orders, creating a circular wait.
* **OrderedLockDemo:** Uses a consistent lock ordering to avoid the demonstrated deadlock. Expected and completed operations: 20,000.
* **ReentrantLockDemo:** Uses timed `tryLock` acquisition; a thread gives up after the configured one-second timeout.

### Evidence

* `deadlock-thread-dump.txt` — JVM thread dump showing `Found one Java-level deadlock` and `Found 1 deadlock`.
* `thread-dump.txt` — additional thread dump evidence.

### Test Status

* Existing `fundamentals.CounterTest`: 11 tests run, 0 failures, 0 errors, 0 skipped.
* The three synchronization demos were compiled and run individually.
* A JUnit test suite for the synchronization demos and repeated-run experiments remain pending.

### Build Note

The project uses Java 17 and JUnit Jupiter 5.11.4. Maven is not currently available through the command line on the development machine, so `mvn test` has not been run as part of this section.

### Handoff

This section builds on the concurrency fundamentals by demonstrating deadlock as a synchronization problem distinct from a lost-update race condition.
