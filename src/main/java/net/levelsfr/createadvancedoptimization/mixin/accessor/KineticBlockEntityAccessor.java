package net.levelsfr.createadvancedoptimization.mixin.accessor;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(KineticBlockEntity.class)
public interface KineticBlockEntityAccessor {

    @Accessor("speed")
    float cao$getSpeedDirect();
}
