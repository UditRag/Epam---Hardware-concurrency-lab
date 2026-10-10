# References - Task 09 (CAS and Atomic Operations)

| Claim | Source |
|---|---|
| `compareAndSet`, `incrementAndGet` semantics | Oracle, Java SE 21 API - AtomicInteger: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/atomic/AtomicInteger.html |
| Atomic operations on object references | Oracle, Java SE 21 API - AtomicReference: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/atomic/AtomicReference.html |
| Reference + stamp, ABA mitigation | Oracle, Java SE 21 API - AtomicStampedReference: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/atomic/AtomicStampedReference.html |
| Happens-before, visibility, ordering | Java Language Specification SE 21, Chapter 17 (Threads and Locks), 17.4.5: https://docs.oracle.com/javase/specs/jls/se21/html/jls-17.html |
| Microbenchmarking (not yet used) | OpenJDK JMH: https://github.com/openjdk/jmh |
