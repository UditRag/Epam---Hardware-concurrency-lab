# Module 11 — Benchmark Environment Template

**Status:** `PENDING LOCAL EXECUTION` — fill in before committing benchmark results.

## Java Runtime

```
java -version         # paste full output here
mvn -version          # paste full output here
```

| Field            | Value         |
|------------------|---------------|
| JDK vendor       | PENDING        |
| JDK version      | PENDING        |
| JVM flags used   | PENDING        |
| Maven version    | PENDING        |
| Compiler release | 17 (fixed)    |

## Machine

| Field                | Value     |
|----------------------|-----------|
| OS name/version      | PENDING   |
| CPU model            | PENDING   |
| Physical cores       | PENDING   |
| Logical cores        | PENDING   |
| Hybrid CPU?          | PENDING   |
| RAM (GB)             | PENDING   |
| Power plan           | PENDING   |

## Benchmark Configuration

| Field                  | Value                              |
|------------------------|------------------------------------|
| JMH version            | 1.37 (project POM)                 |
| Warm-up iterations     | 5 × 1 s (adjust if needed)         |
| Measurement iterations | 5 × 1 s                            |
| Forks                  | 3                                  |
| Thread count confirmed | 1 producer + 1 consumer per group  |
| Throughput unit        | ops/s                              |
| Raw JSON file          | results/raw/m11_jmh_raw.json       |
| Console log file       | results/raw/m11_jmh_console.txt    |

## Run Command

```powershell
# From hardware-concurrency-lab/
mvn clean package -DskipTests

# Verify BenchmarkList
Test-Path "target\classes\META-INF\BenchmarkList"

# Execute
java -jar target\benchmarks.jar "io.lnctqubit.hwconc.m11.benchmarks.*" `
     -wi 5 -i 5 -f 3 -tu s -rf json `
     -rff results\raw\m11_jmh_raw.json `
  | Tee-Object results\raw\m11_jmh_console.txt
```

## Evidence Labels

- `TEAM-MEASURED`: run on this machine with environment recorded above.
- `PENDING LOCAL EXECUTION`: not yet run; do not fill results until executed.
- Do not copy synthetic or sandbox results into published evidence files.
