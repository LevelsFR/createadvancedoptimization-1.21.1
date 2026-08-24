# Commands

All commands require permission level `2`.

## Status

- `/createadvancedoptimization status`
- `/cao status`

Shows:

- loaded Create version
- profiler state
- package counts
- feature toggle states

## Profiling

- `/createadvancedoptimization profile start <seconds>`
- `/cao profile start <seconds>`

Starts a lightweight Create-focused profiling session.

When the requested duration ends, the profiler stops automatically and immediately exports:

- a unique `.txt` debrief
- a unique local `.html` report
- a styled chat message with quick actions for the exported report

- `/createadvancedoptimization profile stop`
- `/cao profile stop`

Stops the current profiling session.

Stopping manually also exports a fresh `.txt` and `.html` report pair.

## Report

- `/createadvancedoptimization report`
- `/cao report`
- `/createadvancedoptimization report last`
- `/cao report last`
Writes a fresh report export to:

- `logs/createadvancedoptimization/reports/`

Each export creates unique filenames so repeated sessions never overwrite the previous report.

Each export now writes:

- a `.txt` debrief
- a richer `.html` report

The report includes:

- timestamp
- session id
- requested duration
- a short verdict/debrief
- average MSPT
- max MSPT
- tick spikes above threshold
- grouped hotspot families
- top profiled Create methods
- per-tick call frequency metrics
- active and peak package counts
- package counts by dimension
- top package-heavy chunks at export time
- basic alerts

The HTML report is fully local and does not require any website or external upload.

The in-game chat output now uses compact action buttons such as:

- `Open HTML`
- `Open TXT`
- `Copy HTML Path`

These buttons avoid spamming the full file path in chat. `Open HTML` and `Open TXT` are most useful when the game client can access the exported file on the same machine.

## Package hotspots

- `/createadvancedoptimization packages top [limit]`
- `/cao packages top [limit]`

Scans loaded levels on demand and prints the most package-heavy chunks.

## Belt diagnostics

- `/createadvancedoptimization belts scan [limit]`
- `/cao belts scan [limit]`

Scans ticking loaded chunks on demand and prints:

- loaded belt segment and controller counts
- moving and item-carrying controller counts
- transported stack and entity-passenger totals
- top controllers ordered by transported stacks, passengers, then belt length

The scan does not maintain a persistent belt registry and does not initialize empty belt inventories.

## Reset

- `/createadvancedoptimization reset`
- `/cao reset`

Resets:

- active profiler sessions
- last completed profiler session
- package diagnostics counters
