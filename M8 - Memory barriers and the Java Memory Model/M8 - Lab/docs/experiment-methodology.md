# Experiment methodology (Udit: concurrency fundamentals)

## Question
Does an unsynchronized shared `int` incremented by several threads lose updates, and do a `synchronized` method and an `AtomicInteger` prevent that?

## Design
- Workload (`Workload.run`): N threads each call `increment()` M times on one shared counter. Threads are created first and released together through a `CountDownLatch` to maximize contention. The main thread reads the result after `join()` on all threads.
- Expected = threads x iterations. Pass = actual == expected.
- A fresh counter is created for every run. Implementations tested: `UnsafeCounter` (plain `count++`), `SynchronizedCounter` (`synchronized` methods), `AtomicCounter` (`AtomicInteger.incrementAndGet`).
- Configurable: threads, iterations, repetitions (args of `RaceExperiment`).

## Test types
- Unit tests (`CounterTest`, JUnit 5): Synchronized and Atomic assert exact results. The Unsafe test deliberately asserts only a sane range, because a race is nondeterministic. A passing unsafe test does not show the code is thread-safe.
- Repeated-run stress experiment (`RaceExperiment`): many repetitions, contended start. It still cannot prove absence of a bug.
- jcstress (OpenJDK concurrency litmus-test harness): not used. PENDING LOCAL EXECUTION (optional).

## Reproduce
```
mvn test
mvn -q compile
java -cp target\classes fundamentals.RaceExperiment 8 1000000 30
java -cp target\classes fundamentals.RaceExperiment 8 10000000 30
```
Raw outputs: `results/raw/`. Environment: `results/environment.md`.

## Runs recorded (TEAM-MEASURED, 2026-10-08 and 2026-10-09)
| File / source | Config | Unsafe incorrect | Sync incorrect | Atomic incorrect |
|---|---|---:|---:|---:|
| run1_8t_1M_30r | 8 x 1,000,000, 30 reps | 3/30 | 0/30 | 0/30 |
| run1b_8t_1M_30r | same, new JVM | 3/30 | 0/30 | 0/30 |
| run1c_8t_1M_30r | same, new JVM | 3/30 | 0/30 | 0/30 |
| run4_8t_10M_30r | 8 x 10,000,000, 30 reps | 3/30 | 0/30 | 0/30 |
| 2026-10-09 re-run, 8 x 1,000,000, 30 reps (console only) | same | 3/30 | 0/30 | 0/30 |
| 2026-10-09 re-run, 8 x 10,000,000, 30 reps (console only) | same | 2/30 | 0/30 | 0/30 |
| mvn test (JUnit, 8 x 200,000), executed twice, 11 tests, 0 failures each time | 8 x 200,000 | 2 unsafe runs lost 1,331,826 and 1,321,991 of 1,600,000 | all repeated tests exact | all repeated tests exact |

Totals: 6 launches, 180 runs per implementation. Unsafe 17 incorrect / 163 correct; Synchronized and Atomic 0 incorrect.

## Observations (not explained)
- Across 6 launches, every Unsafe failure fell within the first 3 runs of its launch; later runs in the same launch were correct.
- One 10M launch had Unsafe run 3 = 60,000,000 (exactly 20,000,000 lost); this did not recur in a later 10M launch.
- Cause not established (THEORETICAL hypotheses untested). Open question for JIT/reordering owners.

## Limitations
- Runs within one JVM are not independent; 17/180 is not a failure rate.
- No claim about the CPU instruction sequence, cache behavior, or JMM litmus outcomes.
- No performance conclusions: timing was not measured.
- Single machine, single JVM build.
- Two launches (2026-10-09) exist as console output only, not archived in `results/raw`.
