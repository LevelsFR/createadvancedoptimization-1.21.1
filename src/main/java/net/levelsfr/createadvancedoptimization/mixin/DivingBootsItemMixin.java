package net.levelsfr.createadvancedoptimization.mixin;

import com.simibubi.create.content.equipment.armor.DivingBootsItem;
import net.levelsfr.createadvancedoptimization.config.CAOServerConfig;
import net.levelsfr.createadvancedoptimization.diagnostics.OptimizationStats;
import net.levelsfr.createadvancedoptimization.diagnostics.profiler.CreateProfilerManager;
import net.levelsfr.createadvancedoptimization.diagnostics.profiler.ProfiledSection;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DivingBootsItem.class)
public abstract class DivingBootsItemMixin {

    @Unique
    private static long cao$startedAt;

    @Inject(method = "affects", at = @At("HEAD"))
    private static void cao$profileStart(LivingEntity entity, CallbackInfoReturnable<Boolean> cir) {
        if (CreateProfilerManager.isProfilingEnabled()) {
            cao$startedAt = CreateProfilerManager.begin(ProfiledSection.DIVING_BOOTS);
        }
    }

    @Inject(method = "affects", at = @At("RETURN"))
    private static void cao$profileEnd(LivingEntity entity, CallbackInfoReturnable<Boolean> cir) {
        if (cao$startedAt != 0L) {
            CreateProfilerManager.end(ProfiledSection.DIVING_BOOTS, cao$startedAt);
            cao$startedAt = 0L;
        }
    }

    @Redirect(method = "affects", at = @At(value = "INVOKE", target = "Lnet/minecraft/nbt/CompoundTag;remove(Ljava/lang/String;)V"))
    private static void cao$skipRedundantHeavyBootsRemoval(CompoundTag tag, String key) {
        if (!CAOServerConfig.GENERAL_ENABLED.get() || !CAOServerConfig.DIVING_BOOTS_ENABLED.get() || tag.contains(key)) {
            tag.remove(key);
            return;
        }

        OptimizationStats.recordDivingBootsMarkerRemovalSkipped();
    }

    @Redirect(method = "affects", at = @At(value = "INVOKE", target = "Lnet/createmod/catnip/nbt/NBTHelper;putMarker(Lnet/minecraft/nbt/CompoundTag;Ljava/lang/String;)V"))
    private static void cao$skipRedundantHeavyBootsMarker(CompoundTag tag, String key) {
        if (!CAOServerConfig.GENERAL_ENABLED.get() || !CAOServerConfig.DIVING_BOOTS_ENABLED.get() || !cao$isTrueBooleanTag(tag, key)) {
            tag.putBoolean(key, true);
            return;
        }

        OptimizationStats.recordDivingBootsMarkerWriteSkipped();
    }

    @Unique
    private static boolean cao$isTrueBooleanTag(CompoundTag tag, String key) {
        return tag.contains(key, Tag.TAG_BYTE) && tag.getBoolean(key);
    }
}
