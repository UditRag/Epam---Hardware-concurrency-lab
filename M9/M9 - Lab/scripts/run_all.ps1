# Compile and run every Task 09 program (Windows PowerShell). Run from the project root.
New-Item -ItemType Directory -Force target\classes, results\raw | Out-Null
javac -d target\classes (Get-ChildItem src\main\java\cas\*.java).FullName
java -cp target\classes cas.CASExample                      | Tee-Object results\raw\run_cas_example.txt
java -cp target\classes cas.CASRetryCounter                 | Tee-Object results\raw\run_cas_retry_counter.txt
java -cp target\classes cas.AbaDemo                         | Tee-Object results\raw\run_aba_demo.txt
java -cp target\classes cas.CounterComparison 2 100000 5    | Tee-Object results\raw\run_counter_comparison_2t_100k.txt
java -cp target\classes cas.CounterComparison 8 1000000 10  | Tee-Object results\raw\run_counter_comparison_8t_1M.txt
java -cp target\classes cas.CasContentionDemo 8 1000000     | Tee-Object results\raw\run_cas_contention_demo.txt
