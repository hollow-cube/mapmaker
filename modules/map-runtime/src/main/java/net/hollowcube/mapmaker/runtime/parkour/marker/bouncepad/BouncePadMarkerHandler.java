package net.hollowcube.mapmaker.runtime.parkour.marker.bouncepad;

import net.hollowcube.mapmaker.map.entity.marker.MarkerEntity;
import net.hollowcube.mapmaker.map.entity.object.ObjectEntityHandler;
import net.hollowcube.mapmaker.runtime.parkour.ParkourMapWorld;
import net.hollowcube.mapmaker.runtime.parkour.ParkourState;
import net.minestom.server.codec.Transcoder;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BouncePadMarkerHandler extends ObjectEntityHandler {
    public static final String ID = "mapmaker:bounce_pad";

    private BouncePadData data;

    public BouncePadMarkerHandler(@NotNull MarkerEntity entity) {
        super(ID, entity);
        onDataChange(null);
    }

    @Override
    public void onDataChange(@Nullable Player player) {
        var bouncePadData = entity.getData().getCompound("bounce_pad");
        this.data = BouncePadData.CODEC.decode(Transcoder.NBT, bouncePadData).orElseThrow();
        this.data.onUpdate(player);
    }

    @Override
    public void onPlayerEnter(@NotNull Player player) {
        if (this.data == null) return;

        // Check for playing so we dont trigger on spec/finished
        var world = ParkourMapWorld.forPlayer(player);
        if (world == null) return;
        if (!(world.getPlayerState(player) instanceof ParkourState.AnyPlaying))
            return;

        this.data.applyVelocity(player);
    }
}
