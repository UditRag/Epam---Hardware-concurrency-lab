# Environment (MEASURED in the run sandbox, 2026-10-10)

| Item | Value |
|---|---|
| OS | Ubuntu 24.04.4 LTS, Linux 6.18.44 x86_64 (container) |
| CPU | Intel Xeon @ 2.10 GHz |
| Cores / logical processors | **1 vCPU** |
| RAM | ~4 GB |
| Java | OpenJDK 21.0.10+7 (Ubuntu), 64-Bit Server VM |
| Build used for this run | `javac` (jdk.compiler module) directly; Maven NOT available offline |
| Compiler target in pom.xml | release 17 |
| Power plan / thermal state / background load | NOT COLLECTED |

**Important:** the 1-vCPU machine makes thread interleaving inside `racyCount++` rare, so the
unsynchronized counter did not lose updates here. Re-run on a multi-core machine
(`scripts/collect_env.sh` or `.ps1` records the environment) before drawing conclusions.
