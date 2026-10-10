# References (EXTERNAL EVIDENCE)

Section numbers are from the Java Language Specification; confirm against the edition you cite. Page numbers for the book are NOT VERIFIED (check your copy).

| Claim | Source | Section/Page | What it establishes |
|---|---|---|---|
| `x++` is read, add 1, store back | JLS, Postfix Increment Operator | 15.14.2 | Increment is described as a sequence of steps on a variable |
| Interleaved read-modify-write lets two threads lose an update | The Java Tutorials, "Thread Interference" (docs.oracle.com/javase/tutorial/essential/concurrency/interf.html) | whole page | Worked example of `c++` decomposed into steps that interleave |
| A data race = conflicting accesses not ordered by happens-before | JLS, Happens-before Order | 17.4.5 | Definition of data race, basis for the unsafe counter being racy |
| `Thread.join()` makes the joined thread's actions visible to the joiner | JLS, Happens-before Order | 17.4.5 | Reading `get()` after `join()` is well-ordered |
| `synchronized` gives mutual exclusion; unlock happens-before later lock | JLS, Synchronization / Happens-before | 17.1, 17.4.5 | Why the synchronized counter is correct |
| `AtomicInteger.incrementAndGet` is an atomic update | Java SE API docs, `java.util.concurrent.atomic.AtomicInteger` and package summary | class + package Javadoc | Atomic read-modify-write guarantee (CAS internals out of scope: Aastha) |
| Race conditions and read-modify-write atomicity | Goetz et al., Java Concurrency in Practice (2006) | Ch. 2 (2.2 Atomicity), page NOT VERIFIED | Textbook treatment of the same counter bug |
| Concurrency stress/litmus testing tool | OpenJDK jcstress project (openjdk.org/projects/code-tools/jcstress/) | project page | What jcstress is for; not run by us |