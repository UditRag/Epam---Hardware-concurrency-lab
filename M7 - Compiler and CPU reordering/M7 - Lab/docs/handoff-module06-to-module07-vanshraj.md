# Handoff Packet — Module 06 to Module 07

**Project:** Team LNCT Qubit — Hardware Concurrency, Memory Models & Lock-Free Design  
**Current owner:** Vishwajeet  
**Completed module:** 06 — False Sharing and Cache-Line Bouncing  
**Next owner:** Vanshraj Singh Rathore — Module 07: Compiler and CPU Reordering

## 1. Shared project decisions

- **Language/JDK target:** Java 17 source target in Maven POM; record the actual installed JDK used for any run.
- **Build tool:** Maven.
- **Benchmark dependency:** JMH 1.37 as configured in `pom.xml`.
- **Package convention:** `cache` for cache experiments; test package mirrors source package.
- **Benchmark interface:** JMH `@Benchmark` method `cache.FalseSharingBench.incrementOwnedSlot` with `stride` parameter values `1` and `16`.

## 2. Files added or changed

- `src/main/java/cache/FalseSharingBench.java` — per-worker counter benchmark; configurable stride.
- `src/main/java/cache/FalseSharingLayout.java` — stride mapping and counter allocation helper.
- `src/test/java/cache/FalseSharingLayoutTest.java` — index, spacing, capacity and invalid-input tests.
- `docs/module06-false-sharing.md` — design, assumptions, commands, results template, limitations and handoff context.
- `docs/module06-references.md` — module references.
- `docs/diagrams/m06-adjacent-vs-padded.svg` and `m06-ownership-bouncing.svg` — conceptual diagrams; not hardware traces.
- `results/raw/m06_false_sharing_results_template.csv` and `m06_environment.md` — templates.

## 3. Technical content established

- True sharing: multiple threads access the same logical location.
- False sharing: threads write distinct locations that may occupy one cache line.
- The benchmark increments one private-to-worker array slot per invocation in both variants.
- `stride=1` means adjacent array elements; `stride=16` means 128 bytes between `long` payload indices, not guaranteed physical alignment.
- Thread counts should be run separately at 1, 2, 4 and 8 (or supported counts) with the same JMH configuration.
- A time/throughput difference is not proof of a specific MESI/MOESI transition. Hardware-counter data may add evidence if available.
- Padding has memory/cache costs and can yield no gain or a regression.

## 4. Verification status

- ZIP contents and source text: inspected.
- Maven build and JUnit tests: **NOT RUN in this environment**; run `mvn clean test` locally.
- JMH measurements and raw JSON: **NOT RUN / PENDING**.
- Chart from real benchmark data: **PENDING**.
- `perf c2c` or equivalent counter capture: **NOT RUN / PENDING**.
- Diagrams: conceptual illustrations only, not traces captured from hardware.

## 5. Instructions for Member 7

Do not repeat Module 06's false-sharing explanation. Focus on source-code order versus compiler/JIT transformations and CPU out-of-order execution, then explain how the Java Memory Model and memory-ordering constructs constrain observable behavior. Distinguish language-level ordering from hardware coherence. Preserve this handoff and note any shared build/API changes.

## 6. Sources

- JMH: https://github.com/openjdk/jmh
- JOL: https://openjdk.org/projects/code-tools/jol/
- Linux perf c2c: https://man7.org/linux/man-pages/man1/perf-c2c.1.html
- Java Language Specification: https://docs.oracle.com/javase/specs/
- Prior project docs: `docs/module04-cache-coherence.md`, `docs/module05-mesi-moesi-state-diagrams.md`, `docs/references.md`.
