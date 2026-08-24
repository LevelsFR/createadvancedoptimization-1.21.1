package net.levelsfr.createadvancedoptimization.mixin;

import com.simibubi.create.content.kinetics.chainConveyor.ServerChainConveyorHandler;
import net.levelsfr.createadvancedoptimization.diagnostics.profiler.CreateProfilerManager;
import net.levelsfr.createadvancedoptimization.diagnostics.profiler.ProfiledSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerChainConveyorHandler.class)
public abstract class ServerChainConveyorHandlerMixin {

    @Unique
    private static long cao$startedAt;

    @Inject(method = "tick", at = @At("HEAD"))
    private static void cao$profileStart(CallbackInfo ci) {
        if (CreateProfilerManager.isProfilingEnabled()) {
            cao$startedAt = CreateProfilerManager.begin(ProfiledSection.CHAIN_CONVEYOR_TICK);
        }
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private static void cao$profileEnd(CallbackInfo ci) {
        if (cao$startedAt != 0L) {
            CreateProfilerManager.end(ProfiledSection.CHAIN_CONVEYOR_TICK, cao$startedAt);
            cao$startedAt = 0L;
        }
    }
}
