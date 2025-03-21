package net.hollowcube.mapmaker.map.block.custom;

import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.entity.object.ObjectEntity;
import net.hollowcube.mapmaker.map.entity.object.ObjectEntityHandler;
import net.hollowcube.mapmaker.map.event.vnext.MapPlayerResetEvent;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ResetMarkerHandler extends ObjectEntityHandler {

    public static final String ID = "mapmaker:reset";

    private boolean fullReset = false;

    public ResetMarkerHandler(@NotNull ObjectEntity entity) {
        super(ID, entity);
        onDataChange(null);
    }

    @Override
    public void onDataChange(@Nullable Player player) {
        this.fullReset = entity.getData().getBoolean("full_reset", false);
    }

    @Override
    public void onPlayerEnter(@NotNull Player player) {
        this.onPlayerInteract(player);
    }

    @Override
    public void onPlayerInteract(@NotNull Player player) {
        var world = MapWorld.forPlayerOptional(player);
        if (world == null) return;
        EventDispatcher.call(new MapPlayerResetEvent(player, world, !this.fullReset));
    }
}
