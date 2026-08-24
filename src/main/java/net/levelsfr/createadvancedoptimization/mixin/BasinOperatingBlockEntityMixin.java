package net.levelsfr.createadvancedoptimization.mixin;

import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.basin.BasinOperatingBlockEntity;
import java.util.List;
import java.util.Optional;
import net.levelsfr.createadvancedoptimization.optimization.processing.ProcessingMemoizationService;
import net.levelsfr.createadvancedoptimization.optimization.processing.ProcessingMemoizationService.BasinMemoizationKey;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BasinOperatingBlockEntity.class)
public abstract class BasinOperatingBlockEntityMixin {

    @Unique
    private BasinMemoizationKey cao$basinMemoizationKey;

    @Unique
    private Level cao$basinMemoizationLevel;

    @Shadow
    protected abstract Optional<BasinBlockEntity> getBasin();

    @Shadow
    protected abstract Object getRecipeCacheKey();

    @Inject(method = "getMatchingRecipes", at = @At("HEAD"), cancellable = true)
    private void cao$useMemoizedRecipes(CallbackInfoReturnable<List<Recipe<?>>> cir) {
        Optional<BasinBlockEntity> basin = getBasin();
        if (basin.isEmpty()) {
            cao$basinMemoizationKey = null;
            cao$basinMemoizationLevel = null;
            return;
        }

        BlockEntity operator = (BlockEntity) (Object) this;
        cao$basinMemoizationKey = ProcessingMemoizationService.buildBasinKey(operator, basin.get(), getRecipeCacheKey());
        cao$basinMemoizationLevel = operator.getLevel();
        List<Recipe<?>> cached = ProcessingMemoizationService.getCachedBasinRecipes(cao$basinMemoizationLevel, cao$basinMemoizationKey);
        if (cached != null) {
            cir.setReturnValue(cached);
            cao$clearBasinMemoizationState();
        }
    }

    @Inject(method = "getMatchingRecipes", at = @At("RETURN"))
    private void cao$storeMemoizedRecipes(CallbackInfoReturnable<List<Recipe<?>>> cir) {
        try {
            ProcessingMemoizationService.storeBasinRecipes(cao$basinMemoizationLevel, cao$basinMemoizationKey, cir.getReturnValue());
        } finally {
            cao$clearBasinMemoizationState();
        }
    }

    @Unique
    private void cao$clearBasinMemoizationState() {
        cao$basinMemoizationKey = null;
        cao$basinMemoizationLevel = null;
    }
}
