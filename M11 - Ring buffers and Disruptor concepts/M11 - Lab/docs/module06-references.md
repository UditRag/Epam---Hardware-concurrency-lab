# Module 06 references — False Sharing and Cache-Line Bouncing

These are background references for validating the report. Check that links and relevant sections remain current when preparing the final submission.

1. OpenJDK, *Java Object Layout (JOL)* — tools for inspecting JVM object layouts: https://openjdk.org/projects/code-tools/jol/
2. OpenJDK JMH — Java Microbenchmark Harness project and samples: https://github.com/openjdk/jmh
3. Linux perf documentation — `perf c2c` and cache-to-cache analysis (availability depends on hardware/kernel): https://man7.org/linux/man-pages/man1/perf-c2c.1.html
4. Intel, *64 and IA-32 Architectures Optimization Reference Manual* — consult the edition matching the target processor for cache hierarchy and optimization guidance: https://www.intel.com/content/www/us/en/developer/articles/technical/intel-sdm.html
5. Oracle, *Java Language Specification*, Java Memory Model: https://docs.oracle.com/javase/specs/ — useful for distinguishing language-level ordering/visibility from hardware cache-coherence behavior.
6. Existing project context: `module04-cache-coherence.md`, `module05-mesi-moesi-state-diagrams.md`, and `references.md`.

A reference to a general manual does not establish that a particular processor uses a specific protocol state or line size. Use the target CPU's authoritative documentation for such claims.
