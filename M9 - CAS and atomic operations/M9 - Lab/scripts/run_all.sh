#!/usr/bin/env bash
# Compile and run every Task 09 program, saving raw output to results/raw/. Run from the project root.
set -e
mkdir -p target/classes results/raw
javac -d target/classes src/main/java/cas/*.java
java -cp target/classes cas.CASExample                       | tee results/raw/run_cas_example.txt
java -cp target/classes cas.CASRetryCounter                  | tee results/raw/run_cas_retry_counter.txt
java -cp target/classes cas.AbaDemo                          | tee results/raw/run_aba_demo.txt
java -cp target/classes cas.CounterComparison 2 100000 5     | tee results/raw/run_counter_comparison_2t_100k.txt
java -cp target/classes cas.CounterComparison 8 1000000 10   | tee results/raw/run_counter_comparison_8t_1M.txt
java -cp target/classes cas.CasContentionDemo 8 1000000      | tee results/raw/run_cas_contention_demo.txt
