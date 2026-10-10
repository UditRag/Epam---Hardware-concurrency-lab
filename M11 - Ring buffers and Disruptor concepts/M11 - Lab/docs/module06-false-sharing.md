# Module 06 — False Sharing and Cache-Line Bouncing

**Owner:** Vishwajeet  
**Status:** Source and experiment instructions prepared. Real benchmark output, charts based on real output, and hardware-counter evidence are **PENDING** until executed on the target machine.

## 1. Objective

Compare independent per-thread counters placed in adjacent `long[]` elements against counters separated by a 16-element stride (128 bytes of payload spacing). Use JMH at multiple thread counts, keep the measured operation identical, preserve raw outputs, and interpret results without assuming padding must win.

## 2. Concepts

**False sharing** occurs when different threads update distinct variables that occupy the same cache line. The variables are logically independent, but coherence tracks a line-sized unit, so writes can trigger ownership requests and invalidations affecting the other variable.

**Cache-line bouncing** is the informal name for repeated movement of write permission/ownership between cores when different cores repeatedly write data on the same line.

**True sharing** means threads access the same logical location. **False sharing** means they access distinct locations that happen to share a cache line. Padding can target false sharing; it does not remove true-sharing synchronization requirements.

The analogy: two people write on different sides of one board, but the entire board must be handed to the person who wants to write. Hardware coherence similarly manages a whole line rather than each Java variable separately.

## 3. Experiment design

- Benchmark: `cache.FalseSharingBench.incrementOwnedSlot`.
- Variant parameter `stride=1`: adjacent `long` elements.
- Variant parameter `stride=16`: counters' payload locations are 16 longs (128 bytes) apart.
- Thread counts: run separate JMH processes with `-t 1`, `-t 2`, `-t 4`, and `-t 8` (or a suitable subset supported by the machine).
- Work: every benchmark invocation performs one increment of the current JMH worker's own array element in both variants.
- Baseline: `-t 1` gives a low-contention/single-thread baseline; it is not expected to exhibit inter-core bouncing.
- JMH configuration: 5 warmup iterations, 5 measurement iterations, 1 second each, 3 forks, throughput in ops/s. Use the same configuration for all variants and thread counts.

The benchmark state allocates 256 slots (or 256 × 16 slots for the strided variant), so supported JMH thread counts must be no greater than 256. The array slots are independent logical counters and do not require atomic increments because each JMH worker maps to a unique slot.

## 4. Layout assumptions and caveats

The padded variant spaces the *payload indices* by 128 bytes, which is larger than a commonly encountered 64-byte cache line. This is not a guarantee that a slot begins on a cache-line boundary or that the target CPU has 64-byte lines. Array headers, base alignment, compressed references, JVM implementation, CPU cache-line size, and JIT behavior can affect physical placement. Do not claim a verified physical layout without evidence from a layout tool such as Java Object Layout (JOL) and relevant platform details.

This PoC uses explicit array striding rather than `@Contended`. `@Contended` is HotSpot-specific, may require JVM flags such as `-XX:-RestrictContended`, and should be used only when access restrictions and the exact JDK configuration are documented. It is not needed for this benchmark.

Padding may improve, do nothing, or regress. It consumes more memory and can change cache/TLB behavior. Thread affinity is not set by this code; the OS scheduler may move worker threads between logical CPUs. Record CPU topology and whether affinity tools were used.

## 5. Build and run

From `hardware-concurrency-lab/` on a machine with a compatible JDK and Maven:

```bash
mvn clean test
mvn -q -DskipTests package
```

PowerShell (Windows) loop to capture separate raw JSON files:

```powershell
New-Item -ItemType Directory -Force results/raw/m06 | Out-Null
foreach ($threads in @(1,2,4,8)) {
  mvn -q org.codehaus.mojo:exec-maven-plugin:3.5.0:java `
    "-Dexec.mainClass=org.openjdk.jmh.Main" `
    "-Dexec.args=cache.FalseSharingBench -t $threads -rf json -rff results/raw/m06/threads-$threads.json"
}
```

If the exec plugin is not available, use the project's existing JMH launch mechanism or an equivalent classpath that includes `jmh-core`, generated JMH classes, and dependencies. Retain console output and raw JSON. Do not mark a compile as a benchmark run.

Before each measurement campaign, record JDK (`java -version`), Maven (`mvn -version`), OS, CPU model, logical/physical core counts if known, power mode, JVM arguments, JMH version, and whether other workloads were running. Run on an otherwise idle machine and repeat the whole campaign if time allows. Thread affinity is deliberately not assumed; if you pin threads, document the tool and CPU mapping.

## 6. Raw output, CSV and chart

Keep JMH JSON as the source of truth. The raw JMH output records score, unit, error/confidence information and raw iteration data. Preserve each JSON file unedited. For analysis, create a CSV with at least:

`thread_count, stride, variant, throughput_ops_per_sec, jmh_error, jmh_score_unit, fork_count, warmup_iterations, measurement_iterations, jdk, cpu, os, raw_json_file`

Use JMH's score and score error directly; do not calculate fake precision from a single run. Plot throughput (ops/s) on the y-axis and thread count on the x-axis, with separate lines for adjacent and padded variants and error bars when available. Include the single-thread point. A chart must be generated from collected JSON/CSV, not from template data. Existing `scripts/analyze_jmh.py` targets Module 03 benchmark names; do not use it to claim Module 06 charting unless it is updated to recognize this benchmark.

## 7. Results (must remain pending until run)

| Thread count | Adjacent stride=1 (ops/s) | Padded stride=16 (ops/s) | JMH error / interval | Raw JSON |
|---:|---:|---:|---|---|
| 1 | PENDING | PENDING | PENDING | Not collected |
| 2 | PENDING | PENDING | PENDING | Not collected |
| 4 | PENDING | PENDING | PENDING | Not collected |
| 8 | PENDING | PENDING | PENDING | Not collected |

No performance result is claimed here. If padding is faster, the result is consistent with reduced false sharing but does not prove it. If the result is similar or slower, possible causes include workload limitations, processor topology, alignment, JVM layout, scheduler movement, measurement noise, or padding overhead.

## 8. What performance counters add

On supported Linux systems and CPUs, `perf c2c` or other suitable PMU events may help identify cache-to-cache transfers or HITM activity. Record tool version, kernel, CPU, command line, permissions, and raw output. Event names and support are platform-specific. Timing alone does **not** prove that a particular MESI/MOESI state transition occurred. If counters cannot be collected, state that clearly rather than using timing as a substitute.

## 9. Handoff to Member 7 — Vanshraj Singh Rathore

Pass along the benchmark source, stride/layout assumptions, pending/actual result files, run commands, environment record, and references. Module 06 investigates performance effects associated with cache-line granularity. Module 07 must separately investigate **language/compiler/JIT reordering versus hardware-level ordering**: source statement order is not necessarily the final instruction order, and cache coherence is not itself a complete explanation of Java Memory Model ordering guarantees. Avoid repeating the false-sharing theory; focus on ordering semantics and evidence appropriate to that question.

## 10. Integration and KT questions

Integration touchpoints: Module 04 (coherence background), Module 05 (MESI/MOESI conceptual state diagrams), Module 09 (profiling/counters), Module 11 (performance interpretation), Module 12 (integration/sign-off).

1. Why can distinct variables create coherence traffic?
2. How is true sharing different from false sharing?
3. Why does `-t 1` provide a useful baseline?
4. Why is a 128-byte stride not proof of physical cache-line alignment?
5. Why can padding regress performance?
6. Why can't elapsed time prove a MESI transition?
7. What extra evidence could `perf c2c` provide?

## References

- OpenJDK JMH: https://github.com/openjdk/jmh
- OpenJDK Java Object Layout (JOL): https://openjdk.org/projects/code-tools/jol/
- Linux `perf c2c` manual: https://man7.org/linux/man-pages/man1/perf-c2c.1.html
- Intel 64 and IA-32 Architectures Optimization Reference Manual: https://www.intel.com/content/www/us/en/developer/articles/technical/intel-sdm.html
- Java Language Specification, Java Memory Model: https://docs.oracle.com/javase/specs/
- Project context: `module04-cache-coherence.md`, `module05-mesi-moesi-state-diagrams.md`, and `references.md`.


## 11. Detailed ownership example and Java implementation choices

### Conceptual cache-line layouts

Assume a 64-byte cache line and 8-byte `long` values for illustration only. Actual cache-line size and object/array alignment must be checked on the target system.

**Adjacent layout (illustrative):**

```text
Cache line (64 B)
+----------------+----------------+------------------------------+
| X (8 B)        | Y (8 B)        | other data (48 B)             |
+----------------+----------------+------------------------------+
       ^ Core A writes X      ^ Core B writes Y
              ownership may bounce between cores
```

**Separated layout (illustrative):**

```text
Cache line A (64 B)                    Cache line B (64 B)
+----------------+------------------+  +----------------+------------------+
| X (8 B)        | padding/unused    |  | Y (8 B)        | padding/unused    |
+----------------+------------------+  +----------------+------------------+
       Core A owns line A                    Core B owns line B
```

These drawings explain the intended design; they are not evidence that the JVM placed a particular field at a specific byte offset.

### Ownership and store path (simplified)

A write generally needs the cache line in a writable/Modified state in a MESI-style protocol. If another core has the line, a read-for-ownership (RFO) or equivalent coherence request may be needed and other cached copies may be invalidated. If Core A writes X and Core B writes Y on the same line repeatedly, writable ownership can ping-pong even though X and Y are different variables. Exact states, messages, and timings depend on the CPU's coherence protocol and topology. Do not claim to have observed the protocol's internal state transitions from elapsed time alone.

A simplified timeline is:

```text
Core A writes X  -> requests writable line -> Core B's copy invalidated
Core B writes Y  -> requests writable line -> Core A's copy invalidated
Core A writes X  -> requests writable line -> Core B's copy invalidated
                         ... repeated transfers ...
```

Store buffers, speculative execution, atomic read-modify-write instructions, and pipeline stalls are microarchitecture-dependent details. An experiment should not state that a store buffer filled or that a particular instruction stalled unless a suitable profiler or hardware evidence supports that claim.

### Java padding techniques and trade-offs

1. **Array stride:** place each counter at `slot * 16` in a `long[]`. Sixteen longs represent 128 bytes of payload spacing. Java arrays store elements contiguously, so the relative element spacing is deterministic; the array base alignment and the physical cache-line boundaries are not thereby guaranteed. A 128-byte stride is a practical test choice, not a universal proof against every architecture's prefetch behavior.
2. **Manual padding fields:** fields such as `p1` through `p7` may be rearranged by JVM object-layout rules and must not be assumed to surround the hot field exactly as written. Class-hierarchy padding has been used in some implementations but remains JVM-specific.
3. **`@Contended`:** `jdk.internal.vm.annotation.Contended` is an internal, non-stable API. Application use may require `-XX:-RestrictContended`; record the exact JDK and flags. Some JDK library classes use contention-aware padding internally, but that does not make the annotation a supported general-purpose application API.

Padding consumes memory and cache capacity. It may improve throughput, have no measurable effect, or regress performance. Use JOL or another appropriate layout inspection method when claiming object offsets, and state its limits.

### A small illustrative example

```java
final class Counters {
    volatile long a; // one thread updates a
    volatile long b; // another thread updates b
}
```

The fields may share a cache line, but source code alone does not prove that they do. `volatile` makes accesses visible according to Java Memory Model rules; it does not automatically separate the fields into different cache lines. This class is an illustration, not a benchmark substitute.

### Benchmark pitfalls and evidence

- A plain, thread-confined counter may be kept in a register or optimized in other ways, so it may not create the intended repeated memory stores. Use an equivalent workload and inspect generated code when necessary.
- JMH warm-up, measurement iterations, and multiple forks help reduce distortions from JIT compilation, on-stack replacement, and run-to-run variation; they do not eliminate all noise. Avoid a single ad hoc `System.nanoTime()` loop as the main evidence.
- CPU affinity and topology matter. SMT siblings share execution resources and parts of the cache hierarchy, so results can differ from runs on separate physical cores. Record whether threads were pinned; do not assume affinity.
- On supported Linux hardware, `perf c2c` and suitable PMU events can add evidence about cache-to-cache transfers and HITM-like activity. Event availability and meaning are CPU/kernel-specific. Preserve the command and raw output.
- Report measured outcomes rather than the expected story. The padded version is not guaranteed to be faster, and throughput timing by itself does not prove an MESI transition.

### Additional KT questions

8. Why does an 8-byte gap usually fail to separate values if they remain in one cache line?
9. Why is 128-byte array stride not equivalent to verified 128-byte alignment?
10. What evidence would be needed before claiming a specific HITM or cache-to-cache event?
11. Why can SMT placement change the observed effect?
12. What are the risks of relying on `@Contended` in application code?
