package net.levelsfr.createadvancedoptimization.diagnostics.profiler;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.levelsfr.createadvancedoptimization.config.CAOServerConfig;
import net.levelsfr.createadvancedoptimization.diagnostics.OptimizationStats;
import net.levelsfr.createadvancedoptimization.diagnostics.packages.PackageEntityMonitor;
import net.minecraft.server.MinecraftServer;

public final class CreateProfilerManager {

    private static ProfileSession activeSession;
    private static ProfileSession lastCompletedSession;

    private CreateProfilerManager() {
    }

    public static long begin(ProfiledSection section) {
        if (!isProfilingEnabled()) {
            return 0L;
        }
        return System.nanoTime();
    }

    public static void end(ProfiledSection section, long startedAtNanos) {
        if (startedAtNanos == 0L) {
            return;
        }
        ProfileSession session = activeSession;
        if (session == null) {
            return;
        }
        session.record(section, System.nanoTime() - startedAtNanos);
    }

    public static boolean isProfilingEnabled() {
        return activeSession != null && CAOServerConfig.GENERAL_ENABLED.get() && CAOServerConfig.DIAGNOSTICS_ENABLED.get();
    }

    public static StartResult start(MinecraftServer server, int durationSeconds, String initiatedBy) {
        if (!CAOServerConfig.GENERAL_ENABLED.get()) {
            return StartResult.MOD_DISABLED;
        }
        if (!CAOServerConfig.DIAGNOSTICS_ENABLED.get()) {
            return StartResult.DIAGNOSTICS_DISABLED;
        }
        if (durationSeconds < 5 || durationSeconds > 3600) {
            return StartResult.INVALID_DURATION;
        }
        if (activeSession != null) {
            return StartResult.ALREADY_ACTIVE;
        }
        activeSession = new ProfileSession(server, durationSeconds, initiatedBy);
        return StartResult.STARTED;
    }

    public static ProfileSession stop() {
        ProfileSession session = activeSession;
        if (session == null) {
            return null;
        }
        activeSession = null;
        session.finish();
        lastCompletedSession = session;
        return session;
    }

    public static void resetAll() {
        activeSession = null;
        lastCompletedSession = null;
    }

    public static boolean isActive() {
        return activeSession != null;
    }

    public static boolean shouldAutoStop(MinecraftServer server) {
        ProfileSession session = activeSession;
        return session != null && server.getTickCount() >= session.endTick();
    }

    public static void onServerTickStart(MinecraftServer server) {
        ProfileSession session = activeSession;
        if (session != null && isProfilingEnabled()) {
            session.tickStartNanos = System.nanoTime();
        }
    }

    public static void onServerTickEnd(MinecraftServer server) {
        ProfileSession session = activeSession;
        if (session == null || !isProfilingEnabled() || session.tickStartNanos == 0L) {
            return;
        }

        double mspt = (System.nanoTime() - session.tickStartNanos) / 1_000_000.0D;
        session.tickStartNanos = 0L;
        session.recordTick(mspt, CAOServerConfig.LAG_SPIKE_THRESHOLD_MS.get());
        session.recordPackageMetrics(PackageEntityMonitor.getInstance());
    }

    public static ProfileSession getActiveSession() {
        return activeSession;
    }

    public static ProfileSession getLastCompletedSession() {
        return lastCompletedSession;
    }

    public enum StartResult {
        STARTED,
        ALREADY_ACTIVE,
        DIAGNOSTICS_DISABLED,
        MOD_DISABLED,
        INVALID_DURATION
    }

    public static final class ProfileSession {

        private final Instant startedAt;
        private final String sessionId;
        private final String initiatedBy;
        private final int requestedDurationSeconds;
        private final int startTick;
        private final int endTick;
        private final Map<ProfiledSection, MethodStats> methodStats;
        private final OptimizationStats.Snapshot optimizationStatsAtStart;
        private final long spawnedPackagesAtStart;
        private OptimizationStats.Snapshot optimizationStatsAtEnd;
        private boolean finished;
        private int exportCount;
        private double totalMspt;
        private double maxMspt;
        private long tickSamples;
        private long ticksAboveThreshold;
        private long tickStartNanos;
        private int peakPackages;

        private ProfileSession(MinecraftServer server, int requestedDurationSeconds, String initiatedBy) {
            this.startedAt = Instant.now();
            this.sessionId = UUID.randomUUID().toString().substring(0, 8);
            this.initiatedBy = initiatedBy;
            this.requestedDurationSeconds = requestedDurationSeconds;
            this.startTick = server.getTickCount();
            this.endTick = this.startTick + requestedDurationSeconds * 20;
            this.methodStats = new EnumMap<>(ProfiledSection.class);
            this.optimizationStatsAtStart = OptimizationStats.snapshot();
            PackageEntityMonitor packageMonitor = PackageEntityMonitor.getInstance();
            this.spawnedPackagesAtStart = packageMonitor.getSpawnedSinceReset();
            this.peakPackages = packageMonitor.getActiveTotal();
        }

        private synchronized void record(ProfiledSection section, long nanos) {
            methodStats.computeIfAbsent(section, ignored -> new MethodStats()).record(nanos);
        }

        private synchronized void recordTick(double mspt, int thresholdMs) {
            totalMspt += mspt;
            maxMspt = Math.max(maxMspt, mspt);
            tickSamples++;
            if (mspt >= thresholdMs) {
                ticksAboveThreshold++;
            }
        }

        private synchronized void recordPackageMetrics(PackageEntityMonitor monitor) {
            peakPackages = Math.max(peakPackages, monitor.getActiveTotal());
        }

        private void finish() {
            optimizationStatsAtEnd = OptimizationStats.snapshot();
            finished = true;
        }

        public Instant startedAt() {
            return startedAt;
        }

        public String sessionId() {
            return sessionId;
        }

        public String initiatedBy() {
            return initiatedBy;
        }

        public int requestedDurationSeconds() {
            return requestedDurationSeconds;
        }

        public int startTick() {
            return startTick;
        }

        public int endTick() {
            return endTick;
        }

        public boolean finished() {
            return finished;
        }

        public double averageMspt() {
            return tickSamples == 0 ? 0.0D : totalMspt / tickSamples;
        }

        public double maxMspt() {
            return maxMspt;
        }

        public long tickSamples() {
            return tickSamples;
        }

        public long ticksAboveThreshold() {
            return ticksAboveThreshold;
        }

        public synchronized int nextExportSequence() {
            exportCount++;
            return exportCount;
        }

        public List<Map.Entry<ProfiledSection, MethodStats>> sortedEntries() {
            List<Map.Entry<ProfiledSection, MethodStats>> entries = new ArrayList<>(methodStats.entrySet());
            entries.sort(Comparator.comparingLong((Map.Entry<ProfiledSection, MethodStats> entry) -> entry.getValue().totalNanos).reversed());
            return entries;
        }

        public OptimizationStats.Snapshot optimizationStatsDelta() {
            OptimizationStats.Snapshot end = optimizationStatsAtEnd == null ? OptimizationStats.snapshot() : optimizationStatsAtEnd;
            return OptimizationStats.delta(optimizationStatsAtStart, end);
        }

        public synchronized int peakPackages() {
            return peakPackages;
        }

        public long spawnedPackages(PackageEntityMonitor monitor) {
            return Math.max(0L, monitor.getSpawnedSinceReset() - spawnedPackagesAtStart);
        }
    }

    public static final class MethodStats {

        private long totalNanos;
        private long calls;

        private void record(long nanos) {
            totalNanos += nanos;
            calls++;
        }

        public long totalNanos() {
            return totalNanos;
        }

        public long calls() {
            return calls;
        }

        public double totalMillis() {
            return totalNanos / 1_000_000.0D;
        }

        public double averageMicros() {
            return calls == 0 ? 0.0D : (totalNanos / 1_000.0D) / calls;
        }
    }
}
