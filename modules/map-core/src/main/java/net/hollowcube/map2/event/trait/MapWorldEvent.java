package net.hollowcube.map2.event.trait;

import net.hollowcube.map2.MapWorld;
import net.hollowcube.mapmaker.map.MapData;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.NotNull;

public interface MapWorldEvent extends InstanceEvent {

    @NotNull
    MapWorld getMapWorld();

    default @NotNull MapData getMap() {
        return getMapWorld().map();
    }

    @Override
    default @NotNull Instance getInstance() {
        return getMapWorld().instance();
    }
}
