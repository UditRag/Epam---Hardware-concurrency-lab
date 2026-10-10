# HANDOFF PACKET — MODULE 09 TO MODULE 10

**Project:** Team LNCT Qubit — Hardware Concurrency, Memory Models & Lock-Free Design  
**Current owner:** Ashi (Module 09 — CAS and Atomic Operations)  
**Next owner:** Vivansh (Module 10 — Lock-Free Design)  

---

## 1. Shared project decisions

- **Java/JDK version:** Java 17 baseline (`maven.compiler.release=17`).
- **Folder conventions:** Module 09 files placed correctly in `src/main/java/atomics/lockfree/` and `src/test/java/atomics/lockfree/`.

## 2. Files finalized in Module 09

| File | Purpose |
|------|---------|
| `src/main/java/atomics/lockfree/CasLoopCounter.java` | Counter using explicit CAS retry loop |
| `src/main/java/atomics/lockfree/RetryDemo.java` | Demo measuring CAS retries under load |
| `docs/HANDOFF_MODULE09_TO_MODULE10.md` | This explicitly requested historical packet |

## 3. Technical content established (Module 09)
Module 09 proves that **Compare-and-Swap (CAS)** maps to hardware atomic primitives (`LOCK CMPXCHG` on x86-64).
It demonstrates the retry-loop pattern needed for atomic RMW (Read-Modify-Write) operations when multiple writers compete.
`CasLoopCounter.java` counts exactly how many times threads "lose" the race and have to retry, proving that CAS guarantees safety, but does not guarantee wait-free execution.

## 4. Verification Status
| Check | Status | Command |
|---|---|---|
| M09 code compiles | RUN AND PASSED | `mvn compile` |
| Tests inclusive of M09 | RUN AND PASSED | `mvn clean test` (Project total runs 59 tests) |

## 5. Next owner's exact scope (Vivansh, Module 10)
**Your Job:** Build on the CAS loops taught here to design general-purpose Lock-Free data structures (like the Treiber Stack).
- Distinguish between **Lock-Free** (system-wide progress) and **Wait-Free** (per-thread guaranteed progress).
- Document non-blocking progress limitations (ABA problem, retry stalling, cache thrashing from CAS conflicts).
