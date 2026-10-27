# Create: Advanced Optimization — project guide

NeoForge server diagnostics and focused optimizations for Create 6.0.10.

Source and publication snapshot: **2026-10-27**. This guide describes the checked-out source; a download page can contain an earlier release.

## Status and official links

- [Source repository](https://github.com/LevelsFR/createadvancedoptimization-1.21.1/tree/master) (public).
- [CurseForge](https://www.curseforge.com/minecraft/mc-mods/create-advanced-optimization) — published project verified under LevelsFR.
- [Modrinth](https://modrinth.com/mod/create-advanced-optimization) — approved project in the LevelsFR profile.

## Features in the current source

- Version-specific Create compatibility guard and per-feature server configuration.
- Profiler and diagnostic summaries for factories, packages, belts and processing systems.
- Timestamped text, HTML and JSON reports with report comparison.
- Bounded local lookup caches and one-tick recipe memoization.
- Belt and transported-item fast paths, monitoring and on-demand belt scans.

## Compatibility and dependencies

| Component | Value |
| --- | --- |
| Source version | `1.6` |
| Minecraft baseline | `1.21.1` |
| JDK | `21` |
| Active branch | `master` |

| Loader | Dependency | Declared constraint | Requirement |
| --- | --- | --- | --- |
| NeoForge | `neoforge` | `[21.1.233,)` | required; BOTH |
| NeoForge | `minecraft` | `[1.21.1]` | required; BOTH |
| NeoForge | `create` | `[6.0.10]` | required; BOTH |

The table reflects loader metadata. Build-time version pins are in [gradle.properties](../gradle.properties). A dependency used only for compilation or optional integration is not automatically required for every installation.

## Installation and first use

1. Choose a released JAR matching both the Minecraft version and loader, when this project has a release page.
2. Install the required dependencies declared for that loader. A source-only development version may require locally built dependencies.
3. Use a separate development instance for unreleased builds. Keep client-side integrations on the correct side and follow the main README for server installation.
4. Start the game/server once to create configuration, then use the project settings or documented commands.

### Command entry points

- `/cao`
- `/cao diagnose`
- `/cao status`
- `/cao stats`
- `/cao belts scan`
- `/cao report compare`

Commands with arguments have contextual completion in game. Permission checks and the complete argument tree are defined in the linked command sources below.

## Configuration and saved data

Configuration and persistence entry points are listed in the source map below and in the existing project documentation.

Keep world data and player data with the matching world backup. Use the mod-specific reload/configuration route documented in the main README; changing an arbitrary file does not imply live reloading. Do not copy development-world data into the source repository.

## Source map

| Directory | Responsibility |
| --- | --- |
| [src](../src) | Root source sets and resources for this project architecture. |
| [gradle](../gradle) | Committed wrapper and build configuration. |

Metadata: [src/main/templates/META-INF/neoforge.mods.toml](../src/main/templates/META-INF/neoforge.mods.toml).

## Build from source

Run commands from the repository root using a matching JDK and the committed Gradle wrapper.

```powershell
.\gradlew.bat build
```

Loader JARs are normally written under the corresponding `build/libs` directory. Aggregate projects document their collected artifact layout in the main README or build helper.

## Documentation index

- [CHANGELOG.md](../CHANGELOG.md)
- [README.md](../README.md)
- [docs/commands.md](../docs/commands.md)
- [docs/configuration.md](../docs/configuration.md)

## Validation and reporting a problem

A source audit, successful compilation, automated checks and in-game validation are different forms of evidence. This documentation update alone does not establish runtime compatibility. Preserve the project-specific remaining release gates.

When reporting a problem, include:

- Exact mod version, Minecraft version, loader and dependency versions.
- Client, integrated server or dedicated-server context.
- Minimal reproduction steps and the relevant log/stack trace.
- Configuration or datapack excerpts required to reproduce it, with credentials and personal data removed.

Before a release, compile every affected loader and configured version, inspect packaged metadata/resources, and perform the relevant client and dedicated-server checks. Rendering, interactions, networking and persistence need real runtime validation.

## License and attribution

See [LICENSE](../LICENSE), [NOTICE](../NOTICE). Retain upstream notices and third-party asset attribution.

## Reproducing a build on another computer

The default build resolves the exact Create `6.0.10` release for Minecraft `1.21.1` from Modrinth Maven using the pinned version ID in `gradle.properties`. GitHub Actions uses the same dependency, so builds do not rely on a developer-specific file path.

For local development with an already downloaded Create JAR, pass its path explicitly:

```powershell
.\gradlew.bat build -Pcreate_dev_jar="C:/path/to/create-1.21.1-6.0.10.jar"
```

When changing the Create target, update `create_version`, `create_version_range`, and `create_modrinth_version` together, then verify both the resolved artifact and the compatibility guard.
