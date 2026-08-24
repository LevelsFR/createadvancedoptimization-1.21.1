package net.levelsfr.createadvancedoptimization.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.levelsfr.createadvancedoptimization.CreateAdvancedOptimization;
import net.levelsfr.createadvancedoptimization.compatibility.CreateCompatibility;
import net.levelsfr.createadvancedoptimization.config.CAOServerConfig;
import net.levelsfr.createadvancedoptimization.diagnostics.OptimizationStats;
import net.levelsfr.createadvancedoptimization.diagnostics.belts.BeltDiagnostics;
import net.levelsfr.createadvancedoptimization.diagnostics.packages.PackageEntityMonitor;
import net.levelsfr.createadvancedoptimization.diagnostics.profiler.CreateProfilerManager;
import net.levelsfr.createadvancedoptimization.diagnostics.profiler.ReportWriter;
import net.levelsfr.createadvancedoptimization.diagnostics.profiler.ReportWriter.ExportedReport;
import net.levelsfr.createadvancedoptimization.optimization.processing.ProcessingMemoizationService;
import net.levelsfr.createadvancedoptimization.util.SignatureUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class CAOCommands {

    private CAOCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(createRoot("createadvancedoptimization"));
        dispatcher.register(createRoot("cao"));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> createRoot(String name) {
        return Commands.literal(name)
            .requires(source -> source.hasPermission(2))
            .then(Commands.literal("status")
                .executes(context -> status(context.getSource())))
            .then(Commands.literal("stats")
                .executes(context -> stats(context.getSource()))
                .then(Commands.literal("reset")
                    .executes(context -> reset(context.getSource()))))
            .then(Commands.literal("profile")
                .then(Commands.literal("start")
                    .then(Commands.argument("seconds", IntegerArgumentType.integer(5, 3600))
                        .executes(context -> startProfile(context.getSource(), IntegerArgumentType.getInteger(context, "seconds")))))
                .then(Commands.literal("stop")
                    .executes(context -> stopProfile(context.getSource()))))
            .then(Commands.literal("report")
                .executes(context -> writeReport(context.getSource()))
                .then(Commands.literal("last")
                    .executes(context -> showLastReport(context.getSource()))))
            .then(Commands.literal("packages")
                .then(Commands.literal("top")
                    .executes(context -> packagesTop(context.getSource(), 10))
                    .then(Commands.argument("limit", IntegerArgumentType.integer(1, 50))
                        .executes(context -> packagesTop(context.getSource(), IntegerArgumentType.getInteger(context, "limit")))))
                .then(Commands.literal("stalled")
                    .executes(context -> packagesStalled(context.getSource(), 10))
                    .then(Commands.argument("limit", IntegerArgumentType.integer(1, 50))
                        .executes(context -> packagesStalled(context.getSource(), IntegerArgumentType.getInteger(context, "limit"))))))
            .then(Commands.literal("belts")
                .then(Commands.literal("scan")
                    .executes(context -> beltsScan(context.getSource(), 10))
                    .then(Commands.argument("limit", IntegerArgumentType.integer(1, 50))
                        .executes(context -> beltsScan(context.getSource(), IntegerArgumentType.getInteger(context, "limit"))))))
            .then(Commands.literal("reset")
                .executes(context -> reset(context.getSource())));
    }

    private static int status(CommandSourceStack source) {
        PackageEntityMonitor monitor = PackageEntityMonitor.getInstance();
        CreateProfilerManager.ProfileSession active = CreateProfilerManager.getActiveSession();
        OptimizationStats.Snapshot stats = OptimizationStats.snapshot();
        PackageEntityMonitor.PackageDiagnostics packageDiagnostics = monitor.collectDiagnostics(source.getServer(), 0);

        send(source, false, titleLine("Create: Advanced Optimization", "Status"));
        send(source, false, statusLine("Create", CreateCompatibility.getLoadedCreateVersion() + " (required " + CreateCompatibility.REQUIRED_CREATE_VERSION + ")"));
        send(source, false, statusLine("Version", CreateAdvancedOptimization.getModVersion()));
        send(source, false, statusLine("General", boolLabel(CAOServerConfig.GENERAL_ENABLED.get())));
        send(source, false, statusLine("Diagnostics", boolLabel(CAOServerConfig.DIAGNOSTICS_ENABLED.get())));
        send(source, false, statusLine("Profiler", active != null && CreateProfilerManager.isProfilingEnabled() ? "active for " + active.requestedDurationSeconds() + "s" : "idle"));
        send(source, false, statusLine("Packages", monitor.getActiveTotal() + " active, peak " + monitor.getPeakActiveTotal()
            + ", avg age " + formatDecimal(packageDiagnostics.averageAgeSeconds()) + "s, stalled " + packageDiagnostics.stationaryCandidates()));
        send(source, false, statusLine("Optimization Events", stats.totalFastRejects()
            + " total, avoided copies~" + stats.estimatedAvoidedStackCopies()
            + ", avoided splits~" + stats.estimatedAvoidedStackSplits()
            + ", Diving Boots fast paths " + stats.divingBootsNoBootFastPaths()));
        send(source, false, statusLine("Spout Cache", cacheLine(stats.spoutCache())));
        send(source, false, statusLine("Basin Memo", cacheLine(stats.basinCache())));
        send(source, false, statusLine("Crafter Memo", cacheLine(stats.crafterCache())));
        send(source, false, statusLine("Diving Boots", boolLabel(CAOServerConfig.DIVING_BOOTS_ENABLED.get())));
        send(source, false, statusLine("Spout Cache Enabled", boolLabel(CAOServerConfig.SPOUT_RECIPE_CACHE_ENABLED.get())));
        send(source, false, statusLine("Processing Memo Enabled", boolLabel(CAOServerConfig.PROCESSING_RECIPE_MEMOIZATION_ENABLED.get())));
        send(source, false, statusLine("Belt Tick Fast Paths", boolLabel(CAOServerConfig.BELT_TICK_FAST_PATHS_ENABLED.get())));
        send(source, false, statusLine("Experimental Packages", boolLabel(CAOServerConfig.EXPERIMENTAL_PACKAGES_ENABLED.get())));
        return 1;
    }

    private static int stats(CommandSourceStack source) {
        OptimizationStats.Snapshot stats = OptimizationStats.snapshot();
        send(source, false, titleLine("Create: Advanced Optimization", "Stats"));
        send(source, false, statusLine("Belt Funnel Fast Rejects", Long.toString(stats.beltFunnelFastRejects())));
        send(source, false, statusLine("Deployer Full-Hand Rejects", Long.toString(stats.deployerFullHandFastRejects())));
        send(source, false, statusLine("Diving Boots Marker Writes Skipped", Long.toString(stats.divingBootsMarkerWritesSkipped())));
        send(source, false, statusLine("Diving Boots Marker Removals Skipped", Long.toString(stats.divingBootsMarkerRemovalsSkipped())));
        send(source, false, statusLine("Spout Cache", detailedCacheLine(stats.spoutCache())));
        send(source, false, statusLine("Basin Memo", detailedCacheLine(stats.basinCache())));
        send(source, false, statusLine("Crafter Memo", detailedCacheLine(stats.crafterCache())));
        send(source, false, Component.literal(" Timing fields are populated only by diagnostic sampling builds; zero means no sampled timing was recorded.")
            .withStyle(ChatFormatting.DARK_GRAY));
        return 1;
    }

    private static int startProfile(CommandSourceStack source, int seconds) {
        MinecraftServer server = source.getServer();
        CreateProfilerManager.StartResult result = CreateProfilerManager.start(server, seconds, source.getDisplayName().getString());
        if (result == CreateProfilerManager.StartResult.STARTED) {
            send(source, true, titleLine("Create Profiler", "Started"));
            send(source, true, Component.literal(" Profiling for ")
                .withStyle(ChatFormatting.GRAY)
                .append(Component.literal(Integer.toString(seconds)).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD))
                .append(Component.literal(" seconds.").withStyle(ChatFormatting.GRAY)));
            send(source, false, Component.literal(" A report will be exported automatically when the session ends.")
                .withStyle(ChatFormatting.DARK_GRAY));
            return 1;
        }
        send(source, false, profilerStartError(result));
        return 0;
    }

    private static int stopProfile(CommandSourceStack source) {
        CreateProfilerManager.ProfileSession session = CreateProfilerManager.stop();
        if (session == null) {
            send(source, false, errorLine(Component.translatable("command.createadvancedoptimization.profile.error.no_active")));
            return 0;
        }
        return exportSessionToSource(source, session, "Create Profiler", "Stopped");
    }

    private static int writeReport(CommandSourceStack source) {
        CreateProfilerManager.ProfileSession session = CreateProfilerManager.getActiveSession();
        if (session == null) {
            session = CreateProfilerManager.getLastCompletedSession();
        }
        if (session == null) {
            send(source, false, errorLine(Component.translatable("command.createadvancedoptimization.profile.error.no_report")));
            return 0;
        }

        try {
            ExportedReport report = ReportWriter.writeReport(session, PackageEntityMonitor.getInstance(), source.getServer());
            sendReportExport(source, true, "Create Report", "Exported", report);
            return 1;
        } catch (Exception exception) {
            CreateAdvancedOptimization.LOGGER.error("Failed to write report.", exception);
            send(source, false, errorLine("Failed to export report: " + exception.getMessage()));
            return 0;
        }
    }

    private static int showLastReport(CommandSourceStack source) {
        try {
            ExportedReport report = ReportWriter.findLatestReport();
            if (report == null) {
                send(source, false, errorLine("No exported report was found yet."));
                return 0;
            }
            send(source, false, reportExportComponent("Create Report", "Last Export", report));
            return 1;
        } catch (Exception exception) {
            CreateAdvancedOptimization.LOGGER.error("Failed to locate the latest report export.", exception);
            send(source, false, errorLine("Failed to locate the latest report export: " + exception.getMessage()));
            return 0;
        }
    }

    private static int packagesTop(CommandSourceStack source, int limit) {
        PackageEntityMonitor monitor = PackageEntityMonitor.getInstance();
        send(source, false, titleLine("PackageEntity Monitor", "Top Chunks"));
        monitor.collectTopChunks(source.getServer(), limit).forEach(entry -> send(source, false,
            Component.literal(" ")
                .withStyle(ChatFormatting.DARK_GRAY)
                .append(Component.literal(SignatureUtil.dimensionId(entry.key().dimension())).withStyle(ChatFormatting.AQUA))
                .append(Component.literal("  chunk[" + entry.key().chunkX() + ", " + entry.key().chunkZ() + "]").withStyle(ChatFormatting.GRAY))
                .append(Component.literal("  blocks x=" + entry.key().minBlockX() + ".." + entry.key().maxBlockX()
                    + " z=" + entry.key().minBlockZ() + ".." + entry.key().maxBlockZ()).withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.literal("  " + entry.count()).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))));
        return 1;
    }

    private static int packagesStalled(CommandSourceStack source, int limit) {
        PackageEntityMonitor.PackageDiagnostics diagnostics = PackageEntityMonitor.getInstance().collectDiagnostics(source.getServer(), limit);
        send(source, false, titleLine("PackageEntity Monitor", "Stalled"));
        send(source, false, statusLine("Active", Integer.toString(diagnostics.active())));
        send(source, false, statusLine("Average Age", formatDecimal(diagnostics.averageAgeSeconds()) + "s"));
        send(source, false, statusLine("Max Age", formatDecimal(diagnostics.maxAgeSeconds()) + "s"));
        send(source, false, statusLine("Stationary Candidates", Integer.toString(diagnostics.stationaryCandidates())));
        send(source, false, statusLine("Older Than 60s", Integer.toString(diagnostics.oldPackages())));

        if (diagnostics.stalledPackages().isEmpty()) {
            send(source, false, Component.literal(" No stationary package candidates found.").withStyle(ChatFormatting.GRAY));
            return 1;
        }

        diagnostics.stalledPackages().forEach(snapshot -> send(source, false,
            Component.literal(" ")
                .withStyle(ChatFormatting.DARK_GRAY)
                .append(Component.literal(SignatureUtil.dimensionId(snapshot.dimension())).withStyle(ChatFormatting.AQUA))
                .append(Component.literal("  pos["
                    + formatDecimal(snapshot.x()) + ", "
                    + formatDecimal(snapshot.y()) + ", "
                    + formatDecimal(snapshot.z()) + "]").withStyle(ChatFormatting.GRAY))
                .append(Component.literal("  chunk[" + snapshot.chunkX() + ", " + snapshot.chunkZ() + "]").withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.literal("  age=" + formatDecimal(snapshot.ageSeconds()) + "s").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
                .append(Component.literal("  speed=" + formatDecimal(snapshot.speed())).withStyle(ChatFormatting.DARK_GRAY))));
        return 1;
    }

    private static int beltsScan(CommandSourceStack source, int limit) {
        if (!CAOServerConfig.diagnosticsEnabledFast()) {
            send(source, false, errorLine(Component.translatable("command.createadvancedoptimization.diagnostics.disabled")));
            return 0;
        }

        BeltDiagnostics.Snapshot diagnostics = BeltDiagnostics.scan(source.getServer(), limit);
        send(source, false, titleLine("Mechanical Belt Diagnostics", "Loaded"));
        send(source, false, statusLine("Segments", Integer.toString(diagnostics.loadedSegments())));
        send(source, false, statusLine("Controllers", diagnostics.controllers() + " total, " + diagnostics.movingControllers() + " moving"));
        send(source, false, statusLine("Transport", diagnostics.itemCarryingControllers() + " item-carrying controllers, "
            + diagnostics.transportedStacks() + " transported stacks"));
        send(source, false, statusLine("Entity Passengers", Integer.toString(diagnostics.entityPassengers())));

        if (diagnostics.topControllers().isEmpty()) {
            send(source, false, Component.literal(" No ticking belt controllers were found in loaded chunks.").withStyle(ChatFormatting.GRAY));
            return 1;
        }

        send(source, false, Component.literal(" Top controllers (transported stacks, passengers, then length):")
            .withStyle(ChatFormatting.DARK_GRAY));
        diagnostics.topControllers().forEach(snapshot -> send(source, false,
            Component.literal(" ")
                .append(Component.literal(snapshot.dimension()).withStyle(ChatFormatting.AQUA))
                .append(Component.literal("  pos[" + snapshot.position().getX() + ", " + snapshot.position().getY() + ", "
                    + snapshot.position().getZ() + "]").withStyle(ChatFormatting.GRAY))
                .append(Component.literal("  length=" + snapshot.length()).withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.literal("  stacks=" + snapshot.transportedStacks()).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
                .append(Component.literal("  passengers=" + snapshot.entityPassengers()).withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.literal("  speed=" + formatDecimal(snapshot.speed())).withStyle(ChatFormatting.DARK_GRAY))));
        return 1;
    }

    private static int reset(CommandSourceStack source) {
        CAOServerConfig.refreshCachedState();
        CreateProfilerManager.resetAll();
        SignatureUtil.invalidateRecipeCaches();
        ProcessingMemoizationService.invalidateAll();
        OptimizationStats.reset();
        PackageEntityMonitor.getInstance().reset(source.getServer());
        send(source, true, titleLine("Create: Advanced Optimization", "Reset"));
        send(source, false, Component.literal(" Profiler sessions, cache statistics, recipe memoization caches, and package diagnostics were cleared.").withStyle(ChatFormatting.GRAY));
        return 1;
    }

    public static void exportCompletedSession(MinecraftServer server, CreateProfilerManager.ProfileSession session) {
        try {
            ExportedReport report = ReportWriter.writeReport(session, PackageEntityMonitor.getInstance(), server);
            Component component = reportExportComponent("Create Profiler", "Finished", report);
            notifyInitiator(server, session.initiatedBy(), component);
            CreateAdvancedOptimization.LOGGER.info("Create profiler finished. TXT={} HTML={} Session={}",
                report.textReport(), report.htmlReport(), session.sessionId());
        } catch (Exception exception) {
            CreateAdvancedOptimization.LOGGER.error("Failed to export completed Create profiler session {}.", session.sessionId(), exception);
            notifyInitiator(server, session.initiatedBy(), errorLine("Create profiler finished, but the report export failed: " + exception.getMessage()));
        }
    }

    private static int exportSessionToSource(CommandSourceStack source, CreateProfilerManager.ProfileSession session, String title, String badge) {
        try {
            ExportedReport report = ReportWriter.writeReport(session, PackageEntityMonitor.getInstance(), source.getServer());
            sendReportExport(source, true, title, badge, report);
            return 1;
        } catch (Exception exception) {
            CreateAdvancedOptimization.LOGGER.error("Failed to write report.", exception);
            send(source, false, errorLine("Failed to export report: " + exception.getMessage()));
            return 0;
        }
    }

    private static void sendReportExport(CommandSourceStack source, boolean broadcastToOps, String title, String badge, ExportedReport report) {
        send(source, broadcastToOps, reportExportComponent(title, badge, report));
    }

    private static MutableComponent reportExportComponent(String title, String badge, ExportedReport report) {
        String textFileName = report.textReport().getFileName().toString();
        String htmlFileName = report.htmlReport().getFileName().toString();
        String reportFolder = report.htmlReport().toAbsolutePath().getParent().toString();

        MutableComponent component = titleLine(title, badge)
            .append(Component.literal("\n"))
            .append(Component.literal(" Report exported successfully.").withStyle(ChatFormatting.GRAY))
            .append(Component.literal("\n"))
            .append(Component.literal(" ").withStyle(ChatFormatting.DARK_GRAY))
            .append(actionButton("Copy HTML Path", ChatFormatting.AQUA, new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, report.htmlReport().toAbsolutePath().toString()),
                "Copy the HTML report path to the clipboard."))
            .append(Component.literal("  "))
            .append(actionButton("Copy TXT Path", ChatFormatting.GOLD, new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, report.textReport().toAbsolutePath().toString()),
                "Copy the TXT report path to the clipboard."))
            .append(Component.literal("  "))
            .append(actionButton("Copy Folder Path", ChatFormatting.LIGHT_PURPLE, new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, reportFolder),
                "Copy the report folder path to the clipboard."))
            .append(Component.literal("\n"))
            .append(Component.literal(" ").withStyle(ChatFormatting.DARK_GRAY))
            .append(Component.literal("Files: ").withStyle(ChatFormatting.DARK_GRAY))
            .append(Component.literal(htmlFileName + " | " + textFileName).withStyle(ChatFormatting.WHITE));
        return component;
    }

    private static MutableComponent titleLine(String title, String badge) {
        return Component.literal(" ")
            .append(Component.literal(title).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
            .append(Component.literal(" "))
            .append(Component.literal("[" + badge + "]").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
    }

    private static MutableComponent statusLine(String label, String value) {
        return Component.literal(" ")
            .append(Component.literal(label + ": ").withStyle(ChatFormatting.DARK_AQUA))
            .append(Component.literal(value).withStyle(ChatFormatting.WHITE));
    }

    private static String boolLabel(boolean enabled) {
        return enabled ? "enabled" : "disabled";
    }

    private static String cacheLine(OptimizationStats.CacheSnapshot cache) {
        return cache.lookups() + " lookups, " + cache.hits() + " hits, " + cache.misses() + " misses, hit rate " + formatDecimal(cache.hitRate() * 100.0D) + "%";
    }

    private static String detailedCacheLine(OptimizationStats.CacheSnapshot cache) {
        return cacheLine(cache)
            + ", evictions " + cache.evictions()
            + ", invalidations " + cache.invalidations()
            + ", negative " + cache.negativeResults()
            + ", max size " + cache.maxSizeReached()
            + ", key ns " + cache.keyBuildNanos()
            + ", original ns " + cache.originalLookupNanos()
            + ", avoided ns~" + cache.estimatedAvoidedNanos();
    }

    private static String formatDecimal(double value) {
        return String.format(java.util.Locale.ROOT, "%.3f", value);
    }

    private static MutableComponent errorLine(String message) {
        return errorLine(Component.literal(message));
    }

    private static MutableComponent errorLine(Component message) {
        return Component.literal(" ")
            .append(Component.literal("Create: Advanced Optimization ").withStyle(ChatFormatting.RED, ChatFormatting.BOLD))
            .append(message.copy().withStyle(ChatFormatting.RED));
    }

    private static MutableComponent profilerStartError(CreateProfilerManager.StartResult result) {
        String key = switch (result) {
            case ALREADY_ACTIVE -> "command.createadvancedoptimization.profile.error.already_active";
            case DIAGNOSTICS_DISABLED -> "command.createadvancedoptimization.profile.error.diagnostics_disabled";
            case MOD_DISABLED -> "command.createadvancedoptimization.profile.error.mod_disabled";
            case INVALID_DURATION -> "command.createadvancedoptimization.profile.error.invalid_duration";
            case STARTED -> "command.createadvancedoptimization.profile.started";
        };
        return errorLine(Component.translatable(key));
    }

    private static MutableComponent actionButton(String label, ChatFormatting color, ClickEvent clickEvent, String hoverText) {
        return Component.literal("[" + label + "]")
            .withStyle(style -> style
                .withColor(color)
                .withBold(true)
                .withClickEvent(clickEvent)
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(hoverText).withStyle(ChatFormatting.GRAY))));
    }

    private static void send(CommandSourceStack source, boolean broadcastToOps, Component message) {
        source.sendSuccess(() -> message, broadcastToOps);
    }

    private static void notifyInitiator(MinecraftServer server, String initiatorName, Component component) {
        boolean delivered = false;
        if (initiatorName != null && !initiatorName.isBlank()) {
            ServerPlayer player = server.getPlayerList().getPlayerByName(initiatorName);
            if (player != null) {
                player.sendSystemMessage(component);
                delivered = true;
            }
        }

        if (!delivered) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (server.getProfilePermissions(player.getGameProfile()) >= 2) {
                    player.sendSystemMessage(component);
                    delivered = true;
                }
            }
        }

        if (!delivered) {
            CreateAdvancedOptimization.LOGGER.info(component.getString());
        }
    }
}
