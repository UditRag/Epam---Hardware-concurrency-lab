# Module 09: CAS and Atomic Operations

**Owner:** Ashi Jain | **Handoff:** Vivansh | **Team:** LNCT Qubit | **Date:** 2026-10-10
**Evidence labels:** MEASURED (sandbox), THEORETICAL, PENDING

## 1. Scope
CAS semantics, success/failure, retry loop, plain vs synchronized vs atomic counters, contention cost, atomicity vs visibility vs ordering, ABA and stamped-reference mitigation, and why an atomic index alone does not make a ring buffer safe. Detailed ring-buffer implementation and JMH throughput comparison are out of scope.

## 2. CAS in one picture
```
compareAndSet(expected, new)
   current == expected ? --yes--> value = new, return true
                       --no---> value unchanged, return false
```
CAS does not retry by itself; the caller decides (see `CASRetryCounter`).

| Condition | Result |
|---|---|
| Expected matches current | Update succeeds, returns `true` |
| Expected differs from current | No update, returns `false` |

## 3. Code map
| File (`src/main/java/cas`) | Demonstrates | Result file |
|---|---|---|
| `CASExample` | CAS success then failure | `results/raw/run_cas_example.txt` |
| `CASRetryCounter` | Retry loop; returns failed-attempt count | `results/raw/run_cas_retry_counter.txt` |
| `CounterComparison` | Plain / synchronized / AtomicInteger / CAS-loop counters | `run_counter_comparison_*.txt` |
| `CasContentionDemo` | Failed CAS attempts as threads grow | `run_cas_contention_demo.txt` |
| `AbaDemo` | ABA missed by `AtomicReference`, caught by `AtomicStampedReference` | `run_aba_demo.txt` |

Tests: `src/test/java/cas/CasTest.java` (JUnit 5, 10 test executions). **Status: written, NOT RUN** (no Maven/JUnit in the run sandbox).

## 4. Atomicity vs visibility vs ordering
| Mechanism | Atomicity | Visibility | Ordering | Blocks? |
|---|---|---|---|---|
| Plain field | No | No guarantee | No guarantee | No |
| `volatile` | Single read/write only (not `count++`) | Yes | Yes | No |
| `synchronized` | Yes | Yes | Yes | Yes |
| `AtomicInteger` / CAS | Yes (single op) | Yes | Yes | No (may retry) |

## 5. Contention
Two threads read 10; T1 CAS(10->11) wins; T2 CAS(10->11) fails, re-reads 11, retries CAS(11->12). Each failure = wasted read + compute + CAS.

## 6. ABA
| Step | Reference | Stamp |
|---|---|---|
| Thread A reads | A | 1 |
| Thread B changes | B | 2 |
| Thread B changes back | A | 3 |
| Thread A CAS with old pair | A | 1 -> **fails** (stamp is 3) |

`AbaDemo` reproduces exactly this, deterministically. Stamps mitigate some ABA cases only if every update advances and checks the stamp.

## 7. Ring buffer boundary
An atomic index gives slot ownership only. A correct design also needs payload publication, readiness detection, full/empty handling and wraparound handling, under a documented protocol (volatile/release-acquire etc.). No ring buffer is implemented in this module.

## 8. Results (MEASURED, 1 vCPU sandbox, OpenJDK 21.0.10)
| Program | Observed |
|---|---|
| CASExample | `true`, 10, `false`, 10 (as expected) |
| CASRetryCounter | `Final value: 2` |
| AbaDemo | AtomicReference CAS = `true` (ABA missed); Stamped CAS = `false` (detected) |
| CounterComparison 2t x 100k x 5 runs | all four counters = 200000 in every run |
| CounterComparison 8t x 1M x 10 runs | all four counters = 8000000 in every run |
| CasContentionDemo 8t x 1M | failed CAS attempts: 1t=0, 2t=3, 4t=8, 8t=12 (per 1M increments per thread) |

**Limitation:** with one vCPU the racy counter never lost an update, and CAS failures were rare. This does **not** show the racy counter is safe, and it is not a contention benchmark. Re-run `scripts/run_all.*` on a multi-core machine.

## 9. Status
| Item | Status |
|---|---|
| Source compiled and run | MEASURED (javac, OpenJDK 21) |
| Maven build / JUnit | NOT RUN |
| JMH benchmark | NOT RUN |
| Multi-core correctness rerun | PENDING |
| Integration with shared project | PENDING |

## 10. Handoff to Vivansh
Re-run on multi-core hardware and keep raw output in `results/raw`; run `mvn test`; if throughput is needed, add JMH separately; review ring-buffer slot ownership/publication/wraparound rather than assuming an atomic index is enough.
