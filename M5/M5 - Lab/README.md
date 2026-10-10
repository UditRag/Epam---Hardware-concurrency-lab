# Module 05: MESI and MOESI State Diagrams (Vivansh)

Follows Module 04 (Cache Coherence, Prachi Dubey). Module 04 left exact protocol states to this module.

- Report: `docs/module05-mesi-moesi.md` (also `docs/MESI_and_MOESI_Cache-Coherence_State_Diagrams.docx`)
- Diagrams: `diagrams/` (bus snooping overview, MESI processor side, MESI snoop side, MOESI snoop side)
- Code: `src/main/java/coherence` (`State`, `Protocol`, `SnoopingBus`, `TraceRunner`, `TransitionTableDump`, `InvariantCheck`)
- Raw output: `results/raw/` ; screenshots: `screenshots/`

## Run
```
javac -d out src/main/java/coherence/*.java
java -cp out coherence.TraceRunner [mesi|moesi]
java -cp out coherence.TransitionTableDump [mesi|moesi]
java -cp out coherence.InvariantCheck [operations]
```
Or `scripts/run.sh` / `scripts/run.ps1`. Needs JDK 17+ (no Maven, no dependencies).

## What each program shows
| Program | Shows |
|---|---|
| `TraceRunner` | The 3 event traces from Module 04 (read sharing, write after read, competing writers) with exact M/O/E/S/I states. Trace 2 step 2 is the MESI vs MOESI difference (S vs O). |
| `TransitionTableDump` | Full processor-side and snoop-side transition table, derived by driving the simulator (not typed by hand). |
| `InvariantCheck` | 2,000,000 random loads/stores/evictions per protocol on 4 caches; checks M/E exclusivity, single O, no O in MESI, no stale copy ever read, memory current when no dirty copy exists. |

## Evidence limitation (important)
- The simulator is a THEORETICAL snooping-bus teaching model. It has no transient states, directory or snoop filter.
- Output is NOT hardware evidence and says nothing about the team's CPU. `InvariantCheck` passing shows the model is self-consistent, not that any CPU behaves this way.
- Screenshots in `screenshots/` were rendered from output captured in a Claude sandbox (Java 21, 2026-10-09). Before submitting as your own work, re-run locally and replace them with real screenshots of your console.
- Status: Java run completed in sandbox. Maven/JUnit not used. Local run on team machine: PENDING. KT STATUS: PENDING.
