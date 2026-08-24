package net.levelsfr.createadvancedoptimization.mixin;

import com.simibubi.create.compat.trainmap.TrainMapSync;
import net.levelsfr.createadvancedoptimization.diagnostics.profiler.CreateProfilerManager;
import net.levelsfr.createadvancedoptimization.diagnostics.profiler.ProfiledSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TrainMapSync.class)
public abstract class TrainMapSyncMixin {

    @Unique
    private static long cao$startedAt;

    @Inject(method = "serverTick", at = @At("HEAD"))
    private static void cao$profileStart(CallbackInfo ci) {
        if (CreateProfilerManager.isProfilingEnabled()) {
            cao$startedAt = CreateProfilerManager.begin(ProfiledSection.TRAIN_MAP_SYNC_TICK);
        }
    }

    @Inject(method = "serverTick", at = @At("RETURN"))
    private static void cao$profileEnd(CallbackInfo ci) {
        if (cao$startedAt != 0L) {
            CreateProfilerManager.end(ProfiledSection.TRAIN_MAP_SYNC_TICK, cao$startedAt);
            cao$startedAt = 0L;
        }
    }
}
