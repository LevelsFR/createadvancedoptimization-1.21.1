package net.levelsfr.createadvancedoptimization.mixin;

import com.simibubi.create.content.logistics.box.PackageEntity;
import net.levelsfr.createadvancedoptimization.diagnostics.profiler.CreateProfilerManager;
import net.levelsfr.createadvancedoptimization.diagnostics.profiler.ProfiledSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PackageEntity.class)
public abstract class PackageEntityMixin {

    @Unique
    private long cao$startedAt;

    @Inject(method = "tick", at = @At("HEAD"))
    private void cao$profileStart(CallbackInfo ci) {
        if (CreateProfilerManager.isProfilingEnabled()) {
            cao$startedAt = CreateProfilerManager.begin(ProfiledSection.PACKAGE_ENTITY_TICK);
        }
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void cao$profileEnd(CallbackInfo ci) {
        if (cao$startedAt != 0L) {
            CreateProfilerManager.end(ProfiledSection.PACKAGE_ENTITY_TICK, cao$startedAt);
            cao$startedAt = 0L;
        }
    }
}
