# Module 08 — environment of the runs archived as `m08_container_*` (NOT the team machine)

These three files were produced by the AI authoring assistant in its sandbox container on 2026-10-10. They are smoke-test evidence only. They are NOT the team's evidence and must not be presented as measurements from the i7-13700HX machine.

| Item | Value |
|---|---|
| OS / arch | Linux (Ubuntu 24.04 userland), x86_64 |
| CPU | Intel(R) Xeon(R) Processor @ 2.10GHz (virtual machine) |
| Logical CPUs visible to the JVM | 1 (`Runtime.availableProcessors()` printed 1) |
| JDK | OpenJDK 21.0.10 (JRE with `jdk.compiler` module; no Maven, no JUnit jars, no JCStress jar, no network) |
| Compile command | `java -m jdk.compiler/com.sun.tools.javac.Main -source 17 -target 17 -Xlint:all,-options -d <dir> src/main/java/fundamentals/*.java src/main/java/memorymodel/*.java` (exit 0, no diagnostics) |
| Why `-source/-target` and not `--release 17` | this JRE has no `ct.sym`; `--release 17` was rejected, so the Java 17 API surface was NOT checked by the compiler |
| Interpretation limit | with 1 logical CPU the two threads time-slice; data-race outcomes are much less likely to appear than on a multi-core machine. "No stale value / no lost update observed" here says almost nothing |

Files: `m08_container_publication_experiment.txt`, `m08_container_volatile_counter.txt`, `m08_container_stub_runner_output.txt` (the last was produced by a hand-written minimal runner against stub annotations, NOT by JUnit).
