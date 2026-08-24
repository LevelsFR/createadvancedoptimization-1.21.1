package net.levelsfr.createadvancedoptimization.mixin;

import com.simibubi.create.content.kinetics.belt.transport.BeltFunnelInteractionHandler;
import com.simibubi.create.content.kinetics.belt.transport.BeltInventory;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.content.logistics.funnel.FunnelBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.inventory.InvManipulationBehaviour;
import com.simibubi.create.foundation.item.ItemHelper;
import net.levelsfr.createadvancedoptimization.config.CAOServerConfig;
import net.levelsfr.createadvancedoptimization.diagnostics.OptimizationStats;
import net.levelsfr.createadvancedoptimization.diagnostics.profiler.CreateProfilerManager;
import net.levelsfr.createadvancedoptimization.diagnostics.profiler.ProfiledSection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(BeltFunnelInteractionHandler.class)
public abstract class BeltFunnelInteractionHandlerMixin {

    @Unique
    private static long cao$startedAt;

    @Inject(method = "checkForFunnels", at = @At("HEAD"))
    private static void cao$profileStart(BeltInventory inventory, TransportedItemStack transported, float nextOffset, CallbackInfoReturnable<Boolean> cir) {
        if (CreateProfilerManager.isProfilingEnabled()) {
            cao$startedAt = CreateProfilerManager.begin(ProfiledSection.BELT_FUNNEL_CHECK);
        }
    }

    @Inject(method = "checkForFunnels", at = @At("RETURN"))
    private static void cao$profileEnd(BeltInventory inventory, TransportedItemStack transported, float nextOffset, CallbackInfoReturnable<Boolean> cir) {
        if (cao$startedAt != 0L) {
            CreateProfilerManager.end(ProfiledSection.BELT_FUNNEL_CHECK, cao$startedAt);
            cao$startedAt = 0L;
        }
    }

    @Inject(
        method = "checkForFunnels",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;copy()Lnet/minecraft/world/item/ItemStack;", ordinal = 0),
        cancellable = true,
        locals = LocalCapture.CAPTURE_FAILHARD
    )
    private static void cao$skipOversizedBlockingInsertCopy(
        BeltInventory inventory,
        TransportedItemStack transported,
        float nextOffset,
        CallbackInfoReturnable<Boolean> cir,
        boolean beltMovementPositive,
        int currentSegment,
        int step,
        int segment,
        BlockPos funnelPos,
        Level level,
        BlockState funnelState,
        Direction funnelFacing,
        Direction movementFacing,
        boolean blocking,
        float funnelEntry,
        boolean crossed,
        BlockEntity blockEntity,
        FunnelBlockEntity funnel,
        InvManipulationBehaviour invManipulation,
        FilteringBehaviour filtering,
        int amount,
        ItemHelper.ExtractionCountMode mode
    ) {
        if (!CAOServerConfig.GENERAL_ENABLED.get() || !CAOServerConfig.BELT_FUNNEL_FAST_REJECT_ENABLED.get()) {
            return;
        }

        ItemStack carried = transported.stack;
        if (blocking && amount > carried.getCount() && mode != ItemHelper.ExtractionCountMode.UPTO) {
            OptimizationStats.recordBeltFunnelFastReject();
            cir.setReturnValue(true);
        }
    }
}
