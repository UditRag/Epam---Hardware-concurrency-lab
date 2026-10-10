# Review verification record — 2026-10-09

This record separates checks actually performed in the review container from checks that require the team’s Windows machine and a Maven installation. The review container is Debian/Linux with OpenJDK 21.0.12.1; it is not the team’s i7-13700HX Windows environment.

## Actually run in the review container

| Check | Status | Evidence / interpretation |
|---|---|---|
| Compile Module 01 and Module 02 Java main sources plus `cache.AccessPatterns` | RUN AND PASSED | `javac -Xlint:all -d /tmp/epam-java-check/classes src/main/java/fundamentals/*.java src/main/java/synchronization/*.java src/main/java/cache/AccessPatterns.java`; no compiler diagnostics. |
| Manual `AccessPatterns` smoke harness | RUN AND PASSED | Verified shuffled permutation, shuffle differs from identity, same checksum for sequential/shuffled/direct sums, single-cycle permutation, full-cycle chase, KiB sizing, payload arithmetic, deterministic seed. Output: `PASS: helper smoke checks (permutation, checksum equivalence, full cycle, chase, size arithmetic, reproducible shuffle)`. Harness was temporary and is not part of the deliverable. |
| `scripts/analyze_jmh.py` syntax | RUN AND PASSED | `python -m py_compile scripts/analyze_jmh.py`; output: `PASS: analyze_jmh.py syntax check`. This does not validate parsing against real JMH JSON. |
| `pom.xml` XML syntax | RUN AND PASSED | Parsed using Python `xml.etree.ElementTree`; output: `PASS: pom.xml is well-formed XML`. This is not a Maven build. |
| `synchronization.OrderedLockDemo` | RUN AND PASSED | Output: `Expected: 20000`, `Completed: 20000`, `PASS: all operations completed`. |
| `synchronization.ReentrantLockDemo` | RUN AND PASSED | Thread 2 timed out after 1 second, Thread 1 released the lock, and demo completed. This is a timed `tryLock` demonstration, not a reentrancy demonstration. |
| `synchronization.DeadlockDemo` | RUN AND OBSERVED AS INTENTIONAL DEADLOCK | Process was terminated by a 3-second timeout (exit 124); output shows each thread acquired one lock and then waited for the other. A separate `jcmd <pid> Thread.print` capture contained `Found one Java-level deadlock`. This was intentionally terminated after capture. |
| Maven executable | BLOCKED | `mvn -version` returned `mvn: command not found`; downloading a Maven distribution from this environment also failed. |

## Not run / pending on the team machine

- `mvn clean test`, full Maven compilation, and real JUnit execution (including `AccessPatternsTest`).
- Confirmation that `target/classes/META-INF/BenchmarkList` exists and JMH lists both benchmark classes.
- JMH smoke test and both full benchmark suites, repeated three times.
- Environment collection from the actual Windows machine, including Coreinfo output for per-core cache topology and line size.
- Real JMH JSON parsing, CSV generation and chart creation.
- Hardware performance-counter profiling with a tool/event set confirmed for the actual CPU and core type.

No Module 03 benchmark numbers are included in this record. All results remain PENDING until real JSON/log output is collected.
