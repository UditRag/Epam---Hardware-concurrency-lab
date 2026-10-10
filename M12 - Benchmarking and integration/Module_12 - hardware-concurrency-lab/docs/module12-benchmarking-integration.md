# Module 12 — Benchmarking & Integration (report, ≤3 pages)

**Status: implemented and executed on the team machine (TEAM-MEASURED, 2026-10-10).** Environment: i7-13700HX (16 cores / 24 logical), 16 GB, Windows 11, Oracle JDK 27+35 (HotSpot; JOL reports a Lilliput/compact-header VM), Maven 3.10.0, JMH 1.37, no extra JVM flags, threads not pinned. Power: AC connected during the final run; Windows power plan named "Acer" (not High performance); no noticeable heating, but no thermal measurement taken, so throttling is not ruled out. JMH total time 4m34s (throughput) + 2m17s (latency). Tests: 59 run, 0 failures (quick-run invocation; same code as the final run).

## 1. What was built (Module 11/prior files untouched; `pom.xml` extended)
- `m12/benchmarks/SpscThroughputBenchmark` — plain vs padded SPSC (capacity 64, 1024) + `ArrayBlockingQueue` reference, 1 producer + 1 consumer per group. 1 op = 1 completed transfer; report the **consumer** row.
- `m12/benchmarks/SpscLatencyBenchmark` — ping-pong round trip (depth 1, capacity 64), `Mode.SampleTime`; use the **ping** row; one-way ≈ RTT/2 (symmetric-leg assumption).
- `m12/tools/OffsetProbe` (dependency-free, `sun.misc.Unsafe`) and `JolLayout` (JOL 0.17).
- `scripts/run_module12.ps1` (env capture → `mvn clean test` → fat jar → layout → JMH → analysis) and `scripts/analyze_m12.py` (JSON → CSV + PNG charts).
- `pom.xml`: `jol-core 0.17`, `maven-shade-plugin 3.6.0` → `target/benchmarks.jar` (JMH `Main`). Not buildable here: **PENDING LOCAL EXECUTION**.

## 2. Benchmark design and why the Module 11 skeleton was not reused
Defects found by static review (STATICALLY REVIEWED): (a) three `@Param`s in one state class create a 2×2×2 cross-product, so each group re-runs under irrelevant parameter values; (b) spin loops without `Control.stopMeasurement` can spin forever when the peer thread stops at iteration end; (c) ABQ `offer/poll` calls were counted as ops even when they failed, inflating "throughput"; (d) the group aggregate in JMH output sums both roles and double-counts transfers.
Defensible settings: 5 warm-up × 1 s, 10 measurement × 1 s, 3 forks (JVM-level variance), per-fork means exported so spread across forks is visible. Payload is a shared preallocated token (no allocation); only queue mechanics are measured.
Limits: ABQ is a locked MPMC queue with different semantics — a reference point, not a like-for-like. Spin-retry policy (`onSpinWait`) is part of the measured cost. Ping-pong measures uncontended hand-off latency, not latency under load (per-item timestamps would perturb the queue). Thread placement is left to the OS; on hybrid CPUs (i7-12650H) P/E-core placement can dominate results — record it or pin threads (e.g. `start /affinity`) and say so.

## 3. Memory-layout validation
Two independent tools agree (`m12_offset_probe.txt` via Unsafe, `m12_jol_layout.txt` via JOL 0.17; JOL's *addresses* are flagged "guessed", the *offsets* are class-layout facts). Team JVM (JDK 27):

| Class | producerSequence | consumerSequence | mask / entries |
|---|---|---|---|
| SpscRingBuffer | 8 | 16 (8 B apart) | 24 / 28 |
| PaddedSpscRingBuffer | 64 | 184 (120 B apart) | 248 / 252 |

Findings: (1) In the padded class the JVM kept the padding: the two sequences, and each of them vs `mask`/`entries`, are ≥ 64 B apart, so none can share a 64 B line whatever the 8 B object alignment. (2) In the plain class everything sits in 32 B, so sharing is certain if the object is not split by a line boundary. (3) Sandbox JDK 21.0.10 gave different offsets (e.g. `mask` only 60 B from `producerSequence` when padded): layout is JVM-specific and must be re-verified per JDK. (4) **NOT PROVEN:** that false sharing occurs or is removed; no hardware counters were available (cache line size assumed 64 B, not queried).

## 4. Results (3 forks × 10 × 1 s after 5 × 1 s warm-up; 99.9% CI from JMH)
Throughput, transfers/s (consumer row; `results/m12/m12_throughput_summary_final.csv`, chart `m12_throughput_final.png`):

| Variant | Cap 64 | Cap 1024 |
|---|---|---|
| SpscRingBuffer (plain) | 16.71 M ± 0.61 M | 17.43 M ± 1.26 M |
| PaddedSpscRingBuffer | 14.95 M ± 0.17 M | 16.02 M ± 0.36 M |
| ArrayBlockingQueue (reference) | 6.16 M ± 0.32 M | 7.82 M ± 0.35 M |

Round trip, ns, capacity 64, depth 1 (`m12_latency_summary_final.csv`, `m12_latency_final.png`):

| Variant | mean | p50 | p99 | p99.9 | max |
|---|---|---|---|---|---|
| plain | 167 | 100 | 300 | 2,300 | 1.6 ms |
| padded | 167 | 100 | 300 | 2,100 | 3.3 ms |
| ABQ | 459 | 200 | 1,500 | 32,096 | 6.6 ms |

## 5. Interpretation (what the data does and does not support)
- Both SPSC queues were ~2.1-2.7× faster than ABQ. This reflects lock-free hand-off vs a locked MPMC queue with different semantics, not a like-for-like win.
- **Padded was not faster.** At capacity 64 plain beat padded by ~12% with non-overlapping CIs. At 1024 the CIs overlap (plain fork means 15.6-19.4 M): no difference established. The padding hypothesis is therefore **not supported on this machine/JVM**; why (e.g. the extra cache lines touched, core placement, JDK 27 layout) is **NOT PROVEN** without counters.
- Latency: mean RTT is identical for plain and padded (~167 ns). p50 = 100 ns is at the resolution of the Windows timer (JMH shows p0 ≈ 0), so read the **mean** and the tail, not p50 or "one-way = 50 ns". The ms-scale maxima are OS scheduling/interrupts, not queue behaviour.
- Fork-to-fork spread is large for plain (±~10%): unpinned threads on a 24-thread hybrid CPU. Treat sub-10% differences as noise.
- Scope: one machine, one JDK, one thread placement, 1P+1C, token payload. No Disruptor library was benchmarked.

## 6. Reproduce
`.\scripts\run_module12.ps1` (add `-SkipTests` to skip Maven tests). Outputs land in `results/m12/`; `results/m12/quick/` holds smoke-run files, not evidence. Two script fixes were needed on this machine (stderr from JDK 27 treated as an error; JMH JSON puts producer/consumer under `secondaryMetrics`) and are included.

## 7. Limits / next steps
Record AC power and thermals; repeat pinned to two P-cores (and with `-prof perfnorm`/`perf c2c` on Linux) before drawing any false-sharing conclusion; compare against the real LMAX Disruptor only if added with a test.
