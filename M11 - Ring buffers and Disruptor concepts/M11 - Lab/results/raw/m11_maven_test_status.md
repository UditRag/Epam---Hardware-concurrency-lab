# Module 11 — Test Execution Evidence

## Environment (TEAM-MEASURED: this machine)

| Field               | Value                                   |
|---------------------|-----------------------------------------|
| JDK                 | OpenJDK 21.0.11 (2026-04-21)           |
| JVM                 | OpenJDK 64-Bit Server VM, mixed mode   |
| Maven               | Apache Maven 3.9.x (from `mvn -version`)|
| Compiler release    | 17 (maven.compiler.release=17)          |
| OS                  | Windows 11 (10.0, amd64)               |
| CPU                 | 12th Gen Intel Core i7-12650H          |
| Logical cores       | 16                                      |
| JUnit Jupiter       | 5.11.4                                  |

---

## Full Regression Test Run

**Command:** `mvn clean test` (from `hardware-concurrency-lab/`)  
**Date:** 2026-10-10  
**Status:** `RUN AND PASSED`

```
Tests run: 55, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

All prior-module tests (Module 01 `fundamentals`, Module 02 `synchronization`,
Module 03/06 `cache`, Module 08 `memorymodel`) continued to pass unchanged.

---

## Module 11 Specific Stress Test

**Command:**
```powershell
mvn surefire:test "-Dtest=SpscRingBufferTest" `
  "-Dm11.events=1000000" "-Dm11.repetitions=5" "-Dm11.capacity=1024"
```

**Date:** 2026-10-10  
**Status:** `RUN AND PASSED`

```
Tests run: (subset = SpscRingBufferTest only), Failures: 0, Errors: 0
BUILD SUCCESS
```

- 5 independent runs × 1,000,000 events each = 5,000,000 total SPSC transfers
- Each event carries a sequence number and checksum (`sequence ^ 0x5A5A5A5AL`)
- Consumer verifies exact FIFO order and checksum integrity for every event
- Bounded 30-second timeout per run; no run timed out
- Queue assumed empty after each run (verified by `assertNull(q.poll())`)

**Limitation:** test passes are evidence from sampled executions, not a proof
of all interleavings or a guarantee under different JVM/hardware configurations.

---

## Benchmark Status

| Benchmark               | Status                     |
|-------------------------|----------------------------|
| `SpscBenchmark.java`    | STATICALLY REVIEWED (compiles) |
| JMH BenchmarkList       | PENDING — verify with `Test-Path target\classes\META-INF\BenchmarkList` after `mvn compile` |
| JMH timing results      | PENDING LOCAL EXECUTION    |

---

## Layout Probe Status

| Probe                   | Status                    |
|-------------------------|---------------------------|
| `LayoutProbe.java`      | STATICALLY REVIEWED (compiles) |
| Field offset output     | PENDING LOCAL EXECUTION   |
| Run: `java -cp target\classes io.lnctqubit.hwconc.m11.tools.LayoutProbe` | After `mvn compile` |
