package net.levelsfr.createadvancedoptimization.util;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import net.levelsfr.createadvancedoptimization.CreateAdvancedOptimization;
import net.levelsfr.createadvancedoptimization.diagnostics.OptimizationStats;
import net.levelsfr.createadvancedoptimization.config.CAOServerConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import org.apache.commons.lang3.tuple.Pair;

public final class SignatureUtil {

    private static final AtomicInteger RECIPE_RELOAD_EPOCH = new AtomicInteger();

    private SignatureUtil() {
    }

    public static int recipeEpoch(Level level) {
        return 31 * RECIPE_RELOAD_EPOCH.get() + System.identityHashCode(level.getRecipeManager());
    }

    public static void invalidateRecipeCaches() {
        int epoch = RECIPE_RELOAD_EPOCH.incrementAndGet();
        OptimizationStats.recordSpoutInvalidation();
        OptimizationStats.recordBasinInvalidation();
        OptimizationStats.recordCrafterInvalidation();
        if (CAOServerConfig.DEBUG_LOGGING.get()) {
            CreateAdvancedOptimization.LOGGER.info("Invalidated Create recipe caches at epoch {}.", epoch);
        }
    }

    public static String itemSignature(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()) + "|" + stack.getCount() + "|" + ItemStack.hashItemAndComponents(stack);
    }

    public static String fluidSignature(FluidStack stack) {
        return BuiltInRegistries.FLUID.getKey(stack.getFluid()) + "|" + stack.getAmount() + "|" + FluidStack.hashFluidAndComponents(stack);
    }

    public static String itemHandlerSignature(IItemHandler itemHandler) {
        StringBuilder builder = new StringBuilder();
        for (int slot = 0; slot < itemHandler.getSlots(); slot++) {
            builder.append(slot).append('=').append(itemSignature(itemHandler.getStackInSlot(slot))).append(';');
        }
        return builder.toString();
    }

    public static String fluidHandlerSignature(IFluidHandler fluidHandler) {
        StringBuilder builder = new StringBuilder();
        for (int tank = 0; tank < fluidHandler.getTanks(); tank++) {
            builder.append(tank).append('=').append(fluidSignature(fluidHandler.getFluidInTank(tank))).append(';');
        }
        return builder.toString();
    }

    public static String groupedItemsSignature(Map<Pair<Integer, Integer>, ItemStack> grid) {
        StringBuilder builder = new StringBuilder();
        grid.entrySet().stream()
            .sorted(Map.Entry.comparingByKey((left, right) -> {
                int compareX = Integer.compare(left.getLeft(), right.getLeft());
                return compareX != 0 ? compareX : Integer.compare(left.getRight(), right.getRight());
            }))
            .forEach(entry -> builder.append(entry.getKey().getLeft())
                .append(',')
                .append(entry.getKey().getRight())
                .append('=')
                .append(itemSignature(entry.getValue()))
                .append(';'));
        return builder.toString();
    }

    public static String dimensionId(ResourceKey<Level> dimension) {
        return dimension.location().toString();
    }
}
