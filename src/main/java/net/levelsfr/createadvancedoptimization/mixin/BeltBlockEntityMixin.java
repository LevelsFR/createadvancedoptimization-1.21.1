package net.levelsfr.createadvancedoptimization.mixin;

import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import java.util.Map;
import net.levelsfr.createadvancedoptimization.config.CAOServerConfig;
import net.levelsfr.createadvancedoptimization.diagnostics.profiler.CreateProfilerManager;
import net.levelsfr.createadvancedoptimization.diagnostics.profiler.ProfiledSection;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BeltBlockEntity.class)
public abstract class BeltBlockEntityMixin {

    @Shadow
    public Map<Entity, ?> passengers;

    @Shadow
    public abstract boolean isController();

    @Unique
    private long cao$startedAt;

    @Unique
    private boolean cao$profiledController;

    @Inject(method = "tick", at = @At("HEAD"))
    private void cao$profileStart(CallbackInfo ci) {
        if (CreateProfilerManager.isProfilingEnabled()) {
            cao$profiledController = isController();
            cao$startedAt = CreateProfilerManager.begin(cao$profiledController
                ? ProfiledSection.BELT_CONTROLLER_TICK
                : ProfiledSection.BELT_SEGMENT_TICK);
        }
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void cao$profileEnd(CallbackInfo ci) {
        cao$finishProfile();
    }

    @Unique
    private void cao$finishProfile() {
        if (cao$startedAt != 0L) {
            CreateProfilerManager.end(cao$profiledController
                ? ProfiledSection.BELT_CONTROLLER_TICK
                : ProfiledSection.BELT_SEGMENT_TICK, cao$startedAt);
            cao$startedAt = 0L;
        }
    }

    // The block entity cache avoids an equivalent world lookup per segment tick.
    @Redirect(
        method = "tick",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;")
    )
    private BlockState cao$reuseBlockEntityState(Level level, BlockPos pos) {
        if (!CAOServerConfig.beltTickFastPathsEnabled()) {
            return level.getBlockState(pos);
        }
        return ((BlockEntity) (Object) this).getBlockState();
    }

    // Item transport has already run here; an empty passenger map has no remaining work.
    @Inject(
        method = "tick",
        at = @At(value = "NEW", target = "java/util/ArrayList", ordinal = 0),
        cancellable = true
    )
    private void cao$skipEmptyPassengerCleanup(CallbackInfo ci) {
        if (CAOServerConfig.beltTickFastPathsEnabled() && passengers != null && passengers.isEmpty()) {
            // A cancellable mid-method return does not pass through the original RETURN injection.
            cao$finishProfile();
            ci.cancel();
        }
    }
}
