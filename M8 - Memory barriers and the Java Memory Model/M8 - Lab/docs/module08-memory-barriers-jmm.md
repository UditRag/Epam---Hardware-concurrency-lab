# Module 08 — Memory Barriers and the Java Memory Model (JMM)

**Owner:** Aastha Pandey (Member 8) · **Project:** Team LNCT Qubit — EPAM CoE: Hardware Concurrency, Memory Models & Lock-Free Design · **Next owner:** Ashi (Member 9, CAS and Atomic Operations)

**Status:** SOURCE/DOCUMENTATION PREPARED. Code compiles at `-source 17` in the authoring container and the two experiment programs were run there (1 virtual CPU, JDK 21). Maven build, real JUnit run, JCStress build/run and all runs on the team machine are **NOT RUN**. Architecture-specific (Intel SDM) claims are **PARTIAL**: the current SDM PDFs could not be opened by the authoring tool. The module is **not COMPLETE** until the items in section I are done by a human.

Companion files: `docs/module08-references.md` (claim-to-source register), `docs/HANDOFF_MEMBER08_TO_MEMBER09.md`, `results/raw/m08_*`.

---

## A. Scope and boundaries

**In scope:** program order, synchronization order, happens-before, data races, volatile, monitors, safe publication, why `volatile count++` is not atomic, the store-buffering litmus test reasoned from the JLS, and what fences/barriers constrain on Intel 64 in relation to store buffers.

**Out of scope (owned elsewhere, only cross-referenced):** compiler/JIT/CPU reordering overview and the plain/volatile store-buffering JCStress tests (Module 07, Vanshraj); cache coherence and MESI/MOESI (Modules 04/05); false sharing (Module 06); CAS and `java.util.concurrent.atomic` internals (Module 09, Ashi); lock-free/wait-free algorithms (Module 10); ring buffer / Disruptor (Module 11); benchmarking/integration (Module 12).

**Cross-module alignment:** Module 07's handoff is present inside the supplied ZIP (section J of `docs/module07-compiler-cpu-reordering.md`) and was read. Module 07's own instruction file and any review feedback beyond the ZIP were **not supplied**. Member 5's and Member 4's reports are present as documents; their authors' own handoff packets to Module 08 were not supplied. The "Head of EPAM India" feedback was **not supplied**, so compliance with it cannot be claimed (section J).

## B. The five layers — keep them apart

| Layer | Question it answers | Example in this module | Authority |
|---|---|---|---|
| 1. Java language guarantee (JMM) | Which outcomes may/must a correct JVM allow? | `(1,0)` is forbidden in `VolatileFlagMessagePassing` | JLS §17.4 (verified, read) |
| 2. JVM/JIT implementation choice | Which machine code realises those guarantees? | Which instruction a volatile store becomes on x86-64 | Not established here (C22) |
| 3. CPU architecture guarantee | What does the ISA allow? | Intel 64 lets a load pass an earlier store to a different address | Intel SDM Vol. 3A (PARTIAL) |
| 4. Microarchitectural explanation | Why might a CPU behave that way? | A store buffer holding a store while a later load runs | Explanation only; not directly observable from Java |
| 5. Measured observation | What happened on one machine/JVM/run? | Counts printed by `PublicationExperiment` | Only for the tested setup |

A rule used throughout: **the JLS decides what is allowed; layers 2–5 can explain or illustrate but never override layer 1.** "May occur" (permitted) is never written as "will occur".

## C. JMM explained for a beginner (all section numbers are JLS SE 17 Chapter 17, read in this session)

**Shared variables (§17.4.1).** Fields, static fields and array elements live in shared heap memory. Local variables and parameters are never shared. Two accesses to the same variable *conflict* if at least one is a write.

**Program order (§17.4.3).** Within one thread, the order its actions would run according to ordinary single-thread semantics. It is a per-thread notion only; it says nothing about what another thread sees.

**Synchronization actions and synchronization order (§17.4.2, §17.4.4).** Volatile reads/writes, monitor lock/unlock, thread start, thread termination detection and a few others are *synchronization actions*. Every execution has one total order over them (the synchronization order), consistent with each thread's program order.

**Synchronizes-with (§17.4.4).** The key rules for this module:
1. Unlock of monitor *m* synchronizes-with every later lock of *m*.
2. A write to a volatile variable *v* synchronizes-with every later read of *v* (by any thread).
3. `Thread.start()` synchronizes-with the first action of the started thread.
4. The last action of a thread synchronizes-with another thread's detection that it terminated (`join()`, `isAlive()`).

**Happens-before (§17.4.5).** Built from program order + synchronizes-with + transitivity. "If one action happens-before another, then the first is visible to and ordered before the second." Happens-before is a *guarantee about visibility*, not a stopwatch: the JLS itself says an implementation may reorder actions that are hb-related as long as results are consistent with a legal execution.

**Data race (§17.4.5).** Two conflicting accesses not ordered by happens-before. Data races are not "undefined behaviour" in Java; the JMM still bounds what a racy read may return, but the set of permitted values is wide and counter-intuitive.

**Visibility.** A read is *allowed to see* a write *w* if the read does not happen-before *w* and there is no intervening write *w'* with hb(w,w') and hb(w',read). Without a happens-before edge, a read may see the old value, the new value, or (for the right pattern) a mix of earlier writes.

**Volatile (§8.3.1.4, §17.4.4).** Each volatile access is a synchronization action. Guarantees: (a) a volatile read sees the last volatile write to that variable in the synchronization order (or a later one) — never an older one; (b) a volatile write synchronizes-with later reads, so ordinary writes made *before* it become visible to a thread that later *reads that same volatile and sees it*; (c) volatile `long`/`double` accesses are atomic (§17.7). Limits: it gives no mutual exclusion and no atomicity across a read-then-write pair; it orders only relative to the matching volatile read/write.

**Monitors / `synchronized` (§17.1, §17.4.4).** One thread at a time holds a monitor. Unlock-then-lock on the **same** object creates the edge. A `synchronized` method locks `this` (or the `Class` object if static). Using different lock objects for reader and writer creates **no** edge.

**Safe publication (convention, not a JLS term — register C13).** Making an object/data visible to another thread through a pattern that establishes happens-before: a volatile field, a lock, `Thread.start`/`join`, a `final`-field-safe constructor, or a `java.util.concurrent` handoff. Merely storing a reference into a plain field is **not** safe publication.

**Why `volatile count++` is not atomic.** `count++` is volatile read → add 1 → volatile write (register C14, from Module 01's reference; not re-verified). Two threads can both read 5 and both write 6. The program has no data race (all accesses are volatile) yet loses an update; §17.4.3 says freedom from data races "still allows errors arising from groups of operations that need to be perceived atomically".

**Transitivity example used in the code.** `data = 42` →(po) `ready = true` →(sw, if the reader's volatile read returns true) `read ready` →(po) `read data` gives hb(write data, read data); the default-value write of `data` happens-before the payload write, so reading 0 is not allowed (`docs/diagrams/m08-happens-before-safe-publication.svg`).

## D. Complete Java examples (package `memorymodel`, `src/main/java/memorymodel/`)

All use only the Java 17 language level and the existing project; **no `pom.xml` change** is required. The existing `fundamentals.Counter` and `fundamentals.Workload` (Module 01) are reused, not modified.

| File | Role | Run |
|---|---|---|
| `SharedBox.java` | Interface: `write(int)`, `tryRead(int maxSpins)`, `name()`, constants `NOT_SEEN = -1`, `PAYLOAD = 42` | — |
| `PlainBox.java` | **Unsafe** publication (plain `data`, plain `ready`) | via `PublicationExperiment` |
| `VolatileFlagBox.java` | **Safe** publication: plain `data`, `volatile boolean ready` | via `PublicationExperiment` |
| `SynchronizedBox.java` | **Safe** publication: both methods `synchronized` on `this` | via `PublicationExperiment` |
| `PublicationExperiment.java` | Runs many one-shot trials per box; prints ok/stale/notSeen | `java -cp target/classes memorymodel.PublicationExperiment [trials] [maxSpins]` |
| `VolatileCounter.java` | `volatile int` counter whose `increment()` is `count++` (implements `fundamentals.Counter`) | — |
| `VolatileCounterExperiment.java` | Part 1 deterministic barrier-forced lost update; Part 2 statistical run next to `SynchronizedCounter`/`AtomicCounter` | `java -cp target/classes memorymodel.VolatileCounterExperiment [threads] [iterations] [repetitions]` |

### D.1 Example 1 — unsafe publication (`PlainBox`)

- **Missing edge:** nothing orders the writer's `data = v; ready = true;` before the reader's `ready`/`data` reads. They are conflicting accesses without happens-before = data race (§17.4.5).
- **Permitted (not required) results:** reader never sees `ready == true` (cf. §17.3 broken loop); reader sees `ready == true` and `data == 0`.
- **Harness design:** the reader thread is started *before* the write so that `Thread.start` does not order the write before the reader's reads; the main thread reads the reader's result only after `join()`.
- **What output can show:** counts of OK/STALE/NOT_SEEN on this machine and JVM.
- **What it cannot show:** that `PlainBox` is safe if STALE is 0 (a race may simply not manifest — on x86-64 the hardware does not reorder store-store, so a stale payload may be rare or absent even though Java permits it); that a STALE result came from the CPU rather than the JIT. The code never promises failure.

### D.2 Example 2 — safe publication (`VolatileFlagBox`, `SynchronizedBox`)

- **`VolatileFlagBox` edge:** write `ready` (volatile) —sw→ read `ready` that returns true; with program order on both sides and transitivity, hb(write data, read data). Correctness conditions: the writer must write `data` *before* the volatile write; the reader must read `data` *after* the volatile read that returned true; both must use the same field.
- **`SynchronizedBox` edge:** writer's `unlock(this)` —sw→ reader's later `lock(this)`. Correctness condition: **every** access to the shared fields uses the same monitor. A reader that locks first simply sees `ready == false` (NOT_SEEN is not a violation).
- **Correctness condition checked by the program:** STALE must be 0 for both boxes; NOT_SEEN is *not* a violation (the JLS gives no time bound for visibility; the reader may start early). Exit code 1 if either safe box shows STALE.

### D.3 Example 3 — volatile does not make `count++` atomic

- **Part 1 (deterministic):** two threads each do `seen = counter.get(); barrier.await(); counter.set(seen + 1);` on a `VolatileCounter` starting at 0. Both read 0 before either writes; the result is **1**, not 2, by construction. This models the read/modify/write decomposition; it does not observe real instructions.
- **Part 2 (statistical):** `fundamentals.Workload.run` on `VolatileCounter`, `SynchronizedCounter`, `AtomicCounter`. `SynchronizedCounter` and `AtomicCounter` are exact by their own guarantees (Module 01); `VolatileCounter` *may* lose updates. `incorrect=0` for `VolatileCounter` proves nothing.
- **Correct alternatives:** `synchronized` (mutual exclusion + hb edge) or `AtomicInteger.incrementAndGet` (single atomic read-modify-write; internals are Module 09's). Module 08 goes no further than this contrast.

## E. Store-buffering litmus test

**Initial values:** `x = 0`, `y = 0` (shared); `r1`, `r2` are thread-local results.

| Thread 1 | Thread 2 |
|---|---|
| `x = 1;` | `y = 1;` |
| `r1 = y;` | `r2 = x;` |

Observed variables: `(r1, r2)`. Candidate outcomes: `(0,1)`, `(1,0)`, `(1,1)` and `(0,0)`.

### E.1 Plain (non-volatile) `x`, `y` — the exact code of Module 07's `ordering.PlainStoreBuffering`

**`(0,0)` is PERMITTED by the JMM.** Reasoning from the JLS:
1. The two threads are not connected by any synchronizes-with edge (no volatile, lock, start/join between the four actions), so `x = 1` does not happen-before `r2 = x`, and `y = 1` does not happen-before `r1 = y`.
2. By the §17.4.5 definition, a read may see the default write if no hb-related intervening write exists; here the intervening write (the other thread's store) is not hb-ordered before the read. So `r1 = y` may read 0 and `r2 = x` may read 0.
3. The JLS gives this exact shape as Example 17.4.5-1 / Table 17.4.5-A (`B = 1; r2 = A;` ‖ `A = 2; r1 = B;`) and states the trace "can observe r2 == 0 and r1 == 0 and still be happens-before consistent". Our code differs only in the constants written (1 and 1 instead of 1 and 2), which do not change the argument.
4. Causality (§17.4.8): both writes are unconditional (they do not depend on the reads), so there is no out-of-thin-air circularity; this is the assistant's reading of §17.4.8, flagged as **inferred**, supported by the JLS example's own conclusion.
**Status of the question "is `(0,0)` allowed?" — resolved: allowed.** **Status of "will it be observed?" — unknown; measurement needed (Module 07's JCStress, NOT RUN).**

### E.2 Both `x` and `y` volatile — the exact code of `ordering.VolatileStoreBuffering`

**`(0,0)` is FORBIDDEN by the JMM.** Proof sketch from §17.4.4 and §17.4.7 item 5: all four actions are synchronization actions, so they are totally ordered by `so`, consistent with each thread's program order. Suppose `r1 = 0` and `r2 = 0`.
- `r1 = y` read the default value of `y`. Synchronization-order consistency forbids a read from seeing a write that is overwritten by an intervening write before it in `so`; hence the write `y = 1` must **not** be before `r1 = y` in `so`: `so(r1 = y, y = 1)`.
- Likewise `so(r2 = x, x = 1)`.
- Program order inside each thread, mirrored in `so`: `so(x = 1, r1 = y)` and `so(y = 1, r2 = x)`.
- Chain: `x = 1 → r1 = y → y = 1 → r2 = x → x = 1` is a cycle in a total order — impossible. Hence `(0,0)` cannot occur.
The other three outcomes are permitted.

### E.3 Same shape inside one shared monitor (`jmm.SynchronizedStoreBuffering`, new in Module 08)

Both threads run `synchronized (lock) { ...; }`. The critical sections are mutually exclusive, so one runs wholly before the other: outcomes `(0,1)` and `(1,0)` only; `(0,0)` and `(1,1)` are FORBIDDEN (unlock −sw→ later lock gives hb; the second block must see the first block's store).

### E.4 JMM-permitted vs hardware-permitted

| Statement | Layer | Source / status |
|---|---|---|
| Plain Java `(0,0)` permitted | JMM | JLS Example 17.4.5-1 — VERIFIED (text read) |
| Volatile Java `(0,0)` forbidden | JMM | JLS §17.4.4/§17.4.7 argument above — VERIFIED (text read) |
| The corresponding *machine-level* pattern `mov [x],1 ; mov r1,[y]` ‖ `mov [y],1 ; mov r2,[x]` may give `(0,0)` on Intel 64 | CPU architecture | Intel SDM Vol. 3A §8.2.3.4 — PARTIAL (older edition/mirror) |
| A JVM running plain Java code on x86-64 *may* expose `(0,0)` because the hardware permits it **or** because the JIT reordered — the Java outcome alone cannot tell which | Cross-layer caution | Module 07 §H agrees |
| A JVM running volatile code on x86-64 **must** emit whatever prevents `(0,0)` there; which instruction it uses is an implementation choice | JVM | NOT established (C22) |
| A JVM on a weaker CPU (e.g. AArch64) must provide the same *Java* guarantee with different instructions | JVM/CPU | Not examined; no source gathered |

Hardware permission is neither necessary nor sufficient for Java permission: the JMM can permit outcomes a given CPU never produces (the JIT could still produce them), and a CPU permitting an outcome says nothing about whether *Java* permits it (the JVM must add barriers to forbid it).

## F. Physical memory ordering — Intel 64 only (the team lead's question)

**Question.** What ordering constraints do fences / memory barriers impose in relation to store buffers and invalidation/coherence mechanisms?

**Architecture chosen:** Intel 64 (x86-64) as described in the Intel SDM, because the team's measured machine is an Intel Core i7-13700HX (`results/raw/environment.md`) and Intel's manual is the authoritative source. Which exact memory-ordering text applies to the hybrid P/E cores of that SKU was **not checked**; this report claims only what the Intel 64 architecture documentation states generally (status PARTIAL, register C15–C19).

**What the cited manual material supports (PARTIAL — older edition/mirror, current v093 not read):**
1. Intel 64 allows a load to be reordered with an *earlier store to a different location*; it does **not** reorder a load with an earlier store to the *same* location; it does not reorder store-store, load-load, or a store with an earlier load (C15).
2. The manual explains the store→load reordering in practice as store-buffer forwarding: a store waiting in a processor's store buffer can satisfy that processor's own loads but is not visible to loads from other processors (C16).
3. Locking instructions/LOCK prefix and serializing instructions force stronger ordering; `SFENCE`/`LFENCE`/`MFENCE` give ordering for specific operation kinds (C17).
4. `MFENCE` guarantees that every load and store before it in program order becomes "globally visible" before any load or store after it (C18).

**Applied to the litmus test (inference from 1–4, not a quotation):** on Intel 64 the only reordering that matters for store buffering is store→load. Placing a full fence (or a locked instruction) between `store x` and `load y` on each processor forbids the `(0,0)` result at the architectural level. A Java volatile write followed by a volatile read of a different variable requires exactly that store→load ordering, which is why a JVM targeting x86-64 must emit something providing it for `VolatileStoreBuffering`. *Which* instruction is unverified (C22).

**Store buffers vs fences — what is and is not supported:**

| Statement | Supported by | Status |
|---|---|---|
| A store can sit in a store buffer; the processor's own later loads can read it (forwarding); other processors cannot yet see it | C16 | PARTIAL |
| A full fence orders earlier loads/stores before later loads/stores with respect to observation by other processors | C18 ("globally visible") | PARTIAL |
| "A fence drains the store buffer" | Common teaching shorthand; **not** a quotation from the manual material read here | **ILLUSTRATIVE / UNVERIFIED as a statement about hardware internals** |
| Fences generate invalidation traffic / flush invalidation queues | **No source read** | **UNRESOLVED (C21)** |
| Coherence (MESI-family) keeps cached copies of a line consistent; ordering is a separate property | Module 04/05 documents (cross-reference; Intel Vol. 3A §11.4 cited there) | Cross-reference only |

**Invalidation queues:** the term is used in popular explanations of weakly ordered CPUs. **No authoritative, specific source supporting an invalidation-queue claim for Intel 64 was found or read, so no such claim is made.** Unresolved question for the team: does Intel 64 documentation use or require the concept at all? Evidence needed: the exact SDM section, or Intel optimization-manual text, that names it.

**Coherence vs ordering (kept deliberately short; Modules 04/05 own coherence):** coherence concerns a *single location's* history being seen consistently; memory ordering concerns the relative observed order of operations on *different* locations. A store buffer sits before the coherence point: a store can be architecturally "not yet visible" even though the coherence protocol is working correctly.

**Diagram:** `docs/diagrams/m08-x86-store-buffer-ordering.svg`, with a text version here. **ILLUSTRATIVE ONLY — a simplified teaching model; not a trace; the buffer and drain steps are not directly observed.**

```text
 ILLUSTRATIVE (not observed)                     Intel 64, plain code, (r1, r2) = (0, 0)

  Logical proc 0                  Logical proc 1
  1. store x = 1  ─┐              1. store y = 1  ─┐
     [store buffer: x=1]             [store buffer: y=1]
  2. load  r1 = y  │ may run          2. load r2 = x │ may run
     reads memory → 0 │ before the       reads memory → 0 │ before the
                      ▼ store drains                      ▼ store drains
            coherent cache / memory:  x == 0, y == 0 at that moment

  With a full fence (or a locked instruction) between 1 and 2 on each processor,
  load 2 cannot be observed before store 1 (SDM: MFENCE / locking instructions) → (0,0) removed.
```

**Java is a language-level contract.** `volatile`, `synchronized`, `Thread.start/join` define *what may be observed*. They do not name machine instructions. The instruction sequence for a volatile store depends on the JVM build, JIT tier (interpreter/C1/C2), target architecture, and surrounding code (a JIT may elide or merge barriers where the JMM permits). No universal sequence is claimed here. Java code cannot read store-buffer or invalidation-queue contents; the only Java-visible effect is the outcome set.

**Assumptions and scope limits:** Intel 64 only; no claim about AMD, Arm or others; SDM sections PARTIAL; fence behaviour on the specific i7-13700HX not separately checked; no hardware counters used.

## G. Tests, experiments and evidence

### G.1 Files

| Path | Purpose |
|---|---|
| `src/test/java/memorymodel/PublicationTest.java` | JUnit 5: single-thread and not-yet-written cases; safe boxes never stale (repeated); plain box accounted for (does not assert failure) |
| `src/test/java/memorymodel/VolatileCounterTest.java` | JUnit 5: exact single-thread; deterministic lost update = 1; contended volatile within sane range; synchronized/atomic exact |
| `experiments/jcstress-jmm-module08/pom.xml` and `src/main/java/jmm/*.java` | Isolated JCStress 0.16 project (same pattern as Module 07): `PlainMessagePassing`, `VolatileFlagMessagePassing`, `SynchronizedMessagePassing`, `SynchronizedStoreBuffering`, `VolatileIncrementLostUpdate` |

Plain and volatile *store-buffering* JCStress classes already exist in `experiments/jcstress-store-buffering` (Module 07) and are not duplicated.

### G.2 Commands (team machine; Windows PowerShell shown, bash equivalent identical apart from `\` vs `/`)

```text
cd hardware-concurrency-lab
mvn clean test
mvn -q compile
java -cp target\classes memorymodel.PublicationExperiment 20000 2000000
java -cp target\classes memorymodel.VolatileCounterExperiment 8 200000 10

cd experiments\jcstress-jmm-module08
mvn clean package
java -jar target\module08-jcstress.jar -m quick     # smoke test only
java -jar target\module08-jcstress.jar              # evidence run; keep the full console output + results folder
```

Needs: JDK 17 or newer, Maven, internet for first dependency resolution. Save raw output under `results/raw/` using the `m08_` prefix; fill `results/raw/m08_environment-template.md` and `results/raw/m08_outcomes-template.csv`.

### G.3 Experiment design

| Item | `PublicationExperiment` | `VolatileCounterExperiment` Part 2 | JCStress project |
|---|---|---|---|
| Hypothesis | H1: safe boxes never show STALE (JLS). H2: plain box *may* show STALE or NOT_SEEN (not required). | H3: volatile counter *may* lose updates; synchronized/atomic never do. | JMM-allowed/forbidden outcome tables in each class |
| Independent variable | publication mechanism (plain / volatile flag / monitor) | counter implementation | test class |
| Dependent variable | counts of OK / STALE / NOT_SEEN | incorrect runs, worst loss | outcome counts per tuple |
| Controls | same trials, same `maxSpins`, same JVM process, fresh box per trial | same threads/iterations, fresh counter per run | same JCStress version and mode |
| Raw data | console text saved to `results/raw/m08_*` | same | JCStress console + results HTML |
| Analysis | check forbidden counts == 0; report permitted counts as observations | same | compare against `@Outcome` classifications |
| Limitations | JIT warm-up effects; same process; NOT_SEEN depends on scheduling; counts are not probabilities | runs inside one JVM are not independent (same caveat as Module 01) | finite exploration; "not observed" ≠ "impossible" |

### G.4 Evidence table (honest status)

Statuses used exactly: **RUN AND PASSED · RUN AND FAILED · NOT RUN · NOT VERIFIED**.

| # | Hypothesis / check | Setup | Command | Expected | Actual (only if measured) | Raw evidence | Status | Limitations / unresolved |
|---|---|---|---|---|---|---|---|---|
| 1 | Main sources compile at language level 17 with no lint warnings | Authoring container, JDK 21.0.10 (JRE + `jdk.compiler`), 1 vCPU | `java -m jdk.compiler/com.sun.tools.javac.Main -source 17 -target 17 -Xlint:all,-options -d <dir> src/main/java/fundamentals/*.java src/main/java/memorymodel/*.java` | exit 0 | exit 0, no diagnostics | `results/raw/m08_environment_container.md` | RUN AND PASSED (container only) | Not Maven; `--release 17` unavailable there, so the Java 17 API surface is NOT checked |
| 2 | `mvn clean test` for the whole project | team machine | `mvn clean test` | all tests pass | — | — | NOT RUN | needs Maven (not available in the authoring container) |
| 3 | JUnit tests for Module 08 | team machine | `mvn -q test -Dtest="memorymodel.*Test"` | all pass | — | — | NOT RUN | |
| 3a | Test logic exercised through a hand-written mini-runner against stub annotations (15 test invocations) | container | (custom runner; not part of the deliverable) | no assertion failure | pass=15 fail=0 | `results/raw/m08_container_stub_runner_output.txt` | RUN AND PASSED (**not JUnit**; does not substitute for row 3) | Stubs, not the real JUnit library |
| 4 | `PublicationExperiment`: safe boxes never stale | container, 1 vCPU | `java -cp classes memorymodel.PublicationExperiment 5000 1000000` | stale = 0 for safe boxes | all three boxes: ok=5000 stale=0 notSeen=0; exit 0 | `results/raw/m08_container_publication_experiment.txt` | RUN AND PASSED (container only) | **1 vCPU**: a data race is very unlikely to manifest; PlainBox stale=0 is expected here and shows nothing about its safety. Not team-machine evidence |
| 5 | `PublicationExperiment` on the team machine | i7-13700HX, JDK 17+ | see G.2 | safe stale = 0; plain: any | — | — | NOT RUN | |
| 6 | Volatile `count++` loses an update (deterministic part) | container | `java -cp classes memorymodel.VolatileCounterExperiment 8 200000 10` | Part 1 prints 1 | Part 1: "two increments on volatile 0 → 1" | `results/raw/m08_container_volatile_counter.txt` | RUN AND PASSED (container only) | Models the decomposition of `++`; does not observe machine instructions |
| 7 | Volatile counter statistical loss | container, then team machine | same | volatile may lose; others exact | container: volatile incorrect=0/10 (sync 0/10, atomic 0/10) | same file | RUN AND PASSED (container, 1 vCPU) / team-machine run NOT RUN | `incorrect=0` for volatile is **not** evidence of safety; needs multi-core run |
| 8 | JCStress project compiles and shades | team machine | `cd experiments\jcstress-jmm-module08 && mvn clean package` | `target/module08-jcstress.jar` exists | — | — | NOT RUN | Syntax of the 5 classes was checked only against hand-written stubs (container: exit 0); real JCStress API/annotation-processor compatibility NOT VERIFIED; version 0.16 resolution NOT VERIFIED |
| 9 | JCStress outcomes match annotations (no FORBIDDEN seen) | team machine | `java -jar target\module08-jcstress.jar` | forbidden counts 0 | — | — | NOT RUN | Absence of an outcome does not prove it impossible; presence of a FORBIDDEN one would indicate a JVM/hardware issue or a mis-annotated test |
| 10 | Whether `(1,0)` in `PlainMessagePassing` / `(1)` in `VolatileIncrementLostUpdate` is ever observed | team machine | same as 9 | unknown | — | — | NOT RUN | Permitted ≠ observed; x86-64 may never show message-passing reordering |
| 11 | Plain/volatile store-buffering outcomes | team machine (Module 07's project) | see Module 07 §F | `(0,0)` allowed (plain), forbidden (volatile) | — | — | NOT RUN (Module 07's) | owned by Module 07 |
| 12 | Which machine instruction HotSpot emits for a volatile store/read on the team JDK | team machine | `-XX:+UnlockDiagnosticVMOptions -XX:+PrintAssembly` + `hsdis`, or JITWatch | to be found | — | — | NOT VERIFIED | would support, not replace, the JLS-based claims |
| 13 | Intel SDM current-edition sections (§8.2.x, §8.2.5, §11.10, MFENCE) | human reads PDF v093 | open https://cdrdv2.intel.com/v1/dl/getContent/671190 | confirm wording and page numbers | — | — | NOT VERIFIED | automated access to the PDF was refused |

## H. Reviewer preparation (concise, technically correct)

1. **What exactly creates a happens-before edge?** Program order within a thread; synchronizes-with edges (monitor unlock → later lock of the same monitor; volatile write → later read of that variable; `Thread.start` → first action of the started thread; thread end → `join`/`isAlive` detection; and a few others in §17.4.4); and transitive closure (§17.4.5). Nothing else — wall-clock order, `sleep`, or "it ran first" creates none (§17.3).
2. **Why is volatile not the same as an atomic increment?** `v++` is a volatile read, an add, and a volatile write — two separate synchronization actions. Another thread's write can fall between them. Volatile prevents data races and stale reads but does not group actions. `AtomicInteger.incrementAndGet` (or a lock) makes the read-modify-write a single indivisible step. Demonstrated deterministically by `VolatileCounterExperiment` Part 1 (result 1).
3. **Is store buffering visible directly from Java?** No. Java sees only outcomes (e.g. `(0,0)`). A `(0,0)` result is consistent with a store buffer, but also with JIT reordering; the Java outcome cannot say which. Seeing the cause needs generated-code inspection plus the architecture manual.
4. **What does a fence guarantee, and what does it not?** On Intel 64 an `MFENCE` orders earlier loads/stores before later ones as observed by other processors (C18). It does not make a sequence of operations atomic, does not guarantee timeliness, does not serialize the instruction stream, and tells you nothing about which Java construct produced it.
5. **How do JMM guarantees relate to x86-64 ordering?** The JMM is the specification; x86-64's ordering is strong (only store→load reordering is permitted, C15), so a JVM needs few barriers there, mainly to forbid store→load reordering for volatile/lock operations. But the JIT may still reorder plain accesses, so x86's strength does not make racy Java code safe. The exact instructions are a JVM choice (C22: unverified).
6. **Why does a passing test not prove the absence of a data race?** A race is a property of the *possible* executions (hb ordering), not of the ones observed. A test samples a few schedules on one JVM/CPU; a permitted bad interleaving may simply not occur (especially on 1 core, or on x86-64 for message passing). Only reasoning from hb edges (or exhaustive tools) shows its absence.
7. **What does JCStress add over a simple loop?** It runs many small two-thread executions with controlled start alignment, collects the full outcome histogram and classifies each tuple as acceptable/interesting/forbidden. A hand-written loop with latches adds extra synchronization that can mask the reordering (Module 07 §H), and has no outcome classification. JCStress still cannot prove an outcome impossible.
8. **Which claims are specified, inferred, measured or unresolved?** *Specified:* C1–C13 (JLS text read). *Inferred:* causality for the plain litmus (E.1 step 4); the application of Intel's rules to the litmus (section F); "full fence removes `(0,0)`". *Measured:* only the container smoke runs (evidence rows 1, 3a, 4, 6, 7), 1 vCPU, not the team machine. *Unresolved:* invalidation queues (C21), HotSpot's emitted instruction (C22), current SDM section/page numbers, JCStress execution.
9. **Is `(0,0)` forbidden or just unlikely for the plain version?** Permitted by the JMM (JLS Example 17.4.5-1 has the same shape); how likely depends on JIT, CPU, timing. Intel 64 also architecturally permits the corresponding machine pattern (PARTIAL).
10. **Does `synchronized` also give visibility, not just exclusion?** Yes: unlock → later lock on the *same* monitor is a synchronizes-with edge (§17.4.4). Different monitors give no edge; this is the most common misuse.
11. **Does the volatile flag publish the `data` field if the reader reads `data` first?** No. The edge only orders what the writer did before the volatile write against what the reader does after the volatile read that sees it. Reading `data` before the flag read is outside the edge.
12. **Does a fence/volatile make a write become visible "immediately"?** The JMM promises ordering/visibility *relative to* synchronization actions, not an immediate wall-clock time. In the experiment, NOT_SEEN for a safe box is not a bug.

## I. Contribution checklist — Aastha Pandey (tick only after the action really happens)

- [ ] Read JLS SE 17 §17.4.1–§17.4.7 yourself and be able to redraw `m08-happens-before-safe-publication.svg` on a whiteboard.
- [ ] Open the **current** Intel SDM PDFs (v093 per the landing page) and confirm/replace the PARTIAL rows C15–C19: record the real section numbers, page numbers and quote ≤ 25 words each.
- [ ] Decide whether §11.10 "Store Buffer" text is useful; read it; update register row C19 if you cite it.
- [ ] Run `mvn clean test` and record the real result; fix any failure honestly (do not delete tests).
- [ ] Run both experiment mains on the team machine; save output as `results/raw/m08_*`; fill the environment template.
- [ ] Build and run the JCStress project (`-m quick`, then a full run); archive the output; fill `m08_outcomes-template.csv`.
- [ ] Optionally inspect generated assembly for the volatile store on your JDK and record what you actually see (row 12).
- [ ] Confirm with Member 7 (Vanshraj) whether message-passing tests were intended in his project, and with Ashi (Member 9) that the handoff is understood.
- [ ] Rehearse KT: every line of `VolatileFlagBox`, the volatile store-buffering cycle argument, and the limits in section F.
- [ ] Update `Status:` at the top and in the README only after the above.

## J. Final self-audit

| Requirement | Status | Evidence / next action |
|---|---|---|
| Scope limited to Module 08 | Covered | No CAS implementation, no ring buffer, no false-sharing benchmark; Module 07's classes referenced, not recreated |
| Existing project inspected | Done | ZIP unpacked and read: README, pom, all `fundamentals` sources, tests, docs, templates, Module 07 JCStress project |
| Handoff from Member 7 | Present inside ZIP (section J of his report); read | His instruction file/feedback not supplied |
| Handoff from Members 4/5 | Their reports are present; their handoff packets for Module 08 were **not supplied** | not claimed as received |
| JLS-grounded explanation | Done | S1 read in full for Ch. 17; register rows C1–C13 |
| Three Java examples | Done | `memorymodel` package |
| Store-buffering litmus with JMM vs hardware distinction | Done | section E |
| Physical ordering explanation | Partially | Intel 64 chosen; SDM rows PARTIAL; invalidation queues UNRESOLVED; no HotSpot instruction claim |
| Source register with honest verification | Done | `docs/module08-references.md` |
| Tests | Written; not run in real JUnit | evidence rows 2, 3 NOT RUN |
| JCStress | Written; not compiled against the real library | rows 8–10 NOT RUN |
| Real results only | Compliant | Only container smoke runs recorded and labelled as such |
| No fabricated contributions | Compliant | checklist unticked |
| Head of EPAM India feedback | **Not supplied** | compliance cannot be claimed; compare against the actual feedback before sign-off |
| Module COMPLETE? | **No** | Pending human verification items in section I |
