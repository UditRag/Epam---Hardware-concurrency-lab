# HANDOFF — Module 08 (Memory Barriers and the JMM) → Module 09 (CAS and Atomic Operations)

**To:** Ashi (Member 9) · **From:** Module 08 (author: Aastha Pandey, Member 8; text drafted by an AI assistant and unverified by a human) · **Project:** Team LNCT Qubit — EPAM CoE: Hardware Concurrency, Memory Models & Lock-Free Design · **Date prepared:** 2026-10-10

---

## 1. Shared project decisions

- **Java / JDK:** language level 17 (`maven.compiler.release` = 17 in `pom.xml`). Actual runtime used on the team machine in Module 01: Oracle build `27+35-2325` (`results/raw/environment.md`). Always record `java -version` in full for any run.
- **Build:** Maven. Main project `hardware-concurrency-lab` (`com.epam.coe:hardware-concurrency-lab:1.0-SNAPSHOT`). Dependencies: JUnit Jupiter 5.11.4 (test), JMH 1.37 (compile scope, annotation processor path configured). **Module 08 made no change to `pom.xml`.**
- **JCStress:** kept in isolated subprojects so the main POM is untouched: Module 07 `experiments/jcstress-store-buffering` and Module 08 `experiments/jcstress-jmm-module08`, both JCStress 0.16, shaded jar, `mainClass org.openjdk.jcstress.Main`. Version 0.16 comes from Module 07; its availability was not checked.
- **Folder/package conventions:** main code `src/main/java/<package>`, tests mirror the package under `src/test/java/<package>`. Packages in use: `fundamentals`, `synchronization`, `cache`; Module 08 added `memorymodel` (main + test); JCStress classes live in `ordering` (Module 07) and `jmm` (Module 08). Docs in `docs/`, diagrams in `docs/diagrams/`, raw results in `results/raw/` with a module prefix (`m08_`).
- **Existing API reused (not modified):** `fundamentals.Counter { void increment(); int get(); }`, `fundamentals.Workload.run(Counter, int threads, int iterations)` (starts threads together via `CountDownLatch`, `join`s, then reads `get()`), `UnsafeCounter`, `SynchronizedCounter`, `AtomicCounter`.
- **Evidence rule:** statuses are exactly `RUN AND PASSED`, `RUN AND FAILED`, `NOT RUN`, `NOT VERIFIED`. A benchmark/timing/test outcome is never described as proof of a hardware mechanism.

## 2. Files added/changed

| Path | Purpose | Depends on |
|---|---|---|
| `src/main/java/memorymodel/SharedBox.java` | interface for one-shot publication (`write`, `tryRead`, `name`; constants `NOT_SEEN=-1`, `PAYLOAD=42`) | — |
| `src/main/java/memorymodel/PlainBox.java` | intentionally unsafe publication | `SharedBox` |
| `src/main/java/memorymodel/VolatileFlagBox.java` | safe publication via `volatile boolean` | `SharedBox` |
| `src/main/java/memorymodel/SynchronizedBox.java` | safe publication via monitor | `SharedBox` |
| `src/main/java/memorymodel/PublicationExperiment.java` | trial harness + `main`; nested `Tally` | the three boxes |
| `src/main/java/memorymodel/VolatileCounter.java` | `volatile int` + `count++`; implements `fundamentals.Counter`; extra `set(int)` | `fundamentals.Counter` |
| `src/main/java/memorymodel/VolatileCounterExperiment.java` | deterministic lost-update demo + statistical comparison; `main` | `VolatileCounter`, `fundamentals.*` |
| `src/test/java/memorymodel/PublicationTest.java`, `VolatileCounterTest.java` | JUnit 5 tests | JUnit 5.11.4 |
| `experiments/jcstress-jmm-module08/pom.xml` + `src/main/java/jmm/{PlainMessagePassing,VolatileFlagMessagePassing,SynchronizedMessagePassing,SynchronizedStoreBuffering,VolatileIncrementLostUpdate}.java` | JCStress tests | JCStress 0.16 |
| `docs/module08-memory-barriers-jmm.md` | main report | — |
| `docs/module08-references.md` | claim-to-source register with verification status | — |
| `docs/diagrams/m08-happens-before-safe-publication.svg`, `m08-x86-store-buffer-ordering.svg` | diagrams (the second is ILLUSTRATIVE) | — |
| `results/raw/m08_environment-template.md`, `m08_outcomes-template.csv` | templates for the team machine | — |
| `results/raw/m08_environment_container.md`, `m08_container_publication_experiment.txt`, `m08_container_volatile_counter.txt`, `m08_container_stub_runner_output.txt` | smoke runs from the AI authoring container (1 vCPU, JDK 21) — NOT team evidence | — |
| `docs/HANDOFF_MEMBER08_TO_MEMBER09.md` | this file | — |
| `README.md` | Module 08 section **appended** at the end; nothing existing edited | — |

No existing source, test or doc was modified apart from the README append.

## 3. Technical content established

**Do not repeat these in Module 09; cross-reference `docs/module08-memory-barriers-jmm.md`.**

- Definitions: program order, synchronization order, synchronizes-with, happens-before, transitivity, data race, visibility, volatile, monitors, "safe publication" (a convention, not a JLS term). Source: JLS SE 17 §17.1, §17.3, §17.4.1–§17.4.7, §17.7 — https://docs.oracle.com/javase/specs/jls/se17/html/jls-17.html — **text read** (register rows C1–C13).
- Why `volatile int` + `count++` loses updates (read, add, write are separate actions; §17.4.3's note that race-freedom does not give atomicity). Deterministic demonstration: `VolatileCounterExperiment.deterministicLostUpdate()` returns 1.
- Store-buffering litmus: plain `(0,0)` permitted (JLS Example 17.4.5-1, Table 17.4.5-A); volatile `(0,0)` forbidden (cycle argument from §17.4.4/§17.4.7 item 5); same shape under one monitor: `(0,0)` and `(1,1)` forbidden.
- Layer separation: JMM guarantee vs JVM/JIT choice vs CPU architecture guarantee vs microarchitectural explanation vs measured observation.
- Intel 64: load may pass an earlier store to a different address; store-buffer forwarding explanation; locking instructions/MFENCE strengthen ordering — **PARTIAL** (older edition / mirror; current SDM v093 PDF not opened). URL of the landing page: https://www.intel.com/content/www/us/en/developer/articles/technical/intel-sdm.html (verified; lists the PDFs).
- JCStress: https://openjdk.org/projects/code-tools/jcstress/ (verified text: a harness for researching concurrency correctness in the JVM, class libraries and hardware).

**Boundary with your module:** Module 08 uses `AtomicCounter` / `synchronized` only as a contrast. It does **not** explain CAS, `compareAndSet`, `VarHandle`, ABA, the `lock cmpxchg` instruction, retry loops, or `AtomicInteger` internals — those are yours. It also makes no claim about which memory-ordering modes (`getAcquire`, `setRelease`, opaque, plain) the atomic classes provide.

## 4. Verification status for every file/test

Environment for rows marked "container": AI authoring sandbox, Linux x86_64, Intel Xeon @ 2.10 GHz virtual machine, **1 logical CPU**, OpenJDK 21.0.10 (JRE with `jdk.compiler`), no Maven/JUnit/JCStress jars, no network (`results/raw/m08_environment_container.md`).

| File / test | Status | Exact command | Actual output / evidence | Environment |
|---|---|---|---|---|
| All `src/main/java/fundamentals/*.java` + `memorymodel/*.java` compile | RUN AND PASSED | `java -m jdk.compiler/com.sun.tools.javac.Main -source 17 -target 17 -Xlint:all,-options -d <dir> src/main/java/fundamentals/*.java src/main/java/memorymodel/*.java` | exit 0, no diagnostics | container |
| Same sources under Maven (`--release 17`) | NOT RUN | `mvn -q compile` | — | — |
| `PublicationTest`, `VolatileCounterTest` under real JUnit | NOT RUN | `mvn -q test -Dtest="memorymodel.*Test"` | — | — |
| Same test classes compile against hand-written stub annotations; 15 invocations run by a mini-runner | RUN AND PASSED (stub only; **not JUnit**) | custom runner | `pass=15 fail=0` in `results/raw/m08_container_stub_runner_output.txt` | container |
| `PublicationExperiment` | RUN AND PASSED (container only) | `java -cp classes memorymodel.PublicationExperiment 5000 1000000` | all three boxes `ok=5000 stale=0 notSeen=0`; exit 0 (`m08_container_publication_experiment.txt`) | container, 1 vCPU |
| `VolatileCounterExperiment` | RUN AND PASSED (container only) | `java -cp classes memorymodel.VolatileCounterExperiment 8 200000 10` | Part 1 → 1; Part 2 all 0/10 incorrect (`m08_container_volatile_counter.txt`) | container, 1 vCPU |
| JCStress classes (5) | NOT RUN (real build); syntax-only check against stubs: exit 0 | `cd experiments/jcstress-jmm-module08 && mvn clean package` | — | — |
| JCStress execution | NOT RUN | `java -jar target/module08-jcstress.jar [-m quick]` | — | — |
| Module 07's JCStress project | NOT RUN (by Module 07; not re-run here) | see his §F | — | — |
| SVG diagrams | RUN AND PASSED (well-formed XML only; not visually reviewed in a browser) | Python `xml.etree.ElementTree.parse` | no exception | container |

## 5. Experimental status

| Experiment | Hypothesis | Variables / controls | Raw data | Actual results | Pending | Limitations |
|---|---|---|---|---|---|---|
| Publication (`PublicationExperiment`) | Safe boxes never show a stale payload; plain box may | IV: publication mechanism. DV: ok/stale/notSeen counts. Controls: same trials/`maxSpins`, fresh box per trial | `results/raw/m08_container_publication_experiment.txt` | container: stale = 0 for all (1 vCPU) | team-machine run; multiple launches | single JVM process; 1 vCPU run says almost nothing about the plain box |
| Volatile counter (`VolatileCounterExperiment`) | Volatile `++` can lose updates; sync/atomic cannot | IV: counter type. DV: incorrect runs, worst loss | `results/raw/m08_container_volatile_counter.txt` | Part 1 deterministic = 1; Part 2 0/10 incorrect for all (1 vCPU) | team-machine multi-core run | runs in one JVM not independent; `incorrect=0` is not evidence of safety |
| JCStress message passing / SB-under-lock / volatile increment | outcome classes in each `@Outcome` | IV: test class; DV: outcome histogram | `results/raw/m08_outcomes-template.csv` (empty template) | none | everything | finite exploration; "not observed" ≠ "impossible" |
| Generated-code inspection of volatile store on the team JDK | informs which instruction implements the barrier | — | none | none | optional | not required for the JLS claims |

## 6. Known issues

**Open questions**
1. Exact current-edition SDM section/page numbers for §8.2.2, §8.2.3.4, §8.2.5, §11.10 and the MFENCE page — the PDFs could not be opened by the authoring tool.
2. Invalidation queues: no source read; **no claim made**; unresolved.
3. Which instruction HotSpot emits for volatile store/load on the team's x86-64 JDK: unverified.

**Contradictions / assumptions**
- Module 07's report says store buffering **and message passing** tests were prepared, but the ZIP's `experiments/jcstress-store-buffering` has only the two store-buffering classes. Module 08 added message-passing tests in its own project; Member 7 should confirm no duplicate intent.
- Module 07 §D cites §17.4.5 for "synchronization-order rules"; per the text read, those are §17.4.4 and §17.4.7 item 5.
- Module 03's report names "Prachi, Vanshraj" as owners of fences/barriers; the current assignment is Module 07 (Vanshraj) and Module 08 (Aastha).
- C14 (`x++` as read/add/store, JLS §15.14.2) is taken from Module 01's register; Chapter 15 was not fetched here.
- Member 5 (Vansh) / Member 4 handoff packets addressed to Module 08 were not supplied; their reports exist in `docs/` and were only cross-referenced.

**Integration risks**
- Two JCStress subprojects each produce a shaded jar; run them from their own directories.
- The container compile used `-source/-target 17`, not `--release 17`; the Java 17 API surface is unchecked until `mvn compile` runs.
- `PublicationExperiment` timings depend on thread scheduling; `maxSpins` too low can inflate `notSeen` (not a JMM violation).
- Hybrid CPU (P/E cores) on the team machine may vary results between runs.

**Required corrections**
- Human must run the NOT RUN items and replace the container smoke runs with team-machine evidence before any "tested" claim.

## 7. Next owner's exact scope (Ashi, Module 09)

**Add (your module):**
- CAS and atomic operations: what compare-and-set is, its retry-loop pattern, `java.util.concurrent.atomic` classes, the memory effects the Java API documents for them, and how they relate to the JMM edges defined in Module 08 (cite the Javadoc/`VarHandle` documentation and JLS; do not assume).
- A hardware-level explanation of atomic read-modify-write on the chosen architecture (Intel `LOCK`-prefixed instructions / `CMPXCHG`) with SDM sections you actually open and verify (note: Module 08 cites §8.2.5 only for "locking instructions strengthen ordering").
- A CAS-based experiment extending the existing contrast: you can add an `AtomicCounter`/CAS-loop variant next to `fundamentals.AtomicCounter` and run it through `fundamentals.Workload`; plain JMH benchmarks belong to Module 12 coordination.
- ABA problem and its mitigations only if your assignment includes it.

**Interfaces to preserve:** `fundamentals.Counter`, `fundamentals.Workload.run`, `memorymodel.SharedBox` (if you reuse it), package names `fundamentals`/`memorymodel`/`jmm`, the `m09_` prefix for raw results, the four status words.

**Out of scope for you:** re-explaining the JMM, happens-before, volatile, safe publication, or the store-buffering litmus test (cross-reference Module 08/07); lock-free/wait-free algorithms (Module 10, Vivansh); ring buffer/Disruptor (Module 11, Kabir); false sharing (Module 06); coherence protocol detail (Modules 04/05).

**Inputs you need from humans:** the actual team JDK `java -version`; confirmation of Maven build status after the Module 08 files are merged; real outputs of Module 07/08 runs if they exist; Head of EPAM India feedback if any applies to Module 09.

## 8. Claims requiring human verification

1. Every PARTIAL row (C15–C19) in `docs/module08-references.md` against the current Intel SDM PDFs.
2. That the JLS section numbers cited are correct for the edition the team cites (the text was read from the Java SE 17 page; Module 04's register uses the SE 21 chapter).
3. That the JCStress sources compile and run with the JCStress version in the POM (0.16 resolvability is inherited from Module 07 and unchecked).
4. That the plain `PlainMessagePassing` `(1,0)` tuple and `VolatileIncrementLostUpdate` outcome `1` are classified as intended by JCStress `Expect` values in the chosen version.
5. The inference in the report that a full fence between the store and the load on each processor removes the Intel 64 `(0,0)` outcome (derived from rules; not quoted).
6. That all container-only results are replaced or clearly labelled when reported.
7. That no result in this repository is presented as team-machine evidence unless a human ran it there.

---

**WARNING:** This file and everything it describes were generated by an AI assistant. Generated content is **not proof of completion**. A human must verify the sources against the real documents, compile and run the code, collect real evidence on the team machine, and review the module with the team before it is described as complete.
