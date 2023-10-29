package net.hollowcube.map.object;

import net.hollowcube.map.world.EditingMapWorld;
import net.hollowcube.map.world.MapWorld;
import net.hollowcube.mapmaker.object.ObjectData;
import net.hollowcube.mapmaker.object.ObjectType;
import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.block.BlockHandler;
import net.minestom.server.utils.NamespaceID;
import org.jetbrains.annotations.NotNull;

public interface ObjectBlockHandler extends BlockHandler {

    @NotNull
    ObjectType objectType();

    default @NotNull String createObjectId(@NotNull Point blockPosition) {
        return String.format("%s/%d_%d_%d", objectType().id(), blockPosition.blockX(), blockPosition.blockY(), blockPosition.blockZ());
    }

    default @NotNull ObjectData createObjectData(@NotNull Point blockPosition) {
        return new ObjectData(createObjectId(blockPosition), objectType(), blockPosition);
    }

    @Override
    default @NotNull NamespaceID getNamespaceId() {
        return objectType().namespaceId();
    }

    @Override
    default void onDestroy(@NotNull Destroy destroy) {
        var world = MapWorld.unsafeFromInstance(destroy.getInstance());
        if (!(world instanceof EditingMapWorld)) return;

        var map = world.map();
        map.removeObject(createObjectData(destroy.getBlockPosition()).id());
    }
}
