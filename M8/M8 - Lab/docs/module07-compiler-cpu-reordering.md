# Module 07 — Compiler and CPU Reordering

**Owner:** Vanshraj Singh Rathore  
**Project:** Team LNCT Qubit — EPAM CoE: Hardware Concurrency, Memory Models & Lock-Free Design  
**Status:** SOURCE/DOCUMENTATION PREPARED; LOCAL BUILD AND JCStress EXECUTION PENDING  
**Scope:** Source/program order, JIT/compiler transformations, CPU execution/observation order, and the constraints imposed by the Java Memory Model (JMM).

## A. Scope and boundaries

**In scope:** explain why source order is not a promise of identical machine-instruction order; demonstrate a store-buffering litmus test with plain versus `volatile` fields; map observations to JMM rules; give repeatable JCStress execution and evidence capture.

**Out of scope:** detailed cache hierarchy, MESI/MOESI transitions, false sharing, lock-free algorithm design, and performance claims about specific CPU instructions. See Modules 03–06 for those subjects.

## B. Four distinct orders

1. **Program order:** order of actions in a thread as defined by the Java program and JLS rules.
2. **Compiler/JIT transformation:** an implementation may reorder, combine, eliminate, or move operations when the transformation preserves all behavior required by the language and runtime contract.
3. **Hardware execution/observation:** a processor can execute operations speculatively or out of order, while architectural results must obey its architecture's rules. The exact mechanism differs by CPU.
4. **Happens-before (JMM):** a language-level relation used to determine which cross-thread observations are guaranteed. It is not a diagram of physical instruction timing.

```text
Java source
   │ compiler/JIT transformations allowed by Java semantics
   ▼
Machine instructions ── CPU execution and memory subsystem
   │
   ▼
Observed result must still satisfy JMM / synchronization constraints
```

The arrows are a conceptual pipeline, not a claim that every JVM uses one fixed compilation path. A data race can permit outcomes that surprise a source-order-only reading; it does not mean every execution will reorder or that a particular instruction was observed moving.

## C. Store-buffering litmus test

Each actor writes its own variable and then reads the other variable.

```text
Actor 1                     Actor 2
x = 1                       y = 1
r1 = y                      r2 = x
```

| Variant | Is `(r1=0, r2=0)` allowed? | Reason |
|---|---|---|
| Plain `int` fields | Yes, permitted by the JMM | No cross-thread synchronization orders the writes before the other actor's reads. |
| `volatile int` fields | No | Volatile actions participate in synchronization order; the all-zero result would require a cycle inconsistent with that order. |

This is a litmus test, not a benchmark. JCStress explores many executions, but a permitted outcome may not appear in a finite run. “Not observed” is not proof that an outcome is forbidden. The volatile variant's forbidden outcome is a specification claim; test output is supporting validation of the harness, not the source of the guarantee.

## D. JMM guarantees vs implementation observations

- **Specification:** JLS §17.4 defines the memory model, data races, and happens-before; §17.4.5 defines happens-before consistency and synchronization-order rules. JLS §17.4.7 discusses allowed executions.
- **Specification:** a volatile write synchronizes-with subsequent reads of that variable in synchronization order; monitor unlock/lock and thread start/join also establish specified ordering edges.
- **Implementation detail:** a JVM may use different machine instructions/barriers on x86-64 and AArch64 while implementing the same Java guarantees.
- **Measured observation:** JCStress output reports outcomes observed on a particular JDK, CPU, OS, configuration and test duration.
- **Hypothesis only:** a specific `StoreLoad` barrier, store buffer, or hardware instruction caused a result. Confirming that requires generated-code inspection and architecture-specific documentation; a Java-level outcome alone does not prove the mechanism.

## E. Files and public surface

Separate runnable experiment project: `experiments/jcstress-store-buffering/`.

- `pom.xml` — Java 17 target, JCStress 0.16, compiler annotation processor, shaded executable JAR.
- `src/main/java/ordering/PlainStoreBuffering.java` — ordinary-field litmus test.
- `src/main/java/ordering/VolatileStoreBuffering.java` — volatile-field comparison.

No existing source file or shared project API is changed. The isolated Maven project avoids altering the main lab's JMH/JUnit dependencies.

## F. Build and run

From the repository root:

```bash
cd experiments/jcstress-store-buffering
mvn clean package
java -jar target/module07-jcstress.jar
java -jar target/module07-jcstress.jar -m quick
java -jar target/module07-jcstress.jar -h
```

Use the full default run for evidence collection; `-m quick` is a smoke test only. Preserve the complete console output. If the built JAR name differs because of Maven plugin behavior, inspect `target/` and use the actual artifact. These commands have **NOT RUN** in this authoring environment because Maven is not installed here.

Capture environment alongside results:

```bash
java -version
mvn -version
# Linux: uname -a; lscpu
# Windows PowerShell: Get-ComputerInfo | Select-Object WindowsProductName,OsVersion,CsProcessors
```

Record exact JCStress command, test mode, JDK vendor/version, CPU model/architecture, OS/kernel, core/SMT topology if known, JVM arguments, run duration and raw output file. Do not compare runs without noting differences.

## G. Experiment design and results

**Hypothesis:** plain-field store buffering may produce `(0,0)`; the volatile version must not produce `(0,0)` under the JMM.

- **Independent variable:** field semantics (`plain` vs `volatile`).
- **Dependent variable:** observed result tuple and JCStress outcome counts.
- **Controls:** same machine/JDK, same JCStress version, equivalent actor code, same run mode and JVM options.
- **Procedure:** build once; run both tests using JCStress; archive full output and environment record; repeat if an outcome is not observed.
- **Raw format:** untouched text output plus environment metadata; optionally extract outcome counts into CSV without deleting raw files.
- **Analysis:** check classification against annotations; distinguish observed counts from allowed/forbidden semantics. Do not calculate throughput from this test.
- **Limitations:** finite exploration, scheduler/JIT/CPU dependence, no direct proof of a physical reorder mechanism, and outcomes depend on correct JCStress harness execution.

| Test | Specification expectation | Actual observation | Status |
|---|---|---|---|
| Plain store buffering | `(0,0)` allowed; other listed tuples allowed | PENDING | NOT RUN |
| Volatile store buffering | `(0,0)` forbidden | PENDING | NOT RUN |

No outcome counts are supplied or fabricated. A successful build is not a test run; a test run that does not observe `(0,0)` does not invalidate its permitted status for plain fields.

## H. Interpretation and likely reviewer questions

**Q: Does Java always execute statements in source order?**  
No. The JMM defines required observable behavior, not a universal machine-instruction sequence. Transformations are allowed only when required semantics are preserved.

**Q: Does a plain-field `(0,0)` result prove CPU reordering?**  
No. It is consistent with store buffering/reordering explanations, but it does not isolate JIT, compiler, or hardware causes. Generated code and architecture-specific evidence would be needed.

**Q: If plain `(0,0)` never appears, is it impossible?**  
No. A finite run cannot prove absence of a permitted outcome.

**Q: Why use JCStress instead of a `CountDownLatch` loop?**  
JCStress is designed to exercise concurrency outcomes. Adding per-iteration coordination can change the synchronization graph and mask the behavior under study.

**Q: Does `volatile` make compound operations atomic?**  
No. It provides specified visibility and ordering for volatile accesses; `x++` remains a read-modify-write sequence and is not atomic merely because `x` is volatile.

**Q: Is this a performance benchmark?**  
No. It explores correctness/outcome space. Use JMH for performance questions and avoid interpreting JCStress outcome counts as speed.

## I. Contribution checklist

- [ ] Confirm assignment and review this module against the project contract.
- [ ] Build the isolated project and record the exact JDK/Maven versions.
- [ ] Run quick smoke test, then a full JCStress campaign; save untouched output.
- [ ] Verify that no forbidden outcome is reported and explain whether the plain `(0,0)` outcome appeared.
- [ ] Review JLS citations and code with the team; capture reviewer feedback.
- [ ] Present the test actors, outcome annotations, and limitations during KT.
- [ ] Update status only after the corresponding action actually occurs.

## J. Handoff packet — Module 07

**Project objective/shared decisions:** Java 17 target; Maven; preserve the existing `hardware-concurrency-lab` project, packages, and APIs. Existing Module 06 uses JMH 1.37 in the main POM; this module uses a separate JCStress 0.16 subproject so the main dependency graph is unchanged.

**Files added:** `experiments/jcstress-store-buffering/pom.xml`; `src/main/java/ordering/PlainStoreBuffering.java`; `src/main/java/ordering/VolatileStoreBuffering.java`; `docs/module07-compiler-cpu-reordering.md`; `docs/module07-references.md`; `results/raw/m07_environment-template.md`; `results/raw/m07_outcomes-template.csv`.

**Public types:** `ordering.PlainStoreBuffering`; `ordering.VolatileStoreBuffering`. They are JCStress test states, not application APIs.

**Established source-backed facts:** JLS §17.4 is the primary basis for JMM claims; the JLS, not one host's observations, determines allowed outcomes. JCStress is an OpenJDK concurrency stress-test harness. No false-sharing theory is repeated here.

**Integration:** run the isolated Maven build from its directory; no main-POM change required. The next module should not duplicate this litmus-test/JMM overview unless its assignment requires it.

**Verification status:** source authored and statically reviewed only. Maven build, JCStress runs, and KT are PENDING / NOT RUN in this environment.

**Open issues:** verify dependency resolution and shaded-JAR launch on team machine; collect real output; optionally inspect generated assembly with JITWatch/hsdis if available, without treating assembly alone as a universal guarantee.

**Inputs needed from next owner/reviewer:** Project Contract/Head feedback if additional required cases exist; exact target JDK/CPU/OS; raw test outputs; review comments; any team-approved JCStress version change.

## K. Final self-audit

| Requirement | Status | Evidence/next action |
|---|---|---|
| Assigned scope only | Covered | Source/JIT/CPU order and JMM litmus; no detailed cache/MESI repeat |
| Complete Java source | Prepared | Two complete JCStress classes and isolated POM |
| Source-backed claims | Prepared | JLS and OpenJDK JCStress references in companion file |
| Reproducible procedure | Prepared | Exact commands and environment capture fields |
| Real results only | Compliant | Results explicitly PENDING; commands NOT RUN |
| Hardware mechanism claims qualified | Compliant | No claim that outcome proves a specific instruction/barrier |
| Handoff | Prepared | Section J |
| Head of EPAM India feedback | Not supplied in provided artifacts | Cannot claim compliance with feedback not present; compare against actual feedback before final sign-off |
