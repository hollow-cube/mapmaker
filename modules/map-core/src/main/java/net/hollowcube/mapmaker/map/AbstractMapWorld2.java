package net.hollowcube.mapmaker.map;

import net.hollowcube.mapmaker.map.instance.MapInstance;
import net.minestom.server.FeatureFlag;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.Collection;
import java.util.List;

@NotNullByDefault
public non-sealed abstract class AbstractMapWorld2 implements MapWorld2 {

    private final MapServer server;
    private final MapData map;
    private final MapInstance instance;

    protected AbstractMapWorld2(MapServer server, MapData map, MapInstance instance) {
        this.server = server;
        this.map = map;
        this.instance = instance;
    }

    @Override
    public MapServer server() {
        return server;
    }

    @Override
    public MapData map() {
        return map;
    }

    @Override
    public MapInstance instance() {
        return instance;
    }

    @Override
    public @UnmodifiableView Collection<Player> players() {
        return List.of();
    }

    @Override
    public void configurePlayer(AsyncPlayerConfigurationEvent event) {
        // TODO(new worlds): Per world registry support.
        // final var player = event.getPlayer();

        // Always add all feature flags to the world. We don't restrict what blocks/items people use.
        FeatureFlag.values().forEach(event::addFeatureFlag);

        event.setSpawningInstance(instance());
        // !! Implementations must set player spawn position.
    }

    
    protected static MapInstance makeMapInstance(MapData map, char classifier) {
        var lightingMode = map.getSetting(MapSettings.LIGHTING)
                ? MapInstance.LightingMode.GENERATED
                : MapInstance.LightingMode.FULL_BRIGHT;
        return new MapInstance(map.createDimensionName(classifier), lightingMode);
    }
}
