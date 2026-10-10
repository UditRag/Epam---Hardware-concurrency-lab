#!/usr/bin/env python3
"""Module 12 analysis: JMH JSON -> summary CSVs + charts. Usage: analyze_m12.py <dir> [tag=final]
Throughput: uses the CONSUMER row of each group (= completed transfers); the group aggregate
sums producer+consumer and would double count. Latency: uses the 'ping' row (round trip); one-way
~ RTT/2 (assumption: symmetric legs). Nothing is estimated or filled in; missing input -> error."""
import json, sys, csv
from pathlib import Path
import matplotlib; matplotlib.use("Agg")
import matplotlib.pyplot as plt

d = Path(sys.argv[1]); tag = sys.argv[2] if len(sys.argv) > 2 else "final"
tp = json.loads((d / f"m12_jmh_SpscThroughputBenchmark_{tag}.json").read_text())
lt = json.loads((d / f"m12_jmh_SpscLatencyBenchmark_{tag}.json").read_text())

def metrics(r):
    n = r["benchmark"].split(".")[-1]
    yield n, r["primaryMetric"]
    for k, v in r.get("secondaryMetrics", {}).items():
        yield f"{n}:{k}", v

rows = []
for r in tp:
    for name, pm in metrics(r):
        if ":" not in name or not name.endswith("Consume"): continue
        grp = name.split(":")[0]
        fm = [sum(f) / len(f) for f in pm["rawData"]]
        rows.append(dict(group=grp, capacity=int(r["params"]["capacity"]), score_ops_s=pm["score"],
                         ci999_half=pm["scoreError"], fork_min=min(fm), fork_max=max(fm), forks=len(fm),
                         iters_per_fork=len(pm["rawData"][0]), unit=pm["scoreUnit"]))
if not rows: sys.exit("no throughput consumer rows found - check JSON")
rows.sort(key=lambda x: (x["capacity"], x["group"]))
with open(d / f"m12_throughput_summary_{tag}.csv", "w", newline="") as f:
    w = csv.DictWriter(f, rows[0].keys()); w.writeheader(); w.writerows(rows)

lrows = []
for r in lt:
    for name, pm in metrics(r):
        if name != "pingPong:ping": continue
        p = pm["scorePercentiles"]
        lrows.append(dict(impl=r["params"]["impl"], mean_rtt_ns=pm["score"], p50_rtt_ns=p["50.0"],
                          p99_rtt_ns=p["99.0"], p999_rtt_ns=p["99.9"], max_rtt_ns=p["100.0"],
                          p50_oneway_ns_approx=p["50.0"] / 2, forks=r["forks"]))
if not lrows: sys.exit("no latency ping rows found - check JSON")
with open(d / f"m12_latency_summary_{tag}.csv", "w", newline="") as f:
    w = csv.DictWriter(f, lrows[0].keys()); w.writeheader(); w.writerows(lrows)

# chart 1: throughput by capacity
fig, ax = plt.subplots(figsize=(6.5, 3.4)); caps = sorted({x["capacity"] for x in rows})
groups = sorted({x["group"] for x in rows}); wd = 0.8 / len(groups)
for i, g in enumerate(groups):
    vals = [next(x for x in rows if x["group"] == g and x["capacity"] == c) for c in caps]
    ax.bar([k + i * wd for k in range(len(caps))], [v["score_ops_s"] / 1e6 for v in vals], wd,
           yerr=[v["ci999_half"] / 1e6 for v in vals], capsize=3, label=g)
ax.set_xticks([k + wd * (len(groups) - 1) / 2 for k in range(len(caps))]); ax.set_xticklabels([f"cap {c}" for c in caps])
ax.set_ylabel("transfers/s (millions)"); ax.set_title("SPSC transfer throughput (1P+1C, JMH; error = 99.9% CI)")
ax.legend(); fig.tight_layout(); fig.savefig(d / f"m12_throughput_{tag}.png", dpi=150)

# chart 2: round-trip percentiles
fig, ax = plt.subplots(figsize=(6.5, 3.4)); keys = ["p50_rtt_ns", "p99_rtt_ns", "p999_rtt_ns"]
wd = 0.8 / len(lrows)
for i, x in enumerate(lrows):
    ax.bar([k + i * wd for k in range(3)], [x[k] for k in keys], wd, label=x["impl"])
ax.set_xticks([k + wd * (len(lrows) - 1) / 2 for k in range(3)]); ax.set_xticklabels(["p50", "p99", "p99.9"])
ax.set_yscale("log"); ax.set_ylabel("round-trip ns (log)"); ax.set_title("Ping-pong round-trip latency (cap 64, depth 1)")
ax.legend(); fig.tight_layout(); fig.savefig(d / f"m12_latency_{tag}.png", dpi=150)
print("wrote summaries + charts for tag", tag)
