package net.levelsfr.createadvancedoptimization.mixin.accessor;

import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.content.kinetics.belt.transport.BeltInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(BeltBlockEntity.class)
public interface BeltBlockEntityAccessor {

    @Accessor("inventory")
    BeltInventory cao$getInventoryDirect();

    @Invoker("isController")
    boolean cao$isController();
}
