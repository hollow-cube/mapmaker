package net.hollowcube.mapmaker.map;

import net.hollowcube.mapmaker.instance.generation.MapGenerators;
import net.hollowcube.mapmaker.map.instance.MapInstance;
import net.hollowcube.mapmaker.map.item.handler.ItemRegistry;
import net.minestom.server.FeatureFlag;
import net.minestom.server.ServerFlag;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.instance.Weather;
import net.minestom.server.instance.WorldBorder;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@NotNullByDefault
public non-sealed abstract class AbstractMapWorld2 implements MapWorld2 {

    private final MapServer server;
    private final MapData map;
    private final MapInstance instance;

    private final Set<Player> players = new HashSet<>();
    private final Set<Player> playersImmutable = Collections.unmodifiableSet(players);

    private final ItemRegistry itemRegistry = new ItemRegistry();

    protected AbstractMapWorld2(MapServer server, MapData map, MapInstance instance) {
        this.server = server;
        this.map = map;
        this.instance = instance;

        configureInstance();
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
        return playersImmutable;
    }

    @Override
    public ItemRegistry itemRegistry() {
        return itemRegistry;
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

    @Override
    public void spawnPlayer(Player player) {
        assert !this.players.contains(player);
        this.players.add(player);
    }

    @Override
    public void removePlayer(Player player) {
        assert this.players.contains(player);
        this.players.remove(player);
    }

    protected void configureInstance() {
        instance().setGenerator(MapGenerators.voidWorld());

        var diameter = map().settings().getSize().size();
        instance().setWorldBorder(new WorldBorder(diameter,
                0f, 0f, 0,
                0, ServerFlag.WORLD_BORDER_SIZE
        ));

        instance().setTime(switch (map().getSetting(MapSettings.TIME_OF_DAY)) {
            case NOON -> 6000;
            case SUNRISE -> 23000;
            case SUNSET -> 13000;
            case NIGHT -> 18000;
        });

        instance().setWeather(switch (map().getSetting(MapSettings.WEATHER_TYPE)) {
            case CLEAR -> new Weather(0, 0);
            case RAINING -> new Weather(1f, 0f);
            case THUNDERSTORM -> new Weather(1f, 1f);
        }, 1);
    }


    protected static MapInstance makeMapInstance(MapData map, char classifier) {
        var lightingMode = map.getSetting(MapSettings.LIGHTING)
                ? MapInstance.LightingMode.GENERATED
                : MapInstance.LightingMode.FULL_BRIGHT;
        return new MapInstance(map.createDimensionName(classifier), lightingMode);
    }
}
