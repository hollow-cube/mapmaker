package net.hollowcube.mapmaker.hub.entity.marker;

import net.hollowcube.mapmaker.hub.merchant.MerchantEntity;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.entity.marker.MarkerLoader;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.FloatBinaryTag;
import net.minestom.server.coordinate.Pos;
import org.jetbrains.annotations.NotNull;

public class HubMarkerLoader implements MarkerLoader {

    @Override
    public boolean loadMarker(@NotNull MapWorld world, @NotNull String type, @NotNull CompoundBinaryTag data, @NotNull Pos pos) {
        if ("mapmaker:merchant".equals(type)) {
            var entity = new MerchantEntity(data);

            if (data.get("yaw") instanceof FloatBinaryTag yaw)
                pos = pos.withYaw(yaw.floatValue());
            if (data.get("pitch") instanceof FloatBinaryTag pitch)
                pos = pos.withPitch(pitch.floatValue());
            entity.setInstance(world.instance(), pos);
        }

        return false;
    }
}
