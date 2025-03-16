package net.hollowcube.mapmaker.map.block.custom;

import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.entity.marker.MarkerEntity;
import net.hollowcube.mapmaker.map.entity.marker.MarkerHandler;
import net.hollowcube.mapmaker.map.event.vnext.MapPlayerResetEvent;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ResetMarkerHandler extends MarkerHandler {

    public static final String ID = "mapmaker:reset";

    private boolean fullReset = false;

    public ResetMarkerHandler(@NotNull MarkerEntity entity) {
        super(ID, entity);
        onDataChange(null);
    }

    @Override
    protected void onDataChange(@Nullable Player player) {
        this.fullReset = entity.getMarkerData().getBoolean("full_reset", false);
    }

    @Override
    protected void onPlayerEnter(@NotNull Player player) {
        var world = MapWorld.forPlayerOptional(player);
        if (world == null) return;
        EventDispatcher.call(new MapPlayerResetEvent(player, world, !this.fullReset));
    }
}
