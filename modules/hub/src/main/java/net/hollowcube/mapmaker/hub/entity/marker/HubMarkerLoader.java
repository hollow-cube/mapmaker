package net.hollowcube.mapmaker.hub.entity.marker;

import net.hollowcube.mapmaker.hub.merchant.MerchantEntity;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.entity.marker.MarkerLoader;
import net.minestom.server.coordinate.Pos;
import org.jetbrains.annotations.NotNull;
import org.jglrxavpok.hephaistos.nbt.NBTCompound;

public class HubMarkerLoader implements MarkerLoader {

    @Override
    public boolean loadMarker(@NotNull MapWorld world, @NotNull String type, @NotNull NBTCompound data, @NotNull Pos pos) {
        if ("mapmaker:merchant".equals(type)) {
            var entity = new MerchantEntity(data);

            var yawOverride = data.getFloat("yaw");
            if (yawOverride != null) pos = pos.withYaw(yawOverride);
            var pitchOverride = data.getFloat("pitch");
            if (pitchOverride != null) pos = pos.withPitch(pitchOverride);
            entity.setInstance(world.instance(), pos);
        }

        return false;
    }
}
