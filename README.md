# Hardware Concurrency Lab

A twelve-module, Java-based learning and experiment repository on **hardware-aware concurrency**: it starts from a concrete shared-state bug (lost updates), moves down through CPU caches, coherence, and memory ordering, and comes back up to atomic primitives, a lock-free stack, a bounded single-producer/single-consumer (SPSC) ring buffer, and a measured benchmark of that ring buffer.

The work was produced for the *EPAM CoE: Hardware Concurrency, Memory Models & Lock-Free Design* programme by Team LNCT Qubit (as stated in the module reports). Each module has its own report, code where applicable, and an explicit **evidence label** that separates what was measured from what is theory, static review, or still pending.

> **Read this first:** the repository is a *progression of snapshots*, not a single multi-module build. Most module folders contain a full copy of the Maven project as it stood at that module. For the complete, integrated code base use the **Module 12** snapshot. Modules 5 and 9 are standalone exceptions. See [How the repository is organised](#how-the-repository-is-organised).

---

## Table of Contents

- [Overview](#overview)
- [How the repository is organised](#how-the-repository-is-organised)
- [Key concepts and learning objectives](#key-concepts-and-learning-objectives)
- [Technology stack](#technology-stack)
- [Repository structure](#repository-structure)
- [Module guide](#module-guide)
- [Learning path](#learning-path)
- [Prerequisites](#prerequisites)
- [Installation and setup](#installation-and-setup)
- [Running the examples and experiments](#running-the-examples-and-experiments)
- [Concurrency engineering notes](#concurrency-engineering-notes)
- [Testing and verification](#testing-and-verification)
- [Benchmarking and performance evaluation](#benchmarking-and-performance-evaluation)
- [Configuration](#configuration)
- [Troubleshooting](#troubleshooting)
- [Working in this repository](#working-in-this-repository)
- [Limitations and open work](#limitations-and-open-work)
- [Authorship and license](#authorship-and-license)
- [Where to start](#where-to-start)

---

## Overview

**Audience:** engineers and students who want to understand *why* concurrent code behaves the way it does on real hardware, and how to evaluate such claims honestly.

**Scope.** The lab connects the following layers in one evidence-aware workflow (see [`Roadmap and Workflow.pdf`](Roadmap%20and%20Workflow.pdf)):

| Layer | Question | Modules |
|---|---|---|
| Correctness | What goes wrong with unsynchronized shared state? | M1 |
| Liveness | What goes wrong *because of* synchronization? | M2 |
| Memory hierarchy | How do locality and working-set size change performance? | M3 |
| Coherence | How do cores keep cached copies of one line consistent? | M4, M5 |
| Contention | When do independent variables interfere on one cache line? | M6 |
| Ordering | What may the compiler, CPU, and Java Memory Model reorder or permit? | M7, M8 |
| Atomicity | What does compare-and-set provide, and what does it not? | M9 |
| Non-blocking design | What do lock-free and wait-free actually guarantee? | M10 |
| Specialised structure | How does a bounded SPSC ring buffer work, and how does it relate to the LMAX Disruptor? | M11 |
| Measurement | What can be concluded from benchmarks, and what cannot? | M12 |

**Evidence discipline.** The reports repeatedly separate several kinds of claim. The labels below are used throughout the repository and in this README:

| Label | Meaning |
|---|---|
| `TEAM-MEASURED` | Collected by the team on its own machine; raw output archived or recorded in a report. |
| `MEASURED ONCE` | A single observation; no rate or distribution claimed. |
| `CONCEPTUAL` / `THEORETICAL` | Explanatory model or teaching simulation; not hardware evidence. |
| `STATICALLY REVIEWED` | Read and reasoned about, not executed. |
| `PENDING` | Prepared but not executed or not archived in this repository. |
| `NOT PROVEN` | A hypothesis the available data does not establish. |

A recurring caution applies to the whole project: **Java timing alone does not prove a particular cache-coherence event, cache miss, store-buffer effect, or fence behaviour.** Every benchmark result is tied to the CPU, JDK, OS, and configuration it was taken on.

---

## How the repository is organised

The repository holds one folder per module (`M1` … `M12`). Each folder typically contains:

- a **report** (PDF, DOCX, or Markdown) authored for that module, and
- a **lab folder** with the code, documents, and results *as they stood at the end of that module*.

Because each module extended the previous one, the Maven lab folders are **cumulative snapshots** and contain duplicated files (for example, the same `fundamentals` sources and the same `results/raw/run*.txt` files appear in many snapshots). Consequences:

- **Use the Module 12 snapshot** to build and run everything that belongs to the integrated Maven project (M1–M3, M6, M8, M10–M12 code).
- The **Module 10 snapshot does not compile the lock-free code**: in that folder the Treiber-stack sources sit under `docs/`, not `src/`. The compiled version lives in the Module 11 and Module 12 snapshots under `src/main/java/atomics/lockfree/`.
- **Module 5** (`coherence` package) and **Module 9** (`cas` package) are **standalone**: their code is *not* part of the Module 12 Maven project. Run them from their own folders.
- Many per-module `README.md` files inside the lab folders are *progress logs* that were appended module by module. They may be stale or contradict the sources (see [Limitations and open work](#limitations-and-open-work)). When in doubt, trust the source code, the `pom.xml`, and the raw files under `results/`.

---

## Key concepts and learning objectives

Concepts are grouped by what the repository actually does with them.

| Theme | Concepts | How it is covered | Evidence |
|---|---|---|---|
| Shared mutable state | Lost updates, data races, `synchronized`, `AtomicInteger` | Runnable counter experiment and unit tests (M1) | `TEAM-MEASURED` |
| Liveness | Lock-order inversion, deadlock, ordered locking, timed `tryLock` | Three runnable demos (M2) | `MEASURED ONCE` |
| Memory hierarchy | Spatial locality, working-set size, dependent loads | Two single-threaded JMH benchmarks and layout tests (M3) | `TEAM-MEASURED` (see [M3 note](#m3--cpu-cache-architecture)) |
| Coherence | Invalidation/ownership of cache lines; coherence vs. memory consistency | Written report with event traces (M4); MESI/MOESI teaching simulator (M5) | `CONCEPTUAL` / `THEORETICAL` |
| False sharing | Cache-line granularity, padding, ownership bouncing | JMH design and layout tests (M6) | Design only; results `PENDING` |
| Ordering | Compiler/JIT reordering, store buffering, message passing, happens-before | Litmus tests for JCStress (M7, M8); Java experiments and tests (M8) | Mostly `PENDING` |
| Atomics | Compare-and-set, retry loops, the ABA problem | Five standalone programs and a test class (M9) | Executed in a sandbox; limited |
| Progress guarantees | Blocking vs. obstruction-free vs. lock-free vs. wait-free | Treiber stack, CAS-loop counter, lock-based comparison (M10) | Implemented; tests pass in M12 regression |
| Ring buffers | Bounded SPSC queue, power-of-two masking, sequence ownership, padded layout variant, comparison with the LMAX Disruptor | Two implementations and a test suite (M11) | `TEAM-MEASURED` (tests, stress) |
| Measurement | JMH design, throughput vs. latency, layout verification, interpreting noise | Benchmarks, layout probes, analysis scripts (M12) | `TEAM-MEASURED` |

---

## Technology stack

Only technologies evidenced by the `pom.xml` files, sources, and scripts are listed.

| Category | Technology | Where established |
|---|---|---|
| Language | Java (compiler `release` 17) | `pom.xml` → `maven.compiler.release` |
| Build | Apache Maven; `maven-compiler-plugin` 3.13.0, `maven-surefire-plugin` 3.5.2, `maven-shade-plugin` 3.6.0 | `pom.xml` |
| Testing | JUnit Jupiter 5.11.4 | `pom.xml` |
| Benchmarking | JMH 1.37 (`jmh-core`, `jmh-generator-annprocess`), fat JAR `target/benchmarks.jar` | `pom.xml` (M12) |
| Layout inspection | JOL 0.17; a dependency-free `sun.misc.Unsafe` offset probe | `pom.xml`, `m12/tools/` |
| Memory-model litmus tests | JCStress 0.16 (two isolated sub-projects) | `experiments/*/pom.xml` |
| Analysis | Python 3 scripts (`matplotlib` for charts) | `scripts/*.py` |
| Automation | PowerShell (`run_module12.ps1`, `collect_env.ps1`) and Bash (`collect_env.sh`) | `scripts/` |
| Reference queue | `java.util.concurrent.ArrayBlockingQueue` as a benchmark baseline | `SpscThroughputBenchmark`, `SpscLatencyBenchmark` |
| Editor settings | VS Code Java settings | `.vscode/settings.json` |

The LMAX Disruptor library is **not** a dependency. It is discussed conceptually in the Module 11 report and is explicitly out of scope for integration and measurement.

---

## Repository structure

Abbreviated; generated and duplicated files are omitted.

```text
.
├── Roadmap and Workflow.pdf                 # Consolidated 12-module flow and evidence matrix
├── M1/                                      # Concurrency fundamentals and race-condition PoC
│   ├── M1 - Lab/                            # Maven snapshot (fundamentals/)
│   └── Udit - Concurrency Fundamentals and Race-Condition PoC.pdf
├── M2/                                      # Deadlocks and synchronization
│   ├── M2 - Lab/                            # Maven snapshot (+ synchronization/)
│   └── Module 02 Deadlocks and Synchronization.docx
├── M3/ … M4/                                # CPU cache architecture; cache coherence (report-only for M4)
├── M5/                                      # MESI/MOESI — STANDALONE, no Maven
│   └── M5 - Lab/ (src/main/java/coherence, scripts/run.sh, scripts/run.ps1)
├── M6/ … M8/                                # False sharing; reordering (JCStress); memory barriers and JMM
├── M9/                                      # CAS and atomics — STANDALONE Maven project (cas/)
├── M10 - Lock-free and wait-free algorithms/
├── M11 - Ring buffers and Disruptor concepts/
└── M12 - Benchmarking and integration/
    ├── Module12_Report.pdf
    ├── module12-benchmarking-integration.md
    └── Module_12 - hardware-concurrency-lab/   # ← integrated project (use this one)
        ├── pom.xml
        ├── src/main/java/
        │   ├── fundamentals/                # M1
        │   ├── synchronization/             # M2
        │   ├── cache/                       # M3, M6
        │   ├── memorymodel/                 # M8
        │   ├── atomics/lockfree/            # M10
        │   └── io/lnctqubit/hwconc/
        │       ├── m11/                     # SPSC queue, ring buffers, tools, benchmark skeleton
        │       └── m12/                     # benchmarks and layout tools
        ├── src/test/java/                   # JUnit 5 tests (7 test classes)
        ├── experiments/                     # isolated JCStress projects (M7, M8)
        ├── scripts/                         # run_module12.ps1, analysis and environment scripts
        ├── results/
        │   ├── raw/                         # earlier-module raw output, templates, evidence status files
        │   └── m12/                         # final M12 JMH JSON/logs, CSVs, charts, layout output
        └── docs/                            # per-module reports, references, handoffs, diagrams
```

Notes on selected locations:

- `docs/` in the Module 12 snapshot holds the Markdown reports for M3–M8, M11, and M12, the claim-to-source tables (`references*.md`), the M12 evidence matrix (`module12-evidence-matrix.md`), and handoff packets between modules.
- `results/m12/quick/` contains **smoke-run** output and is explicitly *not* final evidence.
- `target/` directories checked into `M1/M1 - Lab` and `M2/M2 - Lab` are build output, not sources.

---

## Module guide

### Overview table

| # | Module | Primary artifacts | Runnable? | Evidence (per Roadmap PDF and module reports) |
|---|---|---|---|---|
| M1 | Concurrency fundamentals and race PoC | `fundamentals/`, `CounterTest` | Yes | `TEAM-MEASURED` |
| M2 | Deadlocks and synchronization | `synchronization/` | Yes | `MEASURED ONCE` |
| M3 | CPU cache architecture | `cache/` (2 JMH benchmarks), `AccessPatternsTest` | Yes (JMH) | `TEAM-MEASURED` (raw JMH output not archived) |
| M4 | Cache coherence | Report only | No | `CONCEPTUAL` |
| M5 | MESI / MOESI state diagrams | `coherence/` simulator (standalone), diagrams | Yes (standalone) | `THEORETICAL` |
| M6 | False sharing and cache-line bouncing | `FalseSharingBench`, `FalseSharingLayout`, tests | Yes (JMH) | Results `PENDING` |
| M7 | Compiler and CPU reordering | JCStress project `jcstress-store-buffering` | Yes (JCStress) | Results `PENDING` |
| M8 | Memory barriers and the JMM | `memorymodel/`, JCStress project `jcstress-jmm-module08` | Yes | `PARTIAL` / `PENDING` |
| M9 | CAS and atomic operations | `cas/` (standalone), `CasTest` | Yes (standalone) | Executed in a sandbox; limited |
| M10 | Lock-free and wait-free algorithms | `atomics/lockfree/`, `TreiberStackTest` | Yes | Implemented; see module entry |
| M11 | Ring buffers and Disruptor concepts | `m11/` SPSC queues, tests, layout probe | Yes | `TEAM-MEASURED` (tests, stress) |
| M12 | Benchmarking and integration | `m12/` JMH benchmarks, JOL/offset probes, scripts, results | Yes | `TEAM-MEASURED` |

Module folder names: `M1` … `M9` are short names; the last three are `M10 - Lock-free and wait-free algorithms`, `M11 - Ring buffers and Disruptor concepts`, and `M12 - Benchmarking and integration`.

### M1 — Concurrency fundamentals and race-condition PoC

- **Teaches:** that an unsynchronized `count++` on shared state loses updates, that `synchronized` and `AtomicInteger` prevent it, and that a passing test does not prove race freedom.
- **Inspect first:** `src/main/java/fundamentals/` (`Counter`, `UnsafeCounter`, `SynchronizedCounter`, `AtomicCounter`, `Workload`, `RaceExperiment`), then `docs/experiment-methodology.md`.
- **Run:** `fundamentals.RaceExperiment [threads] [iterations] [repetitions]` (see [Running](#running-the-examples-and-experiments)).
- **Evidence:** the Roadmap reports 17 of 180 unsafe runs incorrect versus 0 of 180 for each of the synchronized and atomic counters (8 threads; 1 M and 10 M increments). Four of six launches are archived in `results/raw/run*.txt`; two were console-only. No performance winner is claimed.
- **Limitation:** failures occurred only in the first runs of each launch; the reports flag this as an open JIT/reordering question. The unit test for the unsafe counter deliberately asserts only a sane range.

### M2 — Deadlocks and synchronization

- **Teaches:** lock-order inversion, the conditions for deadlock, and ordered locking as a fix.
- **Inspect first:** `src/main/java/synchronization/` — `DeadlockDemo`, `OrderedLockDemo`, `ReentrantLockDemo`; and `Module 02 Deadlocks and Synchronization.docx` in the module folder.
- **Run:** each class has a `main`. `DeadlockDemo` is **intended not to terminate**; run it as a separate process you can kill after taking a thread dump. `ReentrantLockDemo` demonstrates a timed `tryLock`; it does not demonstrate reentrant re-acquisition.
- **Evidence:** a deadlock was reproduced and captured once; the ordered version completed 20,000 of 20,000 operations in the observed run. A proposed 30-run batch was not archived, so no rate is claimed.

### M3 — CPU cache architecture

- **Teaches:** why locality and working-set size matter, using single-threaded JMH benchmarks: sequential vs. shuffled access over three array sizes, and a dependent pointer-chase over working sets from 4 KiB to 128 MiB.
- **Inspect first:** `src/main/java/cache/` — `AccessPatterns`, `SequentialVsRandomBenchmark`, `WorkingSetBenchmark`; report `docs/module03-cpu-cache.md`.
- **Evidence:** the supporting PDF and the Roadmap report large penalties for random access at large working sets (about 17.5 M ns/op sequential vs. 324.5 M ns/op shuffled at 128 MiB). **The raw JMH JSON/log files are not included in this repository**, and the Module 12 evidence matrix still lists M3 JMH as pending local execution, so treat the figures as reported, not independently archived.
- **Limitation:** timing is not direct cache-counter evidence; large footprints also introduce TLB and page-walk effects.

### M4 — Cache coherence

- **Teaches:** why cores cannot keep conflicting stale copies of the same line, and the distinction between cache coherence and memory consistency (the JMM). Uses protocol-neutral event traces (read sharing, write-after-read, competing writers).
- **Inspect first:** `docs/module04-cache-coherence.md` (also `module04-cache-coherence-prachi (2).pdf` in `M4/`).
- **Evidence:** conceptual only; no hardware measurements. A demo referenced by an earlier version of the report is recorded as absent and unverified.

### M5 — MESI / MOESI state diagrams

- **Teaches:** the M/E/S/I states and the Owned state added by MOESI, as a snooping-bus teaching model.
- **Inspect first:** `M5/M5 - Lab/docs/module05-mesi-moesi.md`, `diagrams/`, then `src/main/java/coherence/` (`State`, `Protocol`, `SnoopingBus`, `TraceRunner`, `TransitionTableDump`, `InvariantCheck`).
- **Run:** standalone with `javac`/`java`, no Maven and no dependencies (see [Running](#running-the-examples-and-experiments)). `InvariantCheck` drives random loads, stores, and evictions and verifies the model's own invariants.
- **Evidence:** `THEORETICAL`. The simulator has no transient states, directory, or snoop filter. A passing invariant check shows the *model* is self-consistent, not how any CPU behaves. The module README notes that its screenshots were rendered from sandbox output and should be re-captured locally.

### M6 — False sharing and cache-line bouncing

- **Teaches:** that independent variables on one cache line can contend; compares adjacent counter slots (`stride=1`) with separated slots (`stride=16`).
- **Inspect first:** `src/main/java/cache/FalseSharingLayout.java`, `FalseSharingBench.java`; report `docs/module06-false-sharing.md` and the diagrams in `docs/diagrams/`.
- **Evidence:** the experiment design (stride 1 vs. 16 at 1/2/4/8 threads, 5 warm-up and 5 measurement iterations, 3 forks) and layout tests exist; **no M6 benchmark output is archived**, and hardware-counter confirmation is pending.

### M7 — Compiler and CPU reordering

- **Teaches:** the difference between program order, JIT transformations, CPU execution/observation order, and JMM happens-before, via store-buffering and message-passing litmus tests (plain vs. volatile).
- **Inspect first:** `docs/module07-compiler-cpu-reordering.md`, then `experiments/jcstress-store-buffering/` (`PlainStoreBuffering`, `VolatileStoreBuffering`).
- **Run:** build and run the JCStress sub-project (see [Running](#running-the-examples-and-experiments)).
- **Evidence:** sources and result templates prepared; **no JCStress outcomes are archived**.

### M8 — Memory barriers and the Java Memory Model

- **Teaches:** program order, synchronization order, happens-before, `volatile`, monitors, and safe publication; keeps Intel 64 store-buffer discussion distinct from JMM guarantees.
- **Inspect first:** `docs/module08-memory-barriers-jmm.md`, `src/main/java/memorymodel/` (`PlainBox`, `VolatileFlagBox`, `SynchronizedBox`, `PublicationExperiment`, `VolatileCounter`, `VolatileCounterExperiment`), and `experiments/jcstress-jmm-module08/`.
- **Evidence:** JMM reasoning is source-backed; unit tests in `src/test/java/memorymodel/` pass in the M12 regression run. Experiment output on team hardware and JCStress results are pending; the only archived experiment output (`results/raw/m08_container_*`) comes from a single-vCPU sandbox and is not team evidence. The `m08-x86-store-buffer-ordering.svg` diagram is illustrative, not an observed trace.

### M9 — CAS and atomic operations (standalone)

- **Teaches:** compare-and-set success/failure, a CAS retry loop, plain vs. `synchronized` vs. atomic counters under contention, and the ABA problem.
- **Inspect first:** `M9/M9 - Lab/docs/module09-cas-atomic-operations.md`, then `src/main/java/cas/` — `CASExample`, `CASRetryCounter`, `CounterComparison`, `CasContentionDemo`, `AbaDemo`. Tests: `src/test/java/cas/CasTest.java`.
- **Run:** `scripts/run_all.sh` or `scripts/run_all.ps1` (compiles and runs all five programs, saving output to `results/raw/`).
- **Evidence:** the programs were run in a single-vCPU sandbox; there the plain counter did not lose updates, so the multi-core comparison is pending. `CasTest` was written but its recorded status is "not run". The `cas` package is not in the Module 12 Maven project.

### M10 — Lock-free and wait-free algorithms

- **Teaches:** blocking vs. obstruction-free vs. lock-free vs. wait-free; why "no `synchronized`" does not imply lock-free; a Treiber stack with linearization points, JMM visibility reasoning, and an ABA argument.
- **Inspect first:** `src/main/java/atomics/lockfree/` in the **M11/M12 snapshot** — `TreiberStack`, `LockStack` (blocking comparison), `CasLoopCounter`, `RetryDemo`, `ProgressDemo`, `LockFreeChecks`, `StackContentionBenchmark` (JMH); report `Module 10 Technical Report … .pdf` and `docs/MODULE-10.md`.
- **Evidence:** the source documents classify `TreiberStack` and `CasLoopCounter` as **lock-free, not wait-free** and state their assumptions. `TreiberStackTest` (4 tests) passes in the recorded M12 Maven run. Results of `ProgressDemo`, `RetryDemo`, and the contention benchmark are not archived. The Roadmap PDF marks M10 tests as pending; that predates the M12 regression run.
- **Limitation:** the ABA argument holds only while nodes are never recycled (GC-managed fresh allocation); a node pool would invalidate it. `docs/MODULE-10.md` still describes an earlier proposal layout (`io.lnctqubit.hwconc.m10`); the implemented package is `atomics.lockfree`.

### M11 — Ring buffers and the LMAX Disruptor

- **Teaches:** a bounded SPSC ring buffer (power-of-two capacity, preallocated storage, one writer per sequence), a padded-layout variant as a *hypothesis*, and a source-grounded comparison with the LMAX Disruptor.
- **Inspect first:** `src/main/java/io/lnctqubit/hwconc/m11/` — `SpscQueue`, `SpscRingBuffer`, `PaddedSpscRingBuffer`; then `src/test/java/.../m11/SpscRingBufferTest.java` and `docs/module11-ring-buffer-disruptor.md`.
- **Evidence:** the recorded M11 regression and stress evidence is `TEAM-MEASURED` (see [Testing](#testing-and-verification)). The M11 benchmark skeleton `SpscBenchmark` was statically reviewed in M12, where four defects were documented; it was left unchanged and superseded by the M12 benchmarks.
- **Out of scope (stated in the report):** MPSC/MPMC, multiple consumers, blocking or timed APIs, shutdown, production readiness, and any integration of the actual LMAX library.

### M12 — Benchmarking and integration

- **Teaches:** reproducible measurement of the M11 queues with JMH (throughput and ping-pong latency, with `ArrayBlockingQueue` as a reference), memory-layout validation with two independent tools, and disciplined interpretation.
- **Inspect first:** `docs/module12-benchmarking-integration.md` (the same file also sits at the top of `M12 - Benchmarking and integration/`), `docs/module12-evidence-matrix.md`, then `src/main/java/io/lnctqubit/hwconc/m12/`.
- **Run:** `scripts/run_module12.ps1` (PowerShell, Windows) or the manual commands in [Benchmarking](#benchmarking-and-performance-evaluation).
- **Evidence:** `TEAM-MEASURED` on one machine; results are summarised [below](#recorded-m12-results). The report states that the false-sharing mechanism is **not proven** without hardware counters.

---

## Learning path

The diagram shows the documented hand-off order and conceptual dependencies from the Roadmap. It is a *scope* flow, not a claim that each module's code calls the previous one.

```mermaid
flowchart LR
    M1["M1<br/>Race PoC"] --> M2["M2<br/>Deadlocks"]
    M2 --> M3["M3<br/>CPU cache"]
    M3 --> M4["M4<br/>Coherence"]
    M4 --> M5["M5<br/>MESI / MOESI"]
    M5 --> M6["M6<br/>False sharing"]
    M6 --> M7["M7<br/>Reordering"]
    M7 --> M8["M8<br/>JMM / barriers"]
    M8 --> M9["M9<br/>CAS / atomics"]
    M9 --> M10["M10<br/>Lock-free"]
    M10 --> M11["M11<br/>SPSC ring buffer"]
    M11 --> M12["M12<br/>Benchmark and integration"]
```

Reading the flow: M1–M2 cover software-level correctness and liveness; M3–M6 move down to caches, coherence, and line contention; M7–M8 separate compiler/CPU ordering from the Java Memory Model; M9–M11 turn those guarantees into atomic and non-blocking structures; M12 measures the SPSC result and records what remains unproven.

---

## Prerequisites

| Requirement | Detail | Source |
|---|---|---|
| JDK | **17 or newer.** The build sets `maven.compiler.release=17`. Recorded runs used OpenJDK 21.0.11 (M11) and Oracle JDK 27+35 (M1, M12). | `pom.xml`, `results/` |
| Maven | Required for the integrated project, JMH fat JAR, and JCStress sub-projects. Recorded runs used Maven 3.10.0 (M12) and 3.9.x (M11). | `results/` |
| Internet access | Needed once for Maven to resolve dependencies (JUnit, JMH, JOL, JCStress). | `pom.xml` |
| Python 3 + `matplotlib` | Only for `scripts/analyze_m12.py` (and optionally `analyze_jmh.py`, `chart_false_sharing.py` for PNG output). | `scripts/*.py` |
| PowerShell | Only for `scripts/run_module12.ps1` and `collect_env.ps1`; the script uses Windows-specific commands (`Get-CimInstance`, `powercfg`). | `scripts/` |
| Operating system | Recorded team runs: **Windows 11**. Linux appears only in sandbox smoke runs and in `collect_env.sh`. Portability of the full workflow has not been formally verified. | `results/` |
| Hardware | A multi-core CPU is needed for contention experiments to be meaningful. Recorded M12 results come from one 16-core/24-thread laptop CPU. | `results/m12/m12_environment.md` |

Module 5 needs only a JDK (no Maven). Module 9's scripts need only a JDK as well; its `pom.xml` is needed only for `mvn test`.

---

## Installation and setup

```bash
git clone https://github.com/UditRag/Epam---Hardware-concurrency-lab.git
cd Epam---Hardware-concurrency-lab

# Integrated project (Modules 1-3, 6, 8, 10-12)
cd "M12 - Benchmarking and integration/Module_12 - hardware-concurrency-lab"

# Compile and run the JUnit regression suite
mvn clean test

# Build the JMH fat JAR (target/benchmarks.jar) without re-running tests
mvn -q package -DskipTests
```

The Maven coordinates are `com.epam.coe:hardware-concurrency-lab:1.0-SNAPSHOT`. No environment variables or credentials are required.

The remaining commands in this README assume this directory (`Module_12 - hardware-concurrency-lab`) as the working directory unless stated otherwise.

---

## Running the examples and experiments

### Integrated project (Module 12 snapshot)

After `mvn clean test` (or `mvn -q compile`), run classes from `target/classes`. The classpath form below works on Windows and Linux.

| Module | Command | Arguments / notes |
|---|---|---|
| M1 | `java -cp target/classes fundamentals.RaceExperiment 8 1000000 30` | `[threads] [iterations] [repetitions]`; the methodology also uses `8 10000000 30`. |
| M2 | `java -cp target/classes synchronization.OrderedLockDemo` | Terminating, ordered-lock version. |
| M2 | `java -cp target/classes synchronization.ReentrantLockDemo` | Timed `tryLock` example. |
| M2 | `java -cp target/classes synchronization.DeadlockDemo` | **Does not terminate by design.** Run in its own process; take a thread dump, then kill it. |
| M8 | `java -cp target/classes memorymodel.PublicationExperiment 20000 2000000` | `[trials] [maxSpins]` (defaults shown). |
| M8 | `java -cp target/classes memorymodel.VolatileCounterExperiment 8 200000 10` | `[threads] [iterations] [repetitions]` (defaults shown). |
| M10 | `java -cp target/classes atomics.lockfree.LockFreeChecks` | Dependency-free checks with timeouts; exit code `0` means all passed. |
| M10 | `java -cp target/classes atomics.lockfree.RetryDemo` | Failed-CAS counts per thread count (CSV-style output). |
| M10 | `java -cp target/classes atomics.lockfree.ProgressDemo [threads] [seconds]` | Throughput and per-thread fairness spread, Treiber vs. lock-based stack. This is a demonstration, not benchmark evidence. Its in-source usage comment names a different package; the class is `atomics.lockfree.ProgressDemo`. |
| M11 | `java -cp target/classes io.lnctqubit.hwconc.m11.tools.LayoutProbe` | Field-offset probe for the queue classes. |
| M12 | `java -cp target/classes io.lnctqubit.hwconc.m12.tools.OffsetProbe` | `sun.misc.Unsafe` offsets; prints deprecation warnings on recent JDKs. |
| M12 | `java -cp target/benchmarks.jar io.lnctqubit.hwconc.m12.tools.JolLayout` | Needs the fat JAR so JOL is on the classpath. |

### Standalone Module 5 (MESI/MOESI simulator)

From `M5/M5 - Lab`:

```bash
javac -d out src/main/java/coherence/*.java
java -cp out coherence.TraceRunner [mesi|moesi]
java -cp out coherence.TransitionTableDump [mesi|moesi]
java -cp out coherence.InvariantCheck [operations]
```

`scripts/run.sh` and `scripts/run.ps1` run all three programs. The output is a teaching model, not hardware output.

### Standalone Module 9 (CAS)

From `M9/M9 - Lab`:

```bash
./scripts/run_all.sh          # Linux/macOS; use .\scripts\run_all.ps1 on Windows
# or manually:
javac -d target/classes src/main/java/cas/*.java
java -cp target/classes cas.CASExample
java -cp target/classes cas.CounterComparison 8 1000000 10
```

### JCStress sub-projects (M7, M8)

These are isolated Maven projects under `experiments/`. Per the module documentation, from each sub-project directory:

```bash
cd experiments/jcstress-store-buffering     # M7  (or experiments/jcstress-jmm-module08 for M8)
mvn clean package
java -jar target/module07-jcstress.jar      # M8 jar: target/module08-jcstress.jar
```

Per the module READMEs, `-m quick` is a smoke mode only and not evidence. JCStress results have **not** been archived in this repository.

---

## Concurrency engineering notes

These notes describe what the sources implement and the guarantees they claim. They are not a general textbook.

### `SpscRingBuffer` (M11)

- **Ownership contract:** exactly one producer thread calls `offer`; exactly one consumer thread calls `poll`. Other usage is unsupported. `null` items are rejected, so `null` from `poll` unambiguously means "empty".
- **Structure:** a preallocated `Object[]` with power-of-two capacity (at least 2) and an index mask. Two `volatile long` sequences: `producerSequence` (written only by the producer) and `consumerSequence` (written only by the consumer).
- **Coordination:** the producer writes the slot, then publishes it with a volatile write of `producerSequence`; the consumer observes that volatile read before reading the slot, then releases the slot by writing `consumerSequence`. The correctness argument rests on the JLS volatile/happens-before rules (JLS §17.4), recorded as `THEORETICAL / EXTERNAL EVIDENCE`.
- **No CAS:** each sequence has a single writer, so no compare-and-set is used.
- **Behaviour:** `offer` returns `false` when full and `poll` returns `null` when empty; neither blocks, locks, or allocates. Items are handed over **by reference**; mutating an item after publication is the caller's responsibility to avoid.
- **Progress wording:** the sources describe `offer`/`poll` as bounded-step operations *under the SPSC contract*. They deliberately avoid claiming a general lock-free or wait-free classification.
- **`PaddedSpscRingBuffer`:** same contract, with explicit `long` padding fields around the hot sequences. It exists as a *layout hypothesis*. Field layout is JVM-specific (the JVM may reorder fields), which is why M12 verifies the actual offsets.

### Treiber stack and CAS counter (M10)

- `TreiberStack` is an unbounded multi-producer/multi-consumer LIFO built on `AtomicReference.compareAndSet`. A failed CAS means another thread's CAS succeeded, so the *system* makes progress (**lock-free**); an individual thread can retry indefinitely, so it is **not wait-free**.
- Linearization points: successful CAS for `push` and non-empty `pop`; the read that returned `null` for empty `pop`.
- ABA: safe here only because nodes are freshly allocated and never reused under a garbage collector. A node pool would require tagged or stamped references.
- `CasLoopCounter` is likewise lock-free, not wait-free. The sources note that the progress guarantee of `AtomicLong.getAndIncrement()` is not specified by the API or JLS.

### Reading memory-model material

M4/M5 explain how hardware keeps one location coherent; M7/M8 explain that coherence is not the Java Memory Model. The JMM defines which executions are *legal*; hardware and JIT behaviour can explain an implementation but cannot override that guarantee. Likewise, an outcome being *permitted* by the JMM says nothing about how *often* it will be observed.

---

## Testing and verification

All automated tests are JUnit Jupiter 5 tests in `src/test/java/` of the integrated project. Run them with:

```bash
mvn clean test
```

**Recorded result (team machine, 2026-10-10, `results/m12/m12_mvn_test_output.txt`): 59 tests, 0 failures, 0 errors, 0 skipped — BUILD SUCCESS.**

| Test class | Module | Tests run |
|---|---|---:|
| `fundamentals.CounterTest` | M1 | 11 |
| `cache.AccessPatternsTest` | M3 | 7 |
| `cache.FalseSharingLayoutTest` | M6 | 4 |
| `memorymodel.PublicationTest` | M8 | 9 |
| `memorymodel.VolatileCounterTest` | M8 | 6 |
| `atomics.lockfree.TreiberStackTest` | M10 | 4 |
| `io.lnctqubit.hwconc.m11.SpscRingBufferTest` | M11 | 18 |

`SpscRingBufferTest` is parameterised over both `SpscRingBuffer` and `PaddedSpscRingBuffer` and includes a configurable producer/consumer stress test. The recorded M11 stress run used 5 repetitions × 1,000,000 events at capacity 1024 (5 M transfers), checking exact FIFO order and a per-event checksum:

```bash
mvn surefire:test "-Dtest=SpscRingBufferTest" "-Dm11.events=1000000" "-Dm11.repetitions=5" "-Dm11.capacity=1024"
```

(`m11.events`, `m11.repetitions`, and `m11.capacity` default to 500,000, 3, and 1024.)

What these results do and do not establish:

- They are **sampled executions**, not proofs over all interleavings, and not guarantees on other JVMs or hardware.
- There is **no automated test** for M2, M5, the `cas` package (recorded as not run), the JCStress litmus tests, or the M12 benchmarks. Several earlier reports state "not run" for their own tests; the M12 regression run above supersedes those statements only for the test classes listed in the table.
- Only the M5 simulator performs a self-consistency check (`InvariantCheck`), and it checks the model, not hardware.
- No formal verification is present.

---

## Benchmarking and performance evaluation

Benchmarks in the integrated project (all JMH 1.37):

| Benchmark | Module | What it measures |
|---|---|---|
| `io.lnctqubit.hwconc.m12.benchmarks.SpscThroughputBenchmark` | M12 | Completed transfers per second for plain SPSC, padded SPSC, and `ArrayBlockingQueue`; capacities 64 and 1024; one producer and one consumer per group. Read the **consumer** row (the group aggregate double-counts). |
| `io.lnctqubit.hwconc.m12.benchmarks.SpscLatencyBenchmark` | M12 | Ping-pong round-trip time (depth 1, capacity 64), `Mode.SampleTime`. Read the **ping** row; one-way is approximately RTT/2 under a symmetric-leg assumption. |
| `cache.SequentialVsRandomBenchmark`, `cache.WorkingSetBenchmark` | M3 | Single-threaded access-pattern and working-set timing. |
| `cache.FalseSharingBench` | M6 | Adjacent (`stride=1`) vs. separated (`stride=16`) counters, per thread count. |
| `atomics.lockfree.StackContentionBenchmark` | M10 | Lock-free vs. lock-based stack contention (source only; no archived results). |
| `io.lnctqubit.hwconc.m11.benchmarks.SpscBenchmark` | M11 | Skeleton with documented defects; **superseded by the M12 benchmarks**. |

### Reproducing the M12 measurements

Full pipeline (Windows PowerShell, from the project directory):

```powershell
.\scripts\run_module12.ps1            # full run: environment capture, tests, fat JAR, layout, JMH, analysis
.\scripts\run_module12.ps1 -Quick     # smoke run (1 fork, 2 warm-up, 3 measurement): NOT evidence
.\scripts\run_module12.ps1 -SkipTests
```

The script's header estimates the full run at roughly 35–45 minutes. Manual equivalent for one benchmark (settings used by the final run: 5 warm-up × 1 s, 10 measurement × 1 s, 3 forks):

```bash
mvn -q package -DskipTests
java -jar target/benchmarks.jar "io.lnctqubit.hwconc.m12.benchmarks.SpscThroughputBenchmark" \
     -wi 5 -i 10 -f 3 -rf json -rff results/m12/m12_jmh_SpscThroughputBenchmark_final.json
python scripts/analyze_m12.py results/m12 final     # JSON -> CSV summaries + PNG charts (needs matplotlib)
```

Output lands in `results/m12/`. Other benchmarks can be selected with a JMH name pattern on the same JAR (for example the M3 and M6 classes above, using standard JMH options such as `-t`, `-p`, `-f`); that usage follows the module documents and has not been separately recorded here.

### Recorded M12 results

Environment: Intel Core i7-13700HX (16 cores / 24 logical), 16 GB, Windows 11, Oracle JDK 27+35, Maven 3.10.0, JMH 1.37; no extra JVM flags; **threads not pinned**; AC power, Windows power plan not set to "High performance"; no thermal measurement taken (throttling not ruled out). Source: `results/m12/*.csv` and `docs/module12-benchmarking-integration.md`.

**Throughput — transfers/s, consumer row, 3 forks, ±99.9 % CI from JMH**

| Variant | Capacity 64 | Capacity 1024 |
|---|---|---|
| `SpscRingBuffer` (plain) | 16.71 M ± 0.61 M | 17.43 M ± 1.26 M |
| `PaddedSpscRingBuffer` | 14.95 M ± 0.17 M | 16.02 M ± 0.36 M |
| `ArrayBlockingQueue` (reference) | 6.16 M ± 0.32 M | 7.82 M ± 0.35 M |

**Round-trip latency — ns, capacity 64, depth 1**

| Variant | mean | p50 | p99 | p99.9 | max |
|---|---:|---:|---:|---:|---:|
| plain | 167 | 100 | 300 | 2,300 | 1.6 ms |
| padded | 167 | 100 | 300 | 2,100 | 3.3 ms |
| `ArrayBlockingQueue` | 459 | 200 | 1,500 | 32,096 | 6.6 ms |

**Layout validation (JDK 27 on the team machine):** two independent tools (`OffsetProbe` via `Unsafe`, and JOL 0.17) agree on field offsets. In the plain class both sequences sit 8 bytes apart; in the padded class they are 120 bytes apart and each is at least 64 bytes from `mask`/`entries`. Field layout is JVM-specific: the report records different offsets on JDK 21.

### How to interpret these numbers

- Both SPSC queues were roughly 2.1–2.7× faster than `ArrayBlockingQueue`. This compares a handoff queue under an SPSC contract with a locked multi-producer/multi-consumer queue that has different semantics; it is a reference point, not a like-for-like contest.
- **Padding did not improve throughput in this setup.** At capacity 64 plain was about 12 % faster with non-overlapping confidence intervals; at capacity 1024 the intervals overlap, so no difference is established. *Why* is **not proven** without hardware counters.
- The p50 value of 100 ns sits at the Windows timer resolution; rely on the mean and the tail instead. Millisecond-scale maxima reflect OS scheduling and interrupts, not queue behaviour.
- Fork-to-fork spread for the plain queue is about ±10 % on an unpinned, hybrid-core CPU. Treat differences under roughly 10 % as noise.
- Results depend on hardware, JDK, OS, power state, thread placement, and workload (one producer, one consumer, a shared preallocated token payload). They are not portable claims.

---

## Configuration

There are **no environment variables** and no credentials. Configurable surfaces, all taken from the sources and scripts:

| Setting | Where | Purpose |
|---|---|---|
| `maven.compiler.release` = 17 | `pom.xml` | Java language/API baseline. |
| `jmh.version` = 1.37 | `pom.xml` | JMH version (the pom comment suggests checking Maven Central for newer versions). |
| `m11.events`, `m11.repetitions`, `m11.capacity` | JVM system properties read by `SpscRingBufferTest` | Stress-test size; defaults 500,000 / 3 / 1024. |
| Command-line arguments | `RaceExperiment`, `PublicationExperiment`, `VolatileCounterExperiment`, `ProgressDemo` | See [Running](#running-the-examples-and-experiments). |
| JMH options | `java -jar target/benchmarks.jar …` | Warm-up, iterations, forks, threads, parameters, result files. |
| `-Quick`, `-SkipTests` | `scripts/run_module12.ps1` | Smoke run; skip the Maven test phase. |
| `jcstress.version` = 0.16 | `experiments/*/pom.xml` | JCStress version. |
| `.vscode/settings.json` | Module 12 snapshot | JDT language-server JVM arguments (editor only). |

`pom.xml` also declares `javax.annotation-api` (scope `provided`) to resolve `@Generated` annotations emitted by JMH in IDEs on JDK 11+, and configures the JMH annotation processor explicitly so that `META-INF/BenchmarkList` is generated.

---

## Troubleshooting

| Symptom | Likely cause and action |
|---|---|
| JMH reports "no benchmarks" | `META-INF/BenchmarkList` was not generated. The `pom.xml` configures the JMH annotation processor explicitly for this reason; rebuild with `mvn clean package -DskipTests` and run the **fat JAR** (`target/benchmarks.jar`). |
| `target/benchmarks.jar` missing | `run_module12.ps1` stops with this error if packaging failed; run `mvn -q package -DskipTests` and read the Maven output. |
| IDE shows hundreds of `javax.annotation.Generated cannot be resolved` errors | JMH generates `@Generated` annotations removed from the JDK in version 11; the `javax.annotation-api` dependency in the `pom.xml` addresses this. |
| `Unsafe` deprecation warnings from `OffsetProbe` | Expected on recent JDKs; the tool uses `sun.misc.Unsafe` on purpose. Output is JVM-specific. |
| PowerShell script treats JDK stderr as an error | A known issue fixed in the shipped script (`cmd /c` is used for `java -version` and `mvn -v`). If you edit the script, preserve this. |
| `DeadlockDemo` never exits | By design. Run it in a separate process and terminate it after taking a thread dump. |
| Compile errors on JDK older than 17 | The build requires `release` 17. |
| Contention results vary strongly between runs | Expected: thread scheduling, hybrid P/E-core placement, power plan, and thermal state all matter. Use several forks, record the environment, and consider pinning threads before drawing conclusions. |
| JMH results on a single-core machine show no contention | Contention experiments need multiple hardware threads; single-vCPU sandbox runs recorded in some modules (M8, M9) are not team evidence. |

---

## Working in this repository

The repository does not define a contribution policy, branching strategy, or coding standard. The conventions below are visible in the existing material and are worth following when extending the lab:

- **Label evidence.** Use the labels in [Overview](#overview). Never present a template, a sandbox smoke run, or a reasoning argument as measured team evidence.
- **Archive raw output.** Keep raw JMH JSON/logs and an environment description next to any reported number (`results/…`). Smoke runs belong in a separate folder (`results/m12/quick/`).
- **Do not overwrite earlier modules.** Earlier reports and raw results are kept as the record for their module.
- **Package conventions.** Earlier modules use short packages (`fundamentals`, `synchronization`, `cache`, `memorymodel`, `atomics.lockfree`); Module 11 and 12 code uses `io.lnctqubit.hwconc.m11` / `m12`. Tests mirror main packages under `src/test/java`.
- **State assumptions and limits** in Javadoc and reports, as the existing classes do (ownership contracts, progress guarantees, ABA assumptions).
- **Hand-offs.** `docs/HANDOFF_*.md` files record what each module delivered and left open.

---

## Limitations and open work

Taken from the Roadmap, the M12 evidence matrix, and observations of the repository itself.

**Known evidence gaps** (per the Roadmap and module reports):

- M2: one observation per demo; no performance comparison.
- M3: no direct hardware-counter profiling; raw JMH output is not archived here.
- M4, M5: conceptual and simulated models only; no captured coherence traces.
- M6: false-sharing benchmark output not archived.
- M7: JCStress build and results pending.
- M8: team-machine experiment output and JCStress results pending; Intel SDM section verification partial; invalidation-queue behaviour unresolved.
- M9: multi-core rerun, `CasTest` execution record, and integration into the shared Maven build pending.
- M10: contended-test and benchmark results not archived.
- M12: no pinned-core rerun; no hardware counters (`perf`, `perf c2c`, `-prof perfnorm`) — so no false-sharing conclusion; the real LMAX Disruptor is not integrated or benchmarked; thermal state not measured.

**Repository inconsistencies to be aware of** (documentation, not behaviour):

- The progress logs inside the lab-folder `README.md` files are cumulative and partly stale. In the Module 12 snapshot's `README.md`, the M9 entry names `CasLoopCounterTest.java` and the M10 entry names `LockFreeStack.java` and `docs/module10-*.md`; none of these exist in that snapshot (the implemented classes are `TreiberStack`, `CasLoopCounter`, and `TreiberStackTest`). Some text in that README is also garbled (the word "coherence" is corrupted in the M4 and M7 entries).
- Test counts differ between status files: `results/raw/m11_maven_test_status.md` records 55 tests; the later M12 run records 59 (the difference equals the 4 `TreiberStackTest` tests).
- `docs/module12-benchmarking-integration.md` §1 still describes the build as "PENDING LOCAL EXECUTION" while its header and §6 record the team execution; the executed state is the later one.
- `docs/MODULE-10.md` is an early proposal with assumed package names; the implementation is under `atomics.lockfree`.
- Module-level README owner names for some modules differ from the Roadmap's attribution.

**Not planned in the sources:** MPSC/MPMC queues, a real LMAX Disruptor integration, and production hardening of any implementation. None of the implementations here should be treated as production-ready.

---

## Authorship and license

**Attribution.** The Roadmap PDF assigns modules in this order: Udit (M1) → Adrika (M2) → Ujjwal (M3) → Prachi (M4) → Vansh (M5) → Namita (M6) → Vanshraj Rathore (M7) → Aastha (M8) → Ashi (M9) → Vivansh (M10) → Kabir (M11) → Vishwajeet (M12). Some module READMEs name different owners for individual modules (for example M5 and M6); the Roadmap states that its attribution follows the requested sequence. Maintainers should confirm the final attribution before publishing.

**Credit.** Module reports identify the work as Team LNCT Qubit, produced for the EPAM CoE programme.

**License.** The repository snapshot contains **no license file**. Without one, the code and documents are not licensed for reuse. Add a license file before inviting reuse or contributions.

---

## Where to start

1. Read [`Roadmap and Workflow.pdf`](Roadmap%20and%20Workflow.pdf) for the whole progression and the evidence matrix.
2. Open the integrated project in `M12 - Benchmarking and integration/Module_12 - hardware-concurrency-lab` and run `mvn clean test`.
3. Run `fundamentals.RaceExperiment` (M1) to see a lost-update bug, then work through the module guide in order.
4. Finish with `docs/module12-benchmarking-integration.md` and `docs/module12-evidence-matrix.md` to see how the lab separates what was measured from what remains unproven.
