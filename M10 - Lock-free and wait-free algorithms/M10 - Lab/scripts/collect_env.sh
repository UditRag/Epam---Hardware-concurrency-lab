#!/usr/bin/env bash
# Module 03 - environment collection for Linux. Output: results/raw/m03_environment.txt
out=results/raw/m03_environment.txt; mkdir -p results/raw
{
echo "Collected: $(date -Is)"
echo "=== CPU ==="; lscpu
echo "=== Caches (lscpu -C) ==="; lscpu -C 2>/dev/null || lscpu --caches 2>/dev/null
echo "=== sysfs cache of cpu0 (level/type/size/line size/sharing) ==="
for d in /sys/devices/system/cpu/cpu0/cache/index*; do
  printf '%s level=%s type=%s size=%s line=%sB shared_with=%s\n' "$(basename "$d")" \
    "$(cat "$d/level")" "$(cat "$d/type")" "$(cat "$d/size")" "$(cat "$d/coherency_line_size")" "$(cat "$d/shared_cpu_list")"
done
echo "=== getconf ==="; getconf -a 2>/dev/null | grep -i -E "CACHE"
echo "=== OS ==="; uname -a; cat /etc/os-release 2>/dev/null
echo "=== RAM ==="; grep -E "MemTotal" /proc/meminfo
echo "=== Governor ==="; cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor 2>/dev/null || echo "n/a"
echo "=== Java ==="; java -version 2>&1
java -XX:+PrintFlagsFinal -version 2>/dev/null | grep -E "MaxHeapSize|InitialHeapSize|UseG1GC|UseParallelGC|UseSerialGC|UseLargePages|UseTransparentHugePages|AlwaysPreTouch"
echo "=== Maven ==="; mvn -v 2>&1
} > "$out" 2>&1
echo "Wrote $out"
