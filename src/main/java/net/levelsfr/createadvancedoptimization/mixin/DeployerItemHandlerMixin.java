package net.levelsfr.createadvancedoptimization.mixin;

import com.simibubi.create.content.kinetics.deployer.DeployerItemHandler;
import net.levelsfr.createadvancedoptimization.config.CAOServerConfig;
import net.levelsfr.createadvancedoptimization.diagnostics.OptimizationStats;
import net.levelsfr.createadvancedoptimization.diagnostics.profiler.CreateProfilerManager;
import net.levelsfr.createadvancedoptimization.diagnostics.profiler.ProfiledSection;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(DeployerItemHandler.class)
public abstract class DeployerItemHandlerMixin {

    @Unique
    private long cao$startedAt;

    @Inject(method = "insertItem", at = @At("HEAD"))
    private void cao$profileStart(int slot, ItemStack stack, boolean simulate, CallbackInfoReturnable<ItemStack> cir) {
        if (CreateProfilerManager.isProfilingEnabled()) {
            cao$startedAt = CreateProfilerManager.begin(ProfiledSection.DEPLOYER_INSERT);
        }
    }

    @Inject(method = "insertItem", at = @At("RETURN"))
    private void cao$profileEnd(int slot, ItemStack stack, boolean simulate, CallbackInfoReturnable<ItemStack> cir) {
        if (cao$startedAt != 0L) {
            CreateProfilerManager.end(ProfiledSection.DEPLOYER_INSERT, cao$startedAt);
            cao$startedAt = 0L;
        }
    }

    @Inject(
        method = "insertItem",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;copy()Lnet/minecraft/world/item/ItemStack;", ordinal = 0),
        cancellable = true,
        locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void cao$skipFullHandCopy(int slot, ItemStack stack, boolean simulate, CallbackInfoReturnable<ItemStack> cir, ItemStack held, int remainingCapacity) {
        if (!CAOServerConfig.GENERAL_ENABLED.get() || !CAOServerConfig.DEPLOYER_INSERT_FAST_REJECT_ENABLED.get()) {
            return;
        }

        if (remainingCapacity <= 0) {
            OptimizationStats.recordDeployerFullHandFastReject();
            cir.setReturnValue(stack);
        }
    }
}
