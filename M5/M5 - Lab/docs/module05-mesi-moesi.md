# Module 05: MESI and MOESI State Diagrams

**Owner:** Vivansh  **Follows:** Module 04 (Prachi Dubey)  **Date:** 2026-10-09
**Evidence labels:** EXTERNAL EVIDENCE, THEORETICAL, PENDING LOCAL VERIFICATION

> Status: teaching-model documentation plus a Java simulator. No hardware measurement and no claim about the team's CPU.

## A. Scope
In scope: MESI and MOESI stable states, processor-side and snoop-side transitions, the three Module 04 traces with exact states, and a runnable model of them. Out of scope: cache hierarchy (Module 03), the coherence introduction (Module 04), false-sharing and counter measurements (benchmark owner), memory fences and CAS.

## B. MESI stable states (EXTERNAL EVIDENCE: Intel SDM Vol. 3A, Cache Control Protocol)
| State | Other valid copies | Relation to memory | Local read | Local write |
|---|---|---|---|---|
| M Modified | None | Cache is newer, memory stale | Hit | Hit, no bus traffic |
| E Exclusive | None | Clean | Hit | Hit, silently becomes M |
| S Shared | May exist | Clean (basic MESI) | Hit | Needs permission, invalidates others |
| I Invalid | Others may hold copies | Local copy unusable | Miss | Miss, fetch with write permission |

Invariants: M and E are exclusive; S allows many readers; I cannot serve loads; a store to a shared line cannot leave other copies treated as current; a hardware line state does not map one-to-one to a Java field.

## C. Diagrams
See `diagrams/`: `01-bus-snooping-overview.png`, `02-mesi-processor-side.png`, `03-mesi-snoop-side.png`, `04-moesi-snoop-side.png`.

## D. MESI transition table (derived from `SnoopingBus`, THEORETICAL)
| Side | Current | Event | Bus action | Next |
|---|---|---|---|---|
| Processor | M | PrRd | None (hit) | M |
| Processor | M | PrWr | None (hit) | M |
| Processor | E | PrRd | None (hit) | E |
| Processor | E | PrWr | None (hit, silent E to M) | M |
| Processor | S | PrRd | None (hit) | S |
| Processor | S | PrWr | BusUpgr; others invalidate | M |
| Processor | I | PrRd | BusRd; no sharer answers | E |
| Processor | I | PrWr | BusRdX | M |
| Snoop | M | BusRd seen | BusRd; C0 flushes + writes back | S |
| Snoop | M | BusRdX seen | BusRdX; C0 flushes + invalidates | I |
| Snoop | E | BusRd seen | BusRd; sharer signals shared | S |
| Snoop | E | BusRdX seen | BusRdX | I |
| Snoop | S | BusRd seen | BusRd; sharer signals shared | S |
| Snoop | S | BusRdX seen | BusRdX | I |
| Snoop | S | BusUpgr seen | BusUpgr; others invalidate | I |
| Snoop | I | BusRd seen | BusRd; no sharer answers | I |
| Snoop | I | BusRdX seen | BusRdX | I |

## E. MOESI: what Owned adds
In the teaching model an O cache holds dirty data while other caches may hold read-only copies, so a dirty line can be shared without an immediate write-back. Intel's core-to-core article describes the purpose of an Owned state (EXTERNAL EVIDENCE, explanatory, not normative). Arm AMBA ACE uses a different five-state model (UC, UD, SC, SD, I) and must not be relabelled as MOESI.

## F. MOESI transition table (derived from `SnoopingBus`, THEORETICAL)
| Side | Current | Event | Bus action | Next |
|---|---|---|---|---|
| Processor | M | PrRd | None (hit) | M |
| Processor | M | PrWr | None (hit) | M |
| Processor | O | PrRd | None (hit) | O |
| Processor | O | PrWr | BusUpgr; others invalidate | M |
| Processor | E | PrRd | None (hit) | E |
| Processor | E | PrWr | None (hit, silent E to M) | M |
| Processor | S | PrRd | None (hit) | S |
| Processor | S | PrWr | BusUpgr; others invalidate | M |
| Processor | I | PrRd | BusRd; no sharer answers | E |
| Processor | I | PrWr | BusRdX | M |
| Snoop | M | BusRd seen | BusRd; C0 flushes, stays dirty owner | O |
| Snoop | M | BusRdX seen | BusRdX; C0 flushes + invalidates | I |
| Snoop | O | BusRd seen | BusRd; C0 (owner) supplies | O |
| Snoop | O | BusRdX seen | BusRdX; C0 flushes + invalidates | I |
| Snoop | O | BusUpgr seen | BusUpgr; others invalidate | I |
| Snoop | E | BusRd seen | BusRd; sharer signals shared | S |
| Snoop | E | BusRdX seen | BusRdX | I |
| Snoop | S | BusRd seen | BusRd; sharer signals shared | S |
| Snoop | S | BusRdX seen | BusRdX | I |
| Snoop | S | BusUpgr seen | BusUpgr; others invalidate | I |
| Snoop | I | BusRd seen | BusRd; no sharer answers | I |
| Snoop | I | BusRdX seen | BusRdX | I |

## G. Three traces (output of `TraceRunner`)
See `results/raw/trace_mesi.txt` and `results/raw/trace_moesi.txt`. The only difference between protocols is Trace 2 step 2: MESI gives C0=S, C1=S with a write-back; MOESI gives C0=O, C1=S with no write-back.

## H. Verification performed
`InvariantCheck` ran 2,000,000 random operations per protocol (seed 42) on 4 caches: PASS for both (`results/raw/invariant_check.txt`). This checks the model's self-consistency only.

## I. Evidence and Java boundary
| Item | Status | Still needed |
|---|---|---|
| MESI definitions | EXTERNAL EVIDENCE | Cite exact SDM edition and section |
| Owned-state explanation | EXTERNAL EVIDENCE (conceptual) | Vendor spec defining MOESI before any host claim |
| Diagrams, tables, traces, simulator | THEORETICAL | Review each arrow against the chosen protocol |
| Target CPU and coherence counters | PENDING LOCAL VERIFICATION | CPU model, topology, validated counters |

Coherence keeps cached copies consistent. It does not make `count++` atomic and does not create Java happens-before (JLS Chapter 17, section 17.4.5). Keep protocol diagrams separate from Java synchronization claims.

## J. Reviewer Q&A
- Why does E to M need no bus request? E is the only valid copy, so there is nobody to invalidate.
- Why must S ask before a store? Other valid copies may exist and must be invalidated.
- M vs O? Both dirty. M is exclusive; O lets others share while one cache keeps the dirty-data duty.
- Does MOESI guarantee lower latency? No; it depends on workload, interconnect, topology and policy.
- Do these diagrams prove what our CPU does? No; they are teaching models until matched to the target architecture's documentation.
