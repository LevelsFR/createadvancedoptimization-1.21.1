package net.levelsfr.createadvancedoptimization.mixin;

import com.simibubi.create.content.kinetics.belt.transport.BeltInventory;
import net.levelsfr.createadvancedoptimization.diagnostics.profiler.CreateProfilerManager;
import net.levelsfr.createadvancedoptimization.diagnostics.profiler.ProfiledSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BeltInventory.class)
public abstract class BeltInventoryMixin {

    @Unique
    private long cao$startedAt;

    @Unique
    private boolean cao$profiledEmpty;

    @Inject(method = "tick", at = @At("HEAD"))
    private void cao$profileStart(CallbackInfo ci) {
        if (CreateProfilerManager.isProfilingEnabled()) {
            cao$profiledEmpty = ((BeltInventory) (Object) this).getTransportedItems().isEmpty();
            cao$startedAt = CreateProfilerManager.begin(cao$profiledEmpty
                ? ProfiledSection.BELT_INVENTORY_EMPTY_TICK
                : ProfiledSection.BELT_INVENTORY_ACTIVE_TICK);
        }
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void cao$profileEnd(CallbackInfo ci) {
        if (cao$startedAt != 0L) {
            CreateProfilerManager.end(cao$profiledEmpty
                ? ProfiledSection.BELT_INVENTORY_EMPTY_TICK
                : ProfiledSection.BELT_INVENTORY_ACTIVE_TICK, cao$startedAt);
            cao$startedAt = 0L;
        }
    }
}
