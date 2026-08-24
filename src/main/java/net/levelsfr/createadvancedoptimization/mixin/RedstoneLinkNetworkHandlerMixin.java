package net.levelsfr.createadvancedoptimization.mixin;

import com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler;
import net.levelsfr.createadvancedoptimization.diagnostics.profiler.CreateProfilerManager;
import net.levelsfr.createadvancedoptimization.diagnostics.profiler.ProfiledSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RedstoneLinkNetworkHandler.class)
public abstract class RedstoneLinkNetworkHandlerMixin {

    @Unique
    private long cao$startedAt;

    @Inject(method = "updateNetworkOf", at = @At("HEAD"))
    private void cao$profileStart(CallbackInfo ci) {
        if (CreateProfilerManager.isProfilingEnabled()) {
            cao$startedAt = CreateProfilerManager.begin(ProfiledSection.REDSTONE_LINK_UPDATE);
        }
    }

    @Inject(method = "updateNetworkOf", at = @At("RETURN"))
    private void cao$profileEnd(CallbackInfo ci) {
        if (cao$startedAt != 0L) {
            CreateProfilerManager.end(ProfiledSection.REDSTONE_LINK_UPDATE, cao$startedAt);
            cao$startedAt = 0L;
        }
    }
}
