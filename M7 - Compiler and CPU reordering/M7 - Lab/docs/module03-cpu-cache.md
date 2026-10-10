# Module 03: CPU Cache Architecture

Ujjwal Pandey | EPAM CoE Phase 2: Hardware Concurrency, Memory Models & Lock-Free Design | Team LNCT Qubit | 2026-10-09

Evidence labels (project convention): TEAM-MEASURED, EXTERNAL EVIDENCE, THEORETICAL, STATICALLY REVIEWED, PENDING LOCAL EXECUTION. Added here: VERIFIED-IN-SANDBOX, NOT RUN, UNVERIFIED.

**Status in one line:** code, tests, scripts and design are written; previous notes report a plain-Java logic check in a separate Linux sandbox, but this review environment has JDK 21 and no Maven executable. The Maven build, real JUnit suite and JMH benchmark remain NOT RUN here; no Module 03 measurement files are present. Every Module 03 result cell remains PENDING until the actual project is run on the team machine.

## 1. Input audit

**Received:** Udit's PDF and repo zip, Adrika's DOCX and repo zip, the combined handoff template (blank), the master instructions and the Task 3 text. Both zips were opened and checked against the documents.

**Confirmed in the files:**
- Maven project com.epam.coe:hardware-concurrency-lab:1.0-SNAPSHOT, release 17, JUnit Jupiter 5.11.4, surefire 3.5.2. **No JMH dependency existed.**
- Module 01 API: Counter, UnsafeCounter, SynchronizedCounter, AtomicCounter, Workload.run(Counter,int,int), RaceExperiment. Surefire report: 11 tests, 0 failures. The four archived raw files contain 3 Unsafe failures per 30 runs each, matching the PDF for the archived launches. The 5 failures in the two console-only launches cannot be verified.
- Module 02 ran once per demo; thread dump shows OpenJDK 17.0.18. Module 01 reports Oracle build 27+35-2325; treat this as a build identifier and record the exact `java -version` output because the project’s JDKs differ.

**Conflicts and gaps that affect this module:**
1. No formal Project Contract. Conventions inferred: package per module, results/raw, docs/, evidence labels. Package `cache` used; `fundamentals` untouched.
2. Module 02 sources sit under src/ and src/synchronization/, not src/main/java, so Maven does not compile them. Duplicate, differing OrderedLockDemo copies and stale .class files exist. Smallest fix (Adrika's call, not made here): move one copy of each demo to src/main/java/synchronization/.
3. DeadlockDemo threads are non-daemon, so the JVM never exits after the deadlock. Not this module; flagged.
4. README pointed to results/environment.md; the file is results/raw/environment.md.
5. JDK mismatch (17 vs 27). **Record the JDK with every Module 03 result.**
6. Module 01's early-run clustering is unexplained. Warning for this module: JIT warm-up and run-to-run state are real effects, so multiple forks are used and per-fork spread is reported.
7. Test machine is hybrid (i7-13700HX: P-cores and E-cores). Cache sizes differ by core type and the OS can move a thread between them. This is the biggest threat to Experiment 2 (section 9).
8. The Head of EPAM India's feedback was not supplied, so section 11 audits only against the task text.

**Processor-family documentation (EXTERNAL EVIDENCE, not a substitute for local enumeration):** Intel lists the i7-13700HX as 8 P-cores + 8 E-cores, 24 threads and 30 MB Intel Smart Cache. Intel’s 13th Gen processor datasheet describes P-core L1 data cache as 48 KiB, E-core L1 data cache as 32 KiB, P-core L2 as 2 MiB, and a 4 MiB L2 shared by each group of four E-cores. The local machine’s exposed topology and line size still must be collected with Coreinfo/CPUID; BIOS, processor family variants and tools can affect what is reported.

## 2. Scope and design

**Owned:** memory hierarchy, cache lines, locality, working sets, access patterns, JMH experiments on them, counter/profiler options.
**Not owned:** coherence and invalidation (Kabir), MESI/MOESI states (Vivansh), false sharing and line bouncing (Vishwajeet), memory fences and barriers (Prachi, Vanshraj), locks and counters (Udit, Adrika).
**Boundary rule:** both benchmarks are single-threaded. They cannot show coherence traffic, store buffers, invalidation queues, fences or MESI transitions, and this module does not claim to.

**Approach (smallest change):** one new package `cache`, one new dependency (jmh-core 1.37), one compiler-plugin setting (explicit annotation-processor path), no shade plugin. Logic lives in a plain class (AccessPatterns) that can be unit-tested without JMH; the benchmark classes are thin.

## 3. CPU cache architecture

```
   Logical core 0          Logical core 1
 +----------------+      +----------------+
 | Registers      |      | Registers      |   architectural state, NOT a cache level
 | L1 data/instr. |      | L1 data/instr. |   usually private to a physical core
 | L2 (design-dep.)|     | L2 (design-dep.)|  private or shared within a core cluster
       |                     |
 +-----+---------------------+-----+
 |       L3 / last-level cache     |             shared by many cores (design dependent)
 +----------------+----------------+
                  |
 +----------------+----------------+
 |   Main memory (DRAM), in lines  |             large, slowest; data moves in cache LINES
 +---------------------------------+
 Cache line = fixed-size transfer/allocation unit; 64 B is an example here, VERIFY locally
  | int[] payload: [ 0..15 ][ 16..31 ][ 32..47 ] ...  = 16 ints per 64-byte line |
```

**General principles (architecture facts, hedged by CPU):**
- Typical hierarchy trend: upper cache levels are smaller and lower-latency than lower levels, while DRAM is much larger and generally higher-latency. This is a design trend, not a universal numeric law; sharing and access path affect latency.
- Cache storage and fills operate in cache-line-sized units. A demand miss often causes a line-sized fill, but exact traffic and source of the line depend on the microarchitecture and access type.
- Private versus shared levels, line size, associativity and inclusive/non-inclusive policy depend on the processor. Enumerate the tested machine rather than assuming a generic diagram is exact.
- Hardware prefetchers may fetch lines ahead for recognised patterns; they can help regular access and can also consume bandwidth/cache capacity.

**Java facts vs implementation:**
- JLS 4.2.1: int is 32 bits (EXTERNAL EVIDENCE). So a 64-byte line holds 16 ints **if** the line size is 64.
- The JLS does not specify array memory layout. Contiguous primitive arrays, a ~16-byte header and possible GC relocation are HotSpot implementation details. Spatial locality in Java arrays therefore rests on JVM behaviour, not on the language spec.

**Worked example (arithmetic assumes 64-byte lines and 4-byte ints; THEORETICAL, verify line size on the test CPU):**

| Array payload | Lines (payload / 64 B) | Example reading |
| --- | --- | --- |
| 32 KiB | 512 | payload alone equals 32 KiB; header/alignment and competing lines mean this is only a nominal fit |
| 2 MiB | 32,768 | |
| 30 MiB | 491,520 | |
| 128 MiB | 2,097,152 | exceeds any cache listed in section 1 |

- **Spatial locality, sequential read of a cold int[]:** with a 64-byte line and 4-byte ints, one aligned line contains 16 adjacent elements. After the first element brings that line into a cache, the other elements can reuse it while it remains resident. Array alignment can make a boundary line extra; prefetching may fetch additional lines early.
- **Random order over a 128 MiB array:** the permutation visits every element once but scatters adjacent elements in time. When the footprint is larger than effective cache capacity, reuse of the same line is less likely before eviction, so more line fills may be needed than for sequential access. A 16x line-traffic ratio is only an idealized upper-bound illustration, not a predicted or measured result.
- **Temporal locality:** a second pass can reuse data if the relevant lines remain in cache; “the array fits” is not enough by itself because other data and associativity can evict lines. A second pass over a much larger-than-cache working set is more likely to fetch lines again.
- **Why size matters:** capacity pressure can increase misses when reuse distance exceeds a cache’s effective capacity. A timing step may be consistent with a cache-capacity effect, but TLB reach, prefetching, placement, frequency and other factors also affect it.

## 4. Source-backed claims

The full claim-to-source table is in `docs/references.md`, section “Module 03 — CPU Cache Architecture”. The key sources were checked on 2026-10-09.

| Claim used in this module | Evidence and scope | Boundary / status |
| --- | --- | --- |
| The i7-13700HX has 16 cores (8 P + 8 E), 24 threads and 30 MB Intel Smart Cache. | Intel ARK, CPU Specifications. | Vendor specification; not a measurement of current clocks, topology exposed by the OS, or per-core cache residency. VERIFIED EXTERNALLY. |
| Intel’s 13th Gen processor datasheet describes different L1D sizes for P- and E-cores and different L2 sharing arrangements. | Intel 13th Gen Core Processor Datasheet, Vol. 1, “IA Cores Level 1 and Level 2 Caches”. | Family documentation; local CPUID/Coreinfo output must still be collected. VERIFIED EXTERNALLY; LOCAL ENUMERATION PENDING. |
| Intel CPUID leaf 04H enumerates deterministic cache parameters, including cache level/type and line-size-related fields. | Intel SDM Vol. 2, `CPUID`, “Deterministic Cache Parameters”. | This is the correct discovery mechanism; it does not itself give this machine’s captured values. VERIFIED EXTERNALLY. |
| Java `int` is 32-bit; the JLS does not promise HotSpot array-object layout. | JLS §4.2.1 and §10.1–10.2. | Supports 4 bytes per `int`; array headers, alignment and placement remain JVM implementation details. VERIFIED EXTERNALLY. |
| JMH requires care to avoid dead-code elimination and supports warm-up, measurement and fork configuration. | OpenJDK JMH official README, sample `JMHSample_09_Blackholes`, and JMH 1.37 annotation Javadocs. | The benchmark returns its result; no claim that timing isolates cache misses. VERIFIED EXTERNALLY. |
| Annotation processing must be configured explicitly for JDK 23+ javac. | Oracle JDK 23 Release Notes, “Annotation processing in javac disabled by default”. | Supports explicit JMH annotation-processor configuration. VERIFIED EXTERNALLY. |
| Hardware performance-counter events can count selected cache events. | Intel PerfMon Events documentation, selected by matching microarchitecture and core type. | Event availability/meaning must be checked for the actual processor and tool; no counters collected yet. EXTERNAL SOURCE VERIFIED; LOCAL COUNTERS PENDING. |

## 5. Source files

Paths relative to the project root `hardware-concurrency-lab`:

| Path | Role |
| --- | --- |
| pom.xml | Changes: jmh.version 1.37, jmh-core dependency, annotationProcessorPaths. Everything else unchanged |
| src/main/java/cache/AccessPatterns.java | Plain helpers: data fill, identity and Fisher-Yates permutation, Sattolo single cycle, sums, chase, size arithmetic |
| src/main/java/cache/SequentialVsRandomBenchmark.java | Experiment 1 (JMH) |
| src/main/java/cache/WorkingSetBenchmark.java | Experiment 2 (JMH) |
| src/test/java/cache/AccessPatternsTest.java | 7 JUnit sanity tests, no timing |
| scripts/collect_env.ps1, scripts/collect_env.sh | Environment collection (Windows, Linux) |
| scripts/analyze_jmh.py | JMH JSON to CSV and PNG |

**Experiment 1 design.** State: data[size], seqIdx = 0..size-1, rndIdx = random permutation (seed 42), all built in @Setup(Level.Trial). sequentialIndexed and shuffledIndexed run the identical loop sum += data[idx[i]], so both perform exactly size logical reads of data[], read idx[] sequentially, execute the same instructions, and return the same checksum. Only the visiting order of data[] differs. sequentialDirect (no idx) is a reference, not a fair partner. Each method returns its sum, so JMH consumes it and the loop cannot be eliminated. Params: size = 262144, 4194304, 33554432 ints (data+idx footprint 2, 32, 256 MiB). Config: 3 warm-up x 1 s, 5 measurement x 1 s, 3 forks, AverageTime, ns/op. ns per read = score / size.

**Experiment 2 design.** next[] has workingSetKiB x 256 ints, so payload = workingSetKiB KiB exactly, excluding the small array header/alignment. chaseRandom follows p = next[p] over one random cycle (Sattolo), so the whole array is reachable and each load address depends on the previous load. chaseSequential uses next[i] = i+1; it has the same dependent-load source pattern but is friendly to sequential prefetching. Both do HOPS = 2^18 = 262,144 dependent loads per invocation (OperationsPerInvocation), so the score is normalized to ns per hop. HOPS is deliberately bounded so even the largest random working set should produce multiple invocations in a measurement iteration; verify actual invocation counts and stability in JMH output. Sizes (KiB): 4, 8, 16, 32, 64, 128, 256, 512, 1024, 2048, 4096, 8192, 16384, 32768, 65536, 131072 (4 KiB to 128 MiB, powers of two, chosen to straddle the section 1 candidates without assuming them). Same warm-up/measurement/fork config. A plain random permutation can split into several short cycles and silently shrink the working set; the test checks the Sattolo array has one cycle of full length.

## 6. Build and run (Windows PowerShell; all NOT RUN by the module author)

```
cd hardware-concurrency-lab
# 1. environment (also install Sysinternals Coreinfo and put it on PATH for the per-core cache map)
powershell -ExecutionPolicy Bypass -File scripts/collect_env.ps1
# 2. build and unit tests (Udit's 11 tests + 7 new)
mvn -q clean test
mvn -q compile
Test-Path target/classes/META-INF/BenchmarkList     # must print True
# 3. classpath for JMH forks
mvn -q dependency:build-classpath '-Dmdep.outputFile=target/cp.txt'
$cp = Get-Content target/cp.txt
$jmh = @('-cp', ('target/classes;' + $cp), 'org.openjdk.jmh.Main')
# 4. smoke test only, not a result
java @jmh SequentialVsRandomBenchmark -t 1 -p size=262144 -f 1 -wi 1 -i 2 -w 1 -r 1
# 5. Experiment 1 (about 4 min or more), repeat as run1, run2, run3
java @jmh SequentialVsRandomBenchmark -t 1 -rf json -rff results/raw/m03_exp1_run1.json -jvmArgsAppend '-Xms2g -Xmx2g -XX:+AlwaysPreTouch' | Tee-Object results/raw/m03_exp1_run1.log
# 6. Experiment 2 (about 13 min plus JVM start), repeat as run1, run2, run3
java @jmh WorkingSetBenchmark -t 1 -rf json -rff results/raw/m03_exp2_run1.json -jvmArgsAppend '-Xms2g -Xmx2g -XX:+AlwaysPreTouch' | Tee-Object results/raw/m03_exp2_run1.log
# 7. CSV + chart (needs Python; pip install matplotlib for the PNG)
python scripts/analyze_jmh.py results/raw/m03_exp1_run1.json results/raw/m03_exp2_run1.json
```

- **Controls:** plug in AC, set the high-performance power plan, close other apps, do not touch the machine during runs, keep the same JDK for all runs, and record it. JMH writes the JMH version, JDK and JVM arguments into the JSON.
- **Optional pinned Run B (hybrid control):** child JVMs inherit the affinity mask of the process that starts them, for example `cmd /c start /b /wait /affinity 1 java ...` (hex mask; logical CPU 0). Check with Coreinfo or Task Manager which logical CPUs are P-core threads before choosing a mask; do not assume. Report pinned and unpinned runs separately.
- **Expected output format:** JMH prints a table with columns Benchmark, size or workingSetKiB, Mode (avgt), Cnt (15 = 3 forks x 5), Score, Error, Units (ns/op). The shape is documented here; no numbers are given because none exist.
- **Hardware counters:** run `java @jmh -lprof` first; it lists only what works on your machine. Linux: -prof perfnorm or perf stat with L1/LLC load-miss events. Windows: Intel VTune or Windows Performance Recorder/xperf with PMC sources (not verified); event names for hybrid CPUs differ per core type. WSL and VMs often hide PMUs. If none works: record that, and the strongest supported conclusion is the timing statement in section 9.
- **Sanity check before trusting results:** mvn -q test passes (shuffle is a permutation, checksums equal, Sattolo cycle length = N).

## 7. Experiment design

| Item | Experiment 1 | Experiment 2 |
| --- | --- | --- |
| Hypothesis (to be tested, may be refuted) | H1: shuffledIndexed needs more ns per read than sequentialIndexed, and the gap grows as footprint exceeds the cache levels | H2: chaseRandom ns per hop steps up as the working set grows; chaseSequential grows much less |
| Independent variables | visiting order; size | working-set size; order (random vs sequential cycle) |
| Dependent variable | ns per invocation, then ns per read | ns per hop |
| Controls | same reads, same code, same seed, setup outside measurement, same JVM flags | same hops for every size, same code path, same seed |
| Raw data | m03_exp1_runN.json + .log | m03_exp2_runN.json + .log |
| Analysis | analyze_jmh.py CSV: score, 99.9% CI half-width (JMH scoreError), per-fork mean min and max | same |
| Repetition | at least 3 separate runs on separate days or reboots; compare fork means | same |
| Limitations | section 9 | section 9 |

## 8. Results template (all PENDING)

**Run header (one per run):** date | JDK vendor/version | JMH version | JVM flags | forks 3, warm-up 3 x 1 s, measurement 5 x 1 s | power plan and AC/battery | affinity (none or mask) | env file.

**Experiment 1** (reads per invocation = size; unit ns/op; ns/read = score / size)

| Benchmark | size (ints) | data payload | Access pattern | Logical reads | Score ns/op | 99.9% CI | Fork-mean min/max | Interpretation and limits |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| sequentialIndexed | 262144 | 1 MiB | in order | 262144 | PENDING | PENDING | PENDING | PENDING |
| shuffledIndexed | 262144 | 1 MiB | random | 262144 | PENDING | PENDING | PENDING | PENDING |
| sequentialDirect (ref) | 262144 | 1 MiB | in order, no idx | 262144 | PENDING | PENDING | PENDING | not a fair partner |
| sequentialIndexed | 4194304 | 16 MiB | in order | 4194304 | PENDING | PENDING | PENDING | PENDING |
| shuffledIndexed | 4194304 | 16 MiB | random | 4194304 | PENDING | PENDING | PENDING | PENDING |
| sequentialDirect (ref) | 4194304 | 16 MiB | in order, no idx | 4194304 | PENDING | PENDING | PENDING | not a fair partner |
| sequentialIndexed | 33554432 | 128 MiB | in order | 33554432 | PENDING | PENDING | PENDING | PENDING |
| shuffledIndexed | 33554432 | 128 MiB | random | 33554432 | PENDING | PENDING | PENDING | PENDING |
| sequentialDirect (ref) | 33554432 | 128 MiB | in order, no idx | 33554432 | PENDING | PENDING | PENDING | not a fair partner |

**Experiment 2** (reads per invocation = 262,144 hops; unit ns/hop). One row per size; fill both columns from the CSV.

| workingSetKiB | bytes | chaseRandom ns/hop (CI, fork min/max) | chaseSequential ns/hop (CI, fork min/max) | Notes |
| --- | --- | --- | --- | --- |
| 4 | 4,096 | PENDING | PENDING | |
| 8 | 8,192 | PENDING | PENDING | |
| 16 | 16,384 | PENDING | PENDING | |
| 32 | 32,768 | PENDING | PENDING | |
| 64 | 65,536 | PENDING | PENDING | |
| 128 | 131,072 | PENDING | PENDING | |
| 256 | 262,144 | PENDING | PENDING | |
| 512 | 524,288 | PENDING | PENDING | |
| 1024 | 1,048,576 | PENDING | PENDING | |
| 2048 | 2,097,152 | PENDING | PENDING | |
| 4096 | 4,194,304 | PENDING | PENDING | |
| 8192 | 8,388,608 | PENDING | PENDING | |
| 16384 | 16,777,216 | PENDING | PENDING | |
| 32768 | 33,554,432 | PENDING | PENDING | |
| 65536 | 67,108,864 | PENDING | PENDING | |
| 131072 | 134,217,728 | PENDING | PENDING | |

## 9. Analysis guide and limitations

**Fair comparisons:** sequentialIndexed vs shuffledIndexed at the same size; chaseRandom vs chaseSequential at the same working set; one benchmark across sizes (with the caveats below); the same benchmark across your own repeated runs.
**Unfair:** sequentialDirect vs shuffledIndexed (different instruction mix, and the JIT may treat the simple loop differently); Experiment 1 ns/read vs Experiment 2 ns/hop (overlappable throughput vs dependent latency are different quantities); results from different JDKs, machines, power states, or P-core vs E-core runs; Experiment 1 sizes read as if only data[] counted (footprint includes idx[]).

**What the data can support:** that, on this machine and JVM, one access order costs more time per read than another; that ns per hop changes (or does not) between working-set sizes; and whether those effects repeat across forks and runs. State a decision rule *before* looking, for example: report an effect only if the CIs do not overlap and the direction repeats in all three runs.

**What timing alone cannot establish:** which cache level was missed, the number of misses, MESI transitions, store-buffer or fence behaviour, or that a step in the curve sits at a specific cache size. A step near a capacity is consistent with a capacity effect; it is a hypothesis until counters agree.

**Confounds you must name:**
- **TLB:** with 4 KiB pages, a 128 MiB array spans 32,768 pages. Large random working sets also miss the TLB and need page walks, so the slowdown is not purely data-cache misses.
- **Prefetching:** sequential patterns are helped by hardware prefetchers; this is part of the effect, not noise.
- **JIT and layout:** loop unrolling or vectorisation of the simple loop; array header and base alignment (payload may straddle lines); GC may move arrays; the JVM, not the spec, decides layout.
- **Hybrid CPU:** if the thread migrates between P and E cores, effective cache sizes change mid-run. Use the pinned Run B and report it separately.
- **Frequency, thermal and OS:** boost clocks, throttling, background services, antivirus, SMT sibling activity, power plan. Module 01 saw unexplained early-run clustering, so compare fork means, not just pooled iterations. Iterations inside one fork are not independent.
- **Heap flags:** -Xms/-Xmx/AlwaysPreTouch are controls, not neutral; they are recorded in the JSON.

**What hardware counters would add:** counts of L1/L2/LLC load misses and dTLB misses per operation, normalised per hop or per read. That would let you say whether the extra time coincides with misses at a given level. Still only correlation with timing, but a much stronger claim than time alone.

**Claim classes for the final report:**
- Established architectural facts: cache hierarchy principles and processor-family specifications, backed by the source table in section 4; local cache topology still pending.
- Java/JVM guarantees: int is 32-bit (JLS); layout is NOT guaranteed.
- Architecture-specific facts: Intel documents processor-family cache sizes; the actual machine’s exposed topology and cache-line size still require local Coreinfo/CPUID capture. The worked arithmetic uses a 64-byte line as an explicit example, not as a measured value.
- Measured observations: only what the JSON files contain.
- Hypotheses: H1, H2, any explanation of step positions.

## 10. Live walkthrough Q&A

1. **What is a cache line?** A fixed-size unit used by the cache hierarchy for storage/fills. 64 bytes is a common x86 example, but verify the actual CPU; a load does not imply that every access reaches DRAM.
2. **Why is sequential access often efficient?** Adjacent elements can share a cache line (16 ints per 64-byte line), and hardware prefetchers often recognize regular streams.
3. **Why can random access differ?** The next address is less predictable and adjacent elements are reused less soon; sequential prefetching is less effective, particularly when the active footprint exceeds effective cache capacity.
4. **Temporal vs spatial locality?** Temporal: the same data is used again soon. Spatial: nearby data is used soon. Sequential passes exploit spatial; repeated passes over a small array exploit temporal.
5. **Why does array size change performance?** A larger working set can increase cache and TLB pressure. A timing change alone cannot identify a cache level or prove that a capacity boundary caused it.
6. **Why does timing not prove an L1 or L2 miss?** Time is the sum of many effects (prefetch, TLB, frequency, JIT, layout). A slowdown is consistent with misses but does not count them or name the level.
7. **What would counters add?** Selected hardware events can show cache/TLB-related activity per operation. Event definitions and availability are processor-specific; they strengthen, but do not automatically prove, a causal explanation.
8. **How do lines connect to false sharing?** Coherence works per line, so two threads writing different variables in one line contend for it. Layout and ownership of lines is the foundation; the effect itself is Vishwajeet's work.
9. **Hierarchy vs coherence?** Hierarchy is about where data is stored and how fast it is reached for one core. Coherence is the protocol that keeps copies in different cores' caches consistent. These benchmarks are single-threaded and cannot show coherence.
10. **Documentation vs measurement?** Section 4 claims come from documents (with the status shown); every number in section 8 will come from your JMH JSON. Until then there are no measured claims in this report.

## 11. Completion audit

| Deliverable | Status | Evidence |
| --- | --- | --- |
| AccessPatterns.java | RUN AND PASSED (manual smoke harness) | Compiled with `javac -Xlint:all` on OpenJDK 21; checks passed for permutation, checksum equality, cycle length, chase, size arithmetic and deterministic shuffle. This is not the JUnit suite. |
| AccessPatternsTest.java | NOT RUN | JUnit was not available in this review environment; the manual helper smoke harness is not a substitute for running the real test class. |
| SequentialVsRandomBenchmark, WorkingSetBenchmark | NOT RUN | Not compiled against the real JMH dependency in this review environment; annotation processing and execution remain unverified. |
| pom.xml | NOT RUN | Maven is not installed in this review environment. JMH 1.37 artifact and Javadocs were located; dependency resolution and build remain unverified |
| collect_env.sh | PRIOR SANDBOX RUN REPORTED; NOT RE-RUN HERE | Any sandbox output is not the team machine environment. |
| collect_env.ps1, Coreinfo output format | NOT RUN | |
| analyze_jmh.py | PARTIAL | Python syntax check passed; parser has not been tested against a real JMH JSON file because no Module 03 JSON exists yet |
| Experiment results, charts | PENDING | Run section 6 |
| Hardware-counter evidence | BLOCKED until -lprof/VTune/perf availability is known | |
| Source verification | PARTIAL → UPDATED | Official Intel ARK/datasheet, SDM, JLS, JMH and Oracle release notes checked; local cache topology and hardware-counter availability remain pending |
| KT | PENDING | |

**Contribution checklist (tick only what you did):** [ ] read Udit and Adrika handoffs and confirmed section 1 findings; [ ] ran mvn -q test and recorded output; [ ] confirmed BenchmarkList exists; [ ] collected environment; [ ] ran Experiments 1 and 2 three times, archived all JSON and logs; [ ] opened every source and filled the real section numbers; [ ] can explain every line of AccessPatterns and both benchmarks; [ ] recorded counter availability; [ ] KT done.

**Self-audit against the task:** Part B explanation and diagram are present; official source claims are documented with direct links; Part C source is written but real JMH build/run remains unverified; Part D contains only PENDING results; Part E boundaries are explicit; Part F Q&A is present. **Not complete** until the Maven/JUnit/JMH runs, environment capture and real measurements are recorded. Head's feedback was not supplied, so that portion cannot be audited.

## 12. HANDOFF PACKET for Kabir (Module 04: Cache Coherence)

- **Objective and conventions:** as Modules 01 and 02 (real measurements only, evidence labels, package per module, results/raw, docs/). Module 03 uses package cache and JMH 1.37.
- **Files added/changed:** section 5. pom.xml changed (JMH dependency and annotation-processor path); fundamentals untouched.
- **Public API:** cache.AccessPatterns static methods (intsForKiB, payloadBytes, buildData, identity, shuffledPermutation, randomSingleCycle, sequentialCycle, isPermutation, cycleLengthFromZero, sumIndexed, sumDirect, chase); JMH classes cache.SequentialVsRandomBenchmark and cache.WorkingSetBenchmark (single-threaded).
- **Definitions established (do not repeat):** cache line, hierarchy, private vs shared levels, temporal/spatial locality, working set, logical read, sequential/strided/random patterns, pointer chase, TLB as a confound.
- **Benchmark design:** section 5. Fair vs unfair comparisons: section 9.
- **Tests actually run:** only the sandbox compile and harness described in section 11. No Maven, JUnit or JMH run. No results.
- **Open issues:** cache sizes of the test machine unverified; JDK 17 vs 27; hybrid P/E-core placement; Module 02 not in the Maven source root; counters availability unknown; formal contract missing; Module 01 early-run clustering unexplained.
- **Boundaries for you:** explain what coherence is and why it is needed, and cite sources for it. This module did not establish any coherence behaviour. Do not use Experiment 1 or 2 as evidence of invalidations. MESI/MOESI states belong to Vivansh.
- **Inputs you need:** the real m03 JSON/CSV and environment file, the real cache sizes and line size, whether counters work, and the decision on pinned vs unpinned runs.

## 13. Handoff note for Vishwajeet (false sharing)

- **Line size:** confirm it on the test machine (Coreinfo or collect_env). Do not hard-code 64 without checking. 16 ints or 8 longs per 64-byte line if so.
- **Layout:** Java does not guarantee field or array layout. Inspect the real layout (for example with the OpenJDK JOL tool, suggested but not verified here) rather than assuming two variables are on different lines. Padding or JDK-internal annotations (such as @Contended, which needs JVM flags) are JVM-specific.
- **Thread placement:** hyper-threads on one core share L1 and L2; E-cores and P-cores have different caches. Record where threads run and pin them for any claim about line ownership.
- **Baseline:** use a single-threaded run of the same loop (as in this module) so any slowdown can be attributed to sharing, not to size. Keep working sets inside one cache level so size effects do not mix in.
- **Method:** same JMH discipline: setup outside measurement, results consumed, multiple forks, report fork spread, record environment. Use counters (cache-to-cache transfer or cache-line contention events listed for your CPU at the perfmon site) if you want to claim line bouncing; timing alone does not show it.
- **No expected outcome is prescribed here.** Write your hypothesis before measuring and state what result would refute it.
