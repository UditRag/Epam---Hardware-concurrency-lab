# Module 03 - environment collection for Windows PowerShell. NOT RUN by the module author.
# Usage (from project root):  powershell -ExecutionPolicy Bypass -File scripts\collect_env.ps1
$out = "results\raw\m03_environment.txt"
New-Item -ItemType Directory -Force -Path "results\raw" | Out-Null
function Section($t) { "`r`n=== $t ===" | Out-File -Append -Encoding utf8 $out }
"Collected: $(Get-Date -Format o)" | Out-File -Encoding utf8 $out
Section "CPU (Win32_Processor; L2/L3 sizes are KB as reported by Windows - cross-check with Coreinfo)"
Get-CimInstance Win32_Processor | Format-List Name,Manufacturer,NumberOfCores,NumberOfLogicalProcessors,MaxClockSpeed,L2CacheSize,L3CacheSize | Out-String | Out-File -Append -Encoding utf8 $out
Section "Win32_CacheMemory (may be incomplete on hybrid CPUs)"
Get-CimInstance Win32_CacheMemory | Format-Table DeviceID,Purpose,InstalledSize,Associativity,LineSize -AutoSize | Out-String | Out-File -Append -Encoding utf8 $out
Section "Coreinfo -c (Sysinternals; per-logical-CPU cache map incl. line size). Install from Microsoft Sysinternals, put on PATH."
if (Get-Command coreinfo -ErrorAction SilentlyContinue) { coreinfo -accepteula -c 2>&1 | Out-File -Append -Encoding utf8 $out } else { "coreinfo not found on PATH" | Out-File -Append -Encoding utf8 $out }
Section "OS"
Get-CimInstance Win32_OperatingSystem | Format-List Caption,Version,BuildNumber,OSArchitecture | Out-String | Out-File -Append -Encoding utf8 $out
Section "RAM"
Get-CimInstance Win32_ComputerSystem | Format-List TotalPhysicalMemory | Out-String | Out-File -Append -Encoding utf8 $out
Get-CimInstance Win32_PhysicalMemory | Format-Table Manufacturer,Capacity,Speed,ConfiguredClockSpeed -AutoSize | Out-String | Out-File -Append -Encoding utf8 $out
Section "Power plan and battery (laptop: record AC vs battery)"
powercfg /getactivescheme 2>&1 | Out-File -Append -Encoding utf8 $out
Get-CimInstance Win32_Battery -ErrorAction SilentlyContinue | Format-List BatteryStatus,EstimatedChargeRemaining | Out-String | Out-File -Append -Encoding utf8 $out
Section "Java"
(cmd /c "java -version 2>&1") | Out-File -Append -Encoding utf8 $out
(cmd /c "java -XX:+PrintFlagsFinal -version 2>&1") | Select-String -Pattern "MaxHeapSize|InitialHeapSize|UseG1GC|UseParallelGC|UseSerialGC|UseLargePages|UseTransparentHugePages|AlwaysPreTouch" | Out-File -Append -Encoding utf8 $out
Section "Maven"
(cmd /c "mvn -v 2>&1") | Out-File -Append -Encoding utf8 $out
Write-Host "Wrote $out"
