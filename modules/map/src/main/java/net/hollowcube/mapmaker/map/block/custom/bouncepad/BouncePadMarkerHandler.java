package net.hollowcube.mapmaker.map.block.custom.bouncepad;

import net.hollowcube.common.util.dfu.NbtOps;
import net.hollowcube.mapmaker.map.entity.marker.MarkerEntity;
import net.hollowcube.mapmaker.map.entity.marker.MarkerHandler;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BouncePadMarkerHandler extends MarkerHandler {

    public static final String ID = "mapmaker:bounce_pad";

    private BouncePadData data;

    public BouncePadMarkerHandler(@NotNull MarkerEntity entity) {
        super(ID, entity);
        onDataChange(null);
    }

    @Override
    protected void onDataChange(@Nullable Player player) {
        var bouncePadData = entity.getMarkerData().getCompound("bounce_pad");
        this.data = BouncePadData.CODEC.parse(NbtOps.INSTANCE, bouncePadData).getOrThrow();
        this.data.onUpdate(player);
    }

    @Override
    protected void onPlayerEnter(@NotNull Player player) {
        if (this.data == null) return;
        var newVelocity = this.data.getVelocity(player);
        if (newVelocity == null) return;
        player.setVelocity(newVelocity);
    }
}
