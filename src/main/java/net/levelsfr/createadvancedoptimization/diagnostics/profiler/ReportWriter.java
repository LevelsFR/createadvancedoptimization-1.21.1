package net.levelsfr.createadvancedoptimization.diagnostics.profiler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;
import net.levelsfr.createadvancedoptimization.CreateAdvancedOptimization;
import net.levelsfr.createadvancedoptimization.compatibility.CreateCompatibility;
import net.levelsfr.createadvancedoptimization.config.CAOServerConfig;
import net.levelsfr.createadvancedoptimization.diagnostics.OptimizationStats;
import net.levelsfr.createadvancedoptimization.diagnostics.packages.PackageEntityMonitor;
import net.levelsfr.createadvancedoptimization.util.SignatureUtil;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.neoforged.fml.loading.FMLPaths;

public final class ReportWriter {

    private static final DateTimeFormatter FILE_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS").withZone(ZoneOffset.UTC);
    private static final DateTimeFormatter DISPLAY_TIMESTAMP = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneOffset.UTC);
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String LOGO_RESOURCE_NAME = "createadvancedoptimization-logo.png";
    private static final int TOP_CHUNK_LIMIT = 10;
    private static final int TEXT_TOP_METHOD_LIMIT = 5;

    private ReportWriter() {
    }

    public static ExportedReport writeReport(CreateProfilerManager.ProfileSession session, PackageEntityMonitor monitor, MinecraftServer server) throws IOException {
        Path reportsDir = getReportsDir();
        Files.createDirectories(reportsDir);

        int exportSequence = session.nextExportSequence();
        String baseName = "report-"
            + FILE_TIMESTAMP.format(session.startedAt())
            + "-"
            + session.sessionId()
            + "-export"
            + String.format(Locale.ROOT, "%02d", exportSequence);

        Path exportDir = reportsDir.resolve(baseName);
        Files.createDirectories(exportDir);
        Path textReport = exportDir.resolve(baseName + ".txt");
        Path htmlReport = exportDir.resolve(baseName + ".html");
        Path jsonReport = exportDir.resolve(baseName + ".json");

        ReportData reportData = buildReportData(session, monitor, server, textReport.getFileName().toString(), exportDir.toAbsolutePath().toString());

        Files.writeString(textReport, buildTextReport(reportData));
        Files.writeString(htmlReport, buildHtmlReport(reportData));
        Files.writeString(jsonReport, buildJsonReport(reportData));

        return new ExportedReport(textReport, htmlReport, jsonReport);
    }

    public static ExportedReport findLatestReport() throws IOException {
        Path reportsDir = getReportsDir();
        if (!Files.isDirectory(reportsDir)) {
            return null;
        }

        try (Stream<Path> stream = Files.walk(reportsDir, 2)) {
            Path latestHtml = stream
                .filter(path -> Files.isRegularFile(path) && path.getFileName().toString().endsWith(".html"))
                .sorted(Comparator.comparing((Path path) -> path.getFileName().toString()).reversed())
                .findFirst()
                .orElse(null);
            if (latestHtml == null) {
                return null;
            }

            String htmlName = latestHtml.getFileName().toString();
            String textName = htmlName.substring(0, htmlName.length() - ".html".length()) + ".txt";
            Path textReport = latestHtml.resolveSibling(textName);
            if (!Files.exists(textReport)) {
                return null;
            }

            return new ExportedReport(textReport, latestHtml, latestHtml.resolveSibling(htmlName.substring(0, htmlName.length() - ".html".length()) + ".json"));
        }
    }

    public static ReportComparison compareLatestReports() throws IOException {
        Path reportsDir = getReportsDir();
        if (!Files.isDirectory(reportsDir)) {
            return null;
        }

        List<Path> reports;
        try (Stream<Path> stream = Files.walk(reportsDir, 2)) {
            reports = stream
                .filter(path -> Files.isRegularFile(path) && path.getFileName().toString().endsWith(".json"))
                .sorted(Comparator.comparing((Path path) -> path.getFileName().toString()).reversed())
                .limit(2)
                .toList();
        }
        if (reports.size() < 2) {
            return null;
        }

        JsonObject newer = JSON.fromJson(Files.readString(reports.get(0)), JsonObject.class);
        JsonObject older = JSON.fromJson(Files.readString(reports.get(1)), JsonObject.class);
        return new ReportComparison(
            reports.get(0),
            reports.get(1),
            number(newer, "averageMspt"),
            number(older, "averageMspt"),
            number(newer, "maxMspt"),
            number(older, "maxMspt"),
            integer(newer, "ticksAboveThreshold"),
            integer(older, "ticksAboveThreshold"),
            number(newer, "profiledCreateTimeMs"),
            number(older, "profiledCreateTimeMs"),
            topHotspot(newer),
            topHotspotMsPerTick(newer)
        );
    }

    private static Path getReportsDir() {
        return FMLPaths.GAMEDIR.get()
            .resolve("logs")
            .resolve(CreateAdvancedOptimization.MODID)
            .resolve("reports");
    }

    private static ReportData buildReportData(
        CreateProfilerManager.ProfileSession session,
        PackageEntityMonitor monitor,
        MinecraftServer server,
        String textFileName,
        String reportFolderPath
    ) {
        List<Map.Entry<ProfiledSection, CreateProfilerManager.MethodStats>> methodEntries = session.sortedEntries();
        List<ProfiledSection> sampledSections = methodEntries.stream()
            .map(Map.Entry::getKey)
            .toList();
        List<Map.Entry<ResourceKey<Level>, Integer>> dimensionEntries = new ArrayList<>(monitor.getActiveByDimension().entrySet());
        List<PackageEntityMonitor.ChunkPackageCount> topChunks = monitor.collectTopChunks(server, TOP_CHUNK_LIMIT);
        PackageEntityMonitor.PackageDiagnostics packageDiagnostics = monitor.collectDiagnostics(server, TOP_CHUNK_LIMIT);
        OptimizationStats.Snapshot optimizationStats = session.optimizationStatsDelta();
        List<String> alerts = new ArrayList<>();

        if (monitor.getActiveTotal() >= CAOServerConfig.PACKAGE_ENTITY_WARNING_THRESHOLD.get()) {
            alerts.add("Active packages exceeded the configured warning threshold.");
        }
        if (session.maxMspt() >= CAOServerConfig.LAG_SPIKE_THRESHOLD_MS.get()) {
            alerts.add("MSPT spikes exceeded the configured profiler threshold.");
        }
        if (methodEntries.isEmpty()) {
            alerts.add("No targeted Create methods were sampled during this session.");
        }
        if (packageDiagnostics.stationaryCandidates() > 0) {
            alerts.add("Stationary PackageEntity candidates were detected; inspect the stalled package table.");
        }

        double totalProfiledMs = 0.0D;
        Map<String, FamilySummaryAccumulator> familyAccumulators = new LinkedHashMap<>();
        for (Map.Entry<ProfiledSection, CreateProfilerManager.MethodStats> entry : methodEntries) {
            double methodMs = entry.getValue().totalMillis();
            totalProfiledMs += methodMs;
            familyAccumulators.computeIfAbsent(entry.getKey().family(), ignored -> new FamilySummaryAccumulator(entry.getKey().family()))
                .record(methodMs, entry.getValue().calls());
        }

        List<FamilySummary> familySummaries = familyAccumulators.values().stream()
            .map(accumulator -> accumulator.toSummary(session.tickSamples()))
            .sorted(Comparator.comparingDouble(FamilySummary::totalMillis).reversed())
            .toList();

        Debrief debrief = buildDebrief(session, methodEntries, totalProfiledMs, monitor);
        String logoDataUri = loadLogoDataUri();

        return new ReportData(
            session,
            methodEntries,
            sampledSections,
            familySummaries,
            dimensionEntries,
            topChunks,
            alerts,
            monitor.getActiveTotal(),
            session.peakPackages(),
            session.spawnedPackages(monitor),
            packageDiagnostics,
            optimizationStats,
            totalProfiledMs,
            textFileName,
            reportFolderPath,
            debrief,
            logoDataUri
        );
    }

    private static Debrief buildDebrief(
        CreateProfilerManager.ProfileSession session,
        List<Map.Entry<ProfiledSection, CreateProfilerManager.MethodStats>> methodEntries,
        double totalProfiledMs,
        PackageEntityMonitor monitor
    ) {
        List<String> recommendations = new ArrayList<>();
        if (methodEntries.isEmpty()) {
            recommendations.add("Run another profile while the suspected factory, logistics line, or train setup is active.");
            return new Debrief(
                "No targeted Create hotspot was captured.",
                "The session finished without sampling any of the current Create hotspot targets. This usually means the tested setup was idle or the lag source is outside the currently instrumented Create paths.",
                recommendations
            );
        }

        Map.Entry<ProfiledSection, CreateProfilerManager.MethodStats> topEntry = methodEntries.get(0);
        ProfiledSection topSection = topEntry.getKey();
        double topMsPerTick = millisPerTick(session, topEntry.getValue());
        double topShare = totalProfiledMs <= 0.0D ? 0.0D : (topEntry.getValue().totalMillis() / totalProfiledMs) * 100.0D;

        String headline;
        String summary;
        if (session.ticksAboveThreshold() == 0 && session.maxMspt() < CAOServerConfig.LAG_SPIKE_THRESHOLD_MS.get() && topMsPerTick < 0.250D && monitor.getActiveTotal() == 0) {
            headline = "No significant Create-side lag was detected in this sample.";
            summary = topSection.label() + " was the most visible sampled hotspot, but it stayed minor at "
                + formatDecimal(topMsPerTick) + " ms/tick (" + formatDecimal(topShare) + "% of sampled Create time).";
        } else if (session.ticksAboveThreshold() > 0 || session.maxMspt() >= CAOServerConfig.LAG_SPIKE_THRESHOLD_MS.get()) {
            headline = "Create activity deserves a closer look in this sample.";
            summary = topSection.label() + " was the heaviest sampled hotspot at "
                + formatDecimal(topMsPerTick) + " ms/tick. Correlate this with Spark or gameplay timing to confirm whether it matches the visible lag spikes.";
        } else {
            headline = "Create work was visible, but not clearly severe.";
            summary = topSection.label() + " led the sampled Create work at "
                + formatDecimal(topMsPerTick) + " ms/tick. The sample suggests localized pressure more than a full server-wide Create failure.";
        }

        recommendations.add("Primary hotspot: " + topSection.label() + " [" + topSection.displayName() + "].");
        recommendations.add(recommendationForFamily(topSection.family()));
        if (monitor.getActiveTotal() > 0) {
            recommendations.add("PackageEntity count at export: " + monitor.getActiveTotal() + " active, peak " + monitor.getPeakActiveTotal() + ".");
        }
        if (session.maxMspt() >= CAOServerConfig.LAG_SPIKE_THRESHOLD_MS.get()) {
            recommendations.add("A spike crossed the configured threshold, so compare this export with a Spark sample recorded during the same event.");
        }

        return new Debrief(headline, summary, recommendations);
    }

    private static String recommendationForFamily(String family) {
        return switch (family) {
            case "Spouts" -> "Check belt plus spout lines for repeated filling checks, especially identical items waiting under multiple spouts.";
            case "Fluids" -> "Inspect large fluid pipe grids, busy tanks, and source-search loops around pumps or dense pipe intersections.";
            case "Funnels" -> "Inspect funnels and extraction points that query inventories constantly or sit against blocked item paths.";
            case "Belts" -> "Inspect funnel-heavy belt lines, blocked handoffs, and exact-count filters that repeatedly reject transported items.";
            case "Deployers" -> "Inspect belt-fed deployer arrays for full hands, blocked outputs, or filters causing repeated insertion attempts.";
            case "Packages" -> "Inspect package factories and routing lines for bursts of active PackageEntity instances or entity-heavy handoff loops.";
            case "Logistics" -> "Inspect chain conveyors, package routing, and logistics handoff paths for dense or constantly changing traffic.";
            case "Trains" -> "Inspect large rail graphs, frequent train state changes, and repeated route or sync churn around stations and track edits.";
            case "Core Machines" -> "Inspect clusters of active Create machines to find the specific machine family doing the most repeated block entity work.";
            case "Equipment" -> "This hotspot is usually minor. Treat it as background noise unless huge numbers of affected entities are involved.";
            case "Redstone" -> "Inspect dense Redstone Link traffic or rapidly changing wireless channels.";
            default -> "Correlate the hotspot with the active Create setup in-game and compare the same moment with Spark for wider context.";
        };
    }

    private static String buildTextReport(ReportData reportData) {
        CreateProfilerManager.ProfileSession session = reportData.session();
        StringBuilder builder = new StringBuilder();
        builder.append("Create: Advanced Optimization Debrief").append(System.lineSeparator());
        builder.append("Date: ").append(DISPLAY_TIMESTAMP.format(session.startedAt())).append(System.lineSeparator());
        builder.append("Session ID: ").append(session.sessionId()).append(System.lineSeparator());
        builder.append("Requested Duration Seconds: ").append(session.requestedDurationSeconds()).append(System.lineSeparator());
        builder.append("Mod Version: ").append(CreateAdvancedOptimization.getModVersion()).append(System.lineSeparator());
        builder.append("Create Version: ").append(CreateCompatibility.getLoadedCreateVersion()).append(System.lineSeparator());
        builder.append(System.lineSeparator());

        builder.append("Verdict").append(System.lineSeparator());
        builder.append("- ").append(reportData.debrief().headline()).append(System.lineSeparator());
        builder.append("- ").append(reportData.debrief().summary()).append(System.lineSeparator());
        builder.append(System.lineSeparator());

        builder.append("Key Stats").append(System.lineSeparator());
        builder.append("- Tick Samples: ").append(session.tickSamples()).append(System.lineSeparator());
        builder.append("- Average MSPT: ").append(formatDecimal(session.averageMspt())).append(System.lineSeparator());
        builder.append("- Maximum MSPT: ").append(formatDecimal(session.maxMspt())).append(System.lineSeparator());
        builder.append("- Ticks Above ").append(CAOServerConfig.LAG_SPIKE_THRESHOLD_MS.get()).append(" ms: ").append(session.ticksAboveThreshold()).append(System.lineSeparator());
        builder.append("- Profiled Create Time: ").append(formatDecimal(reportData.totalProfiledMs())).append(" ms").append(System.lineSeparator());
        builder.append(System.lineSeparator());

        builder.append("Session Optimization Events").append(System.lineSeparator());
        builder.append("- Total Optimization Events: ").append(reportData.optimizationStats().totalFastRejects()).append(System.lineSeparator());
        builder.append("- Belt Funnel Fast Rejects: ").append(reportData.optimizationStats().beltFunnelFastRejects()).append(System.lineSeparator());
        builder.append("- Deployer Full Hand Fast Rejects: ").append(reportData.optimizationStats().deployerFullHandFastRejects()).append(System.lineSeparator());
        builder.append("- Diving Boots Marker Writes Skipped: ").append(reportData.optimizationStats().divingBootsMarkerWritesSkipped()).append(System.lineSeparator());
        builder.append("- Diving Boots Marker Removals Skipped: ").append(reportData.optimizationStats().divingBootsMarkerRemovalsSkipped()).append(System.lineSeparator());
        builder.append("- Estimated Avoided ItemStack Copies: ").append(reportData.optimizationStats().estimatedAvoidedStackCopies()).append(System.lineSeparator());
        builder.append("- Estimated Avoided ItemStack Splits: ").append(reportData.optimizationStats().estimatedAvoidedStackSplits()).append(System.lineSeparator());
        builder.append(System.lineSeparator());

        builder.append("Top Hotspots").append(System.lineSeparator());
        if (reportData.methodEntries().isEmpty()) {
            builder.append("- none").append(System.lineSeparator());
        } else {
            int limit = Math.min(TEXT_TOP_METHOD_LIMIT, reportData.methodEntries().size());
            for (int index = 0; index < limit; index++) {
                Map.Entry<ProfiledSection, CreateProfilerManager.MethodStats> entry = reportData.methodEntries().get(index);
                ProfiledSection section = entry.getKey();
                double share = reportData.totalProfiledMs() <= 0.0D ? 0.0D : (entry.getValue().totalMillis() / reportData.totalProfiledMs()) * 100.0D;
                builder.append("- ")
                    .append(section.label())
                    .append(" [")
                    .append(section.displayName())
                    .append("]")
                    .append(": msPerTick=")
                    .append(formatDecimal(millisPerTick(session, entry.getValue())))
                    .append(", totalMs=")
                    .append(formatDecimal(entry.getValue().totalMillis()))
                    .append(", callsPerTick=")
                    .append(formatDecimal(callsPerTick(session, entry.getValue())))
                    .append(", share=")
                    .append(formatDecimal(share))
                    .append("%")
                    .append(System.lineSeparator());
                builder.append("  note=").append(section.symptoms()).append(System.lineSeparator());
            }
        }
        builder.append(System.lineSeparator());

        builder.append("Recommended Checks").append(System.lineSeparator());
        for (String recommendation : reportData.debrief().recommendations()) {
            builder.append("- ").append(recommendation).append(System.lineSeparator());
        }
        builder.append(System.lineSeparator());

        builder.append("PackageEntity Summary").append(System.lineSeparator());
        builder.append("- Active Packages: ").append(reportData.activePackages()).append(System.lineSeparator());
        builder.append("- Peak Active Packages: ").append(reportData.peakPackages()).append(System.lineSeparator());
        builder.append("- Spawned Packages During Profile: ").append(reportData.spawnedPackages()).append(System.lineSeparator());
        builder.append("- Average Package Age: ").append(formatDecimal(reportData.packageDiagnostics().averageAgeSeconds())).append(" s").append(System.lineSeparator());
        builder.append("- Max Package Age: ").append(formatDecimal(reportData.packageDiagnostics().maxAgeSeconds())).append(" s").append(System.lineSeparator());
        builder.append("- Stationary Package Candidates: ").append(reportData.packageDiagnostics().stationaryCandidates()).append(System.lineSeparator());
        builder.append("- Packages Older Than 60s: ").append(reportData.packageDiagnostics().oldPackages()).append(System.lineSeparator());
        if (!reportData.dimensionEntries().isEmpty()) {
            builder.append("- Packages By Dimension:").append(System.lineSeparator());
            for (Map.Entry<ResourceKey<Level>, Integer> entry : reportData.dimensionEntries()) {
                builder.append("  - ")
                    .append(SignatureUtil.dimensionId(entry.getKey()))
                    .append(": ")
                    .append(entry.getValue())
                    .append(System.lineSeparator());
            }
        }
        if (!reportData.topChunks().isEmpty()) {
            builder.append("- Top Package Chunks:").append(System.lineSeparator());
            for (PackageEntityMonitor.ChunkPackageCount entry : reportData.topChunks()) {
                builder.append("  - ")
                    .append(SignatureUtil.dimensionId(entry.key().dimension()))
                    .append(" chunk[")
                    .append(entry.key().chunkX())
                    .append(",")
                    .append(entry.key().chunkZ())
                    .append("] blocks x=")
                    .append(entry.key().minBlockX())
                    .append("..")
                    .append(entry.key().maxBlockX())
                    .append(" z=")
                    .append(entry.key().minBlockZ())
                    .append("..")
                    .append(entry.key().maxBlockZ())
                    .append(": ")
                    .append(entry.count())
                    .append(System.lineSeparator());
            }
        }
        if (!reportData.packageDiagnostics().stalledPackages().isEmpty()) {
            builder.append("- Stalled Package Candidates:").append(System.lineSeparator());
            for (PackageEntityMonitor.PackageSnapshot snapshot : reportData.packageDiagnostics().stalledPackages()) {
                builder.append("  - ")
                    .append(SignatureUtil.dimensionId(snapshot.dimension()))
                    .append(" pos[")
                    .append(formatDecimal(snapshot.x()))
                    .append(",")
                    .append(formatDecimal(snapshot.y()))
                    .append(",")
                    .append(formatDecimal(snapshot.z()))
                    .append("] chunk[")
                    .append(snapshot.chunkX())
                    .append(",")
                    .append(snapshot.chunkZ())
                    .append("] age=")
                    .append(formatDecimal(snapshot.ageSeconds()))
                    .append("s speed=")
                    .append(formatDecimal(snapshot.speed()))
                    .append(System.lineSeparator());
            }
        }
        builder.append(System.lineSeparator());

        builder.append("Alerts").append(System.lineSeparator());
        if (reportData.alerts().isEmpty()) {
            builder.append("- none").append(System.lineSeparator());
        } else {
            for (String alert : reportData.alerts()) {
                builder.append("- ").append(alert).append(System.lineSeparator());
            }
        }

        return builder.toString();
    }

    private static String buildJsonReport(ReportData reportData) {
        CreateProfilerManager.ProfileSession session = reportData.session();
        JsonObject root = new JsonObject();
        root.addProperty("schemaVersion", 1);
        root.addProperty("generatedAt", DISPLAY_TIMESTAMP.format(java.time.Instant.now()));
        root.addProperty("startedAt", DISPLAY_TIMESTAMP.format(session.startedAt()));
        root.addProperty("sessionId", session.sessionId());
        root.addProperty("requestedDurationSeconds", session.requestedDurationSeconds());
        root.addProperty("modVersion", CreateAdvancedOptimization.getModVersion());
        root.addProperty("createVersion", CreateCompatibility.getLoadedCreateVersion());
        root.addProperty("tickSamples", session.tickSamples());
        root.addProperty("averageMspt", session.averageMspt());
        root.addProperty("maxMspt", session.maxMspt());
        root.addProperty("ticksAboveThreshold", session.ticksAboveThreshold());
        root.addProperty("lagSpikeThresholdMs", CAOServerConfig.LAG_SPIKE_THRESHOLD_MS.get());
        root.addProperty("profiledCreateTimeMs", reportData.totalProfiledMs());
        root.addProperty("activePackages", reportData.activePackages());
        root.addProperty("peakPackages", reportData.peakPackages());
        root.addProperty("spawnedPackages", reportData.spawnedPackages());

        JsonArray hotspots = new JsonArray();
        for (Map.Entry<ProfiledSection, CreateProfilerManager.MethodStats> entry : reportData.methodEntries()) {
            ProfiledSection section = entry.getKey();
            CreateProfilerManager.MethodStats stats = entry.getValue();
            JsonObject hotspot = new JsonObject();
            hotspot.addProperty("label", section.label());
            hotspot.addProperty("method", section.displayName());
            hotspot.addProperty("family", section.family());
            hotspot.addProperty("totalMs", stats.totalMillis());
            hotspot.addProperty("msPerTick", millisPerTick(session, stats));
            hotspot.addProperty("calls", stats.calls());
            hotspot.addProperty("callsPerTick", callsPerTick(session, stats));
            hotspot.addProperty("sharePercent", reportData.totalProfiledMs() <= 0.0D
                ? 0.0D : (stats.totalMillis() / reportData.totalProfiledMs()) * 100.0D);
            hotspots.add(hotspot);
        }
        root.add("hotspots", hotspots);

        JsonArray families = new JsonArray();
        for (FamilySummary family : reportData.familySummaries()) {
            JsonObject entry = new JsonObject();
            entry.addProperty("family", family.family());
            entry.addProperty("totalMs", family.totalMillis());
            entry.addProperty("msPerTick", family.millisPerTick());
            entry.addProperty("callsPerTick", family.callsPerTick());
            entry.addProperty("calls", family.calls());
            families.add(entry);
        }
        root.add("families", families);

        JsonObject packages = new JsonObject();
        packages.addProperty("active", reportData.packageDiagnostics().active());
        packages.addProperty("averageAgeSeconds", reportData.packageDiagnostics().averageAgeSeconds());
        packages.addProperty("maxAgeSeconds", reportData.packageDiagnostics().maxAgeSeconds());
        packages.addProperty("stationaryCandidates", reportData.packageDiagnostics().stationaryCandidates());
        packages.addProperty("oldPackages", reportData.packageDiagnostics().oldPackages());
        JsonArray stalled = new JsonArray();
        for (PackageEntityMonitor.PackageSnapshot snapshot : reportData.packageDiagnostics().stalledPackages()) {
            JsonObject entry = new JsonObject();
            entry.addProperty("dimension", SignatureUtil.dimensionId(snapshot.dimension()));
            entry.addProperty("x", snapshot.x());
            entry.addProperty("y", snapshot.y());
            entry.addProperty("z", snapshot.z());
            entry.addProperty("chunkX", snapshot.chunkX());
            entry.addProperty("chunkZ", snapshot.chunkZ());
            entry.addProperty("ageSeconds", snapshot.ageSeconds());
            entry.addProperty("insertionDelay", snapshot.insertionDelay());
            entry.addProperty("speed", snapshot.speed());
            stalled.add(entry);
        }
        packages.add("stalled", stalled);
        root.add("packages", packages);

        JsonObject optimization = new JsonObject();
        OptimizationStats.Snapshot optimizationStats = reportData.optimizationStats();
        optimization.addProperty("totalEvents", optimizationStats.totalFastRejects());
        optimization.addProperty("beltFunnelFastRejects", optimizationStats.beltFunnelFastRejects());
        optimization.addProperty("deployerFullHandFastRejects", optimizationStats.deployerFullHandFastRejects());
        optimization.addProperty("divingBootsMarkerWritesSkipped", optimizationStats.divingBootsMarkerWritesSkipped());
        optimization.addProperty("divingBootsMarkerRemovalsSkipped", optimizationStats.divingBootsMarkerRemovalsSkipped());
        optimization.addProperty("estimatedAvoidedStackCopies", optimizationStats.estimatedAvoidedStackCopies());
        optimization.addProperty("estimatedAvoidedStackSplits", optimizationStats.estimatedAvoidedStackSplits());
        optimization.add("spoutCache", cacheJson(optimizationStats.spoutCache()));
        optimization.add("basinMemo", cacheJson(optimizationStats.basinCache()));
        optimization.add("crafterMemo", cacheJson(optimizationStats.crafterCache()));
        root.add("optimization", optimization);

        root.add("alerts", stringArray(reportData.alerts()));
        root.add("recommendations", stringArray(reportData.debrief().recommendations()));
        return JSON.toJson(root);
    }

    private static JsonObject cacheJson(OptimizationStats.CacheSnapshot cache) {
        JsonObject object = new JsonObject();
        object.addProperty("lookups", cache.lookups());
        object.addProperty("hits", cache.hits());
        object.addProperty("misses", cache.misses());
        object.addProperty("hitRate", cache.hitRate());
        object.addProperty("evictions", cache.evictions());
        object.addProperty("invalidations", cache.invalidations());
        object.addProperty("negativeResults", cache.negativeResults());
        object.addProperty("keyBuildNanos", cache.keyBuildNanos());
        object.addProperty("maxSizeReached", cache.maxSizeReached());
        return object;
    }

    private static JsonArray stringArray(List<String> values) {
        JsonArray array = new JsonArray();
        values.forEach(array::add);
        return array;
    }

    private static String buildHtmlReport(ReportData reportData) {
        CreateProfilerManager.ProfileSession session = reportData.session();
        StringBuilder builder = new StringBuilder();
        builder.append("<!DOCTYPE html>")
            .append("<html lang=\"en\">")
            .append("<head>")
            .append("<meta charset=\"utf-8\">")
            .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">")
            .append("<title>Create: Advanced Optimization Report</title>")
            .append("<style>")
            .append("body{margin:0;font-family:Segoe UI,Arial,sans-serif;background:#09111f;color:#e7edf7;}")
            .append(".page{max-width:1240px;margin:0 auto;padding:32px 20px 48px;}")
            .append(".hero{background:linear-gradient(135deg,#13233c,#0c5f6d);border:1px solid rgba(255,255,255,.08);border-radius:24px;padding:24px;box-shadow:0 24px 60px rgba(0,0,0,.22);}")
            .append(".hero-top{display:flex;gap:20px;align-items:center;}")
            .append(".hero-logo{width:120px;height:120px;object-fit:contain;border-radius:50%;background:rgba(255,255,255,.08);border:1px solid rgba(255,255,255,.14);padding:8px;box-shadow:0 18px 40px rgba(0,0,0,.24);}")
            .append(".hero-copy{flex:1;min-width:0;}")
            .append(".eyebrow{font-size:12px;letter-spacing:.16em;text-transform:uppercase;color:#9dc7d6;}")
            .append("h1{margin:8px 0 10px;font-size:34px;line-height:1.1;}")
            .append(".subtitle{margin:0;color:#c9d7ea;max-width:760px;line-height:1.55;}")
            .append(".hero-meta{display:flex;flex-wrap:wrap;gap:10px;margin-top:16px;}")
            .append(".chip{background:rgba(255,255,255,.08);border:1px solid rgba(255,255,255,.12);border-radius:999px;padding:8px 12px;font-size:13px;}")
            .append(".actions{display:flex;flex-wrap:wrap;gap:10px;margin-top:18px;}")
            .append(".actions a{display:inline-block;background:#f5c85c;color:#17212f;text-decoration:none;font-weight:700;border-radius:999px;padding:10px 16px;}")
            .append(".actions a.alt{background:#2a384f;color:#e7edf7;}")
            .append(".grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(220px,1fr));gap:16px;margin-top:20px;}")
            .append(".card{background:#111c30;border:1px solid rgba(255,255,255,.08);border-radius:18px;padding:18px;box-shadow:0 14px 35px rgba(0,0,0,.18);}")
            .append(".label{font-size:12px;letter-spacing:.08em;text-transform:uppercase;color:#97acc4;}")
            .append(".value{margin-top:8px;font-size:30px;font-weight:700;}")
            .append(".value-small{margin-top:8px;font-size:20px;font-weight:700;}")
            .append(".section{margin-top:24px;}")
            .append(".section h2{margin:0 0 12px;font-size:22px;}")
            .append(".split{display:grid;grid-template-columns:1.15fr .85fr;gap:16px;}")
            .append(".guide-grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(280px,1fr));gap:16px;}")
            .append("table{width:100%;border-collapse:collapse;overflow:hidden;border-radius:16px;background:#111c30;border:1px solid rgba(255,255,255,.08);}")
            .append("th,td{padding:12px 14px;text-align:left;border-bottom:1px solid rgba(255,255,255,.06);font-size:14px;vertical-align:top;}")
            .append("th{font-size:12px;letter-spacing:.08em;text-transform:uppercase;color:#97acc4;background:rgba(255,255,255,.03);}")
            .append("tr:last-child td{border-bottom:none;}")
            .append(".bar{height:8px;background:#1d2940;border-radius:999px;overflow:hidden;}")
            .append(".bar span{display:block;height:100%;background:linear-gradient(90deg,#f5c85c,#45d5ae);}")
            .append(".hotspot-title{font-size:18px;font-weight:700;color:#f4f7fb;}")
            .append(".hotspot-tech{margin-top:6px;font-family:Consolas,monospace;font-size:12px;color:#8cc6db;word-break:break-word;}")
            .append(".hotspot-copy{margin-top:12px;color:#d7e2ef;line-height:1.55;}")
            .append(".pill-row{display:flex;flex-wrap:wrap;gap:8px;margin-top:12px;}")
            .append(".pill{display:inline-block;padding:6px 10px;border-radius:999px;background:#1f3047;border:1px solid rgba(255,255,255,.08);font-size:12px;color:#c4d4e8;}")
            .append(".debrief{background:#0f1a2d;border:1px solid rgba(255,255,255,.08);border-radius:18px;padding:20px;}")
            .append(".debrief-headline{font-size:24px;font-weight:700;color:#f7fbff;}")
            .append(".debrief-copy{margin-top:10px;color:#d6e2ef;line-height:1.6;}")
            .append(".debrief-list{margin:14px 0 0;padding-left:18px;color:#d6e2ef;}")
            .append(".debrief-list li{margin:8px 0;}")
            .append(".empty{padding:16px;border-radius:16px;background:#111c30;border:1px dashed rgba(255,255,255,.12);color:#c9d7ea;}")
            .append(".alerts{display:grid;gap:12px;}")
            .append(".alert{background:#2c1b1b;border:1px solid rgba(255,120,120,.28);color:#ffd8d8;border-radius:14px;padding:14px;}")
            .append(".ok{background:#12251d;border:1px solid rgba(87,214,146,.22);color:#d8ffe7;border-radius:14px;padding:14px;}")
            .append(".muted{color:#97acc4;}")
            .append("@media (max-width:960px){.split{grid-template-columns:1fr;}.hero-top{flex-direction:column;align-items:flex-start;}}")
            .append("@media (max-width:720px){h1{font-size:28px}.page{padding:20px 14px 32px}.hero-logo{width:96px;height:96px}}")
            .append("</style>")
            .append("</head>")
            .append("<body>")
            .append("<div class=\"page\">")
            .append("<section class=\"hero\">")
            .append("<div class=\"hero-top\">");

        if (reportData.logoDataUri() != null) {
            builder.append("<img class=\"hero-logo\" src=\"")
                .append(reportData.logoDataUri())
                .append("\" alt=\"Create: Advanced Optimization logo\">");
        }

        builder.append("<div class=\"hero-copy\">")
            .append("<div class=\"eyebrow\">Create diagnostics report</div>")
            .append("<h1>Create: Advanced Optimization</h1>")
            .append("<p class=\"subtitle\">Focused server-side Create performance report with a short debrief first, then the deeper hotspot breakdown for admins and modpack developers.</p>")
            .append("<div class=\"hero-meta\">")
            .append("<div class=\"chip\" title=\"Unique session identifier\">Session ").append(escapeHtml(session.sessionId())).append("</div>")
            .append("<div class=\"chip\" title=\"Pinned Create version\">Create ").append(escapeHtml(CreateCompatibility.getLoadedCreateVersion())).append("</div>")
            .append("<div class=\"chip\" title=\"CAO mod version\">Mod ").append(escapeHtml(CreateAdvancedOptimization.getModVersion())).append("</div>")
            .append("<div class=\"chip\" title=\"UTC start timestamp\">Started ").append(escapeHtml(DISPLAY_TIMESTAMP.format(session.startedAt()))).append("</div>")
            .append("</div>")
            .append("<div class=\"actions\">")
            .append("<a href=\"").append(escapeHtml(reportData.textFileName())).append("\" title=\"Open the debrief text export\">Open TXT Debrief</a>")
            .append("<a class=\"alt\" href=\"./\" title=\"Open the folder containing this report\">Open Report Folder</a>")
            .append("<a class=\"alt\" href=\"#\" onclick=\"navigator.clipboard.writeText('")
            .append(escapeJs(reportData.reportFolderPath()))
            .append("');return false;\" title=\"Copy the report folder path\">Copy Folder Path</a>")
            .append("</div>")
            .append("</div>")
            .append("</div>")
            .append("</section>");

        builder.append("<section class=\"section\">")
            .append("<div class=\"debrief\">")
            .append("<div class=\"debrief-headline\">").append(escapeHtml(reportData.debrief().headline())).append("</div>")
            .append("<div class=\"debrief-copy\">").append(escapeHtml(reportData.debrief().summary())).append("</div>")
            .append("<ul class=\"debrief-list\">");
        for (String recommendation : reportData.debrief().recommendations()) {
            builder.append("<li>").append(escapeHtml(recommendation)).append("</li>");
        }
        builder.append("</ul></div></section>");

        builder.append("<section class=\"grid\">");
        appendSummaryCard(builder, "Average MSPT", formatDecimal(session.averageMspt()));
        appendSummaryCard(builder, "Maximum MSPT", formatDecimal(session.maxMspt()));
        appendSummaryCard(builder, "Ticks Above " + CAOServerConfig.LAG_SPIKE_THRESHOLD_MS.get() + " ms", Long.toString(session.ticksAboveThreshold()));
        appendSummaryCard(builder, "Active Packages", Integer.toString(reportData.activePackages()));
        appendSummaryCard(builder, "Peak Packages", Integer.toString(reportData.peakPackages()));
        appendSummaryCard(builder, "Profiled Create Time", formatDecimal(reportData.totalProfiledMs()) + " ms");
        appendSummaryCard(builder, "Optimization Events", Long.toString(reportData.optimizationStats().totalFastRejects()));
        appendSummaryCard(builder, "Avoided Copies", Long.toString(reportData.optimizationStats().estimatedAvoidedStackCopies()));
        builder.append("</section>");

        builder.append("<section class=\"section\">")
            .append("<h2>Session Optimization Events</h2>")
            .append("<table><thead><tr><th>Optimization</th><th>Events</th><th>Estimated Avoided Copies</th><th>Estimated Avoided Splits</th></tr></thead><tbody>")
            .append("<tr><td>Belt Funnel exact oversized insertion reject</td><td>")
            .append(reportData.optimizationStats().beltFunnelFastRejects())
            .append("</td><td>")
            .append(reportData.optimizationStats().beltFunnelFastRejects())
            .append("</td><td>0</td></tr>")
            .append("<tr><td>Deployer full hand insertion reject</td><td>")
            .append(reportData.optimizationStats().deployerFullHandFastRejects())
            .append("</td><td>")
            .append(reportData.optimizationStats().deployerFullHandFastRejects())
            .append("</td><td>")
            .append(reportData.optimizationStats().deployerFullHandFastRejects())
            .append("</td></tr>")
            .append("<tr><td>Diving Boots redundant marker write skipped</td><td>")
            .append(reportData.optimizationStats().divingBootsMarkerWritesSkipped())
            .append("</td><td>0</td><td>0</td></tr>")
            .append("<tr><td>Diving Boots redundant marker removal skipped</td><td>")
            .append(reportData.optimizationStats().divingBootsMarkerRemovalsSkipped())
            .append("</td><td>0</td><td>0</td></tr>")
            .append("<tr><td><strong>Total</strong></td><td><strong>")
            .append(reportData.optimizationStats().totalFastRejects())
            .append("</strong></td><td><strong>")
            .append(reportData.optimizationStats().estimatedAvoidedStackCopies())
            .append("</strong></td><td><strong>")
            .append(reportData.optimizationStats().estimatedAvoidedStackSplits())
            .append("</strong></td></tr>")
            .append("</tbody></table>")
            .append("</section>");

        builder.append("<section class=\"section\">")
            .append("<h2>Hotspot Families</h2>");
        if (reportData.familySummaries().isEmpty()) {
            builder.append("<div class=\"empty\">No targeted Create families were sampled during this session.</div>");
        } else {
            builder.append("<table><thead><tr><th>Family</th><th>Total ms</th><th>ms/tick</th><th>calls/tick</th><th>Total calls</th><th>Share</th></tr></thead><tbody>");
            for (FamilySummary familySummary : reportData.familySummaries()) {
                double share = reportData.totalProfiledMs() <= 0.0D ? 0.0D : (familySummary.totalMillis() / reportData.totalProfiledMs()) * 100.0D;
                builder.append("<tr>")
                    .append("<td>").append(escapeHtml(familySummary.family())).append("</td>")
                    .append("<td>").append(formatDecimal(familySummary.totalMillis())).append("</td>")
                    .append("<td>").append(formatDecimal(familySummary.millisPerTick())).append("</td>")
                    .append("<td>").append(formatDecimal(familySummary.callsPerTick())).append("</td>")
                    .append("<td>").append(familySummary.calls()).append("</td>")
                    .append("<td><div class=\"bar\"><span style=\"width:")
                    .append(formatDecimal(Math.min(100.0D, share)))
                    .append("%\"></span></div></td>")
                    .append("</tr>");
            }
            builder.append("</tbody></table>");
        }
        builder.append("</section>");

        builder.append("<section class=\"section\">")
            .append("<h2>Hotspot Guide</h2>");
        if (reportData.sampledSections().isEmpty()) {
            builder.append("<div class=\"empty\">No sampled hotspot guide is available because none of the targeted Create methods were hit during this session.</div>");
        } else {
            builder.append("<div class=\"guide-grid\">");
            for (ProfiledSection section : reportData.sampledSections()) {
                builder.append("<article class=\"card\">")
                    .append("<div class=\"hotspot-title\">").append(escapeHtml(section.label())).append("</div>")
                    .append("<div class=\"hotspot-tech\" title=\"Technical method name used by Create and mixin profiling\">").append(escapeHtml(section.displayName())).append("</div>")
                    .append("<div class=\"pill-row\">")
                    .append("<span class=\"pill\">Family: ").append(escapeHtml(section.family())).append("</span>")
                    .append("</div>")
                    .append("<div class=\"hotspot-copy\"><strong>What it is:</strong> ").append(escapeHtml(section.description())).append("</div>")
                    .append("<div class=\"hotspot-copy\"><strong>Typical symptoms:</strong> ").append(escapeHtml(section.symptoms())).append("</div>")
                    .append("</article>");
            }
            builder.append("</div>");
        }
        builder.append("</section>");

        builder.append("<section class=\"section\">")
            .append("<h2>Profiled Create Methods</h2>");
        if (reportData.methodEntries().isEmpty()) {
            builder.append("<div class=\"empty\">No targeted Create methods were sampled during this session.</div>");
        } else {
            builder.append("<table><thead><tr><th>Hotspot</th><th>Family</th><th>Total ms</th><th>Avg micros</th><th>ms/tick</th><th>calls/tick</th><th>Total calls</th><th>Share</th></tr></thead><tbody>");
            for (Map.Entry<ProfiledSection, CreateProfilerManager.MethodStats> entry : reportData.methodEntries()) {
                ProfiledSection section = entry.getKey();
                double share = reportData.totalProfiledMs() <= 0.0D ? 0.0D : (entry.getValue().totalMillis() / reportData.totalProfiledMs()) * 100.0D;
                builder.append("<tr>")
                    .append("<td title=\"")
                    .append(escapeHtml(section.description()))
                    .append(" Typical symptoms: ")
                    .append(escapeHtml(section.symptoms()))
                    .append("\"><strong>")
                    .append(escapeHtml(section.label()))
                    .append("</strong><br><span class=\"muted\">")
                    .append(escapeHtml(section.displayName()))
                    .append("</span></td>")
                    .append("<td class=\"muted\">").append(escapeHtml(section.family())).append("</td>")
                    .append("<td>").append(formatDecimal(entry.getValue().totalMillis())).append("</td>")
                    .append("<td>").append(formatDecimal(entry.getValue().averageMicros())).append("</td>")
                    .append("<td>").append(formatDecimal(millisPerTick(session, entry.getValue()))).append("</td>")
                    .append("<td>").append(formatDecimal(callsPerTick(session, entry.getValue()))).append("</td>")
                    .append("<td>").append(entry.getValue().calls()).append("</td>")
                    .append("<td><div class=\"bar\"><span style=\"width:")
                    .append(formatDecimal(Math.min(100.0D, share)))
                    .append("%\"></span></div></td>")
                    .append("</tr>");
            }
            builder.append("</tbody></table>");
        }
        builder.append("</section>");

        builder.append("<section class=\"section split\">");
        builder.append("<div>")
            .append("<h2>PackageEntity Summary</h2>");
        if (reportData.dimensionEntries().isEmpty()) {
            builder.append("<div class=\"empty\">No active Create PackageEntity instances were tracked by dimension at export time.</div>");
        } else {
            builder.append("<table><thead><tr><th>Dimension</th><th>Active Packages</th></tr></thead><tbody>");
            for (Map.Entry<ResourceKey<Level>, Integer> entry : reportData.dimensionEntries()) {
                builder.append("<tr>")
                    .append("<td>").append(escapeHtml(SignatureUtil.dimensionId(entry.getKey()))).append("</td>")
                    .append("<td>").append(entry.getValue()).append("</td>")
                    .append("</tr>");
            }
            builder.append("</tbody></table>");
        }
        builder.append("<div class=\"grid\">");
        appendSmallCard(builder, "Spawned During Profile", Long.toString(reportData.spawnedPackages()));
        appendSmallCard(builder, "Requested Duration", session.requestedDurationSeconds() + " s");
        appendSmallCard(builder, "Tick Samples", Long.toString(session.tickSamples()));
        appendSmallCard(builder, "Average Age", formatDecimal(reportData.packageDiagnostics().averageAgeSeconds()) + " s");
        appendSmallCard(builder, "Max Age", formatDecimal(reportData.packageDiagnostics().maxAgeSeconds()) + " s");
        appendSmallCard(builder, "Stationary Candidates", Integer.toString(reportData.packageDiagnostics().stationaryCandidates()));
        appendSmallCard(builder, "Older Than 60s", Integer.toString(reportData.packageDiagnostics().oldPackages()));
        builder.append("</div></div>");

        builder.append("<div>")
            .append("<h2>Top Package Chunks</h2>");
        if (reportData.topChunks().isEmpty()) {
            builder.append("<div class=\"empty\">No package-heavy chunks were detected in loaded levels at export time.</div>");
        } else {
            builder.append("<table><thead><tr><th>Dimension</th><th>Chunk</th><th>Block Range</th><th>Packages</th></tr></thead><tbody>");
            for (PackageEntityMonitor.ChunkPackageCount entry : reportData.topChunks()) {
                builder.append("<tr>")
                    .append("<td>").append(escapeHtml(SignatureUtil.dimensionId(entry.key().dimension()))).append("</td>")
                    .append("<td>[").append(entry.key().chunkX()).append(", ").append(entry.key().chunkZ()).append("]</td>")
                    .append("<td>x=").append(entry.key().minBlockX()).append("..").append(entry.key().maxBlockX())
                    .append("<br>z=").append(entry.key().minBlockZ()).append("..").append(entry.key().maxBlockZ()).append("</td>")
                    .append("<td>").append(entry.count()).append("</td>")
                    .append("</tr>");
            }
            builder.append("</tbody></table>");
        }
        builder.append("</div>")
            .append("</section>");

        builder.append("<section class=\"section\">")
            .append("<h2>Stalled Package Candidates</h2>");
        if (reportData.packageDiagnostics().stalledPackages().isEmpty()) {
            builder.append("<div class=\"empty\">No stationary package candidates were detected at export time.</div>");
        } else {
            builder.append("<table><thead><tr><th>Dimension</th><th>Position</th><th>Chunk</th><th>Age</th><th>Insertion Delay</th><th>Speed</th></tr></thead><tbody>");
            for (PackageEntityMonitor.PackageSnapshot snapshot : reportData.packageDiagnostics().stalledPackages()) {
                builder.append("<tr>")
                    .append("<td>").append(escapeHtml(SignatureUtil.dimensionId(snapshot.dimension()))).append("</td>")
                    .append("<td>[")
                    .append(formatDecimal(snapshot.x())).append(", ")
                    .append(formatDecimal(snapshot.y())).append(", ")
                    .append(formatDecimal(snapshot.z())).append("]</td>")
                    .append("<td>[").append(snapshot.chunkX()).append(", ").append(snapshot.chunkZ()).append("]</td>")
                    .append("<td>").append(formatDecimal(snapshot.ageSeconds())).append(" s</td>")
                    .append("<td>").append(snapshot.insertionDelay()).append(" ticks</td>")
                    .append("<td>").append(formatDecimal(snapshot.speed())).append("</td>")
                    .append("</tr>");
            }
            builder.append("</tbody></table>");
        }
        builder.append("</section>");

        builder.append("<section class=\"section\">")
            .append("<h2>Alerts</h2>")
            .append("<div class=\"alerts\">");
        if (reportData.alerts().isEmpty()) {
            builder.append("<div class=\"ok\">No configured warning threshold was exceeded during this export.</div>");
        } else {
            for (String alert : reportData.alerts()) {
                builder.append("<div class=\"alert\">").append(escapeHtml(alert)).append("</div>");
            }
        }
        builder.append("</div></section>")
            .append("</div>")
            .append("</body>")
            .append("</html>");

        return builder.toString();
    }

    private static String loadLogoDataUri() {
        try (InputStream inputStream = ReportWriter.class.getClassLoader().getResourceAsStream(LOGO_RESOURCE_NAME)) {
            if (inputStream == null) {
                return null;
            }
            byte[] bytes = inputStream.readAllBytes();
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes);
        } catch (IOException ignored) {
            return null;
        }
    }

    private static void appendSummaryCard(StringBuilder builder, String label, String value) {
        builder.append("<div class=\"card\">")
            .append("<div class=\"label\">").append(escapeHtml(label)).append("</div>")
            .append("<div class=\"value\">").append(escapeHtml(value)).append("</div>")
            .append("</div>");
    }

    private static void appendSmallCard(StringBuilder builder, String label, String value) {
        builder.append("<div class=\"card\">")
            .append("<div class=\"label\">").append(escapeHtml(label)).append("</div>")
            .append("<div class=\"value-small\">").append(escapeHtml(value)).append("</div>")
            .append("</div>");
    }

    private static double millisPerTick(CreateProfilerManager.ProfileSession session, CreateProfilerManager.MethodStats methodStats) {
        return session.tickSamples() == 0 ? 0.0D : methodStats.totalMillis() / session.tickSamples();
    }

    private static double number(JsonObject object, String key) {
        return object.has(key) ? object.get(key).getAsDouble() : 0.0D;
    }

    private static long integer(JsonObject object, String key) {
        return object.has(key) ? object.get(key).getAsLong() : 0L;
    }

    private static String topHotspot(JsonObject object) {
        JsonArray hotspots = object.getAsJsonArray("hotspots");
        return hotspots == null || hotspots.isEmpty() ? "none" : hotspots.get(0).getAsJsonObject().get("label").getAsString();
    }

    private static double topHotspotMsPerTick(JsonObject object) {
        JsonArray hotspots = object.getAsJsonArray("hotspots");
        return hotspots == null || hotspots.isEmpty() ? 0.0D : hotspots.get(0).getAsJsonObject().get("msPerTick").getAsDouble();
    }

    private static double callsPerTick(CreateProfilerManager.ProfileSession session, CreateProfilerManager.MethodStats methodStats) {
        return session.tickSamples() == 0 ? 0.0D : methodStats.calls() / (double) session.tickSamples();
    }

    private static String formatDecimal(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }

    private static String escapeHtml(String value) {
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;");
    }

    private static String escapeJs(String value) {
        return value
            .replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\r", "\\r")
            .replace("\n", "\\n");
    }

    public record ExportedReport(Path textReport, Path htmlReport, Path jsonReport) {
    }

    public record ReportComparison(
        Path newerReport,
        Path olderReport,
        double newerAverageMspt,
        double olderAverageMspt,
        double newerMaxMspt,
        double olderMaxMspt,
        long newerTicksAboveThreshold,
        long olderTicksAboveThreshold,
        double newerProfiledCreateMs,
        double olderProfiledCreateMs,
        String newerTopHotspot,
        double newerTopHotspotMsPerTick
    ) {
    }

    private record ReportData(
        CreateProfilerManager.ProfileSession session,
        List<Map.Entry<ProfiledSection, CreateProfilerManager.MethodStats>> methodEntries,
        List<ProfiledSection> sampledSections,
        List<FamilySummary> familySummaries,
        List<Map.Entry<ResourceKey<Level>, Integer>> dimensionEntries,
        List<PackageEntityMonitor.ChunkPackageCount> topChunks,
        List<String> alerts,
        int activePackages,
        int peakPackages,
        long spawnedPackages,
        PackageEntityMonitor.PackageDiagnostics packageDiagnostics,
        OptimizationStats.Snapshot optimizationStats,
        double totalProfiledMs,
        String textFileName,
        String reportFolderPath,
        Debrief debrief,
        String logoDataUri
    ) {
    }

    private record Debrief(String headline, String summary, List<String> recommendations) {
    }

    private record FamilySummary(String family, double totalMillis, double millisPerTick, double callsPerTick, long calls) {
    }

    private static final class FamilySummaryAccumulator {
        private final String family;
        private double totalMillis;
        private long calls;

        private FamilySummaryAccumulator(String family) {
            this.family = family;
        }

        private void record(double methodMillis, long methodCalls) {
            totalMillis += methodMillis;
            calls += methodCalls;
        }

        private FamilySummary toSummary(long tickSamples) {
            double sampledTicks = tickSamples == 0 ? 1.0D : tickSamples;
            return new FamilySummary(family, totalMillis, totalMillis / sampledTicks, calls / sampledTicks, calls);
        }
    }
}
