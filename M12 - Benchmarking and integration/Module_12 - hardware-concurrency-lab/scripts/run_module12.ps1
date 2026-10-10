# Module 12 one-command evidence run. From hardware-concurrency-lab/ in PowerShell:
#   .\scripts\run_module12.ps1            # full (~35-45 min): tests + layout + JMH 3 forks
#   .\scripts\run_module12.ps1 -Quick     # smoke: 1 fork, 2 warmup, 3 measure (NOT final evidence)
param([switch]$Quick, [switch]$SkipTests)
$ErrorActionPreference = 'Stop'
$out = 'results/m12'; New-Item -ItemType Directory -Force $out | Out-Null
function Step($n) { Write-Host "=== $n ===" }

Step 'environment'
$env_md = "$out/m12_environment.md"
@(
 "Collected: $(Get-Date -Format o)",
 "## java -version", (cmd /c "java -version 2>&1"),
 "## mvn -v", (cmd /c "mvn -v 2>&1"),
 "## CPU", (Get-CimInstance Win32_Processor | Select-Object Name,NumberOfCores,NumberOfLogicalProcessors,MaxClockSpeed | Format-List | Out-String),
 "## OS/RAM", (Get-CimInstance Win32_OperatingSystem | Select-Object Caption,Version,TotalVisibleMemorySize | Format-List | Out-String),
 "## Power plan", (powercfg /getactivescheme),
 "## Cache line size (not queried; assume 64 B on x86 - verify with CPU-Z/coreinfo)"
) | Out-File -Encoding utf8 $env_md

if (-not $SkipTests) { Step 'mvn clean test'; mvn clean test 2>&1 | Tee-Object "$out/m12_mvn_test_output.txt" }

Step 'package (fat jar + BenchmarkList)'
mvn -q package -DskipTests 2>&1 | Tee-Object "$out/m12_mvn_package_output.txt"
if (-not (Test-Path target/benchmarks.jar)) { throw 'target/benchmarks.jar missing' }

Step 'layout (reflection offsets via Unsafe + JOL)'
$cp = 'target/classes'
java -cp $cp io.lnctqubit.hwconc.m12.tools.OffsetProbe 2>&1 | Out-File -Encoding utf8 "$out/m12_offset_probe.txt"
java -jar target/benchmarks.jar -h > $null  # jar sanity
java -cp target/benchmarks.jar io.lnctqubit.hwconc.m12.tools.JolLayout 2>&1 | Out-File -Encoding utf8 "$out/m12_jol_layout.txt"

if ($Quick) { $wi=2; $mi=3; $f=1; $tag='quick' } else { $wi=5; $mi=10; $f=3; $tag='final' }
foreach ($b in 'SpscThroughputBenchmark','SpscLatencyBenchmark') {
  Step "JMH $b ($tag)"
  java -jar target/benchmarks.jar "io.lnctqubit.hwconc.m12.benchmarks.$b" -wi $wi -i $mi -f $f `
       -rf json -rff "$out/m12_jmh_${b}_$tag.json" 2>&1 | Tee-Object "$out/m12_jmh_${b}_$tag.log"
}

Step 'analysis + charts'
python scripts/analyze_m12.py $out $tag
Write-Host "Done. Review $out; keep the 'quick' tag out of the final report."
