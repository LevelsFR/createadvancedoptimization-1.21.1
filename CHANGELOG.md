# Changelog

## 1.4.0

# Create: Advanced Optimization v1.4

## Mechanical belts
- Added an enabled-by-default belt tick fast path that reuses the `BlockEntity`'s current cached `BlockState` instead of asking the level for the same state on every loaded belt segment every tick.
- Added an enabled-by-default empty-passenger fast path that skips Create's temporary removal-list allocation and empty map traversal on moving belt controllers with no entity passengers.
- Kept full tick cadence, transported-item speed, throughput, collision behavior, and belt-side interaction order unchanged.
- Added `optimizations.belts.tickFastPaths` as a single rollback toggle for both conservative belt tick changes.

## Belt diagnostics
- Split belt profiling into non-controller segment ticks and controller ticks.
- Split `BeltInventory.tick` profiling into empty and active transported-item inventories.
- Added `/cao belts scan [limit]` and `/createadvancedoptimization belts scan [limit]` for an on-demand scan of ticking loaded chunks.
- The scan reports segments, controllers, moving controllers, item-carrying controllers, transported stacks, entity passengers, and the busiest controller positions.
- The scan reads the controller inventory field directly and does not create empty inventories as a diagnostic side effect.

## Compatibility and validation
- Continued to target Minecraft `1.21.1`, NeoForge, Java `21`, and exact Create `6.0.10` compatibility.
- Rechecked the public Create distributions on August 25, 2026; `6.0.10` remains the latest released NeoForge build for Minecraft `1.21.1` while `6.0.11` is not yet a public release.
- Verified the local source-of-truth JAR reports Create `6.0.10` and Git hash `ac0c444d9828da3453ae8cc65338e8de063286fb`.

## 1.4.0-fr

# Create: Advanced Optimization v1.4

## Mechanical Belts
- Ajout d'un fast-path actif par défaut qui réutilise le `BlockState` courant déjà conservé par la `BlockEntity`, au lieu de relire le même état dans le niveau pour chaque segment à chaque tick.
- Ajout d'un fast-path actif par défaut qui évite l'allocation d'une liste temporaire et le parcours d'une map vide sur les contrôleurs sans entité passagère.
- La cadence, la vitesse des items, le débit, les collisions et l'ordre des interactions restent inchangés.
- Ajout de `optimizations.belts.tickFastPaths` pour désactiver ensemble ces deux optimisations conservatrices.

## Diagnostics belts
- Séparation du profiling entre segments non-contrôleurs et contrôleurs.
- Séparation du profiling de `BeltInventory.tick` entre inventaires vides et actifs.
- Ajout de `/cao belts scan [limit]` pour inspecter à la demande les chunks chargés qui tickent.
- Le scan n'initialise pas artificiellement les inventaires vides.

## Compatibilité
- Cible toujours Minecraft `1.21.1`, NeoForge, Java `21` et exactement Create `6.0.10`.
- Vérification des distributions publiques le 25 août 2026 : `6.0.10` reste la dernière release Create pour NeoForge 1.21.1 ; `6.0.11` n'est pas encore une release publique.

## 1.2.0

# Create: Advanced Optimization v1.2

## Performance
- Kept Spout recipe lookup caching active, but made disabled mode call Create's original `FillingBySpout` path without creating cache keys, stack copies, fluid copies, or cache maps.
- Kept one-tick Basin and Mechanical Crafter memoization with stricter dimension-aware keys and cache statistics so server owners can verify whether the caches are producing useful hits.
- Reduced Diving Boots optimization to redundant marker write/removal guards and removed the stale-prone no-boots `WeakHashMap` shortcut.

## Improvements
- Mechanical Crafter memoization now distinguishes a missing cache entry from a cached `null` recipe result.
- Basin memoization keys now include dimension, Create recipe cache key, operator type, operator/basin positions, heat proxy state, item snapshots, and fluid snapshots.
- Mechanical Crafter keys now include dimension and immutable grid snapshots, including item components.
- Added `/cao stats` and `/cao stats reset` alongside the existing reset command.
- Added `en_us` and `fr_fr` language files for command-facing keys.

## Diagnostics
- Added cache counters for Spout, Basin, and Mechanical Crafter lookups, hits, misses, evictions, invalidations, and negative results.
- `diagnostics.enabled = false` now stops PackageEntity event tracking, rescans, snapshot scans, and warning emission.
- Status output now shows cache hit/miss summaries separately from diagnostic-only profiler hooks.

## Fixes
- Fixed a Mechanical Crafter crash where a no-recipe `null` result could be stored with `result.copy()`.
- Added explicit recipe cache invalidation on datapack sync/reload and server lifecycle resets.
- Avoided reusing Spout cache entries across dimensions or recipe reload epochs.

## Compatibility
- Continued targeting Minecraft `1.21.1`, NeoForge `21.1.233`, Java `21`, and Create `6.0.10`.
- Updated the development Create jar path to the supplied Create `6.0.10` jar, with the previous local path retained as a fallback.
- Profiling mixins remain diagnostics only and are not presented as direct optimizations.

## 1.2.0-fr

# Create: Advanced Optimization v1.2

## Performance
- Le cache de recettes Spout reste actif, mais le mode desactive appelle maintenant le chemin original `FillingBySpout` de Create sans creer de cle, copie de stack, copie de fluide ou map de cache.
- La memoisation Basin et Mechanical Crafter reste limitee au tick courant, avec des cles tenant compte des dimensions et des statistiques pour verifier les hits reels.
- L'optimisation Diving Boots est reduite aux ecritures/suppressions redondantes du marqueur; le raccourci `WeakHashMap` potentiellement stale a ete retire.

## Improvements
- La memoisation Mechanical Crafter distingue maintenant une entree absente du cache d'un resultat `null` mis en cache.
- Les cles Basin incluent maintenant dimension, cle de cache recette Create, type d'operateur, positions operateur/basin, etat thermique proxy, items et fluides.
- Les cles Mechanical Crafter incluent maintenant dimension et snapshot immutable de la grille, composants d'items inclus.
- Ajout de `/cao stats` et `/cao stats reset`.
- Ajout de fichiers de langue `en_us` et `fr_fr`.

## Diagnostics
- Ajout de compteurs de cache Spout, Basin et Mechanical Crafter: recherches, hits, misses, evictions, invalidations et resultats negatifs.
- `diagnostics.enabled = false` stoppe maintenant le suivi PackageEntity, les rescans, les snapshots et les avertissements.
- La commande de statut separe mieux les optimisations actives des hooks de profiling uniquement diagnostiques.

## Fixes
- Correction du crash Mechanical Crafter ou un resultat sans recette `null` pouvait etre stocke avec `result.copy()`.
- Ajout d'une invalidation explicite des caches de recettes lors des reloads/synchronisations datapack et des resets serveur.
- Les entrees de cache Spout ne peuvent plus etre reutilisees entre dimensions ou epochs de reload.

## Compatibility
- Cible toujours Minecraft `1.21.1`, NeoForge `21.1.233`, Java `21` et Create `6.0.10`.
- Le chemin du JAR Create de developpement pointe vers le JAR fourni, avec l'ancien chemin local en fallback.
- Les mixins de profiling restent classes comme diagnostics et ne sont pas presentees comme optimisations directes.

## 1.1.0

Spark response update for belt/funnel/deployer and package-heavy Create servers.

### Diagnostics

- Added profiling sections for `BeltBlockEntity.tick`, `BeltFunnelInteractionHandler.checkForFunnels`, and `DeployerItemHandler.insertItem`.
- Added optimization effectiveness counters for belt funnel, deployer, and Diving Boots fast paths.
- Added optimization savings output to reports, including estimated avoided `ItemStack.copy()` and `ItemStack.split()` calls.
- Added report folder buttons to open or copy the diagnostics report directory more easily.
- Added `/cao packages stalled` to list stationary PackageEntity candidates with position, chunk, age, insertion delay, and speed.
- Improved package top-chunk diagnostics with exact block coordinate ranges.
- Added PackageEntity age statistics, stationary candidate counts, and old-package counts to reports.

### Optimizations

- Added a Diving Boots no-boots fast path to reduce repeated `HeavyBoots` NBT checks on entities not wearing the boots.
- Added a guard to skip redundant Diving Boots marker writes when the marker is already present.
- Added a belt funnel fast-reject path to avoid a redundant `ItemStack.copy()` when a blocking funnel cannot accept an exact oversized stack.
- Added a deployer insertion fast-reject path to avoid redundant `copy/split` work when the deployer hand is already full.
- Reduced CAO diagnostic hook overhead when no CAO profiling session is active.

### Configuration

- Added `optimizations.beltFunnels.fastRejectOversizedExactInsertions`.
- Added `optimizations.deployers.fastRejectFullHandInsertions`.
- Added disabled-by-default `optimizations.experimentalPackages.enabled` as a safe placeholder for future PackageEntity behavior experiments.

## 1.0.0

Initial public release of Create: Advanced Optimization for NeoForge `1.21.1`.

### Highlights

- Added the first complete server-side foundation for Create-focused diagnostics and conservative performance optimization.
- Targeted Create `6.0.10` specifically with strict compatibility checks and required dependency metadata.
- Kept the mod focused on server TPS, lag spikes, logistics, trains, packages, contraptions, and machine processing.
- Explicitly excluded client FPS, rendering, shaders, Embeddium, Sodium, Iris, and other graphics-only features.

### Diagnostics

- Added `/createadvancedoptimization` and `/cao` admin command trees.
- Added manual profiling sessions with automatic timed export and manual stop support.
- Added lightweight targeted profiling for selected Create hotspots including:
  - `DivingBootsItem`
  - `SpoutBlockEntity`
  - `FillingBySpout`
  - `PackageEntity`
  - `GlobalLogisticsManager`
  - `GlobalRailwayManager`
  - `RedstoneLinkNetworkHandler`
  - `ServerChainConveyorHandler`
  - `TrainMapSync`
  - `TrackGraphSync`
- Added report export to `logs/createadvancedoptimization/reports/`.
- Added a concise `.txt` debrief export plus a richer local `.html` report.
- Added human-readable hotspot explanations, technical method names, symptom hints, and a short debrief verdict.
- Added integrated mod branding in the HTML report output.

### Monitoring

- Added `PackageEntity` monitoring with active count, peak count, spawned count, per-dimension tracking, and on-demand chunk hotspot lookup.
- Added warnings when package counts exceed the configured threshold.
- Added reset support for profiler data and package diagnostics.

### Optimizations

- Added a conservative `DivingBootsItem.affects()` patch to skip redundant `HeavyBoots` tag removal when the tag is already absent.
- Added bounded local Spout recipe lookup caching for safe repeated official Create fill checks.
- Enabled the bounded Spout cache by default after validating the cache scope and invalidation strategy.
- Added short-lived processing recipe memoization for Basin and Mechanical Crafter workflows.
- Added a belt funnel fast-reject path to avoid a redundant `ItemStack.copy()` when a blocking funnel cannot accept an exact oversized stack.
- Added a deployer insertion fast-reject path to avoid redundant `copy/split` work when the deployer hand is already full.
- Reduced CAO diagnostic hook overhead when no CAO profiling session is active.

### Configuration

- Added structured server config with per-feature toggles.
- Added configurable diagnostics thresholds and profiling defaults.
- Added config toggles for belt funnel and deployer micro-optimizations.
- Added experimental config sections for future modules while keeping risky behavior-changing features disabled by default.

### Compatibility And Safety

- Added strict Create `6.0.10` compatibility handling.
- Designed the mod for NeoForge-only server use on Minecraft `1.21.1`.
- Avoided async access to live world data.
- Avoided global throttling, forced machine slowdowns, and risky gameplay-changing behavior in V1.
- Kept mixins targeted and avoided overwrites for normal optimization paths.

### Project Cleanup

- Removed NeoForge template example content.
- Replaced template metadata with proper mod information, dependency declarations, mixin registration, and branding.
- Added initial project documentation for analysis, configuration, commands, and release notes.
