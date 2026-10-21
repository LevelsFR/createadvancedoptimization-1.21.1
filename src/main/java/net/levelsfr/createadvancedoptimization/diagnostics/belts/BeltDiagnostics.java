package net.levelsfr.createadvancedoptimization.diagnostics.belts;

import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.content.kinetics.belt.transport.BeltInventory;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.levelsfr.createadvancedoptimization.mixin.accessor.BeltBlockEntityAccessor;
import net.levelsfr.createadvancedoptimization.mixin.accessor.ChunkMapAccessor;
import net.levelsfr.createadvancedoptimization.mixin.accessor.KineticBlockEntityAccessor;
import net.levelsfr.createadvancedoptimization.util.SignatureUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

public final class BeltDiagnostics {

    private BeltDiagnostics() {
    }

    public static Snapshot scan(MinecraftServer server, int limit) {
        int loadedSegments = 0;
        int controllers = 0;
        int movingControllers = 0;
        int itemCarryingControllers = 0;
        int transportedStacks = 0;
        int entityPassengers = 0;
        List<ControllerSnapshot> controllerSnapshots = new ArrayList<>();

        for (ServerLevel level : server.getAllLevels()) {
            Iterable<ChunkHolder> holders = ((ChunkMapAccessor) (Object) level.getChunkSource().chunkMap).cao$getVisibleChunks();
            for (ChunkHolder holder : holders) {
                LevelChunk chunk = holder.getTickingChunk();
                if (chunk == null) {
                    continue;
                }

                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    if (!(blockEntity instanceof BeltBlockEntity belt)) {
                        continue;
                    }

                    loadedSegments++;
                    BeltBlockEntityAccessor beltAccessor = (BeltBlockEntityAccessor) (Object) belt;
                    if (!beltAccessor.cao$isController()) {
                        continue;
                    }

                    controllers++;
                    float speed = ((KineticBlockEntityAccessor) (Object) belt).cao$getSpeedDirect();
                    boolean moving = speed != 0.0F;
                    if (moving) {
                        movingControllers++;
                    }

                    BeltInventory inventory = beltAccessor.cao$getInventoryDirect();
                    int stacks = inventory == null ? 0 : inventory.getTransportedItems().size();
                    int passengers = belt.passengers == null ? 0 : belt.passengers.size();
                    if (stacks > 0) {
                        itemCarryingControllers++;
                    }
                    transportedStacks += stacks;
                    entityPassengers += passengers;

                    controllerSnapshots.add(new ControllerSnapshot(
                        SignatureUtil.dimensionId(level.dimension()),
                        blockEntity.getBlockPos().immutable(),
                        belt.beltLength,
                        speed,
                        stacks,
                        passengers
                    ));
                }
            }
        }

        controllerSnapshots.sort(Comparator
            .comparingInt(ControllerSnapshot::transportedStacks).reversed()
            .thenComparing(Comparator.comparingInt(ControllerSnapshot::entityPassengers).reversed())
            .thenComparing(Comparator.comparingInt(ControllerSnapshot::length).reversed()));
        List<ControllerSnapshot> topControllers = List.copyOf(controllerSnapshots.subList(0, Math.min(limit, controllerSnapshots.size())));

        return new Snapshot(
            loadedSegments,
            controllers,
            movingControllers,
            itemCarryingControllers,
            transportedStacks,
            entityPassengers,
            topControllers
        );
    }

    public record Snapshot(
        int loadedSegments,
        int controllers,
        int movingControllers,
        int itemCarryingControllers,
        int transportedStacks,
        int entityPassengers,
        List<ControllerSnapshot> topControllers
    ) {
    }

    public record ControllerSnapshot(
        String dimension,
        BlockPos position,
        int length,
        float speed,
        int transportedStacks,
        int entityPassengers
    ) {
    }
}
