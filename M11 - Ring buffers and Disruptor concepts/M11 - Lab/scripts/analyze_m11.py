#!/usr/bin/env python3
"""
analyze_m11.py — Module 11 JMH result parser.

STATUS: PARTIAL — tested against JMH 1.37 JSON schema structure.
Requires real JMH JSON output to fully validate. Do not use synthetic
or fabricated JSON as final evidence.

Usage:
    python scripts/analyze_m11.py results/raw/m11_jmh_raw.json

Output:
    - Console summary table
    - results/raw/m11_summary.csv (if --csv flag or auto-detected output dir)

JMH 1.37 JSON top-level schema (array of benchmark result objects):
[
  {
    "benchmark": "io.lnctqubit.hwconc.m11.benchmarks.SpscBenchmark.plainProduce",
    "mode": "thrpt",
    "threads": 1,
    "forks": 3,
    "warmupIterations": 5,
    "measurementIterations": 5,
    "primaryMetric": {
      "score": 12345678.9,
      "scoreError": 100000.0,
      "scoreUnit": "ops/s",
      "scorePercentiles": { "50.0": ..., "99.0": ... }
    },
    "params": { "plainCapacity": "1024" }
  },
  ...
]
"""

import json
import sys
import csv
import os
from pathlib import Path


M11_PREFIX = "io.lnctqubit.hwconc.m11.benchmarks"


def short_name(full_name: str) -> str:
    """Strip the package prefix for readability."""
    return full_name.replace(M11_PREFIX + ".", "")


def parse_jmh_json(path: str) -> list[dict]:
    with open(path, "r", encoding="utf-8") as f:
        data = json.load(f)
    if not isinstance(data, list):
        raise ValueError(f"Expected JSON array at top level, got {type(data).__name__}")
    return data


def extract_rows(results: list[dict]) -> list[dict]:
    rows = []
    for r in results:
        name = r.get("benchmark", "UNKNOWN")
        if M11_PREFIX not in name:
            continue  # skip non-M11 benchmarks if file contains mixed output
        mode = r.get("mode", "?")
        threads = r.get("threads", "?")
        forks = r.get("forks", "?")
        params = r.get("params", {})
        pm = r.get("primaryMetric", {})
        score = pm.get("score", float("nan"))
        error = pm.get("scoreError", float("nan"))
        unit = pm.get("scoreUnit", "?")
        rows.append({
            "benchmark": short_name(name),
            "mode": mode,
            "threads": threads,
            "forks": forks,
            "params": str(params),
            "score": score,
            "scoreError": error,
            "scoreUnit": unit,
        })
    return rows


def print_table(rows: list[dict]) -> None:
    if not rows:
        print("[analyze_m11] No M11 benchmark results found in file.")
        return
    header = f"{'Benchmark':<55} {'Capacity':>10} {'Score (ops/s)':>18} {'± Error':>15} {'Forks':>6}"
    print(header)
    print("-" * len(header))
    for r in rows:
        try:
            capacity = json.loads(r["params"].replace("'", '"')).get(
                "plainCapacity", r["params"].replace("'", '"')
            )
        except Exception:
            capacity = r["params"]
        print(
            f"{r['benchmark']:<55} {str(capacity):>10}"
            f" {r['score']:>18.0f} {r['scoreError']:>15.0f} {r['forks']:>6}"
        )


def write_csv(rows: list[dict], out_path: str) -> None:
    fieldnames = ["benchmark", "mode", "threads", "forks", "params",
                  "score", "scoreError", "scoreUnit"]
    with open(out_path, "w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=fieldnames)
        writer.writeheader()
        writer.writerows(rows)
    print(f"[analyze_m11] CSV written to: {out_path}")


def main() -> None:
    if len(sys.argv) < 2:
        print("Usage: python analyze_m11.py <path-to-jmh-json> [--csv <output.csv>]")
        print("Example: python scripts/analyze_m11.py results/raw/m11_jmh_raw.json")
        sys.exit(1)

    json_path = sys.argv[1]
    if not Path(json_path).exists():
        print(f"[analyze_m11] ERROR: file not found: {json_path}")
        sys.exit(1)

    csv_path = None
    if "--csv" in sys.argv:
        idx = sys.argv.index("--csv")
        if idx + 1 < len(sys.argv):
            csv_path = sys.argv[idx + 1]
    else:
        # Auto-detect: write CSV next to JSON
        csv_path = str(Path(json_path).with_suffix("")) + "_summary.csv"

    print(f"[analyze_m11] Parsing: {json_path}")
    try:
        results = parse_jmh_json(json_path)
    except (json.JSONDecodeError, ValueError) as e:
        print(f"[analyze_m11] ERROR parsing JSON: {e}")
        sys.exit(1)

    rows = extract_rows(results)
    print(f"[analyze_m11] Found {len(rows)} M11 benchmark result(s).\n")
    print_table(rows)

    if rows and csv_path:
        write_csv(rows, csv_path)


if __name__ == "__main__":
    main()
