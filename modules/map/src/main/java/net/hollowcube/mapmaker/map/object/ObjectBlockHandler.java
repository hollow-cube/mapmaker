package net.hollowcube.mapmaker.map.object;

import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.world.EditingMapWorld;
import net.hollowcube.mapmaker.object.ObjectData;
import net.hollowcube.mapmaker.object.ObjectType;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockHandler;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public interface ObjectBlockHandler extends BlockHandler {

    static @NotNull String createObjectId(@NotNull ObjectType objectType, @NotNull Point blockPosition) {
        return String.format("%s/%d_%d_%d", objectType.id(), blockPosition.blockX(), blockPosition.blockY(), blockPosition.blockZ());
    }

    @NotNull
    ObjectType objectType();

    default @NotNull String createObjectId(@NotNull Point blockPosition) {
        return createObjectId(objectType(), blockPosition);
    }

    default @NotNull ObjectData createObjectData(@NotNull Point blockPosition) {
        return new ObjectData(createObjectId(blockPosition), objectType(), blockPosition);
    }

    @Override
    default @NotNull Key getKey() {
        return objectType().key();
    }

    @Override
    default void onPlace(@NotNull BlockHandler.Placement placement) {
        var world = MapWorld.unsafeFromInstance(placement.getInstance());
        if (!(world instanceof EditingMapWorld)) return;

        var map = world.map();
        boolean added = map.addObject(createObjectData(placement.getBlockPosition()));
        if (!added) {
            world.instance().setBlock(placement.getBlockPosition(), Block.AIR);
            if (placement instanceof PlayerPlacement pp) {
                var chunk = world.instance().getChunkAt(pp.getBlockPosition());
                pp.getPlayer().sendChunk(Objects.requireNonNull(chunk));
                pp.getPlayer().sendMessage(Component.translatable("map.ram.usage_exceeded"));
            }
            return;
        }
    }

    @Override
    default void onDestroy(@NotNull Destroy destroy) {
        var world = MapWorld.unsafeFromInstance(destroy.getInstance());
        if (!(world instanceof EditingMapWorld)) return;

        var map = world.map();
        map.removeObject(createObjectData(destroy.getBlockPosition()).id());
    }
}
