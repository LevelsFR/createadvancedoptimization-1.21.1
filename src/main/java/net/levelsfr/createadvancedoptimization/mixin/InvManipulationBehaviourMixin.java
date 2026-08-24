package net.levelsfr.createadvancedoptimization.mixin;

import com.simibubi.create.foundation.blockEntity.behaviour.inventory.InvManipulationBehaviour;
import net.levelsfr.createadvancedoptimization.diagnostics.profiler.CreateProfilerManager;
import net.levelsfr.createadvancedoptimization.diagnostics.profiler.ProfiledSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(InvManipulationBehaviour.class)
public abstract class InvManipulationBehaviourMixin {

    @Unique
    private long cao$startedAt;

    @Inject(
        method = "extract(Lcom/simibubi/create/foundation/item/ItemHelper$ExtractionCountMode;ILjava/util/function/Predicate;)Lnet/minecraft/world/item/ItemStack;",
        at = @At("HEAD")
    )
    private void cao$profileStart(CallbackInfoReturnable<?> cir) {
        if (CreateProfilerManager.isProfilingEnabled()) {
            cao$startedAt = CreateProfilerManager.begin(ProfiledSection.INV_MANIPULATION_EXTRACT);
        }
    }

    @Inject(
        method = "extract(Lcom/simibubi/create/foundation/item/ItemHelper$ExtractionCountMode;ILjava/util/function/Predicate;)Lnet/minecraft/world/item/ItemStack;",
        at = @At("RETURN")
    )
    private void cao$profileEnd(CallbackInfoReturnable<?> cir) {
        if (cao$startedAt != 0L) {
            CreateProfilerManager.end(ProfiledSection.INV_MANIPULATION_EXTRACT, cao$startedAt);
            cao$startedAt = 0L;
        }
    }
}
