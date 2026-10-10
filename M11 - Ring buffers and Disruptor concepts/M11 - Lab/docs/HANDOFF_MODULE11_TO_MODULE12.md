# HANDOFF PACKET — MODULE 11 TO MODULE 12

**Project:** Team LNCT Qubit — Hardware Concurrency, Memory Models & Lock-Free Design  
**Current owner:** Module 11 implementation/documentation owner  
**Completed module:** Module 11 — Ring Buffers and LMAX Disruptor  
**Next owner:** Module 12 integration / benchmark interpretation owner

---

## 1. Shared project decisions

- **Java/JDK version:** Java 17 language/API baseline (`maven.compiler.release=17`). Tests executed on OpenJDK 21.0.11 (Windows); Java 17 source compatibility is enforced by the compiler plugin.
- **Build tool and dependencies:** Existing Maven project `com.epam.coe:hardware-concurrency-lab:1.0-SNAPSHOT`. JUnit Jupiter 5.11.4 and JMH 1.37 configuration unchanged. Module 11 core adds **no runtime dependency**.
- **Folder/package conventions:**
  - Main source: `src/main/java/io/lnctqubit/hwconc/m11/`
  - Tests: `src/test/java/io/lnctqubit/hwconc/m11/`
  - Report: `docs/module11-ring-buffer-disruptor.md`
  - Evidence: `results/raw/m11_*`
- **Public API:** `io.lnctqubit.hwconc.m11.SpscQueue<T>` (interface), `SpscRingBuffer<T>` (implements `SpscQueue<T>`), `PaddedSpscRingBuffer<T>` (implements `SpscQueue<T>`).

---

## 2. Files added or changed (Module 11 only)

### Added

| File | Purpose |
|------|---------|
| `src/main/java/io/lnctqubit/hwconc/m11/SpscQueue.java` | Minimal SPSC queue interface |
| `src/main/java/io/lnctqubit/hwconc/m11/PaddedSpscRingBuffer.java` | Padded variant (layout hypothesis only) |
| `src/main/java/io/lnctqubit/hwconc/m11/benchmarks/SpscBenchmark.java` | JMH benchmark skeleton; results PENDING |
| `src/main/java/io/lnctqubit/hwconc/m11/tools/LayoutProbe.java` | Field layout diagnostic (reflection only; no JOL) |
| `results/raw/m11_results_template.csv` | Benchmark results template; all rows PENDING |
| `results/raw/m11_environment_template.md` | Environment capture template |
| `results/raw/m11_maven_test_status.md` | Real test execution evidence |
| `results/raw/m11-smoke-status.md` | Updated execution status |
| `scripts/analyze_m11.py` | JMH JSON parser (PARTIAL — requires real output) |
| `docs/module11-ring-buffer-disruptor.md` | Full A–O report (updated) |
| `docs/HANDOFF_MODULE11_TO_MODULE12.md` | This document |
| `docs/module11-integration-map.md` | Module boundary map |

### Modified

| File | Change |
|------|--------|
| `src/main/java/io/lnctqubit/hwconc/m11/SpscRingBuffer.java` | Now implements `SpscQueue<T>`; `@Override` on `offer`, `poll`, `capacity` |
| `src/test/java/io/lnctqubit/hwconc/m11/SpscRingBufferTest.java` | Expanded to cover all A–K test cases |

### Not changed

`pom.xml`, all prior module source/test/docs/results files.

---

## 3. Technical content established

- **Core contract:** one producer thread, one consumer thread; no MPSC/MPMC; no reentrant calls; no blocking/spin inside methods.
- **Storage:** preallocated `Object[]`; power-of-two capacity ≥ 2; mask indexing.
- **JMM protocol:** producer writes slot → volatile `producerSequence = p+1` (publish). Consumer reads volatile `producerSequence` (acquire) → reads slot → clears slot → volatile `consumerSequence = c+1` (release). Producer reads volatile `consumerSequence` before reuse. Full JLS §17.4.4–17.4.5 argument in the report.
- **Progress:** bounded-step, non-blocking operations under strict SPSC contract. Caller retry loops may spin indefinitely if peer stops.
- **Padded variant:** same correctness contract; padding adds longs around sequence fields as a false-sharing hypothesis. JVM layout not guaranteed; actual offsets require JOL on team machine. Label: `THEORETICAL / PENDING LOCAL EXECUTION`.
- **Benchmark:** JMH group skeleton provides plain SPSC, padded SPSC, and ABQ non-blocking baseline. No timing results. Label: `PENDING LOCAL EXECUTION`.

---

## 4. Verification status

| File/test | Status | Command / Evidence | Environment |
|---|---|---|---|
| `SpscRingBuffer.java` | RUN AND PASSED | `mvn clean test`; 59 tests, 0 fail | OpenJDK 21.0.11, Windows 11, i7-12650H |
| `SpscRingBufferTest.java` — full suite | RUN AND PASSED | `mvn clean test` — EXIT=0 | Same |
| `SpscRingBufferTest` — stress 1M×5 reps | RUN AND PASSED | `mvn surefire:test -Dtest=SpscRingBufferTest -Dm11.events=1000000 -Dm11.repetitions=5 -Dm11.capacity=1024` | Same |
| All prior module tests | RUN AND PASSED | Included in `mvn clean test` (59 total) | Same |
| `PaddedSpscRingBuffer.java` | RUN AND PASSED | Covered by `SpscRingBufferTest` (K tests) | Same |
| `SpscBenchmark.java` | STATICALLY REVIEWED | Compiles; BenchmarkList verification PENDING | — |
| `LayoutProbe.java` | STATICALLY REVIEWED | Compiles; field output PENDING | — |
| JMH benchmark results | NOT RUN / PENDING | No JMH timing run attempted | — |
| Disruptor integration | NOT VERIFIED / OUT OF SCOPE | No library dependency added | — |

---

## 5. Experimental status

- **Functional:** TEAM-MEASURED — 5,000,000 SPSC transfers verified exact order and checksum on team machine.
- **Performance hypothesis (padded vs plain):** PENDING — no JMH run. Direction of effect not assumed.
- **Layout hypothesis:** PENDING — `LayoutProbe` not yet run; field offsets unknown.
- **False sharing:** THEORETICAL — padding exists as a controlled variable; no hardware-counter or cache-miss evidence.
- **Raw data:** `results/raw/m11_maven_test_status.md`, `results/raw/m11_results_template.csv` (PENDING cells).

---

## 6. Known issues

- JMH `META-INF/BenchmarkList` generation: verify after `mvn compile` with `Test-Path target\classes\META-INF\BenchmarkList`. The fat-jar build (`mvn package`) is required for standalone `java -jar` JMH execution.
- `size()` is best-effort only. Never use it for correctness or admission decisions.
- `long` sequence overflow is a theoretical limit far beyond practical horizons.
- No blocking/timed offer, interruption, shutdown, MPSC/MPMC, or consumer dependency graph is implemented.
- `analyze_m11.py` requires real JMH JSON to validate fully.
- Canonical Master Instructions, Project Contract, and authoritative handoff packets from Modules 09/10 were not present as standalone files in the supplied archive.

---

## 7. Next owner's exact scope

### Required actions

1. Run `mvn clean package -DskipTests` and confirm `target/classes/META-INF/BenchmarkList` lists `SpscBenchmark`.
2. Execute JMH benchmark with:
   ```powershell
   java -jar target\benchmarks.jar "io.lnctqubit.hwconc.m11.benchmarks.*" `
        -wi 5 -i 5 -f 3 -tu s -rf json `
        -rff results\raw\m11_jmh_raw.json | Tee-Object results\raw\m11_jmh_console.txt
   ```
3. Populate `results/raw/m11_results_template.csv` with real values.
4. Fill `results/raw/m11_environment_template.md` with actual JDK/CPU/OS details.
5. Run `java -cp target\classes io.lnctqubit.hwconc.m11.tools.LayoutProbe > results\raw\m11_layout_probe.txt`.
6. If adding real LMAX Disruptor, add it as a separately-scoped optional experiment; do not replace the educational implementation.

### Interfaces to preserve

- `io.lnctqubit.hwconc.m11.SpscQueue<T>` — minimal interface contract.
- `SpscRingBuffer<T>` and `PaddedSpscRingBuffer<T>` public method signatures.
- Java 17 source target; Maven/JUnit/JMH versions unchanged.

### Explicitly out of scope

Rewriting Modules 03–10, claiming production readiness, inventing benchmark values, changing `pom.xml` without a proven technical need.

---

## 8. Claims requiring human verification

| Claim | Status |
|---|---|
| All 59 tests pass on team machine | TEAM-MEASURED (this machine: OpenJDK 21.0.11, Windows 11, i7-12650H) |
| Tests pass on Java 17 runtime | PENDING — only compiled to --release 17; runtime target verified by source |
| Padded variant reduces false sharing | NOT MEASURED — hypothesis only |
| Any latency/CPU/throughput conclusion | NOT MEASURED |
| LMAX API behaviour/version specifics | Review linked official docs |
| Actual field offsets in JVM layout | PENDING — LayoutProbe not run |
| Canonical Project Contract API compatibility | NOT VERIFIED (contract not supplied as standalone file) |

**Handoff status: Module 11 source is COMPLETE. Full Maven/JUnit regression passed (59 tests). Stress test passed (1M events × 5 reps). Benchmark and layout probe remain PENDING.**
