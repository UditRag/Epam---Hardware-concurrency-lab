#!/usr/bin/env bash
# Usage: ./scripts/run.sh   (needs a JDK 17+; run from the project root)
set -e
mkdir -p out
javac -d out src/main/java/coherence/*.java
java -cp out coherence.TraceRunner
java -cp out coherence.TransitionTableDump
java -cp out coherence.InvariantCheck
