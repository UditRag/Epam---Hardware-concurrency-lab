# hardware-concurrency-lab: Udit section (concurrency fundamentals)

Java PoC: lost updates on an unsynchronized shared counter, vs `synchronized` and `AtomicInteger`.

- Code: `src/main/java/fundamentals` (Counter, UnsafeCounter, SynchronizedCounter, AtomicCounter, Workload, RaceExperiment)
- Tests: `src/test/java/fundamentals/CounterTest.java`
- Run: `mvn test`, then `mvn -q compile` and `java -cp target\classes fundamentals.RaceExperiment [threads] [iterations] [repetitions]`
- Method and results: `docs/experiment-methodology.md`; raw output: `results/raw/`; environment: `results/raw/environment.md`; sources: `docs/references.md`

## Contribution record (Udit)
- Code: 6 main classes. Tests: `CounterTest` (11 tests, 0 failures, TEAM-MEASURED)
- Experiment: 6 launches x 30 repetitions per implementation, TEAM-MEASURED (4 archived in `results/raw`, 2 console-only)
- Docs: methodology, references, environment, README
- Issues: files initially in wrong folders, fixed; VS Code package-mismatch squiggles came from that
- Open question for JIT/reordering owners: Unsafe failures only within the first 3 runs of each launch
- Handoff to Adrika (deadlocks): this work shows why synchronization is needed and that a race condition (unordered access to shared state) is narrower than the wider class of synchronization problems
- KT STATUS: PENDING

---
# Module 03: CPU Cache Architecture (Ujjwal Pandey)

- Report: `docs/module03-cpu-cache.md` (design, sources, run steps, results template, Q&A, handoffs)
- Code: `src/main/java/cache` (AccessPatterns, SequentialVsRandomBenchmark, WorkingSetBenchmark); tests: `src/test/java/cache/AccessPatternsTest.java`
- Scripts: `scripts/collect_env.ps1` (Windows), `scripts/collect_env.sh` (Linux), `scripts/analyze_jmh.py` (JMH JSON to CSV/PNG)
- `pom.xml` changed: JMH 1.37 dependency + explicit annotation-processor path. Nothing in `fundamentals` changed.
- Status: Maven build, JUnit and JMH runs NOT RUN; results PENDING. Raw output goes to `results/raw/m03_*`.
- Module 02 (Adrika) sources are integrated at `src/main/java/synchronization/`; the duplicate source/class artifacts from the supplied ZIP are not retained. See `docs/prior-module-audit.md`.


## Module 02 integration (Adrika)

- Sources: `src/main/java/synchronization/` (`DeadlockDemo`, `OrderedLockDemo`, `ReentrantLockDemo`).
- Run the non-terminating deadlock demonstration only as a separate process you can terminate after collecting a thread dump.
- `ReentrantLockDemo` is a timed `tryLock` example; it does not demonstrate reentrant reacquisition.

## Module 03 (Ujjwal Pandey)

- Report: `docs/module03-cpu-cache.md`.
- Claim-to-source table: `docs/references.md`, Module 03 section.
- Prior-module audit and integration changes: `docs/prior-module-audit.md`.
- Benchmarks are single-threaded by design. All Module 03 measurements are PENDING until real JMH JSON/log files are collected.

**## Module 04: Cache Coherence (Prachi Dubey)**

* Report: `docs/module04-cache-coherence.md` (cache coherence, coherence vs JMM, conceptual event traces, sources, and false-sharing hypothesis).
* Java demo: `cachecoherence-demo/FalseSharingDemo.java`; run using `javac FalseSharingDemo.java` and `java FalseSharingDemo`.
* Results: 5 runs; compact average **1094.66 ms**, padded average **525.87 ms**. Output screenshot: `screenshots/module04-false-sharing-demo.png`.
* Evidence limitation: timing differences alone do not prove false sharing or specific MESI transitions.
* Status: Java demo completed; formal JMH benchmark and hardware-counter evidence PENDING.