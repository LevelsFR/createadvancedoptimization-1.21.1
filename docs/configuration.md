# Configuration

Create: Advanced Optimization uses a server config only.

## General

`[general]`

- `enabled = true`
  Master switch for the mod's server logic.
- `debugLogging = false`
  Enables additional debug logging around diagnostics and cache behavior.

## Diagnostics

`[diagnostics]`

- `enabled = true`
  Enables commands, profiling, reports, and package diagnostics.
- `lagSpikeThresholdMs = 50`
  MSPT threshold used to count lag spikes in reports.
- `profileDefaultDurationSeconds = 30`
  Default profiling window for admin workflows.
- `packageEntityWarningThreshold = 200`
  Logs a warning when active Create `PackageEntity` counts exceed this value.

## Optimizations

`[optimizations.divingBoots]`

- `enabled = true`
  Skips redundant `HeavyBoots` marker writes and removals when the marker is already in the desired state.

`[optimizations.spoutRecipeCache]`

- `enabled = true`
  Enables the bounded local spout recipe cache for official Create filling lookups.
- `maxEntriesPerSpout = 4`
  Maximum cached lookup states stored per spout.

`[optimizations.processingRecipeMemoization]`

- `enabled = true`
  Enables one-tick memoization for Basin and Mechanical Crafter recipe windows.

`[optimizations.belts]`

- `tickFastPaths = true`
  Reuses the block state already maintained by each belt block entity and skips temporary entity-passenger cleanup allocation when a moving controller has no passengers. This does not skip ticks or alter transported-item cadence.

`[optimizations.beltFunnels]`

- `fastRejectOversizedExactInsertions = true`
  Avoids redundant stack copies when a blocking belt funnel already knows the carried stack is too small for its exact extraction amount.

`[optimizations.deployers]`

- `fastRejectFullHandInsertions = true`
  Avoids redundant copy/split work when belt or inventory insertion attempts hit a deployer hand that is already full.

`[optimizations.experimentalPackages]`

- `enabled = false`
  Reserved for future `PackageEntity` behavior experiments. V1.4 keeps package handling diagnostic-only by default.

## Notes

- Options below are either active in the current release or explicitly marked as reserved.
- The spout cache is enabled by default because the current implementation only memoizes bounded official Create filling lookups and still leaves dynamic external capability-based filling uncached.
- The belt tick fast paths are enabled by default because they remove redundant lookup/allocation work without delaying any belt tick.
- The experimental package section is intentionally off by default and currently only exposes its reserved status flag; package handling remains diagnostic-only.
- No client config is generated.
