# Prior-module audit and minimal integration corrections

Date: 2026-10-09. Owner of this audit: Module 03 workstream. This note records only evidence-backed corrections needed to keep the shared project buildable and avoid overstating earlier results; it does not regenerate Modules 01 or 02.

## Module 01 — Udit: preserve, clarify, do not overclaim

- The PDF reports 17 incorrect unsafe-counter outcomes across 180 reported runs (12 at 1,000,000 increments/thread and 5 at 10,000,000 increments/thread). The four archived text outputs in `results/raw/` show 3 failures per archived launch. The two additional launches are console-only and cannot be independently verified from the repository. Keep 17/180 labelled as the report’s aggregate, not as a stable probability or an independent-run failure rate.
- The old README path `results/environment.md` was wrong; the collected file is `results/raw/environment.md`. This README pointer is corrected in the integrated repository.
- The environment log reports Oracle build `27+35-2325`; preserve the exact version output and do not casually label it a normal GA JDK without checking the full `java -version` output. Module 02’s dump reports OpenJDK 17.0.18, so the two modules were not measured on the same Java runtime.
- No code or counter result in Module 01 was changed by this Module 03 work.

## Module 02 — Adrika: preserve behaviour, fix project layout and terminology

- The supplied Module 02 ZIP contained `DeadlockDemo.java`, `OrderedLockDemo.java`, and `ReentrantLockDemo.java` under `src/`, plus a second `OrderedLockDemo.java` under `src/synchronization/`, with stale `.class` files. These are not Maven standard source locations. The integrated project now places one copy of each source at `src/main/java/synchronization/` and excludes duplicate/stale compiled artifacts. Package names and demo logic are preserved.
- `deadlock-error.txt` contains a failed launch (`main class DeadlockDemo`, wrong binary name because the source declares `package synchronization`). It is not evidence that the deadlock demo failed internally. Use the qualified name `synchronization.DeadlockDemo` when launching from the project classpath. The captured thread dump, not that error file, is the evidence for the deadlock cycle.
- `ReentrantLockDemo` demonstrates a timed `tryLock` attempt and release, not reentrant acquisition of the same lock. Its existing class name is retained to avoid a public-name change; the distinction is called out so KT does not overstate the demo.
- The deadlock demo is intentionally non-terminating when the deadlock is reached. Do not invoke it directly from a unit-test JVM without a subprocess timeout/termination strategy.
- The reported three demo runs are one observation each; the proposed 30-run batch tests and JUnit suite remain unverified unless fresh output is supplied. No lock-performance claim is made.

## Module 03-specific corrections

- `docs/references.md` now retains Module 01 references and appends a separately labelled Module 03 claim-to-source table.
- The hierarchy text distinguishes registers from cache levels, avoids treating cache-line size and layout as universal Java guarantees, and labels CPU-specific cache figures as processor-family specifications that still require local topology capture.
- The working-set pointer chase uses 262,144 dependent loads per invocation (matching `@OperationsPerInvocation`) to improve the number of samples at the largest working set. This is a benchmark-design change, not a measured-performance claim.
- Both benchmarks declare one thread by default, and the documented run commands explicitly use `-t 1`.
- No Module 03 Maven/JUnit/JMH execution was possible in the current review container because Maven is not installed and network downloads are unavailable. Do not mark the Maven build, JUnit suite, or JMH runs passed until run on the team machine.
