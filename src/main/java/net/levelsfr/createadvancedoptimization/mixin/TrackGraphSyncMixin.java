package net.levelsfr.createadvancedoptimization.mixin;

import com.simibubi.create.content.trains.graph.TrackGraphSync;
import net.levelsfr.createadvancedoptimization.diagnostics.profiler.CreateProfilerManager;
import net.levelsfr.createadvancedoptimization.diagnostics.profiler.ProfiledSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TrackGraphSync.class)
public abstract class TrackGraphSyncMixin {

    @Unique
    private long cao$startedAt;

    @Inject(method = "serverTick", at = @At("HEAD"))
    private void cao$profileStart(CallbackInfo ci) {
        if (CreateProfilerManager.isProfilingEnabled()) {
            cao$startedAt = CreateProfilerManager.begin(ProfiledSection.TRACK_GRAPH_SYNC_TICK);
        }
    }

    @Inject(method = "serverTick", at = @At("RETURN"))
    private void cao$profileEnd(CallbackInfo ci) {
        if (cao$startedAt != 0L) {
            CreateProfilerManager.end(ProfiledSection.TRACK_GRAPH_SYNC_TICK, cao$startedAt);
            cao$startedAt = 0L;
        }
    }
}
