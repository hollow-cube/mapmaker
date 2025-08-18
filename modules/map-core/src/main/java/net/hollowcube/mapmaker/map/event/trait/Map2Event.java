package net.hollowcube.mapmaker.map.event.trait;

import net.hollowcube.mapmaker.map.MapWorld;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.NotNullByDefault;

@NotNullByDefault
public interface Map2Event extends InstanceEvent {
    MapWorld world();

    @Override
    default Instance getInstance() {
        return world().instance();
    }
}
