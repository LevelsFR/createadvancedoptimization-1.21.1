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

- `/createadvancedoptimization profile start [seconds]`
- `/cao profile start [seconds]`

Starts a lightweight Create-focused profiling session.

When `seconds` is omitted, the configured `diagnostics.profileDefaultDurationSeconds` value is used.

When the requested duration ends, the profiler stops automatically and immediately exports:

- a unique `.txt` debrief
- a unique local `.html` report
- a machine-readable `.json` report
- a styled chat message with quick actions for the exported report

- `/createadvancedoptimization profile stop`
- `/cao profile stop`

Stops the current profiling session.

Stopping manually also exports a fresh `.txt`, `.html`, and `.json` report set.

## Report

- `/createadvancedoptimization report`
- `/cao report`
- `/createadvancedoptimization report last`
- `/cao report last`
Writes a fresh report export to:

- `logs/createadvancedoptimization/reports/`

Each export creates a dedicated subfolder so the files from repeated sessions never mix together.

Each export now writes:

- a `.txt` debrief
- a richer `.html` report
- a machine-readable `.json` report

The JSON report contains the session metrics, hotspot methods and families, package diagnostics, optimization counters, alerts, and recommendations.

The report's peak and spawned PackageEntity values are measured for that profiling session; using `/cao reset` before every profile is not required.

- `/createadvancedoptimization report compare`
- `/cao report compare`

Compares the two most recent JSON reports and displays the MSPT, lag-spike, profiled Create time, and top-hotspot deltas.

The comparison also provides direct buttons for opening the older and newer HTML reports when running on an integrated or local server.

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

The in-game chat output now uses compact action buttons:

- `Open HTML`
- `Open TXT`
- `Open JSON`
- `Open Report Folder`

The buttons use Minecraft's native local-file action. They work when the client can access the same filesystem as the server, such as an integrated or local development server.

On a remote dedicated server, the report files remain on the server machine; vanilla chat cannot directly open a server-side file on a player's computer.

## Unified diagnosis

- `/createadvancedoptimization diagnose`
- `/cao diagnose`

Combines the latest profiler signal, PackageEntity status, and belt scan into a short administrator summary. It deliberately reports signals and follow-up commands rather than claiming a single proven cause.

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
