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
* The previous README version referenced `cachecoherence-demo/FalseSharingDemo.java`, `screenshots/module04-false-sharing-demo.png`, and five-run timing averages. These artifacts are **UNVERIFIED IN THE SUPPLIED PROJECT SNAPSHOT**: the referenced source and screenshot are absent, and raw measurements were not available to validate the averages.
* Evidence limitation: timing differences alone do not prove false sharing or specific MESI transitions.
* Status: conceptual report present; demo source, screenshot, and reported timing values require recovery and verification. Formal JMH benchmark and hardware-counter evidence remain PENDING.

## Module 05: MESI/MOESI State Diagrams (Vivansh — confirm assignment)

- Report: `docs/module05-mesi-moesi-state-diagrams.md` (MESI states, simplified transition diagrams, MOESI Owned state, event traces, source-backed limits, and handoff).
- PDF copy: `docs/module05-mesi-moesi-state-diagrams.pdf`.
- Scope: documentation only. No Java source, Maven dependency, test, benchmark, or hardware-counter capture was added.
- Evidence: diagrams are THEORETICAL teaching models until reviewed against the target CPU's exact authoritative protocol documentation.
- Status: STATICALLY PREPARED / PENDING LOCAL REVIEW.

## Module 06: False Sharing and Cache-Line Bouncing

- Report: `docs/module06-false-sharing.md` (concepts, layouts, ownership timeline, Java experiment, limitations, references, KT questions).
- PoC: `src/main/java/cache/FalseSharingLayout.java` and `FalseSharingBench.java` (JMH adjacent vs padded counter slots).
- Tests: `src/test/java/cache/FalseSharingLayoutTest.java`.
- Results template: `results/raw/m06_false_sharing_results_template.csv`; environment template: `results/raw/m06_environment.md`.
- Status: SOURCE/DOCUMENTATION PREPARED; local Maven/JMH execution and hardware-counter confirmation PENDING. No benchmark numbers are fabricated.


## Module 06 — False Sharing and Cache-Line Bouncing (Vishwajeet)

- Report: `docs/module06-false-sharing.md`.
- Benchmark: `src/main/java/cache/FalseSharingBench.java`; compare `stride=1` and `stride=16` at `-t 1, 2, 4, 8` in separate JMH runs.
- Tests: `src/test/java/cache/FalseSharingLayoutTest.java`.
- Handoff packet for Vanshraj: `docs/handoff-module06-to-module07-vanshraj.md`.
- Raw JMH JSON, chart, and hardware-counter evidence are **PENDING** until collected on the target machine; do not invent results.


## Module 07: Compiler and CPU Reordering (Vanshraj Singh Rathore)

- Report: `docs/module07-compiler-cpu-reordering.md`; claim/source register: `docs/module07-references.md`.
- Isolated JCStress project: `experiments/jcstress-store-buffering/` (plain vs volatile store-buffering litmus tests).
- Build/run from that directory: `mvn clean package`, then `java -jar target/module07-jcstress.jar`; `-m quick` is a smoke test only.
- Result templates: `results/raw/m07_environment-template.md`, `results/raw/m07_outcomes-template.csv`.
- Status: source/documentation prepared; build and JCStress execution NOT RUN here; raw outcomes PENDING. No results are fabricated.
