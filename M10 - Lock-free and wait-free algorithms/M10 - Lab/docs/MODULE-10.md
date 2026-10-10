# Module 10 — Lock-Free and Wait-Free Algorithms (owner: Vivansh)
STATUS: proposal. Project Contract and Aastha's handoff were NOT supplied; everything marked ASSUMED must be reconciled.

## A. Scope
In: progress guarantees, Treiber stack (lock-free), CAS-loop counter (lock-free vs wait-free contrast), tests, experiment plan.
Out (do not repeat): CAS/ABA definitions (Aastha, M09), JMM/happens-before theory (Prachi, M08), cache/coherence (M03–M06), SPSC ring buffer (Namita, M11).
Why Treiber stack: unbounded MPMC LIFO, so it does not overlap Namita's bounded SPSC ring buffer. (ASSUMED: Namita has not chosen a stack; confirm with her.)

## B. Progress guarantees (definitions; Herlihy 1991, Herlihy-Shavit book)
| Guarantee | Meaning | Example here |
|---|---|---|
| Blocking | A stalled/descheduled thread can stop others (lock holder) | synchronized stack |
| Obstruction-free | A thread running alone (others paused) finishes in bounded steps | weaker than lock-free; no contention-management claim needed |
| Lock-free | Some thread always completes in a finite number of system steps; an individual thread may starve | TreiberStack push/pop |
| Wait-free | EVERY thread completes in a bounded number of its own steps | would need a bound independent of contention |

"No `synchronized`" is not sufficient: a spin-wait on a flag set by another thread is non-blocking-looking code that is still blocking. Lock-freedom requires that a thread's failure to progress is attributable to another thread's progress.

### Why TreiberStack is lock-free, not wait-free
- `push`/`pop` retry only when `compareAndSet(head, ...)` fails; failure means `top` changed, i.e. another thread's CAS succeeded. So the system as a whole progresses.
- A fixed thread can lose the race forever, so there is no per-thread step bound: NOT wait-free.
- Limits on the claim: (1) `new Node` inside the loop, so allocation/GC pauses can delay a thread (the claim is at the algorithm level, assuming allocation does not block); (2) `pop` returns null on empty rather than waiting, so there is no blocking wait; (3) the argument assumes a fair-enough scheduler for individual completion but not for lock-freedom itself.

### Correctness argument (to be explainable live)
- Linearization points: push = successful CAS; pop non-empty = successful CAS; pop empty = the read returning null (documented in source).
- Visibility [JLS guarantee]: the successful CAS has volatile read+write memory effects (java.util.concurrent.atomic package docs), and Node fields are final (JLS 17.5), so a popper sees a fully built node and its `next` chain.
- ABA [design argument]: nodes are freshly allocated and never recycled; GC keeps a referenced node from being reused, so `top == head` at CAS time means the same node object. A node pool would break this; then use `AtomicStampedReference`.
- Invariant tested: multiset of popped items == multiset of pushed items; stack empty at end.

## C. Source-backed claims (verify each URL/section yourselves before submission)
| # | Claim | Source |
|---|---|---|
| 1 | Wait-free = every process finishes in finite own steps; CAS is universal (consensus number ∞) | M. Herlihy, "Wait-Free Synchronization", ACM TOPLAS 13(1), 1991, pp.124–149 |
| 2 | Linearizability as correctness condition, linearization points | Herlihy & Wing, "Linearizability: A Correctness Condition for Concurrent Objects", ACM TOPLAS 12(3), 1990 |
| 3 | Obstruction-freedom definition | Herlihy, Luchangco, Moir, "Obstruction-Free Synchronization: Double-Ended Queues as an Example", ICDCS 2003 |
| 4 | Treiber stack origin | R. K. Treiber, "Systems Programming: Coping with Parallelism", IBM Almaden RJ 5118, 1986 |
| 5 | Lock-free/wait-free definitions and Treiber stack discussion | Herlihy & Shavit, *The Art of Multiprocessor Programming* (progress conditions chapter; stack chapter) — team to fill exact section numbers from their edition |
| 6 | CAS has volatile read/write memory effects; compareAndSet semantics | Oracle, `java.util.concurrent.atomic` package summary, https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/atomic/package-summary.html |
| 7 | Volatile write happens-before subsequent read of same variable; final-field semantics | JLS SE 21 §17.4.5 and §17.5, https://docs.oracle.com/javase/specs/jls/se21/html/jls-17.html |
| 8 | Whether `getAndIncrement` is a hardware XADD or a CAS loop (H3) | OpenJDK source `jdk.internal.misc.Unsafe#getAndAddLong` and C2 intrinsic: team must open the source for their JDK build and cite the exact file/line. Not asserted here. |

Evidence tiers: JLS/API guarantees = claims 6, 7. Algorithm theory = 1–5. JVM/JIT/CPU detail = claim 8 (hypothesis). Measured = none yet.

## D. Files (ASSUMED layout `modules/10-lockfree/`; here under `m10-lockfree/`)
- `src/main/java/io/lnctqubit/hwconc/m10/TreiberStack.java` — `push(E)`, `E pop()` (null if empty), `peek()`, `isEmpty()`
- `src/main/java/io/lnctqubit/hwconc/m10/CasLoopCounter.java` — `int incrementCountingRetries()`, `long get()`
- `src/test/java/io/lnctqubit/hwconc/m10/LockFreeChecks.java` — dependency-free checks with hard timeouts (convert to JUnit 5 if Udit's contract mandates it; `check(...)` calls map 1:1 to assertions)

## E. Build / run (no Maven needed for these files)
```
# with a full JDK 17/21
javac -d out $(find src -name '*.java')
java -cp out io.lnctqubit.hwconc.m10.LockFreeChecks     # exit 0 = pass, every wait is bounded by timeouts
```
Correctness conditions: sequential LIFO; pop-empty == null; for 20 repetitions of 4P/4C x 50 000 each value popped exactly once and the stack ends empty; CAS counter final == threads x perThread.

## F. Experiment plan (all results PENDING)
E1 correctness stress (done as checks above). A stress pass is evidence, not proof: use jcstress for outcome tests if desired.
E2 lock-based vs lock-free throughput (JMH, in Aashi's benchmark harness):
- Hypothesis: none about direction. Under contention the lock-free stack may be faster, slower, or equal to a `ReentrantLock`/`synchronized` stack; the result is to be measured.
- Independent: implementation {TreiberStack, lock-based stack with same semantics}, threads {1,2,4,8,..}. Dependent: ops/s (throughput), optionally per-op latency. Controls: same item count, same push/pop mix, same null-on-empty semantics, same JDK/flags, @Fork≥3, warm-up, Blackhole consumption.
- Raw data: JMH JSON/CSV output; env: CPU model, cores, OS, JDK, flags.
E3 retry rate: `incrementCountingRetries` summed per thread, plotted against thread count. Observation only (retries are not equal to cache misses).
Table (PENDING): | impl | threads | score ± error | unit | raw file | env |

## G. Interpretation, limits, likely reviewer questions
- Passing the checks does not prove lock-freedom or linearizability; they only fail to find a bug in tested interleavings.
- The sandbox run below had 1 CPU, so true parallelism was minimal; rerun on your multicore machine.
- Q: Is it wait-free? A: No; a thread can lose CAS races repeatedly. Q: Is `AtomicLong.getAndIncrement` wait-free? A: the API/JLS do not promise it; JDK/CPU dependent (H3, unverified). Q: Faster than locks? A: unmeasured; progress guarantee is separate from performance. Q: ABA? A: not possible here because nodes are never reused and GC keeps referenced nodes alive; it returns with pooling. Q: Why can allocation break the claim? A: a GC/alloc stall delays a thread regardless of CAS success. Q: Linearization point of pop-empty? A: the read of `top` that observed null.

## H. Contribution checklist (tick only if true)
[ ] Reviewed code line by line  [ ] Ran checks on own machine  [ ] Verified each source/section  [ ] Coordinated with Namita on overlap  [ ] Got Aastha's handoff and reconciled names  [ ] Ran E2 and saved raw JMH output  [ ] Ready for KT

## I. HANDOFF PACKET → Namita
Project: Team LNCT Qubit. Owner: Vansh (M10). Next: Namita (M11).
1. Shared decisions: Java 17+ (tested on 21) ASSUMED; package `io.lnctqubit.hwconc.m10` ASSUMED; no external deps for this module.
2. Files: see D.
3. Established: progress-guarantee definitions, Treiber stack argument, linearization points, ABA-not-applicable-by-GC argument. Do not repeat these or CAS definitions.
4. Verification: compile OK and `LockFreeChecks` RUN AND PASSED (all 23 checks) on OpenJDK 21.0.10, Ubuntu 24.04 sandbox, 1 logical CPU, via javax.tools compiler API; NOT RUN on the team's multicore hardware; NOT RUN under Maven/JUnit.
5. Experiments: E2/E3 PENDING; no performance numbers exist.
6. Issues: contract/Aastha handoff missing; package/layout ASSUMED; claim 8 (H3) unverified; Herlihy-Shavit section numbers to fill.
7. Namita's scope: bounded SPSC ring buffer with explicit capacity, sequence numbers, wraparound, no overwrite of unread items, JMM justification (volatile/acquire-release style publication). Preserve: `null`-on-empty style or document a different contract. Out of scope: re-explaining CAS/ABA/progress definitions. Note SPSC needs no CAS (single writer per index), so say so rather than calling it a CAS algorithm; state its progress guarantee precisely (a full/empty return is non-blocking; a spinning wait is not).
8. Claims not yet proven: "TreiberStack is faster/slower than locks", "getAndIncrement is wait-free", "tests prove linearizability".

## J. Self-audit
Met: scope boundaries, definitions, one algorithm with progress argument and stated limits, tests with timeouts, NOT RUN marked where applicable, no invented numbers, overlap with Namita avoided.
Gaps: contract/handoff reconciliation, JMH lock-vs-lock-free benchmark (Head's Mutex-vs-CAS example) not yet run, source section numbers need human verification, no jcstress test, KT not done.

---
# ADDENDUM 1 — Formal justification and "lock-free is not automatically faster / wait-free" demonstration
(Task text received: owner "Vivansh Rathore", M10. NOTE: the master plan lists Vivansh as M05 (MESI) and Vansh as M10. Confirm the owner name for the contribution matrix.)

## 1. Model and definitions
Shared memory = single AtomicReference `top`. A step = one access to `top` (get or CAS). An operation is *pending* from invocation to return.
- Lock-free: in every execution in which some operation is pending and processes keep taking steps, some operation completes within a finite number of system-wide steps (so infinitely many steps imply infinitely many completions).
- Wait-free: every operation completes within a bound B on its OWN steps, independent of other threads.

## 2. Theorem 1: TreiberStack push/pop are lock-free
1. `top` changes only through successful CAS (the only writes in the class).
2. One loop iteration = one `get` + at most one CAS. If CAS succeeds, the operation completes (push/pop non-empty). If `pop` reads null, it returns after that single read.
3. Suppose an iteration's CAS fails. CAS(expected=h, new) fails iff the current value is not h, while the thread read h earlier. So `top` was modified in between, i.e. some other operation executed a successful CAS in that interval, and that operation completes at that CAS (its linearization point is its last shared write).
4. Charge each failed iteration to that completed operation. A completed operation is charged at most once per concurrently pending thread, so failures in any finite window are bounded by (number of threads) x (completed operations in the window).
5. Hence if some thread fails infinitely often, infinitely many operations by others complete. System-wide progress holds. ∎
Assumptions: allocation (`new Node`) terminates, GC does not stall threads forever, JVM executes threads (the guarantee is about algorithm steps, not scheduler fairness). No lock is taken, but note this is a statement about the algorithm, not an API/JLS promise.

## 3. Theorem 2: TreiberStack is NOT wait-free
Take 3 threads A, B, C, A calling push. Adversarial schedule (exists; we do not claim it happens often): A executes `top.get()`; B completes a full push or pop; A executes CAS, which fails; A executes `get`; C completes an operation; A's CAS fails; repeat. A takes unboundedly many steps and never completes while B and C complete infinitely many operations. No bound B exists, so it is not wait-free. ∎ (This is the standard starvation argument; it shows allowed behaviour, and real runs may never exhibit it.)

## 4. Linearizability sketch (Herlihy-Wing)
Abstract state S = sequence of items from `top` following `next`. Because Nodes are immutable and never reused, a node reachable from `top` has a fixed tail. Successful push-CAS maps S to item·S atomically; successful pop-CAS maps item·S' to S'; empty-pop read observes S = empty. Each operation takes effect at its linearization point (section B of the module), which lies inside its invocation-response interval, and the resulting sequential history satisfies the stack spec. Memory visibility of node contents comes from CAS volatile semantics + final fields (JLS 17.4.5, 17.5). This is a sketch, not a machine-checked proof.

## 5. Demonstrating the three claims (what to run, what each can show)
| Claim | How demonstrated | What the evidence can and cannot show |
|---|---|---|
| Lock-free is not wait-free | Theorem 2 (logical). `ProgressDemo` prints min/max ops per thread. | A runtime run cannot prove wait-freedom or its absence: a small spread is allowed under lock-freedom, a large spread is consistent with starvation but also with scheduling/affinity. |
| Lock-free is not automatically faster | `ProgressDemo` (quick, CSV) then `StackBench` (JMH, `-t 1,2,4,8`) vs `LockStack` | Only your machine/JDK/threads. Any outcome is acceptable (faster, slower, equal, or crossing over with thread count). Report it as measured, do not pre-commit to a direction. |
| CAS retry behaviour under contention | `CasLoopCounter.incrementCountingRetries()` totals per thread count (extend `ProgressDemo` or LockFreeChecks) | Retries show contention in the algorithm; they are not cache-miss or coherence evidence (needs perf counters). |

Run:
```
javac -d out $(find src -name '*.java')
java -cp out io.lnctqubit.hwconc.m10.ProgressDemo 1 5; java -cp out io.lnctqubit.hwconc.m10.ProgressDemo 2 5
java -cp out io.lnctqubit.hwconc.m10.ProgressDemo 4 5; java -cp out io.lnctqubit.hwconc.m10.ProgressDemo 8 5
```
Save the CSV plus the `env:` line. For publication-grade numbers use StackBench (JMH).

## 6. New file status
| File | Status |
|---|---|
| `LockStack.java` (blocking baseline, same semantics) | compiled OK; semantic equivalence NOT covered by LockFreeChecks yet (add the same exactly-once test for it) |
| `ProgressDemo.java` | compiled OK; smoke run executed once in a 1-CPU sandbox only to confirm it terminates. Those numbers are intentionally NOT reported: one CPU has no real parallel contention and the run is a demo, not a benchmark. Team results PENDING |
| `StackBench.java.txt` (JMH) | NOT COMPILED, NOT RUN (no JMH in sandbox); rename to .java and add JMH to the shared pom |

## 7. Extra reviewer questions
- "Does 'no locks' make it faster?" Not implied by any definition; progress guarantees describe worst-case behaviour of individual operations, throughput is empirical.
- "Why charge failures to completions?" It is the step-counting argument that turns "CAS failed" into "someone else made progress".
- "Is the lock-based stack wait-free or lock-free?" Neither; blocking: a descheduled lock holder stops everyone.
- "Why is allocation a limitation?" Progress argument assumes `new` terminates without waiting on other threads.
