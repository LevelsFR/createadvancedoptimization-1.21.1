package net.levelsfr.createadvancedoptimization.mixin.accessor;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Entity.class)
public interface EntityPersistentDataAccessor {

    @Accessor("persistentData")
    CompoundTag cao$getPersistentData();
}
