#!/usr/bin/env python3
"""Module 03 - convert JMH JSON results into a CSV and (optionally) a chart.

Usage:
  python scripts/analyze_jmh.py results/raw/m03_exp1.json results/raw/m03_exp2.json

Writes next to each input:  <name>.csv  and, if matplotlib is installed, <name>.png.
It only reformats numbers JMH produced; it invents nothing. Columns:
  benchmark, param, working_set_bytes, logical_reads_per_invocation, jmh_score, jmh_unit,
  ns_per_read, ci99_9_half_width, ns_per_read_ci, forks, warmup_it, measure_it,
  fork_mean_min, fork_mean_max, jmh_version, jdk, vm_args
ns_per_read:
  * SequentialVsRandomBenchmark: score (ns/invocation) / size   (one invocation = size reads)
  * WorkingSetBenchmark: score is already ns/hop (OperationsPerInvocation = HOPS)
Between-fork spread (fork_mean_min/max) is reported because iterations inside one JVM fork are
not independent samples; the fork means are the more honest view of run-to-run consistency.
"""
import csv, json, os, sys

def num(x):
    return x if isinstance(x, (int, float)) and x == x else None

def short(name):
    return name.rsplit(".", 1)[-1]

def rows_from(path):
    with open(path, encoding="utf-8") as f:
        data = json.load(f)
    out = []
    for e in data:
        pm = e["primaryMetric"]
        cls, meth = e["benchmark"].rsplit(".", 1)
        params = e.get("params", {})
        score, err = pm["score"], num(pm.get("scoreError"))
        if cls.endswith("SequentialVsRandomBenchmark"):
            size = int(params["size"])
            reads = size
            ws = size * 4 * 2            # data[] + idx[] payload (direct variant touches only data[])
            if meth == "sequentialDirect":
                ws = size * 4
            npr = score / reads
            npr_err = (err / reads) if err is not None else None
            p = f"size={size}"
        elif cls.endswith("WorkingSetBenchmark"):
            kib = int(params["workingSetKiB"])
            ws = kib * 1024
            reads = 1 << 18              # must match WorkingSetBenchmark.HOPS
            npr, npr_err = score, err
            p = f"workingSetKiB={kib}"
        else:
            continue
        raw = pm.get("rawData") or []
        fork_means = [sum(x) / len(x) for x in raw if x]
        if cls.endswith("SequentialVsRandomBenchmark"):
            fork_means = [m / reads for m in fork_means]
        out.append({
            "benchmark": short(cls) + "." + meth, "param": p, "working_set_bytes": ws,
            "logical_reads_per_invocation": reads,
            "jmh_score": score, "jmh_unit": pm["scoreUnit"], "ns_per_read": npr,
            "ci99_9_half_width": err, "ns_per_read_ci": npr_err,
            "forks": e.get("forks"), "warmup_it": e.get("warmupIterations"),
            "measure_it": e.get("measurementIterations"),
            "fork_mean_min": min(fork_means) if fork_means else None,
            "fork_mean_max": max(fork_means) if fork_means else None,
            "jmh_version": e.get("jmhVersion"), "jdk": e.get("jdkVersion"),
            "vm_args": " ".join(e.get("jvmArgs", [])),
        })
    return out

def write_csv(rows, path):
    if not rows:
        print("no matching benchmarks in", path); return
    with open(path, "w", newline="", encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=list(rows[0].keys()))
        w.writeheader(); w.writerows(rows)
    print("wrote", path)

def chart(rows, path):
    try:
        import matplotlib
        matplotlib.use("Agg")
        import matplotlib.pyplot as plt
    except ImportError:
        print("matplotlib not installed - CSV only (pip install matplotlib for charts)"); return
    series = {}
    for r in rows:
        series.setdefault(r["benchmark"], []).append(r)
    fig, ax = plt.subplots(figsize=(8, 5))
    for name, rs in sorted(series.items()):
        rs.sort(key=lambda r: r["working_set_bytes"])
        xs = [r["working_set_bytes"] / 1024 for r in rs]
        ys = [r["ns_per_read"] for r in rs]
        es = [r["ns_per_read_ci"] or 0 for r in rs]
        ax.errorbar(xs, ys, yerr=es, marker="o", capsize=3, label=name)
    ax.set_xscale("log", base=2); ax.set_yscale("log")
    ax.set_xlabel("working-set payload (KiB, log2)"); ax.set_ylabel("ns per logical read (log)")
    ax.set_title("JMH result - error bars: JMH 99.9% CI half-width"); ax.legend(fontsize=7); ax.grid(True, which="both", alpha=.3)
    fig.tight_layout(); fig.savefig(path, dpi=150); print("wrote", path)

if __name__ == "__main__":
    if len(sys.argv) < 2:
        sys.exit(__doc__)
    for p in sys.argv[1:]:
        rows = rows_from(p)
        base = os.path.splitext(p)[0]
        write_csv(rows, base + ".csv")
        if rows:
            chart(rows, base + ".png")
