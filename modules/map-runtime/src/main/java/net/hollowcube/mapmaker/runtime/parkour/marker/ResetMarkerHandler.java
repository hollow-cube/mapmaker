package net.hollowcube.mapmaker.runtime.parkour.marker;

import net.hollowcube.mapmaker.map.entity.object.ObjectEntity;
import net.hollowcube.mapmaker.map.entity.object.ObjectEntityHandler;
import net.hollowcube.mapmaker.runtime.parkour.ParkourMapWorld;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Nullable;

public class ResetMarkerHandler extends ObjectEntityHandler {
    public static final String ID = "mapmaker:reset";

    private boolean fullReset = false;

    public ResetMarkerHandler(ObjectEntity entity) {
        super(ID, entity);
        onDataChange(null);
    }

    @Override
    public void onDataChange(@Nullable Player player) {
        this.fullReset = entity.getData().getBoolean("full_reset", false);
    }

    @Override
    public void onPlayerEnter(Player player) {
        this.onPlayerInteract(player);
    }

    @Override
    public void onPlayerInteract(Player player) {
        var world = ParkourMapWorld.forPlayer(player);
        if (world == null) return;

        if (this.fullReset) world.hardResetPlayer(player);
        else world.softResetPlayer(player);
    }
}
