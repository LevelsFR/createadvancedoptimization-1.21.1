package net.levelsfr.createadvancedoptimization.mixin;

import com.simibubi.create.content.kinetics.crafter.RecipeGridHandler;
import net.levelsfr.createadvancedoptimization.optimization.processing.ProcessingMemoizationService;
import net.levelsfr.createadvancedoptimization.optimization.processing.ProcessingMemoizationService.CachedCrafterResult;
import net.levelsfr.createadvancedoptimization.optimization.processing.ProcessingMemoizationService.CrafterMemoizationKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RecipeGridHandler.class)
public abstract class RecipeGridHandlerMixin {

    @Unique
    private static final ThreadLocal<CrafterMemoizationKey> cao$crafterMemoizationKey = new ThreadLocal<>();

    @Inject(method = "tryToApplyRecipe", at = @At("HEAD"), cancellable = true)
    private static void cao$useMemoizedResult(Level level, RecipeGridHandler.GroupedItems groupedItems, CallbackInfoReturnable<ItemStack> cir) {
        CrafterMemoizationKey key = ProcessingMemoizationService.buildCrafterKey(level, groupedItems);
        cao$crafterMemoizationKey.set(key);
        CachedCrafterResult cached = ProcessingMemoizationService.getCachedCrafterResult(level, key);
        if (cached.cached()) {
            cir.setReturnValue(cached.result());
            cao$clearCrafterMemoizationState();
        }
    }

    @Inject(method = "tryToApplyRecipe", at = @At("RETURN"))
    private static void cao$storeMemoizedResult(Level level, RecipeGridHandler.GroupedItems groupedItems, CallbackInfoReturnable<ItemStack> cir) {
        try {
            ProcessingMemoizationService.storeCrafterResult(level, cao$crafterMemoizationKey.get(), cir.getReturnValue());
        } finally {
            cao$clearCrafterMemoizationState();
        }
    }

    @Unique
    private static void cao$clearCrafterMemoizationState() {
        cao$crafterMemoizationKey.remove();
    }
}
