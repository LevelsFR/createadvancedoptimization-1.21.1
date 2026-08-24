# Create: Advanced Optimization

Create: Advanced Optimization is a NeoForge server-side optimization and diagnostics mod for Create on Minecraft 1.21.1.

It targets Create `6.0.10` specifically and focuses on reducing Create-related TPS drops and lag spikes caused by large factories, processing chains, logistics, packages, trains, and contraptions while preserving normal gameplay behavior.

## Target

- Minecraft: `1.21.1`
- Loader: `NeoForge`
- Java: `21`
- Create: `6.0.10`
- Mod ID: `createadvancedoptimization`

## Server-side focus

This mod is intentionally server-oriented.

It does not optimize:

- shaders
- rendering
- client FPS
- Embeddium
- Sodium
- Iris
- other purely graphical client features

## V1.4 features

- strict Create `6.0.10` compatibility guard
- server config with per-feature toggles
- lightweight profiler with `/createadvancedoptimization` and `/cao` commands
- Create diagnostics reports written to `logs/createadvancedoptimization/reports/` as unique `.txt` debriefs plus richer local `.html` reports
- `PackageEntity` monitoring with per-dimension tracking and chunk hotspots on demand
- conservative Diving Boots marker-write cleanup without persistent entity-side memoization
- bounded local spout lookup cache for official Create filling recipe checks, enabled by default
- one-tick Basin and Mechanical Crafter recipe memoization with dimension-aware keys and nullable Crafter results
- belt funnel and deployer micro-optimizations for redundant stack-copy hot paths seen in Spark profiles
- per-segment belt tick fast path that reuses Minecraft's block-entity state instead of repeating a level lookup
- empty belt passenger fast path that avoids a temporary removal-list allocation on moving controllers with no entity passengers
- separate profiler categories for non-controller segments, controller ticks, empty inventories, and active transported-item inventories
- on-demand `/cao belts scan [limit]` diagnostics for loaded belt segments and busiest controllers
- cache and optimization effectiveness counters in `/cao status` and `/cao stats`
- PackageEntity age, stationary-candidate, and top-chunk diagnostics for logistics-heavy servers
- disabled-by-default experimental package config placeholder for future behavior work

## What the mod does not do

- no global "every other tick" throttling
- no forced slowdown of belts, funnels, fluids, machines, trains, or contraptions
- no async access to live world data
- no redistribution or patching of the Create jar itself
- no client-only config

## Installation

1. Install NeoForge for Minecraft `1.21.1`.
2. Install Create `6.0.10`.
3. Place the generated `createadvancedoptimization` jar on the server with Create.
4. Configure server settings through the generated server config if needed.

## Compatibility

- Designed for large NeoForge modpacks.
- Does not require Cobblemon, Pehkui, KubeJS, ModernFix, FastSuite, Create Lazy Tick, Create Ergonomic, or other Create addons.
- The mod only targets Create APIs and methods directly and does not intentionally manipulate third-party mod classes.

## License

This project is `All Rights Reserved`.

You may still:

- download and use the mod normally
- include the compiled jar in modpacks
- run it on public or private servers

You may not redistribute modified versions or reuse the code, assets, branding, or logo without permission.

See `LICENSE` for the full terms.

## Experimental caution

V1.4 keeps risky behavior-changing ideas out of active optimization paths unless they are proven safe. Belt cadence, item speed, throughput, logistics, trains, redstone links, and contraptions are never throttled by the new belt fast paths.

## Documentation

- `docs/configuration.md`
- `docs/commands.md`
- `CHANGELOG.md`
