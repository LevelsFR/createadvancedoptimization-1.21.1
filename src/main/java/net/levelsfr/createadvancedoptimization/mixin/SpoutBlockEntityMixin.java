package net.levelsfr.createadvancedoptimization.mixin;

import com.simibubi.create.content.fluids.spout.SpoutBlockEntity;
import com.simibubi.create.content.kinetics.belt.behaviour.BeltProcessingBehaviour;
import java.util.Map;
import net.levelsfr.createadvancedoptimization.config.CAOServerConfig;
import net.levelsfr.createadvancedoptimization.diagnostics.OptimizationStats;
import net.levelsfr.createadvancedoptimization.diagnostics.profiler.CreateProfilerManager;
import net.levelsfr.createadvancedoptimization.diagnostics.profiler.ProfiledSection;
import net.levelsfr.createadvancedoptimization.optimization.spout.SpoutRecipeCacheHolder;
import net.levelsfr.createadvancedoptimization.optimization.spout.SpoutRecipeLookupKey;
import net.levelsfr.createadvancedoptimization.optimization.spout.SpoutRecipeLookupOptimizer;
import net.levelsfr.createadvancedoptimization.optimization.spout.SpoutRecipeLookupResult;
import net.levelsfr.createadvancedoptimization.util.BoundedLruMap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SpoutBlockEntity.class)
public abstract class SpoutBlockEntityMixin implements SpoutRecipeCacheHolder {

    @Unique
    private Map<SpoutRecipeLookupKey, SpoutRecipeLookupResult> cao$spoutRecipeCache;

    @Unique
    private int cao$cacheCapacity = -1;

    @Unique
    private long cao$onItemReceivedStartedAt;

    @Unique
    private long cao$whenItemHeldStartedAt;

    @Override
    public Map<SpoutRecipeLookupKey, SpoutRecipeLookupResult> cao$getSpoutRecipeCache() {
        int configuredCapacity = CAOServerConfig.SPOUT_RECIPE_CACHE_MAX_ENTRIES.get();
        if (cao$spoutRecipeCache == null || configuredCapacity != cao$cacheCapacity) {
            cao$spoutRecipeCache = new BoundedLruMap<>(configuredCapacity, ignored -> OptimizationStats.recordSpoutEviction());
            cao$cacheCapacity = configuredCapacity;
        }
        return cao$spoutRecipeCache;
    }

    @Inject(method = "onItemReceived", at = @At("HEAD"))
    private void cao$profileOnItemReceivedStart(CallbackInfoReturnable<BeltProcessingBehaviour.ProcessingResult> cir) {
        if (CreateProfilerManager.isProfilingEnabled()) {
            cao$onItemReceivedStartedAt = CreateProfilerManager.begin(ProfiledSection.SPOUT_ON_ITEM_RECEIVED);
        }
    }

    @Inject(method = "onItemReceived", at = @At("RETURN"))
    private void cao$profileOnItemReceivedEnd(CallbackInfoReturnable<BeltProcessingBehaviour.ProcessingResult> cir) {
        if (cao$onItemReceivedStartedAt != 0L) {
            CreateProfilerManager.end(ProfiledSection.SPOUT_ON_ITEM_RECEIVED, cao$onItemReceivedStartedAt);
            cao$onItemReceivedStartedAt = 0L;
        }
    }

    @Inject(method = "whenItemHeld", at = @At("HEAD"))
    private void cao$profileWhenItemHeldStart(CallbackInfoReturnable<BeltProcessingBehaviour.ProcessingResult> cir) {
        if (CreateProfilerManager.isProfilingEnabled()) {
            cao$whenItemHeldStartedAt = CreateProfilerManager.begin(ProfiledSection.SPOUT_WHEN_ITEM_HELD);
        }
    }

    @Inject(method = "whenItemHeld", at = @At("RETURN"))
    private void cao$profileWhenItemHeldEnd(CallbackInfoReturnable<BeltProcessingBehaviour.ProcessingResult> cir) {
        if (cao$whenItemHeldStartedAt != 0L) {
            CreateProfilerManager.end(ProfiledSection.SPOUT_WHEN_ITEM_HELD, cao$whenItemHeldStartedAt);
            cao$whenItemHeldStartedAt = 0L;
        }
    }

    @Redirect(method = "onItemReceived", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/fluids/spout/FillingBySpout;canItemBeFilled(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;)Z"))
    private boolean cao$redirectCanItemBeFilledOnReceive(Level level, ItemStack stack) {
        return SpoutRecipeLookupOptimizer.canItemBeFilled((SpoutBlockEntity) (Object) this, level, stack);
    }

    @Redirect(method = "onItemReceived", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/fluids/spout/FillingBySpout;getRequiredAmountForItem(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;Lnet/neoforged/neoforge/fluids/FluidStack;)I"))
    private int cao$redirectRequiredAmountOnReceive(Level level, ItemStack stack, FluidStack fluidStack) {
        return SpoutRecipeLookupOptimizer.getRequiredAmountForItem((SpoutBlockEntity) (Object) this, level, stack, fluidStack);
    }

    @Redirect(method = "whenItemHeld", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/fluids/spout/FillingBySpout;canItemBeFilled(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;)Z"))
    private boolean cao$redirectCanItemBeFilledWhenHeld(Level level, ItemStack stack) {
        return SpoutRecipeLookupOptimizer.canItemBeFilled((SpoutBlockEntity) (Object) this, level, stack);
    }

    @Redirect(method = "whenItemHeld", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/fluids/spout/FillingBySpout;getRequiredAmountForItem(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;Lnet/neoforged/neoforge/fluids/FluidStack;)I"))
    private int cao$redirectRequiredAmountWhenHeld(Level level, ItemStack stack, FluidStack fluidStack) {
        return SpoutRecipeLookupOptimizer.getRequiredAmountForItem((SpoutBlockEntity) (Object) this, level, stack, fluidStack);
    }
}
