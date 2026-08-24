package net.levelsfr.createadvancedoptimization.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class CAOServerConfig {

    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue GENERAL_ENABLED;
    public static final ModConfigSpec.BooleanValue DEBUG_LOGGING;

    public static final ModConfigSpec.BooleanValue DIAGNOSTICS_ENABLED;
    public static final ModConfigSpec.IntValue LAG_SPIKE_THRESHOLD_MS;
    public static final ModConfigSpec.IntValue PROFILE_DEFAULT_DURATION_SECONDS;
    public static final ModConfigSpec.IntValue PACKAGE_ENTITY_WARNING_THRESHOLD;

    public static final ModConfigSpec.BooleanValue DIVING_BOOTS_ENABLED;
    public static final ModConfigSpec.BooleanValue SPOUT_RECIPE_CACHE_ENABLED;
    public static final ModConfigSpec.IntValue SPOUT_RECIPE_CACHE_MAX_ENTRIES;
    public static final ModConfigSpec.BooleanValue PROCESSING_RECIPE_MEMOIZATION_ENABLED;
    public static final ModConfigSpec.BooleanValue BELT_TICK_FAST_PATHS_ENABLED;
    public static final ModConfigSpec.BooleanValue BELT_FUNNEL_FAST_REJECT_ENABLED;
    public static final ModConfigSpec.BooleanValue DEPLOYER_INSERT_FAST_REJECT_ENABLED;
    public static final ModConfigSpec.BooleanValue EXPERIMENTAL_PACKAGES_ENABLED;

    private static volatile boolean diagnosticsEnabled = true;
    private static volatile boolean beltTickFastPathsEnabled = true;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("general");
        GENERAL_ENABLED = builder.comment("Master switch for all Create: Advanced Optimization server logic.")
            .define("enabled", true);
        DEBUG_LOGGING = builder.comment("Enables additional debug logging for diagnostics and cache invalidation.")
            .define("debugLogging", false);
        builder.pop();

        builder.push("diagnostics");
        DIAGNOSTICS_ENABLED = builder.comment("Enables diagnostics, commands, and report generation.")
            .define("enabled", true);
        LAG_SPIKE_THRESHOLD_MS = builder.comment("MSPT threshold used to flag lag spikes in profiler reports.")
            .defineInRange("lagSpikeThresholdMs", 50, 1, 5000);
        PROFILE_DEFAULT_DURATION_SECONDS = builder.comment("Default profiler duration used by commands when needed.")
            .defineInRange("profileDefaultDurationSeconds", 30, 5, 3600);
        PACKAGE_ENTITY_WARNING_THRESHOLD = builder.comment("Warns when active Create PackageEntity counts exceed this threshold.")
            .defineInRange("packageEntityWarningThreshold", 200, 1, 100000);
        builder.pop();

        builder.push("optimizations");

        builder.push("divingBoots");
        DIVING_BOOTS_ENABLED = builder.comment("Skips redundant HeavyBoots marker writes/removals when the marker is already in the desired state.")
            .define("enabled", true);
        builder.pop();

        builder.push("spoutRecipeCache");
        SPOUT_RECIPE_CACHE_ENABLED = builder.comment("Caches bounded per-spout official Create filling recipe lookups. Generic capability-based filling remains uncached.")
            .define("enabled", true);
        SPOUT_RECIPE_CACHE_MAX_ENTRIES = builder.comment("Maximum bounded cache entries stored per spout.")
            .defineInRange("maxEntriesPerSpout", 4, 1, 32);
        builder.pop();

        builder.push("processingRecipeMemoization");
        PROCESSING_RECIPE_MEMOIZATION_ENABLED = builder.comment("Enables one-tick memoization for Basin and Mechanical Crafter recipe lookup windows.")
            .define("enabled", true);
        builder.pop();

        builder.push("belts");
        BELT_TICK_FAST_PATHS_ENABLED = builder.comment(
                "Reuses the belt block entity state already maintained by Minecraft and skips empty entity-passenger cleanup allocations. "
                    + "Does not throttle belts or change item movement cadence.")
            .define("tickFastPaths", true);
        builder.pop();

        builder.push("beltFunnels");
        BELT_FUNNEL_FAST_REJECT_ENABLED = builder.comment("Skips redundant stack copies when a blocking belt funnel already knows the carried stack is too small for its exact extraction amount.")
            .define("fastRejectOversizedExactInsertions", true);
        builder.pop();

        builder.push("deployers");
        DEPLOYER_INSERT_FAST_REJECT_ENABLED = builder.comment("Skips redundant copy/split work when inserting into a deployer hand that is already full.")
            .define("fastRejectFullHandInsertions", true);
        builder.pop();

        builder.push("experimentalPackages");
        EXPERIMENTAL_PACKAGES_ENABLED = builder.comment("Reserved for future PackageEntity behavior experiments. Kept disabled by default; current V1.1 logic is diagnostic-only.")
            .define("enabled", false);
        builder.pop();

        builder.pop();

        SPEC = builder.build();
    }

    private CAOServerConfig() {
    }

    public static void refreshCachedState() {
        try {
            diagnosticsEnabled = GENERAL_ENABLED.get() && DIAGNOSTICS_ENABLED.get();
            beltTickFastPathsEnabled = GENERAL_ENABLED.get() && BELT_TICK_FAST_PATHS_ENABLED.get();
        } catch (IllegalStateException ignored) {
            diagnosticsEnabled = true;
            beltTickFastPathsEnabled = true;
        }
    }

    public static boolean diagnosticsEnabledFast() {
        return diagnosticsEnabled;
    }

    public static boolean beltTickFastPathsEnabled() {
        return beltTickFastPathsEnabled;
    }
}
