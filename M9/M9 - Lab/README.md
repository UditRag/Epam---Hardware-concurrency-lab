# hardware-concurrency-lab: Task 09 (CAS and Atomic Operations)

Java PoC for Compare-And-Set: success/failure, retry loop, counter comparison, contention, ABA.

- Code: `src/main/java/cas` (CASExample, CASRetryCounter, CounterComparison, CasContentionDemo, AbaDemo)
- Tests: `src/test/java/cas/CasTest.java`
- Run: `mvn test`, then `mvn -q compile` and `java -cp target/classes cas.CASExample` (or `scripts/run_all.sh` / `scripts/run_all.ps1`)
- Report: `docs/module09-cas-atomic-operations.md`; sources: `docs/references.md`
- Raw output: `results/raw/`; environment: `results/raw/environment.md`; screenshots: `screenshots/`

## Contribution record (Ashi Jain)
- Code: 5 main classes. Tests: `CasTest` written, **NOT RUN** (Maven/JUnit unavailable in run sandbox)
- Runs: all 5 programs compiled and executed on OpenJDK 21.0.10 (1 vCPU sandbox); raw output archived in `results/raw`
- Screenshots are renders of the captured console output (`screenshots/01..06`)
- Evidence limitation: on 1 vCPU the plain counter did not lose updates; multi-core rerun PENDING
- Handoff to Vivansh: multi-core rerun, `mvn test`, optional JMH, ring-buffer publication protocol review
- KT STATUS: PENDING

## Module 09: CAS and Atomic Operations (Ashi Jain)
- Report: `docs/module09-cas-atomic-operations.md`
- Status: Java demos completed and run; Maven/JUnit, JMH and multi-core correctness evidence PENDING.
