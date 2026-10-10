# Module 07 — Source and Claim Register

| Source | Relevant section | Claim supported | URL |
|---|---|---|---|
| Java Language Specification, Java SE 17, Oracle | §17.4, Memory Model; §17.4.5, Happens-before Order; §17.4.7, Well-Formed Executions | Java memory model, happens-before and execution consistency are language-level rules; allowed outcomes are not inferred solely from one machine's observation. | https://docs.oracle.com/javase/specs/jls/se17/html/jls-17.html |
| OpenJDK JCStress | Project README and usage documentation | JCStress is a harness for testing concurrency behavior and allowed outcomes on JVMs/platforms. | https://github.com/openjdk/jcstress |
| Java Language Specification, Java SE 17 | §8.3.1.4, volatile Fields; §17.4.5 | Volatile field semantics and synchronization-order constraints. | https://docs.oracle.com/javase/specs/jls/se17/html/jls-8.html#jls-8.3.1.4 |
| Java Virtual Machine Specification, Java SE 17 | §2.11.1, Types and the Java Virtual Machine; implementation-specific instruction details must be taken from the relevant JVM/CPU documentation | JVM bytecode and implementation are distinct from source-level program order. | https://docs.oracle.com/javase/specs/jvms/se17/html/ |

## Claim discipline

- Use the JLS for Java guarantees; do not use a benchmark as the source of a language guarantee.
- JCStress results support statements about what was observed on the tested setup, not universal claims about all JVMs or CPUs.
- A claim about a particular barrier, instruction, store buffer, or CPU pipeline needs generated-code evidence plus the applicable vendor architecture documentation. No such host-specific evidence is bundled here.
