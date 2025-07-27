package net.hollowcube.mapmaker.map;

import net.hollowcube.common.util.OpUtils;
import net.hollowcube.mapmaker.instance.generation.MapGenerators;
import net.hollowcube.mapmaker.map.instance.MapInstance;
import net.hollowcube.mapmaker.map.item.handler.ItemRegistry;
import net.minestom.server.FeatureFlag;
import net.minestom.server.ServerFlag;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.event.trait.PlayerEvent;
import net.minestom.server.event.trait.PlayerInstanceEvent;
import net.minestom.server.instance.Weather;
import net.minestom.server.instance.WorldBorder;
import net.minestom.server.tag.Tag;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.*;

import java.util.*;

import static net.hollowcube.mapmaker.map.util.EventUtil.playerEventNode;

@NotNullByDefault
public non-sealed abstract class AbstractMapWorld2<S extends PlayerState<S, W>, W extends AbstractMapWorld2<S, W>> implements MapWorld2 {
    static final Tag<MapWorld2> ROOT_MAP_WORLD_TAG = Tag.Transient("root_map_world");

    private final MapServer server;
    private final MapData map;
    private final MapInstance instance;

    private final Set<Player> players = new HashSet<>();
    private final Set<Player> playersImmutable = Collections.unmodifiableSet(players);
    private final EventNode<InstanceEvent> eventNode = EventNode.event(
            UUID.randomUUID().toString(), EventFilter.INSTANCE,
            event -> !(event instanceof PlayerEvent playerEvent) || players.contains(playerEvent.getPlayer())
    );

    private final Class<S> stateClass;
    private final Set<Player>[] playersByState;
    private final EventNode<PlayerInstanceEvent>[] eventNodesByState;
    private final Map<Player, S> playerStates = new HashMap<>();
    // pendingStateChanges is a map to deduplicate single tick changes (it should be impossible to
    // change a player's state multiple times per tick, latest wins).
    private final Map<Player, S> pendingStateChanges = new HashMap<>();

    private final ItemRegistry itemRegistry = new ItemRegistry();

    protected AbstractMapWorld2(MapServer server, MapData map, MapInstance instance, Class<S> stateClass) {
        this.server = server;
        this.map = map;
        this.instance = instance;

        Check.argCondition(!stateClass.isSealed(), "stateClass must be sealed");
        this.stateClass = stateClass;
        this.playersByState = new Set[stateClass.getPermittedSubclasses().length];
        this.eventNodesByState = new EventNode[playersByState.length];
        for (int i = 0; i < this.playersByState.length; i++) {
            final var players = new HashSet<Player>();
            this.playersByState[i] = players;
            this.eventNodesByState[i] = OpUtils.build(playerEventNode(players), eventNode()::addChild);
        }


        // TODO: we shouldn't always be setting this
        instance.setTag(ROOT_MAP_WORLD_TAG, this);

        instance.eventNode().addChild(itemRegistry.eventNode());
        instance.eventNode().addChild(eventNode);

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
    public EventNode<InstanceEvent> eventNode() {
        return eventNode;
    }

    @Override
    public @UnmodifiableView Collection<Player> players() {
        return playersImmutable;
    }

    @Override
    public ItemRegistry itemRegistry() {
        return itemRegistry;
    }

    // region Player Lifecycle

    public @Nullable S getPlayerState(Player player) {
        return playerStates.get(player);
    }

    public void changePlayerState(Player player, S nextState) {
        pendingStateChanges.put(player, nextState);
    }

    @NonBlocking
    protected abstract S initialState(Player player);

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

        final var initialState = initialState(player);
        initialState.configurePlayer((W) this, player, null);
        playerStates.put(player, initialState);
    }

    @Override
    public void removePlayer(Player player) {
        assert this.players.contains(player);
        this.players.remove(player);

        final var state = playerStates.remove(player);
        playersByState[stateIndex(state)].remove(player);
        pendingStateChanges.remove(player);

        state.resetPlayer((W) this, player, null);
    }

    @Override
    @NonBlocking
    public void safePointTick() {
        MapWorld2.super.safePointTick();

        var iter = pendingStateChanges.entrySet().iterator();
        while (iter.hasNext()) {
            // todo some concept of async pending change would be nice.
            final var entry = iter.next();
            final var player = entry.getKey();
            final var lastState = playerStates.get(player);
            final var nextState = entry.getValue();

            try {
                playersByState[stateIndex(lastState)].remove(player);
                lastState.resetPlayer((W) this, player, nextState);

                // Any failure of state handling will result in the player not having a state,
                // ie this dead zone between calls.
                // todo what should we do if theres a failure here?

                nextState.configurePlayer((W) this, player, lastState);
                playerStates.put(player, nextState);
                playersByState[stateIndex(nextState)].add(player);
            } finally {
                iter.remove();
            }
        }
    }

    // endregion

    // region World Lifecycle

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

    @Blocking
    protected abstract void loadWorld();

    // endregion

    private int stateIndex(S state) {
        for (int i = 0; i < stateClass.getPermittedSubclasses().length; i++) {
            if (stateClass.getPermittedSubclasses()[i].isInstance(state)) {
                return i;
            }
        }
        throw new IllegalArgumentException("State " + state + " is not a permitted subclass of " + stateClass);
    }


    protected static MapInstance makeMapInstance(MapData map, char classifier) {
        var lightingMode = map.getSetting(MapSettings.LIGHTING)
                ? MapInstance.LightingMode.GENERATED
                : MapInstance.LightingMode.FULL_BRIGHT;
        return new MapInstance(map.createDimensionName(classifier), lightingMode);
    }
}
