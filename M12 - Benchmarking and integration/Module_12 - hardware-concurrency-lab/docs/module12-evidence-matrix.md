# Module 12 — Integration evidence matrix

Labels: TEAM-MEASURED, EXTERNAL EVIDENCE, THEORETICAL, STATICALLY REVIEWED, PENDING LOCAL EXECUTION, BLOCKED, NOT PROVEN.
"SANDBOX" = executed by the AI assistant in a 1-vCPU Linux container (not team hardware). Prior-module rows are
taken from the repo's own docs/results and were NOT re-executed in Module 12.

| Module / item | Evidence | Label | Source / gap |
|---|---|---|---|
| M01 race PoC: Unsafe 17/180 incorrect; Sync/Atomic 0 | 4 archived outputs show 3 failures each; 2 launches console-only | TEAM-MEASURED (partly unarchived) | results/raw/run*.txt; docs/prior-module-audit.md |
| M02 deadlock demos | single observations, thread dump | TEAM-MEASURED (n=1); lock-performance NOT PROVEN | docs/prior-module-audit.md |
| M03 cache/JMH benchmarks | JMH never executed (no Maven in review container) | PENDING LOCAL EXECUTION | prior-module-audit.md |
| M06 false-sharing JMH results | template 8/9 lines PENDING | PENDING LOCAL EXECUTION | results/raw/m06_false_sharing_results_template.csv |
| M07 reordering outcomes | template 2/3 lines PENDING | PENDING LOCAL EXECUTION | results/raw/m07_outcomes-template.csv |
| M08 JMM outcomes | container smoke only, template 7/8 PENDING | SANDBOX smoke; team run PENDING | results/raw/m08_environment_container.md |
| M09/M10 lock-free sources + TreiberStackTest | in `mvn clean test` M11 run (59 tests) | TEAM-MEASURED (per M11 report; not re-run) | M11 report §H |
| M11 SPSC queue correctness: 59 tests, 5M-event stress | OpenJDK 21.0.11, i7-12650H | TEAM-MEASURED (not re-run in M12: no Maven/JUnit in sandbox) | results/raw/m11_maven_test_status.md |
| M11 JMM publication/reclamation argument | JLS §17.4.4-5 | THEORETICAL / EXTERNAL EVIDENCE | M11 report §E |
| M11 benchmark skeleton | defects: cross-product @Param; spin loops without `Control.stopMeasurement` (can hang at iteration end); ABQ ops counted on failed offers | STATICALLY REVIEWED (defects found, file left unchanged) | m11/benchmarks/SpscBenchmark.java |
| M12 build + 59 tests | `mvn clean test` BUILD SUCCESS; fat jar built | TEAM-MEASURED | results/m12/m12_mvn_test_output.txt |
| M12 SPSC throughput plain/padded/ABQ | plain 16.7M/17.4M, padded 14.9M/16.0M, ABQ 6.2M/7.8M ops/s (cap 64/1024) | TEAM-MEASURED (JDK 27, unpinned, AC power, no thermal measurement) | results/m12/m12_throughput_summary_final.csv |
| M12 round-trip latency | mean ~167 ns plain/padded, 459 ns ABQ; p50 at timer resolution | TEAM-MEASURED | results/m12/m12_latency_summary_final.csv |
| Field offsets, team JDK 27 | padded: sequences 120 B apart, mask/entries ≥64 B from both | TEAM-MEASURED; sandbox JDK 21 differed | m12_offset_probe.txt; m12_offset_probe_sandbox.txt |
| JOL layout cross-check | offsets identical to OffsetProbe; addresses "guessed" per JOL | TEAM-MEASURED (offsets only) | m12_jol_layout.txt |
| Padding eliminates false sharing | offsets only show distance | NOT PROVEN (needs HW counters) | §3 of report |
| Padded faster/slower than plain | plain faster at cap 64 (~12%, CIs disjoint); cap 1024 CIs overlap | TEAM-MEASURED; cause NOT PROVEN | throughput CSV |
| Real LMAX Disruptor integration/benchmark | not added | NOT PROVEN / out of scope | M11 report §I (EXTERNAL EVIDENCE docs only) |
| Canonical Project Contract / Module 12 API contract | not supplied | UNVERIFIED | M11 report §B |
