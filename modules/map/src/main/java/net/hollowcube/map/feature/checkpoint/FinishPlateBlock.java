package net.hollowcube.map.feature.checkpoint;

import net.hollowcube.map.block.handler.AbstractPlateHandler;
import net.hollowcube.map.event.MapWorldCompleteEvent;
import net.hollowcube.map.world.MapWorldNew;
import net.hollowcube.mapmaker.model.MapData;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.utils.NamespaceID;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class FinishPlateBlock extends AbstractPlateHandler {

    public static final NamespaceID ID = NamespaceID.from("mapmaker:finish_plate");
    public static final String POI_TYPE = "mapmaker:finish_plate";

    @Override
    public @NotNull NamespaceID getNamespaceId() {
        return ID;
    }

    @Override
    public void onPlatePressed(@NotNull Tick tick, @NotNull Player player) {
        var instance = tick.getInstance();
        EventDispatcher.call(new MapWorldCompleteEvent(MapWorldNew.forPlayer(player), player));
    }

    @Override
    public void onPlace(@NotNull Placement placement) {
        MapData map;
        if (placement instanceof PlayerPlacement pp) {
            map = MapWorldNew.forPlayer(pp.getPlayer()).map();
        } else {
            // OK to choose the first editing world, the block is only placed in editing world.
            var world = MapWorldNew.unsafeFromInstance(placement.getInstance());
            if (world == null || (world.flags() & MapWorldNew.FLAG_EDITING) == 0) return;
            map = world.map();
        }
        map.addPOI(new MapData.POI(POI_TYPE, UUID.randomUUID().toString(), placement.getBlockPosition()));
    }

    @Override
    public void onDestroy(@NotNull Destroy destroy) {
        MapData map;
        if (destroy instanceof PlayerDestroy pd) {
            map = MapWorldNew.forPlayer(pd.getPlayer()).map();
        } else {
            // OK to choose the first editing world, the block is only placed in editing world.
            var world = MapWorldNew.unsafeFromInstance(destroy.getInstance());
            if (world == null || (world.flags() & MapWorldNew.FLAG_EDITING) == 0) return;
            map = world.map();
        }
        map.removePOI(destroy.getBlockPosition());
    }
}
