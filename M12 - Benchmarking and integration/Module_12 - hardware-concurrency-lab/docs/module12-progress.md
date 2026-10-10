# Module 12 progress checkpoint (2026-10-10)

## Done
- New code: `m12/benchmarks/{SpscThroughputBenchmark,SpscLatencyBenchmark}`, `m12/tools/{OffsetProbe,JolLayout}`.
- `pom.xml`: + jol-core 0.17, + maven-shade-plugin (benchmarks.jar).
- Scripts: `scripts/run_module12.ps1`, `scripts/analyze_m12.py`.
- Docs: `docs/module12-benchmarking-integration.md`, `docs/module12-evidence-matrix.md`.
- Static review of the Module 11 benchmark: 4 defects documented; M11 file left unchanged.

## Executed commands and real outcomes (sandbox: Linux, OpenJDK 21.0.10 JRE, 1 vCPU, no Maven/JMH/JUnit/JOL, no network)
- `javac` (via `java -m jdk.compiler/...`) of SpscQueue, SpscRingBuffer, PaddedSpscRingBuffer, OffsetProbe: OK (3 Unsafe warnings).
- `OffsetProbe` x3 JVM flag sets: real offsets saved to `results/m12/m12_offset_probe_sandbox.txt`.
- Type-check of both new benchmarks against hand-written JMH annotation stubs: OK (stubs approximate JMH; not a real build).
- `analyze_m12.py` against a tiny synthetic schema fixture in /tmp: ran OK (fixture discarded; not evidence).
- NOT run: mvn test/package, JMH, JOL, JUnit regression.

## Team-machine execution (2026-10-10)
- `run_module12.ps1 -Quick`, then final run completed; 59 tests 0 failures; JMH 3 forks; results in results/m12. Script fixes: ErrorActionPreference, cmd /c for stderr, secondaryMetrics parsing.

## Blockers
- None for execution. Unrecorded: thermal measurement. Not done: pinned rerun, HW counters, real Disruptor.

## Next action
Review report; optionally pinned rerun.
