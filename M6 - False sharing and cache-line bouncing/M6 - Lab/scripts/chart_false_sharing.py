#!/usr/bin/env python3
"""Convert Module 06 JMH JSON files to CSV and a throughput chart.

Usage: python scripts/chart_false_sharing.py results/raw/m06/threads-*.json
Requires matplotlib only for PNG output. The script only uses JMH-reported scores/errors.
"""
import csv, glob, json, os, re, sys

def parse(path):
    with open(path, encoding='utf-8') as f: data=json.load(f)
    out=[]
    m=re.search(r'threads-(\d+)', os.path.basename(path))
    thread_count=int(m.group(1)) if m else None
    for item in data:
        if not item.get('benchmark','').endswith('FalseSharingBench.incrementOwnedSlot'): continue
        pm=item['primaryMetric']; params=item.get('params',{})
        stride=int(params.get('stride', -1))
        out.append({'raw_json_file':path,'thread_count':thread_count,'stride':stride,
                    'variant':'adjacent' if stride==1 else ('padded' if stride==16 else 'unknown'),
                    'throughput_ops_per_sec':pm.get('score'),'jmh_error':pm.get('scoreError'),
                    'jmh_score_unit':pm.get('scoreUnit'),'fork_count':item.get('forks'),
                    'warmup_iterations':item.get('warmupIterations'),
                    'measurement_iterations':item.get('measurementIterations'),
                    'jdk':item.get('jdkVersion')})
    return out

if __name__=='__main__':
    paths=[]
    for arg in sys.argv[1:]: paths.extend(glob.glob(arg))
    if not paths: sys.exit(__doc__)
    rows=[]
    for p in paths: rows.extend(parse(p))
    if not rows: sys.exit('No matching FalseSharingBench JSON entries found.')
    csvpath='results/raw/m06_false_sharing_results.csv'
    os.makedirs(os.path.dirname(csvpath),exist_ok=True)
    with open(csvpath,'w',newline='',encoding='utf-8') as f:
        w=csv.DictWriter(f,fieldnames=list(rows[0].keys())); w.writeheader(); w.writerows(rows)
    print('wrote',csvpath)
    try:
        import matplotlib; matplotlib.use('Agg')
        import matplotlib.pyplot as plt
    except ImportError:
        print('matplotlib not installed; CSV created. Install with: python -m pip install matplotlib'); sys.exit(0)
    fig,ax=plt.subplots(figsize=(8,5))
    for variant in ('adjacent','padded'):
        rs=sorted((r for r in rows if r['variant']==variant and r['thread_count'] is not None),key=lambda r:r['thread_count'])
        if rs:
            xs=[r['thread_count'] for r in rs]; ys=[r['throughput_ops_per_sec'] for r in rs]
            es=[r['jmh_error'] or 0 for r in rs]
            ax.errorbar(xs,ys,yerr=es,marker='o',capsize=3,label=variant)
    ax.set_xlabel('JMH worker thread count'); ax.set_ylabel('Throughput (ops/s)')
    ax.set_title('Module 06 false-sharing benchmark (JMH-reported error bars)')
    ax.legend(); ax.grid(True,alpha=.3); fig.tight_layout()
    png='results/raw/m06_false_sharing_throughput.png'; fig.savefig(png,dpi=160); print('wrote',png)
