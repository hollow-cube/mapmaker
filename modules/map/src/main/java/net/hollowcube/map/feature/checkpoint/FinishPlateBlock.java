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
        EventDispatcher.call(new MapWorldCompleteEvent(MapWorldNew.fromInstance(instance), player));
    }

    @Override
    public void onPlace(@NotNull Placement placement) {
        var map = MapWorldNew.fromInstance(placement.getInstance()).map();
        map.addPOI(new MapData.POI(POI_TYPE, UUID.randomUUID().toString(), placement.getBlockPosition()));
    }

    @Override
    public void onDestroy(@NotNull Destroy destroy) {
        var map = MapWorldNew.fromInstance(destroy.getInstance()).map();
        map.removePOI(destroy.getBlockPosition());
    }
}
