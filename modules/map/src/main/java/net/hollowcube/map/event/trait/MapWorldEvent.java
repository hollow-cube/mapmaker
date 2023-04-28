package net.hollowcube.map.event.trait;

import net.hollowcube.map.world.MapWorldNew;
import net.hollowcube.mapmaker.model.MapData;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.NotNull;

public interface MapWorldEvent extends InstanceEvent {

    @NotNull MapWorldNew getMapWorld();

    default @NotNull MapData getMap() {
        return getMapWorld().map();
    }

    @Override
    default @NotNull Instance getInstance() {
        return getMapWorld().instance();
    }
}
