package net.hollowcube.mapmaker.map.event;

import net.hollowcube.mapmaker.map.MapWorld2;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.NotNullByDefault;

@NotNullByDefault
public interface Map2Event extends InstanceEvent {
    MapWorld2 world();

    @Override
    default Instance getInstance() {
        return world().instance();
    }
}
