package net.hollowcube.map.feature.checkpoint;

import net.hollowcube.map.block.handler.AbstractPlateHandler;
import net.hollowcube.map.block.handler.PointOfInterestHandlerMixin;
import net.hollowcube.map.event.MapWorldCompleteEvent;
import net.hollowcube.map.world.MapWorld;
import net.hollowcube.mapmaker.map.MapVariant;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.utils.NamespaceID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FinishPlateBlock extends AbstractPlateHandler implements PointOfInterestHandlerMixin {

    @Override
    public @NotNull NamespaceID getNamespaceId() {
        return NamespaceID.from("mapmaker:finish_plate");
    }

    @Override
    public void onPlatePressed(@NotNull Tick tick, @NotNull Player player) {
        EventDispatcher.call(new MapWorldCompleteEvent(MapWorld.forPlayer(player), player));
    }

    @Override
    public @NotNull String poiType() {
        return "mapmaker:finish_plate";
    }

    @Override
    public @Nullable MapVariant requiredVariant() {
        return MapVariant.PARKOUR;
    }

}
