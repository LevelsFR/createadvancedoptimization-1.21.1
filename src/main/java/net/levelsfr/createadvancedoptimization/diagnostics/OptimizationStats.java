package net.levelsfr.createadvancedoptimization.diagnostics;

import java.util.concurrent.atomic.LongAdder;
import net.levelsfr.createadvancedoptimization.config.CAOServerConfig;

public final class OptimizationStats {

    private static final LongAdder BELT_FUNNEL_FAST_REJECTS = new LongAdder();
    private static final LongAdder DEPLOYER_FULL_HAND_FAST_REJECTS = new LongAdder();
    private static final LongAdder DIVING_BOOTS_MARKER_WRITES_SKIPPED = new LongAdder();
    private static final LongAdder DIVING_BOOTS_MARKER_REMOVALS_SKIPPED = new LongAdder();
    private static final CacheCounters SPOUT_CACHE = new CacheCounters();
    private static final CacheCounters BASIN_CACHE = new CacheCounters();
    private static final CacheCounters CRAFTER_CACHE = new CacheCounters();

    private OptimizationStats() {
    }

    public static void recordBeltFunnelFastReject() {
        if (!isEnabled()) {
            return;
        }
        BELT_FUNNEL_FAST_REJECTS.increment();
    }

    public static void recordDeployerFullHandFastReject() {
        if (!isEnabled()) {
            return;
        }
        DEPLOYER_FULL_HAND_FAST_REJECTS.increment();
    }

    public static void recordDivingBootsMarkerWriteSkipped() {
        if (!isEnabled()) {
            return;
        }
        DIVING_BOOTS_MARKER_WRITES_SKIPPED.increment();
    }

    public static void recordDivingBootsMarkerRemovalSkipped() {
        if (!isEnabled()) {
            return;
        }
        DIVING_BOOTS_MARKER_REMOVALS_SKIPPED.increment();
    }

    public static void recordSpoutLookup() {
        if (!isEnabled()) {
            return;
        }
        SPOUT_CACHE.lookups.increment();
    }

    public static void recordSpoutHit() {
        if (!isEnabled()) {
            return;
        }
        SPOUT_CACHE.hits.increment();
    }

    public static void recordSpoutMiss() {
        if (!isEnabled()) {
            return;
        }
        SPOUT_CACHE.misses.increment();
    }

    public static void recordSpoutEviction() {
        if (!isEnabled()) {
            return;
        }
        SPOUT_CACHE.evictions.increment();
    }

    public static void recordSpoutInvalidation() {
        if (!isEnabled()) {
            return;
        }
        SPOUT_CACHE.invalidations.increment();
    }

    public static void recordSpoutNegativeResult() {
        if (!isEnabled()) {
            return;
        }
        SPOUT_CACHE.negativeResults.increment();
    }

    public static void recordBasinLookup() {
        if (!isEnabled()) {
            return;
        }
        BASIN_CACHE.lookups.increment();
    }

    public static void recordBasinHit() {
        if (!isEnabled()) {
            return;
        }
        BASIN_CACHE.hits.increment();
    }

    public static void recordBasinMiss() {
        if (!isEnabled()) {
            return;
        }
        BASIN_CACHE.misses.increment();
    }

    public static void recordBasinEviction() {
        if (!isEnabled()) {
            return;
        }
        BASIN_CACHE.evictions.increment();
    }

    public static void recordBasinInvalidation() {
        if (!isEnabled()) {
            return;
        }
        BASIN_CACHE.invalidations.increment();
    }

    public static void recordBasinNegativeResult() {
        if (!isEnabled()) {
            return;
        }
        BASIN_CACHE.negativeResults.increment();
    }

    public static void recordCrafterLookup() {
        if (!isEnabled()) {
            return;
        }
        CRAFTER_CACHE.lookups.increment();
    }

    public static void recordCrafterHit() {
        if (!isEnabled()) {
            return;
        }
        CRAFTER_CACHE.hits.increment();
    }

    public static void recordCrafterMiss() {
        if (!isEnabled()) {
            return;
        }
        CRAFTER_CACHE.misses.increment();
    }

    public static void recordCrafterEviction() {
        if (!isEnabled()) {
            return;
        }
        CRAFTER_CACHE.evictions.increment();
    }

    public static void recordCrafterInvalidation() {
        if (!isEnabled()) {
            return;
        }
        CRAFTER_CACHE.invalidations.increment();
    }

    public static void recordCrafterNegativeResult() {
        if (!isEnabled()) {
            return;
        }
        CRAFTER_CACHE.negativeResults.increment();
    }

    public static void recordBasinKeyBuildNanos(long nanos) {
        if (!isEnabled()) {
            return;
        }
        BASIN_CACHE.keyBuildNanos.add(nanos);
    }

    public static void recordCrafterKeyBuildNanos(long nanos) {
        if (!isEnabled()) {
            return;
        }
        CRAFTER_CACHE.keyBuildNanos.add(nanos);
    }

    public static void recordBasinMaxSize(int size) {
        if (!isEnabled()) {
            return;
        }
        BASIN_CACHE.recordMaxSize(size);
    }

    public static void recordCrafterMaxSize(int size) {
        if (!isEnabled()) {
            return;
        }
        CRAFTER_CACHE.recordMaxSize(size);
    }

    public static Snapshot snapshot() {
        long beltFunnelFastRejects = BELT_FUNNEL_FAST_REJECTS.sum();
        long deployerFullHandFastRejects = DEPLOYER_FULL_HAND_FAST_REJECTS.sum();
        long divingBootsMarkerWritesSkipped = DIVING_BOOTS_MARKER_WRITES_SKIPPED.sum();
        long divingBootsMarkerRemovalsSkipped = DIVING_BOOTS_MARKER_REMOVALS_SKIPPED.sum();
        return new Snapshot(
            beltFunnelFastRejects,
            deployerFullHandFastRejects,
            divingBootsMarkerWritesSkipped,
            divingBootsMarkerRemovalsSkipped,
            beltFunnelFastRejects + deployerFullHandFastRejects + divingBootsMarkerWritesSkipped + divingBootsMarkerRemovalsSkipped,
            beltFunnelFastRejects + deployerFullHandFastRejects,
            deployerFullHandFastRejects,
            SPOUT_CACHE.snapshot(),
            BASIN_CACHE.snapshot(),
            CRAFTER_CACHE.snapshot()
        );
    }

    public static Snapshot delta(Snapshot start, Snapshot end) {
        long beltFunnelFastRejects = delta(start.beltFunnelFastRejects, end.beltFunnelFastRejects);
        long deployerFullHandFastRejects = delta(start.deployerFullHandFastRejects, end.deployerFullHandFastRejects);
        long divingBootsMarkerWritesSkipped = delta(start.divingBootsMarkerWritesSkipped, end.divingBootsMarkerWritesSkipped);
        long divingBootsMarkerRemovalsSkipped = delta(start.divingBootsMarkerRemovalsSkipped, end.divingBootsMarkerRemovalsSkipped);
        return new Snapshot(
            beltFunnelFastRejects,
            deployerFullHandFastRejects,
            divingBootsMarkerWritesSkipped,
            divingBootsMarkerRemovalsSkipped,
            beltFunnelFastRejects + deployerFullHandFastRejects + divingBootsMarkerWritesSkipped + divingBootsMarkerRemovalsSkipped,
            beltFunnelFastRejects + deployerFullHandFastRejects,
            deployerFullHandFastRejects,
            delta(start.spoutCache, end.spoutCache),
            delta(start.basinCache, end.basinCache),
            delta(start.crafterCache, end.crafterCache)
        );
    }

    public static void reset() {
        BELT_FUNNEL_FAST_REJECTS.reset();
        DEPLOYER_FULL_HAND_FAST_REJECTS.reset();
        DIVING_BOOTS_MARKER_WRITES_SKIPPED.reset();
        DIVING_BOOTS_MARKER_REMOVALS_SKIPPED.reset();
        SPOUT_CACHE.reset();
        BASIN_CACHE.reset();
        CRAFTER_CACHE.reset();
    }

    public record Snapshot(
        long beltFunnelFastRejects,
        long deployerFullHandFastRejects,
        long divingBootsMarkerWritesSkipped,
        long divingBootsMarkerRemovalsSkipped,
        long totalFastRejects,
        long estimatedAvoidedStackCopies,
        long estimatedAvoidedStackSplits,
        CacheSnapshot spoutCache,
        CacheSnapshot basinCache,
        CacheSnapshot crafterCache
    ) {
    }

    public record CacheSnapshot(
        long lookups,
        long hits,
        long misses,
        long evictions,
        long invalidations,
        long negativeResults,
        long keyBuildNanos,
        long maxSizeReached
    ) {
        public double hitRate() {
            return lookups == 0L ? 0.0D : hits / (double) lookups;
        }
    }

    private static CacheSnapshot delta(CacheSnapshot start, CacheSnapshot end) {
        return new CacheSnapshot(
            delta(start.lookups, end.lookups),
            delta(start.hits, end.hits),
            delta(start.misses, end.misses),
            delta(start.evictions, end.evictions),
            delta(start.invalidations, end.invalidations),
            delta(start.negativeResults, end.negativeResults),
            delta(start.keyBuildNanos, end.keyBuildNanos),
            end.maxSizeReached
        );
    }

    private static long delta(long start, long end) {
        return Math.max(0L, end - start);
    }

    private static final class CacheCounters {

        private final LongAdder lookups = new LongAdder();
        private final LongAdder hits = new LongAdder();
        private final LongAdder misses = new LongAdder();
        private final LongAdder evictions = new LongAdder();
        private final LongAdder invalidations = new LongAdder();
        private final LongAdder negativeResults = new LongAdder();
        private final LongAdder keyBuildNanos = new LongAdder();
        private volatile long maxSizeReached;

        private CacheSnapshot snapshot() {
            return new CacheSnapshot(
                lookups.sum(),
                hits.sum(),
                misses.sum(),
                evictions.sum(),
                invalidations.sum(),
                negativeResults.sum(),
                keyBuildNanos.sum(),
                maxSizeReached
            );
        }

        private void reset() {
            lookups.reset();
            hits.reset();
            misses.reset();
            evictions.reset();
            invalidations.reset();
            negativeResults.reset();
            keyBuildNanos.reset();
            maxSizeReached = 0L;
        }

        private void recordMaxSize(int size) {
            long current = maxSizeReached;
            while (size > current) {
                maxSizeReached = size;
                return;
            }
        }
    }

    private static boolean isEnabled() {
        return CAOServerConfig.diagnosticsEnabledFast();
    }
}
