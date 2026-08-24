package net.levelsfr.createadvancedoptimization.mixin;

import com.simibubi.create.content.fluids.PipeConnection;
import net.levelsfr.createadvancedoptimization.diagnostics.profiler.CreateProfilerManager;
import net.levelsfr.createadvancedoptimization.diagnostics.profiler.ProfiledSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PipeConnection.class)
public abstract class PipeConnectionMixin {

    @Unique
    private long cao$manageSourceStartedAt;

    @Unique
    private long cao$manageFlowsStartedAt;

    @Inject(method = "manageSource", at = @At("HEAD"))
    private void cao$profileManageSourceStart(CallbackInfo ci) {
        if (CreateProfilerManager.isProfilingEnabled()) {
            cao$manageSourceStartedAt = CreateProfilerManager.begin(ProfiledSection.PIPE_CONNECTION_MANAGE_SOURCE);
        }
    }

    @Inject(method = "manageSource", at = @At("RETURN"))
    private void cao$profileManageSourceEnd(CallbackInfo ci) {
        if (cao$manageSourceStartedAt != 0L) {
            CreateProfilerManager.end(ProfiledSection.PIPE_CONNECTION_MANAGE_SOURCE, cao$manageSourceStartedAt);
            cao$manageSourceStartedAt = 0L;
        }
    }

    @Inject(method = "manageFlows", at = @At("HEAD"))
    private void cao$profileManageFlowsStart(CallbackInfoReturnable<Boolean> cir) {
        if (CreateProfilerManager.isProfilingEnabled()) {
            cao$manageFlowsStartedAt = CreateProfilerManager.begin(ProfiledSection.PIPE_CONNECTION_MANAGE_FLOWS);
        }
    }

    @Inject(method = "manageFlows", at = @At("RETURN"))
    private void cao$profileManageFlowsEnd(CallbackInfoReturnable<Boolean> cir) {
        if (cao$manageFlowsStartedAt != 0L) {
            CreateProfilerManager.end(ProfiledSection.PIPE_CONNECTION_MANAGE_FLOWS, cao$manageFlowsStartedAt);
            cao$manageFlowsStartedAt = 0L;
        }
    }
}
